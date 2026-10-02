package travelagency;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import travelagency.Models.Hotel;

/**
 * "Hotels": choose a city, hotel, room type, dates and meal plan; free rooms and
 * the price are worked out live from the database.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class HotelPage extends JPanel implements PortalFrame.Refreshable {

    private final CustomerPortal portal;
    private final JComboBox<String> city = Ui.combo();
    private final JComboBox<Hotel> hotel = Ui.combo();
    private final JComboBox<String> roomType = Ui.combo("Single", "Double", "Suite");
    private final JSpinner rooms = Ui.number(1, 1, 10);
    private final JSpinner guests = Ui.number(1, 1, 40);
    private final JSpinner checkIn = Ui.datePicker(LocalDate.now().plusDays(1));
    private final JSpinner checkOut = Ui.datePicker(LocalDate.now().plusDays(3));
    private final JComboBox<String> meal = Ui.combo();
    private final JTextField requests = Ui.field();

    private final JLabel hotelName = Ui.label(" ", Ui.TITLE, Ui.INK);
    private final JLabel hotelAddress = Ui.label(" ", Ui.BODY, Ui.MUTED);
    private final JLabel rate = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel mealCost = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel nights = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel free = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel total = Ui.label(Ui.money(0), Ui.NUMBER, Ui.CORAL_DARK);
    private final JButton book = Ui.button("Continue to payment", Ui.Style.ACCENT);

    private List<Hotel> hotels = new ArrayList<>();
    private Map<String, Double> mealPlans = Map.of();
    private boolean updating;

    public HotelPage(CustomerPortal portal) {
        super(new GridBagLayout());
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        requests.setToolTipText("Optional, e.g. late check-in or a room with a view");

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.55;
        c.insets = new Insets(0, 0, 0, 12);
        JComponent form = formCard();
        form.setPreferredSize(new Dimension(100, 100));
        add(form, c);
        c.weightx = 0.45;
        c.insets = new Insets(0, 12, 0, 0);
        JComponent summary = summaryCard();
        summary.setPreferredSize(new Dimension(100, 100));
        add(summary, c);

        city.addActionListener(e -> {
            if (!updating) {
                fillHotels();
            }
        });
        hotel.addActionListener(e -> {
            if (!updating) {
                updateSummary();
            }
        });
        roomType.addActionListener(e -> updateSummary());
        meal.addActionListener(e -> {
            if (!updating) {
                updateSummary();
            }
        });
        rooms.addChangeListener(e -> updateSummary());
        guests.addChangeListener(e -> updateSummary());
        checkIn.addChangeListener(e -> {
            // Keep check-out after check-in automatically.
            if (!Ui.date(checkOut).isAfter(Ui.date(checkIn))) {
                Ui.setDate(checkOut, Ui.date(checkIn).plusDays(1));
            }
            updateSummary();
        });
        checkOut.addChangeListener(e -> updateSummary());
        book.addActionListener(e -> book());
    }

    private JComponent formCard() {
        Ui.Card card = new Ui.Card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(Ui.label("Reserve a hotel room", Ui.TITLE, Ui.INK), c);
        c.insets = new Insets(0, 0, 20, 0);
        card.add(Ui.label("Choose where you want to stay and for how long.", Ui.BODY, Ui.MUTED), c);
        c.insets = new Insets(0, 0, 14, 0);
        card.add(Ui.row(Ui.field("City", city), Ui.field("Hotel", hotel)), c);
        card.add(Ui.row(Ui.field("Check-in", checkIn), Ui.field("Check-out", checkOut)), c);
        card.add(Ui.row(Ui.field("Room type", roomType), Ui.field("Rooms", rooms), Ui.field("Guests", guests)), c);
        card.add(Ui.row(Ui.field("Meal plan", meal), Ui.field("Special requests (optional)", requests)), c);
        c.weighty = 1;
        card.add(new JLabel(), c);
        return card;
    }

    private JComponent summaryCard() {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 16));
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 4));
        top.setOpaque(false);
        top.add(Ui.label("YOUR STAY", Ui.SMALL, Ui.MUTED));
        top.add(hotelName);
        top.add(hotelAddress);
        card.add(top, BorderLayout.NORTH);

        JPanel facts = new JPanel(new GridLayout(0, 2, 10, 14));
        facts.setOpaque(false);
        facts.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(16, 0, 16, 0)));
        facts.add(Ui.label("Room rate / night", Ui.BODY, Ui.MUTED));
        facts.add(rate);
        facts.add(Ui.label("Meals / room / night", Ui.BODY, Ui.MUTED));
        facts.add(mealCost);
        facts.add(Ui.label("Nights", Ui.BODY, Ui.MUTED));
        facts.add(nights);
        facts.add(Ui.label("Rooms free", Ui.BODY, Ui.MUTED));
        facts.add(free);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(facts, BorderLayout.NORTH);
        card.add(wrap, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        JPanel totalBox = new JPanel(new GridLayout(2, 1));
        totalBox.setOpaque(false);
        totalBox.add(Ui.label("TOTAL", Ui.SMALL, Ui.MUTED));
        totalBox.add(total);
        bottom.add(totalBox, BorderLayout.NORTH);
        book.setPreferredSize(new Dimension(100, 46));
        bottom.add(book, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    @Override
    public void refresh() {
        try {
            hotels = portal.app().catalog.activeHotels();
            mealPlans = portal.app().catalog.mealPlans();
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
        updating = true;
        Object keepCity = city.getSelectedItem();
        city.removeAllItems();
        new TreeSet<>(hotels.stream().map(Hotel::city).toList()).forEach(city::addItem);
        if (keepCity != null) {
            city.setSelectedItem(keepCity);
        }
        Object keepMeal = meal.getSelectedItem();
        meal.removeAllItems();
        mealPlans.keySet().forEach(meal::addItem);
        if (keepMeal != null) {
            meal.setSelectedItem(keepMeal);
        }
        updating = false;
        fillHotels();
    }

    private void fillHotels() {
        updating = true;
        hotel.removeAllItems();
        hotels.stream().filter(h -> h.city().equals(city.getSelectedItem())).forEach(hotel::addItem);
        updating = false;
        updateSummary();
    }

    private void updateSummary() {
        Hotel h = (Hotel) hotel.getSelectedItem();
        if (h == null || updating) {
            if (h == null) {
                hotelName.setText("No hotel selected");
                book.setEnabled(false);
            }
            return;
        }
        String type = (String) roomType.getSelectedItem();
        LocalDate in = Ui.date(checkIn);
        LocalDate out = Ui.date(checkOut);
        long n = ChronoUnit.DAYS.between(in, out);
        int r = (Integer) rooms.getValue();
        double mealPrice = mealPlans.getOrDefault((String) meal.getSelectedItem(), 0.0);
        hotelName.setText(h.name());
        hotelAddress.setText("★".repeat(h.stars()) + "   " + h.address());
        rate.setText(Ui.money(h.rate(type)) + "  (" + type + ")");
        mealCost.setText(mealPrice == 0 ? "Room only" : Ui.money(mealPrice));
        nights.setText(n > 0 ? String.valueOf(n) : "Check-out must be after check-in");
        nights.setForeground(n > 0 ? Ui.INK : Ui.RED);
        if (n < 1 || in.isBefore(LocalDate.now())) {
            free.setText(in.isBefore(LocalDate.now()) ? "Check-in is in the past" : "-");
            free.setForeground(Ui.RED);
            total.setText(Ui.money(0));
            book.setEnabled(false);
            return;
        }
        total.setText(Ui.money(BookingRepository.hotelPrice(h, type, r, n, mealPrice)));
        try {
            int available = portal.app().bookings.hotelRoomsLeft(h, type, in, out);
            free.setText(Math.max(0, available) + " of " + h.rooms(type) + " " + type);
            free.setForeground(available < r ? Ui.RED : Ui.GREEN);
            book.setEnabled(available >= r);
        } catch (SQLException ex) {
            free.setText("?");
            book.setEnabled(false);
        }
    }

    private void book() {
        Hotel h = (Hotel) hotel.getSelectedItem();
        if (h == null) {
            return;
        }
        String type = (String) roomType.getSelectedItem();
        LocalDate in = Ui.date(checkIn);
        LocalDate out = Ui.date(checkOut);
        long n = ChronoUnit.DAYS.between(in, out);
        int r = (Integer) rooms.getValue();
        int g = (Integer) guests.getValue();
        String plan = (String) meal.getSelectedItem();
        double amount = BookingRepository.hotelPrice(h, type, r, n, mealPlans.getOrDefault(plan, 0.0));
        String method = PaymentDialog.ask(this, h.name(), List.of(
                new String[]{"City", h.city()},
                new String[]{"Stay", Ui.date(in) + " to " + Ui.date(out) + " (" + n + " night(s))"},
                new String[]{"Rooms", r + " " + type + ", " + g + " guest(s)"},
                new String[]{"Meal plan", plan}), amount);
        if (method == null) {
            return;
        }
        try {
            int id = portal.app().bookings.bookHotel(portal.customer().id(), h.id(), type, r, g, in, out, plan,
                    requests.getText().trim(), method);
            requests.setText("");
            portal.booked(id, h.name() + ", " + r + " " + type + " room(s) for " + n + " night(s).");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
            updateSummary();
        }
    }
}
