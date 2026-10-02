package travelagency;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import travelagency.Models.TravelPackage;

/**
 * "Tour Packages": every bookable package as a card with price, dates and seats left.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class PackagesPage extends JPanel implements PortalFrame.Refreshable {

    private static final Map<String, Color[]> TYPE_COLORS = Map.of(
            "Adventure", new Color[]{new Color(21, 128, 61), new Color(20, 83, 45)},
            "Family", new Color[]{new Color(2, 132, 199), new Color(7, 89, 133)},
            "Economy", new Color[]{new Color(13, 148, 136), new Color(17, 94, 89)},
            "Luxury", new Color[]{new Color(180, 83, 9), new Color(120, 53, 15)},
            "Honeymoon", new Color[]{new Color(219, 39, 119), new Color(157, 23, 77)},
            "Standard", new Color[]{new Color(71, 85, 105), new Color(30, 41, 59)});

    private final CustomerPortal portal;
    private final JTextField search = Ui.field();
    private final JComboBox<String> typeFilter = Ui.combo("All types", "Adventure", "Family", "Economy", "Standard",
            "Luxury", "Honeymoon");
    private final Grid grid = new Grid();
    private final JLabel count = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private List<TravelPackage> packages = List.of();

    public PackagesPage(CustomerPortal portal) {
        super(new BorderLayout(0, 18));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(26, 30, 20, 30));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("Explore tour packages", Ui.TITLE, Ui.INK));
        titles.add(count);
        search.setPreferredSize(new Dimension(240, 40));
        search.setToolTipText("Search by name or destination");
        typeFilter.setPreferredSize(new Dimension(160, 40));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);
        filters.add(Ui.label("Search", Ui.LABEL, Ui.INK));
        filters.add(search);
        filters.add(typeFilter);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titles, BorderLayout.WEST);
        top.add(filters, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(Ui.clearScroll(grid), BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                render();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                render();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                render();
            }
        });
        typeFilter.addActionListener(e -> render());
    }

    @Override
    public void refresh() {
        try {
            packages = portal.app().catalog.bookablePackages();
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
        render();
    }

    private void render() {
        String text = search.getText().trim().toLowerCase(Locale.ENGLISH);
        String type = (String) typeFilter.getSelectedItem();
        grid.removeAll();
        int shown = 0;
        for (TravelPackage p : packages) {
            boolean matches = (p.name() + " " + p.destination()).toLowerCase(Locale.ENGLISH).contains(text)
                    && (type == null || type.startsWith("All") || type.equals(p.type()));
            if (matches) {
                grid.add(card(p));
                shown++;
            }
        }
        if (shown == 0) {
            JLabel none = Ui.label("No packages match your search.", Ui.BODY, Ui.MUTED);
            none.setBorder(BorderFactory.createEmptyBorder(30, 6, 0, 0));
            grid.add(none);
        }
        count.setText(shown + " package(s) available to book");
        grid.revalidate();
        grid.repaint();
    }

    private JPanel card(TravelPackage p) {
        Color[] colors = TYPE_COLORS.getOrDefault(p.type(), new Color[]{Ui.TEAL, Ui.TEAL_DARK});
        Ui.Card card = new Ui.Card(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder());

        JPanel banner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.smooth(g);
                g2.setPaint(new GradientPaint(0, 0, colors[0], getWidth(), getHeight(), colors[1]));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 22, 22, 22);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(getWidth() - 90, -40, 140, 140);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(100, 96));
        banner.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel bannerTop = new JPanel(new BorderLayout());
        bannerTop.setOpaque(false);
        bannerTop.add(Ui.pill(p.type().toUpperCase(Locale.ENGLISH), colors[1], new Color(255, 255, 255, 220)), BorderLayout.WEST);
        bannerTop.add(Ui.label(p.days() + " days", Ui.LABEL, Color.WHITE), BorderLayout.EAST);
        banner.add(bannerTop, BorderLayout.NORTH);
        banner.add(Ui.label(p.destination(), new Font("Segoe UI", Font.BOLD, 26), Color.WHITE), BorderLayout.SOUTH);
        card.add(banner, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(0, 1, 0, 6));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 4, 18));
        body.add(Ui.label(p.name(), Ui.HEADING, Ui.INK));
        body.add(Ui.label(Ui.date(p.startDate()) + "  →  " + Ui.date(p.endDate()), Ui.BODY, Ui.MUTED));
        JLabel includes = Ui.label("Includes: " + p.includes(), Ui.SMALL, Ui.MUTED);
        includes.setToolTipText(p.description());
        body.add(includes);
        card.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 18, 18, 18));
        JPanel price = new JPanel(new GridLayout(2, 1));
        price.setOpaque(false);
        price.add(Ui.label(Ui.money(p.pricePerPerson()), new Font("Segoe UI", Font.BOLD, 20), Ui.CORAL_DARK));
        price.add(Ui.label("per person", Ui.SMALL, Ui.MUTED));
        boolean low = p.seatsLeft() <= 5;
        JLabel seats = p.seatsLeft() <= 0 ? Ui.pill("SOLD OUT", Ui.RED, Ui.RED_LIGHT)
                : Ui.pill(p.seatsLeft() + " seats left", low ? Ui.RED : Ui.TEAL_DARK, low ? Ui.RED_LIGHT : Ui.TEAL_LIGHT);
        JButton book = Ui.button("Book now", Ui.Style.PRIMARY);
        book.setEnabled(p.seatsLeft() > 0);
        book.addActionListener(e -> book(p));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);
        right.add(seats);
        right.add(book);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void book(TravelPackage p) {
        JSpinner travellers = Ui.number(1, 1, Math.min(20, p.seatsLeft()));
        JLabel total = Ui.label(Ui.money(p.pricePerPerson()), Ui.HEADING, Ui.CORAL_DARK);
        travellers.addChangeListener(e -> total.setText(Ui.money(p.pricePerPerson() * (Integer) travellers.getValue())));
        JPanel ask = new JPanel(new GridLayout(0, 1, 0, 8));
        ask.add(Ui.label(p.name(), Ui.HEADING, Ui.INK));
        ask.add(Ui.label(p.seatsLeft() + " seat(s) left. " + p.description(), Ui.SMALL, Ui.MUTED));
        ask.add(Ui.field("Number of travellers", travellers));
        ask.add(total);
        int answer = javax.swing.JOptionPane.showConfirmDialog(this, ask, "Book " + p.name(),
                javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (answer != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }
        int people = (Integer) travellers.getValue();
        double amount = p.pricePerPerson() * people;
        String method = PaymentDialog.ask(this, p.name(), List.of(
                new String[]{"Destination", p.destination()},
                new String[]{"Dates", Ui.date(p.startDate()) + " to " + Ui.date(p.endDate())},
                new String[]{"Includes", p.includes()},
                new String[]{"Travellers", people + " x " + Ui.money(p.pricePerPerson())}), amount);
        if (method == null) {
            return;
        }
        try {
            int id = portal.app().bookings.bookPackage(portal.customer().id(), p.id(), people, method);
            portal.booked(id, p.name() + " for " + people + " traveller(s), " + Ui.money(amount) + " paid by " + method + ".");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
            refresh();
        }
    }

    /** Three-column grid that fills the width of the scroll pane. */
    private static class Grid extends JPanel implements Scrollable {
        Grid() {
            super(new GridLayout(0, 3, 20, 20));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 12, 8));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
