package travelagency;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import travelagency.Models.BusRoute;

/**
 * "Bus Tickets": choose a route, date, departure and class; the price and free
 * seats update live from the database.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class BusPage extends JPanel implements PortalFrame.Refreshable {

    private final CustomerPortal portal;
    private final JComboBox<String> from = Ui.combo();
    private final JComboBox<String> to = Ui.combo();
    private final JSpinner date = Ui.datePicker(LocalDate.now().plusDays(1));
    private final JComboBox<String> time = Ui.combo();
    private final JComboBox<String> seatClass = Ui.combo("Economy", "Business");
    private final JSpinner seats = Ui.number(1, 1, 10);

    private final JLabel routeLabel = Ui.label(" ", Ui.TITLE, Ui.INK);
    private final JLabel company = Ui.label(" ", Ui.BODY, Ui.MUTED);
    private final JLabel fare = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel left = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel when = Ui.label(" ", Ui.LABEL, Ui.INK);
    private final JLabel total = Ui.label(Ui.money(0), Ui.NUMBER, Ui.CORAL_DARK);
    private final JButton book = Ui.button("Continue to payment", Ui.Style.ACCENT);

    private List<BusRoute> routes = new ArrayList<>();
    private boolean updating;

    public BusPage(CustomerPortal portal) {
        super(new GridBagLayout());
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

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

        from.addActionListener(e -> {
            if (!updating) {
                fillDestinations();
            }
        });
        to.addActionListener(e -> {
            if (!updating) {
                fillTimes();
            }
        });
        date.addChangeListener(e -> {
            if (!updating) {
                fillTimes();
            }
        });
        time.addActionListener(e -> {
            if (!updating) {
                updateSummary();
            }
        });
        seatClass.addActionListener(e -> updateSummary());
        seats.addChangeListener(e -> updateSummary());
        book.addActionListener(e -> book());
    }

    private JComponent formCard() {
        Ui.Card card = new Ui.Card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(Ui.label("Book a bus ticket", Ui.TITLE, Ui.INK), c);
        c.insets = new Insets(0, 0, 22, 0);
        card.add(Ui.label("Pick your route, travel date and seat class.", Ui.BODY, Ui.MUTED), c);
        c.insets = new Insets(0, 0, 16, 0);
        card.add(Ui.row(Ui.field("From", from), Ui.field("To", to)), c);
        card.add(Ui.row(Ui.field("Travel date", date), Ui.field("Departure time", time)), c);
        card.add(Ui.row(Ui.field("Seat class", seatClass), Ui.field("Seats (1-10)", seats)), c);
        c.weighty = 1;
        card.add(new JLabel(), c);
        return card;
    }

    private JComponent summaryCard() {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 16), Ui.WHITE);
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 4));
        top.setOpaque(false);
        top.add(Ui.label("YOUR TRIP", Ui.SMALL, Ui.MUTED));
        top.add(routeLabel);
        top.add(company);
        card.add(top, BorderLayout.NORTH);

        JPanel facts = new JPanel(new GridLayout(0, 2, 10, 14));
        facts.setOpaque(false);
        facts.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(16, 0, 16, 0)));
        facts.add(Ui.label("Departure", Ui.BODY, Ui.MUTED));
        facts.add(when);
        facts.add(Ui.label("Fare per seat", Ui.BODY, Ui.MUTED));
        facts.add(fare);
        facts.add(Ui.label("Seats free", Ui.BODY, Ui.MUTED));
        facts.add(left);
        JPanel factsWrap = new JPanel(new BorderLayout());
        factsWrap.setOpaque(false);
        factsWrap.add(facts, BorderLayout.NORTH);
        card.add(factsWrap, BorderLayout.CENTER);

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
            routes = portal.app().catalog.activeRoutes();
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
        Object keepFrom = from.getSelectedItem();
        updating = true;
        from.removeAllItems();
        new TreeSet<>(routes.stream().map(BusRoute::from).toList()).forEach(from::addItem);
        if (keepFrom != null) {
            from.setSelectedItem(keepFrom);
        }
        updating = false;
        fillDestinations();
    }

    private void fillDestinations() {
        Object keep = to.getSelectedItem();
        updating = true;
        to.removeAllItems();
        routes.stream().filter(r -> r.from().equals(from.getSelectedItem())).map(BusRoute::to).sorted().forEach(to::addItem);
        if (keep != null) {
            to.setSelectedItem(keep);
        }
        updating = false;
        fillTimes();
    }

    private void fillTimes() {
        BusRoute route = selectedRoute();
        Object keep = time.getSelectedItem();
        updating = true;
        time.removeAllItems();
        if (route != null) {
            LocalDate d = Ui.date(date);
            for (String t : route.times()) {
                // Buses that already left today are not offered.
                if (!d.equals(LocalDate.now()) || LocalTime.parse(t).isAfter(LocalTime.now())) {
                    time.addItem(t);
                }
            }
        }
        if (keep != null) {
            time.setSelectedItem(keep);
        }
        updating = false;
        updateSummary();
    }

    private BusRoute selectedRoute() {
        return routes.stream().filter(r -> r.from().equals(from.getSelectedItem()) && r.to().equals(to.getSelectedItem()))
                .findFirst().orElse(null);
    }

    private void updateSummary() {
        BusRoute route = selectedRoute();
        String cls = (String) seatClass.getSelectedItem();
        LocalDate d = Ui.date(date);
        if (route == null) {
            routeLabel.setText("No route selected");
            company.setText(routes.isEmpty() ? "No bus routes are available yet." : " ");
            book.setEnabled(false);
            return;
        }
        routeLabel.setText(route.from() + "  →  " + route.to());
        company.setText(route.company());
        fare.setText(Ui.money(route.fare(cls)) + "  (" + cls + ")");
        int n = (Integer) seats.getValue();
        total.setText(Ui.money(route.fare(cls) * n));
        String t = (String) time.getSelectedItem();
        if (d.isBefore(LocalDate.now()) || t == null) {
            when.setText(d.isBefore(LocalDate.now()) ? "Date is in the past" : "No buses left today");
            when.setForeground(Ui.RED);
            left.setText("-");
            book.setEnabled(false);
            return;
        }
        when.setForeground(Ui.INK);
        when.setText(Ui.date(d) + " at " + t);
        try {
            int free = portal.app().bookings.busSeatsLeft(route, d, t, cls);
            left.setText(free + " of " + route.seats(cls) + " " + cls);
            left.setForeground(free < n ? Ui.RED : Ui.GREEN);
            book.setEnabled(free >= n);
        } catch (SQLException ex) {
            left.setText("?");
            book.setEnabled(false);
        }
    }

    private void book() {
        BusRoute route = selectedRoute();
        String t = (String) time.getSelectedItem();
        if (route == null || t == null) {
            return;
        }
        String cls = (String) seatClass.getSelectedItem();
        int n = (Integer) seats.getValue();
        LocalDate d = Ui.date(date);
        double amount = route.fare(cls) * n;
        String method = PaymentDialog.ask(this, route.from() + " to " + route.to(), List.of(
                new String[]{"Bus", route.company()},
                new String[]{"Departure", Ui.date(d) + " at " + t},
                new String[]{"Seats", n + " " + cls + " x " + Ui.money(route.fare(cls))}), amount);
        if (method == null) {
            return;
        }
        try {
            int id = portal.app().bookings.bookBus(portal.customer().id(), route.id(), d, t, cls, n, method);
            portal.booked(id, route.from() + " to " + route.to() + " on " + Ui.date(d) + " at " + t + ", " + n + " "
                    + cls + " seat(s).");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
            updateSummary();
        }
    }
}
