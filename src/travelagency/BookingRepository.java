package travelagency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import travelagency.Models.BookingView;
import travelagency.Models.BusRoute;
import travelagency.Models.Feedback;
import travelagency.Models.Hotel;
import travelagency.Models.TravelPackage;

/**
 * Bookings, payments and feedback.
 *
 * Every booking is saved inside one transaction: the catalogue row is locked
 * (SELECT ... FOR UPDATE), availability is checked again, and then the booking,
 * its detail row and its payment are inserted together. So two people can never
 * take the last seat or room at the same time, and a half-saved booking is impossible.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class BookingRepository {

    public static final String[] PAYMENT_METHODS = {"Cash", "Card", "Bank Transfer", "JazzCash", "EasyPaisa"};

    private final Database db;

    public BookingRepository(Database db) {
        this.db = db;
    }

    // ===================== Availability and prices =====================

    public int packageSeatsLeft(int packageId) throws SQLException {
        Integer n = db.queryOne("SELECT fn_package_seats_left(?)", rs -> rs.getInt(1), packageId);
        return n == null ? 0 : n;
    }

    public int busSeatsLeft(BusRoute route, LocalDate date, String time, String seatClass) throws SQLException {
        try (Connection c = db.connect()) {
            return busSeatsLeft(c, route, date, time, seatClass);
        }
    }

    private int busSeatsLeft(Connection c, BusRoute route, LocalDate date, String time, String seatClass)
            throws SQLException {
        int booked = intQuery(c, "SELECT COALESCE(SUM(bb.seats), 0) FROM bus_bookings bb "
                        + "JOIN bookings b ON b.booking_id = bb.booking_id WHERE bb.route_id = ? AND b.travel_date = ? "
                        + "AND bb.departure_time = ? AND bb.seat_class = ? AND b.status <> 'Cancelled'",
                route.id(), date, time, seatClass);
        return route.seats(seatClass) - booked;
    }

    public int hotelRoomsLeft(Hotel hotel, String roomType, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        try (Connection c = db.connect()) {
            return hotelRoomsLeft(c, hotel, roomType, checkIn, checkOut);
        }
    }

    /**
     * Free rooms for the whole stay: the busiest night decides. The busiest night is either the
     * first night of the stay or the check-in night of another booking that overlaps the stay.
     */
    private int hotelRoomsLeft(Connection c, Hotel hotel, String roomType, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        int peak = intQuery(c, "SELECT COALESCE(MAX(occupied), 0) FROM ("
                        + " SELECT n.night, (SELECT COALESCE(SUM(o.rooms), 0) FROM hotel_bookings o"
                        + "   JOIN bookings ob ON ob.booking_id = o.booking_id"
                        + "   WHERE o.hotel_id = ? AND o.room_type = ? AND ob.status <> 'Cancelled'"
                        + "   AND o.check_in <= n.night AND o.check_out > n.night) AS occupied"
                        + " FROM (SELECT ? AS night UNION SELECT hb.check_in FROM hotel_bookings hb"
                        + "   WHERE hb.hotel_id = ? AND hb.room_type = ? AND hb.check_in > ? AND hb.check_in < ?) AS n"
                        + ") AS nights",
                hotel.id(), roomType, checkIn, hotel.id(), roomType, checkIn, checkOut);
        return hotel.rooms(roomType) - peak;
    }

    public static double hotelPrice(Hotel hotel, String roomType, int rooms, long nights, double mealPerNight) {
        return (hotel.rate(roomType) + mealPerNight) * rooms * nights;
    }

    // ===================== Creating bookings =====================

    public int bookPackage(int customerId, int packageId, int travellers, String method) throws SQLException {
        if (travellers < 1 || travellers > 20) {
            throw new IllegalArgumentException("Travellers must be between 1 and 20.");
        }
        return inTransaction(c -> {
            TravelPackage p = lockPackage(c, packageId);
            if (p == null || !p.active()) {
                throw new IllegalArgumentException("This package is no longer available.");
            }
            if (!p.startDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("This package has already started, so it cannot be booked.");
            }
            int left = intQuery(c, "SELECT fn_package_seats_left(?)", packageId);
            if (travellers > left) {
                throw new IllegalArgumentException(left == 0 ? "Sorry, this package is fully booked."
                        : "Only " + left + " seat(s) are left on this package.");
            }
            double total = p.pricePerPerson() * travellers;
            int id = newBooking(c, customerId, "Package", p.startDate(), total);
            Database.insert(c, "INSERT INTO package_bookings (booking_id, package_id, travellers) VALUES (?, ?, ?)",
                    id, packageId, travellers);
            pay(c, id, total, method);
            return id;
        });
    }

    public int bookBus(int customerId, int routeId, LocalDate date, String time, String seatClass, int seats,
                       String method) throws SQLException {
        if (seats < 1 || seats > 10) {
            throw new IllegalArgumentException("You can book 1 to 10 seats at a time.");
        }
        if (date.isBefore(LocalDate.now())
                || date.equals(LocalDate.now()) && !LocalTime.parse(time).isAfter(LocalTime.now())) {
            throw new IllegalArgumentException("This bus has already left. Please choose a later date or time.");
        }
        return inTransaction(c -> {
            BusRoute route = lockRoute(c, routeId);
            if (route == null || !route.active() || !route.times().contains(time)) {
                throw new IllegalArgumentException("This bus is no longer available.");
            }
            int left = busSeatsLeft(c, route, date, time, seatClass);
            if (seats > left) {
                throw new IllegalArgumentException(left <= 0 ? "Sorry, all " + seatClass + " seats on this bus are booked."
                        : "Only " + left + " " + seatClass + " seat(s) are left on this bus.");
            }
            double total = route.fare(seatClass) * seats;
            int id = newBooking(c, customerId, "Bus", date, total);
            Database.insert(c, "INSERT INTO bus_bookings (booking_id, route_id, departure_time, seat_class, seats) "
                    + "VALUES (?, ?, ?, ?, ?)", id, routeId, time, seatClass, seats);
            pay(c, id, total, method);
            return id;
        });
    }

    public int bookHotel(int customerId, int hotelId, String roomType, int rooms, int guests, LocalDate checkIn,
                         LocalDate checkOut, String mealPlan, String requests, String method) throws SQLException {
        if (rooms < 1 || rooms > 10) {
            throw new IllegalArgumentException("You can book 1 to 10 rooms at a time.");
        }
        int maxGuests = rooms * ("Single".equals(roomType) ? 1 : "Double".equals(roomType) ? 2 : 4);
        if (guests < 1 || guests > maxGuests) {
            throw new IllegalArgumentException(rooms + " " + roomType + " room(s) can hold at most " + maxGuests + " guest(s).");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in cannot be in the past.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1 || nights > 30) {
            throw new IllegalArgumentException("Check-out must be 1 to 30 nights after check-in.");
        }
        String notes = Validate.text(requests == null ? "" : requests, "Special requests", 0, 255);
        return inTransaction(c -> {
            Hotel hotel = lockHotel(c, hotelId);
            if (hotel == null || !hotel.active()) {
                throw new IllegalArgumentException("This hotel is no longer available.");
            }
            int left = hotelRoomsLeft(c, hotel, roomType, checkIn, checkOut);
            if (rooms > left) {
                throw new IllegalArgumentException(left <= 0 ? "Sorry, no " + roomType + " rooms are free for these dates."
                        : "Only " + left + " " + roomType + " room(s) are free for these dates.");
            }
            double meal = doubleQuery(c, "SELECT price_per_night FROM meal_plans WHERE plan = ?", mealPlan);
            double total = hotelPrice(hotel, roomType, rooms, nights, meal);
            int id = newBooking(c, customerId, "Hotel", checkIn, total);
            Database.insert(c, "INSERT INTO hotel_bookings (booking_id, hotel_id, room_type, rooms, guests, check_in, "
                            + "check_out, meal_plan, special_requests) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id, hotelId, roomType, rooms, guests, checkIn, checkOut, mealPlan, notes.isEmpty() ? null : notes);
            pay(c, id, total, method);
            return id;
        });
    }

    private static int newBooking(Connection c, int customerId, String type, LocalDate travelDate, double total)
            throws SQLException {
        return Database.insert(c, "INSERT INTO bookings (customer_id, booking_type, travel_date, total_amount) "
                + "VALUES (?, ?, ?, ?)", customerId, type, travelDate, total);
    }

    private static void pay(Connection c, int bookingId, double amount, String method) throws SQLException {
        if (!List.of(PAYMENT_METHODS).contains(method)) {
            throw new IllegalArgumentException("Please choose a payment method.");
        }
        Database.insert(c, "INSERT INTO payments (booking_id, amount, method) VALUES (?, ?, ?)", bookingId, amount, method);
    }

    // ===================== Reading bookings =====================

    private static final String OVERVIEW = "SELECT v.*, f.rating FROM v_booking_overview v "
            + "LEFT JOIN feedback f ON f.booking_id = v.booking_id ";

    private static BookingView mapBooking(ResultSet rs) throws SQLException {
        Integer rating = rs.getObject("rating") == null ? null : rs.getInt("rating");
        return new BookingView(rs.getInt("booking_id"), rs.getInt("customer_id"), rs.getString("customer_name"),
                rs.getString("booking_type"), rs.getString("details"), rs.getObject("travel_date", LocalDate.class),
                rs.getDouble("total_amount"), rs.getString("status"), rs.getString("payment_method"),
                rs.getString("payment_status"), rs.getObject("booked_at", LocalDateTime.class),
                rating);
    }

    public List<BookingView> customerBookings(int customerId) throws SQLException {
        return db.query(OVERVIEW + "WHERE v.customer_id = ? ORDER BY v.booked_at DESC", BookingRepository::mapBooking,
                customerId);
    }

    /** All bookings for the admin, with optional filters (null or "All" means no filter). */
    public List<BookingView> allBookings(String type, String status, String search) throws SQLException {
        StringBuilder sql = new StringBuilder(OVERVIEW + "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (type != null && !type.startsWith("All")) {
            sql.append("AND v.booking_type = ? ");
            params.add(type);
        }
        if (status != null && !status.startsWith("All")) {
            sql.append("AND v.status = ? ");
            params.add(status);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (v.customer_name LIKE ? OR v.details LIKE ? OR v.booking_id = ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(search.trim().replaceAll("\\D", "").isEmpty() ? -1 : Integer.parseInt(search.trim().replaceAll("\\D", "")));
        }
        sql.append("ORDER BY FIELD(v.status, 'Pending', 'Approved', 'Cancelled'), v.booked_at DESC");
        return db.query(sql.toString(), BookingRepository::mapBooking, params.toArray());
    }

    // ===================== Status changes =====================

    public void approve(int bookingId, int adminId) throws SQLException {
        int changed = db.update("UPDATE bookings SET status = 'Approved', handled_by = ? "
                + "WHERE booking_id = ? AND status = 'Pending' AND travel_date >= CURDATE()", adminId, bookingId);
        if (changed == 0) {
            throw new IllegalArgumentException("Only pending bookings for upcoming dates can be approved.");
        }
    }

    /** Admin cancel. The trigger trg_refund_on_cancel marks the payment as refunded. */
    public void cancelByAdmin(int bookingId, int adminId) throws SQLException {
        int changed = db.update("UPDATE bookings SET status = 'Cancelled', handled_by = ? "
                + "WHERE booking_id = ? AND status <> 'Cancelled' AND travel_date >= CURDATE()", adminId, bookingId);
        if (changed == 0) {
            throw new IllegalArgumentException("This booking is already cancelled or its travel date has passed.");
        }
    }

    /** Customers can cancel their own bookings until the day before travel. */
    public void cancelByCustomer(int bookingId, int customerId) throws SQLException {
        int changed = db.update("UPDATE bookings SET status = 'Cancelled' WHERE booking_id = ? AND customer_id = ? "
                + "AND status <> 'Cancelled' AND travel_date > CURDATE()", bookingId, customerId);
        if (changed == 0) {
            throw new IllegalArgumentException("This booking can no longer be cancelled (it is already cancelled "
                    + "or travel is today or earlier).");
        }
    }

    // ===================== Feedback =====================

    public void addFeedback(int bookingId, int customerId, int rating, String comments) throws SQLException {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Please choose a rating from 1 to 5 stars.");
        }
        String status = db.queryOne("SELECT status FROM bookings WHERE booking_id = ? AND customer_id = ?",
                rs -> rs.getString(1), bookingId, customerId);
        if (!"Approved".equals(status)) {
            throw new IllegalArgumentException("You can give feedback only on approved bookings.");
        }
        if (db.queryOne("SELECT 1 FROM feedback WHERE booking_id = ?", rs -> 1, bookingId) != null) {
            throw new IllegalArgumentException("You have already given feedback for this booking.");
        }
        db.insert("INSERT INTO feedback (booking_id, customer_id, rating, comments) VALUES (?, ?, ?, ?)",
                bookingId, customerId, rating, Validate.text(comments, "Comments", 0, 500));
    }

    public List<Feedback> allFeedback() throws SQLException {
        return db.query("SELECT f.feedback_id, f.booking_id, v.customer_name, v.details, f.rating, f.comments, f.created_at "
                        + "FROM feedback f JOIN v_booking_overview v ON v.booking_id = f.booking_id ORDER BY f.created_at DESC",
                rs -> new Feedback(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                        rs.getString(6), rs.getObject(7, LocalDateTime.class)));
    }

    // ===================== Helpers =====================

    private interface TransactionWork {
        int run(Connection c) throws SQLException;
    }

    private int inTransaction(TransactionWork work) throws SQLException {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                int result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static TravelPackage lockPackage(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT *, 0 AS seats_left FROM travel_packages WHERE package_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new TravelPackage(rs.getInt("package_id"), rs.getString("name"),
                        rs.getString("package_type"), rs.getString("destination"), rs.getString("description"),
                        rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                        rs.getDouble("price_per_person"), rs.getInt("total_seats"), rs.getBoolean("flight_included"),
                        rs.getBoolean("hotel_included"), rs.getBoolean("transport_included"), rs.getString("meal_plan"),
                        rs.getBoolean("is_active"), 0) : null;
            }
        }
    }

    private static BusRoute lockRoute(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM bus_routes WHERE route_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new BusRoute(rs.getInt("route_id"), rs.getString("departure_city"),
                        rs.getString("destination_city"), rs.getString("bus_company"), rs.getString("departure_times"),
                        rs.getInt("economy_seats"), rs.getInt("business_seats"), rs.getDouble("economy_fare"),
                        rs.getDouble("business_fare"), rs.getBoolean("is_active")) : null;
            }
        }
    }

    private static Hotel lockHotel(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM hotels WHERE hotel_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Hotel(rs.getInt("hotel_id"), rs.getString("name"), rs.getString("city"),
                        rs.getString("address"), rs.getInt("star_rating"), rs.getInt("single_rooms"),
                        rs.getInt("double_rooms"), rs.getInt("suite_rooms"), rs.getDouble("single_rate"),
                        rs.getDouble("double_rate"), rs.getDouble("suite_rate"), rs.getBoolean("is_active")) : null;
            }
        }
    }

    private static int intQuery(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            Database.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static double doubleQuery(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            Database.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Please choose a valid meal plan.");
                }
                return rs.getDouble(1);
            }
        }
    }
}
