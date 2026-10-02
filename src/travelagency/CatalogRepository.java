package travelagency;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import travelagency.Models.BusRoute;
import travelagency.Models.Hotel;
import travelagency.Models.TravelPackage;

/**
 * Travel packages, bus routes, hotels and meal plans: everything customers can book.
 * Admins add, change and remove them from the admin portal.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class CatalogRepository {

    private final Database db;

    public CatalogRepository(Database db) {
        this.db = db;
    }

    // ===================== Travel packages =====================

    private static TravelPackage mapPackage(ResultSet rs) throws SQLException {
        return new TravelPackage(rs.getInt("package_id"), rs.getString("name"), rs.getString("package_type"),
                rs.getString("destination"), rs.getString("description"), rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class), rs.getDouble("price_per_person"), rs.getInt("total_seats"),
                rs.getBoolean("flight_included"), rs.getBoolean("hotel_included"), rs.getBoolean("transport_included"),
                rs.getString("meal_plan"), rs.getBoolean("is_active"), rs.getInt("seats_left"));
    }

    /** Packages customers can still book: active, not started yet, with free seats. */
    public List<TravelPackage> bookablePackages() throws SQLException {
        return db.query("SELECT tp.*, fn_package_seats_left(tp.package_id) AS seats_left FROM travel_packages tp "
                + "WHERE tp.is_active AND tp.start_date > CURDATE() ORDER BY tp.start_date", CatalogRepository::mapPackage);
    }

    public List<TravelPackage> allPackages() throws SQLException {
        return db.query("SELECT tp.*, fn_package_seats_left(tp.package_id) AS seats_left FROM travel_packages tp "
                + "ORDER BY tp.is_active DESC, tp.start_date", CatalogRepository::mapPackage);
    }

    public void savePackage(Integer id, String name, String type, String destination, String description,
                            LocalDate start, LocalDate end, double price, int seats, boolean flight, boolean hotel,
                            boolean transport, String meal, boolean active, int adminId) throws SQLException {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (id == null) {
            if (!start.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("A new package must start in the future.");
            }
            db.insert("INSERT INTO travel_packages (name, package_type, destination, description, start_date, end_date, "
                            + "price_per_person, total_seats, flight_included, hotel_included, transport_included, meal_plan, "
                            + "is_active, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    name, type, destination, description, start, end, price, seats, flight, hotel, transport, meal,
                    active, adminId);
        } else {
            int booked = bookedPackageSeats(id);
            TravelPackage old = db.queryOne("SELECT tp.*, 0 AS seats_left FROM travel_packages tp WHERE package_id = ?",
                    CatalogRepository::mapPackage, id);
            if (booked > 0 && old != null && (!old.startDate().equals(start) || !old.endDate().equals(end))) {
                throw new IllegalArgumentException("Customers have already booked this package, so its dates cannot "
                        + "be changed. Create a new package for other dates.");
            }
            if (seats < booked) {
                throw new IllegalArgumentException(booked + " seats are already booked, so total seats cannot be less than "
                        + booked + ".");
            }
            db.update("UPDATE travel_packages SET name = ?, package_type = ?, destination = ?, description = ?, "
                            + "start_date = ?, end_date = ?, price_per_person = ?, total_seats = ?, flight_included = ?, "
                            + "hotel_included = ?, transport_included = ?, meal_plan = ?, is_active = ? WHERE package_id = ?",
                    name, type, destination, description, start, end, price, seats, flight, hotel, transport, meal,
                    active, id);
        }
    }

    public int bookedPackageSeats(int packageId) throws SQLException {
        Integer n = db.queryOne("SELECT COALESCE(SUM(pb.travellers), 0) FROM package_bookings pb "
                + "JOIN bookings b ON b.booking_id = pb.booking_id WHERE pb.package_id = ? AND b.status <> 'Cancelled'",
                rs -> rs.getInt(1), packageId);
        return n == null ? 0 : n;
    }

    public void deletePackage(int id) throws SQLException {
        db.update("DELETE FROM travel_packages WHERE package_id = ?", id);
    }

    // ===================== Bus routes =====================

    private static BusRoute mapRoute(ResultSet rs) throws SQLException {
        return new BusRoute(rs.getInt("route_id"), rs.getString("departure_city"), rs.getString("destination_city"),
                rs.getString("bus_company"), rs.getString("departure_times"), rs.getInt("economy_seats"),
                rs.getInt("business_seats"), rs.getDouble("economy_fare"), rs.getDouble("business_fare"),
                rs.getBoolean("is_active"));
    }

    public List<BusRoute> activeRoutes() throws SQLException {
        return db.query("SELECT * FROM bus_routes WHERE is_active ORDER BY departure_city, destination_city",
                CatalogRepository::mapRoute);
    }

    public List<BusRoute> allRoutes() throws SQLException {
        return db.query("SELECT * FROM bus_routes ORDER BY departure_city, destination_city", CatalogRepository::mapRoute);
    }

    public void saveRoute(Integer id, String from, String to, String company, String times, int economySeats,
                          int businessSeats, double economyFare, double businessFare, boolean active) throws SQLException {
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("Departure and destination cities must be different.");
        }
        if (economySeats + businessSeats == 0) {
            throw new IllegalArgumentException("The bus needs at least one seat.");
        }
        if (id == null) {
            db.insert("INSERT INTO bus_routes (departure_city, destination_city, bus_company, departure_times, "
                            + "economy_seats, business_seats, economy_fare, business_fare, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    from, to, company, times, economySeats, businessSeats, economyFare, businessFare, active);
        } else {
            checkRouteChange(id, from, to, times, economySeats, businessSeats);
            db.update("UPDATE bus_routes SET departure_city = ?, destination_city = ?, bus_company = ?, departure_times = ?, "
                            + "economy_seats = ?, business_seats = ?, economy_fare = ?, business_fare = ?, is_active = ? "
                            + "WHERE route_id = ?",
                    from, to, company, times, economySeats, businessSeats, economyFare, businessFare, active, id);
        }
    }

    /** Stops an edit that would leave upcoming bookings without a seat or departure time. */
    private void checkRouteChange(int id, String from, String to, String times, int economySeats, int businessSeats)
            throws SQLException {
        BusRoute old = db.queryOne("SELECT * FROM bus_routes WHERE route_id = ?", CatalogRepository::mapRoute, id);
        List<Object[]> upcoming = db.query("SELECT b.travel_date, bb.departure_time, bb.seat_class, SUM(bb.seats) "
                + "FROM bus_bookings bb JOIN bookings b ON b.booking_id = bb.booking_id "
                + "WHERE bb.route_id = ? AND b.status <> 'Cancelled' AND b.travel_date >= CURDATE() "
                + "GROUP BY b.travel_date, bb.departure_time, bb.seat_class",
                rs -> new Object[]{rs.getObject(1, LocalDate.class), rs.getString(2), rs.getString(3), rs.getInt(4)}, id);
        if (upcoming.isEmpty() || old == null) {
            return;
        }
        if (!old.from().equals(from) || !old.to().equals(to)) {
            throw new IllegalArgumentException("This route has upcoming bookings, so its cities cannot be changed.");
        }
        List<String> newTimes = List.of(times.split(";"));
        for (Object[] row : upcoming) {
            String time = (String) row[1];
            int booked = (Integer) row[3];
            int seats = "Business".equals(row[2]) ? businessSeats : economySeats;
            if (!newTimes.contains(time)) {
                throw new IllegalArgumentException("The " + time + " bus on " + row[0] + " has bookings, so this "
                        + "departure time cannot be removed.");
            }
            if (booked > seats) {
                throw new IllegalArgumentException(booked + " " + row[2] + " seats are booked on " + row[0] + " at "
                        + time + ", so there must be at least " + booked + " " + row[2] + " seats.");
            }
        }
    }

    public void deleteRoute(int id) throws SQLException {
        db.update("DELETE FROM bus_routes WHERE route_id = ?", id);
    }

    // ===================== Hotels and meal plans =====================

    private static Hotel mapHotel(ResultSet rs) throws SQLException {
        return new Hotel(rs.getInt("hotel_id"), rs.getString("name"), rs.getString("city"), rs.getString("address"),
                rs.getInt("star_rating"), rs.getInt("single_rooms"), rs.getInt("double_rooms"), rs.getInt("suite_rooms"),
                rs.getDouble("single_rate"), rs.getDouble("double_rate"), rs.getDouble("suite_rate"),
                rs.getBoolean("is_active"));
    }

    public List<Hotel> activeHotels() throws SQLException {
        return db.query("SELECT * FROM hotels WHERE is_active ORDER BY city, name", CatalogRepository::mapHotel);
    }

    public List<Hotel> allHotels() throws SQLException {
        return db.query("SELECT * FROM hotels ORDER BY city, name", CatalogRepository::mapHotel);
    }

    public void saveHotel(Integer id, String name, String city, String address, int stars, int singleRooms,
                          int doubleRooms, int suiteRooms, double singleRate, double doubleRate, double suiteRate,
                          boolean active) throws SQLException {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Star rating must be from 1 to 5.");
        }
        if (id == null) {
            db.insert("INSERT INTO hotels (name, city, address, star_rating, single_rooms, double_rooms, suite_rooms, "
                            + "single_rate, double_rate, suite_rate, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    name, city, address, stars, singleRooms, doubleRooms, suiteRooms, singleRate, doubleRate, suiteRate,
                    active);
        } else {
            checkHotelRooms(id, "Single", singleRooms);
            checkHotelRooms(id, "Double", doubleRooms);
            checkHotelRooms(id, "Suite", suiteRooms);
            db.update("UPDATE hotels SET name = ?, city = ?, address = ?, star_rating = ?, single_rooms = ?, "
                            + "double_rooms = ?, suite_rooms = ?, single_rate = ?, double_rate = ?, suite_rate = ?, is_active = ? "
                            + "WHERE hotel_id = ?",
                    name, city, address, stars, singleRooms, doubleRooms, suiteRooms, singleRate, doubleRate, suiteRate,
                    active, id);
        }
    }

    /**
     * The most rooms of one type that are occupied on the same night by upcoming bookings.
     * The busiest night always starts on some booking's check-in date, so only those dates are checked.
     */
    public int peakRoomsBooked(int hotelId, String roomType) throws SQLException {
        Integer peak = db.queryOne("SELECT COALESCE(MAX(occupied), 0) FROM ("
                + " SELECT d.check_in AS night, SUM(o.rooms) AS occupied"
                + " FROM hotel_bookings d JOIN bookings db ON db.booking_id = d.booking_id"
                + " JOIN hotel_bookings o ON o.hotel_id = d.hotel_id AND o.room_type = d.room_type"
                + "   AND o.check_in <= d.check_in AND o.check_out > d.check_in"
                + " JOIN bookings ob ON ob.booking_id = o.booking_id"
                + " WHERE d.hotel_id = ? AND d.room_type = ? AND db.status <> 'Cancelled' AND ob.status <> 'Cancelled'"
                + "   AND d.check_out > CURDATE()"
                + " GROUP BY d.check_in) AS nights", rs -> rs.getInt(1), hotelId, roomType);
        return peak == null ? 0 : peak;
    }

    private void checkHotelRooms(int hotelId, String type, int rooms) throws SQLException {
        int peak = peakRoomsBooked(hotelId, type);
        if (rooms < peak) {
            throw new IllegalArgumentException(peak + " " + type + " rooms are already booked for the same night, "
                    + "so there must be at least " + peak + " " + type + " rooms.");
        }
    }

    public void deleteHotel(int id) throws SQLException {
        db.update("DELETE FROM hotels WHERE hotel_id = ?", id);
    }

    /** Meal plan name to price per room per night, in a fixed order. */
    public Map<String, Double> mealPlans() throws SQLException {
        Map<String, Double> plans = new LinkedHashMap<>();
        for (Object[] row : db.query("SELECT plan, price_per_night FROM meal_plans ORDER BY price_per_night",
                rs -> new Object[]{rs.getString(1), rs.getDouble(2)})) {
            plans.put((String) row[0], (Double) row[1]);
        }
        return plans;
    }
}
