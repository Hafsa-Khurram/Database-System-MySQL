package travelagency;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import travelagency.Models.BookingView;

/**
 * "My Bookings": the customer's bookings with status and payment, plus cancel and feedback.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class MyBookingsPage extends JPanel implements PortalFrame.Refreshable {

    private final CustomerPortal portal;
    private final JComboBox<String> statusFilter = Ui.combo("All bookings", "Pending", "Approved", "Cancelled");
    private final DefaultTableModel model = Ui.model(new String[]{"No.", "Type", "Details", "Travel date", "Amount",
        "Status", "Payment", "Your rating"}, Integer.class, String.class, String.class, LocalDate.class, Double.class,
            String.class, String.class, String.class);
    private final JTable table = Ui.table(model, "You have no bookings yet. Book a package, bus or hotel to get started.");
    private final JLabel summary = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private List<BookingView> all = new ArrayList<>();
    private final List<BookingView> shown = new ArrayList<>();

    public MyBookingsPage(CustomerPortal portal) {
        super(new BorderLayout(0, 16));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));
        table.getColumnModel().getColumn(5).setCellRenderer(Ui.statusRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(Ui.statusRenderer());
        Ui.widths(table, 55, 80, 360, 115, 100, 115, 105, 95);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("My bookings", Ui.TITLE, Ui.INK));
        titles.add(summary);
        statusFilter.setPreferredSize(new Dimension(170, 40));
        statusFilter.addActionListener(e -> render());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titles, BorderLayout.WEST);
        top.add(statusFilter, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 14));
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton cancel = Ui.button("Cancel booking", Ui.Style.OUTLINE);
        JButton feedback = Ui.button("Give feedback", Ui.Style.PRIMARY);
        cancel.addActionListener(e -> cancel());
        feedback.addActionListener(e -> feedback());
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(Ui.label("<html>New bookings are <b>Pending</b> until the agency approves them. "
                + "Cancelled bookings are refunded automatically.</html>", Ui.SMALL, Ui.MUTED), BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(feedback);
        actions.add(buttons, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            all = portal.app().bookings.customerBookings(portal.customer().id());
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
        render();
    }

    private void render() {
        String status = (String) statusFilter.getSelectedItem();
        model.setRowCount(0);
        shown.clear();
        double spent = 0;
        for (BookingView b : all) {
            if (!"Cancelled".equals(b.status())) {
                spent += b.total();
            }
            if (status != null && !status.startsWith("All") && !status.equals(b.status())) {
                continue;
            }
            shown.add(b);
            model.addRow(new Object[]{b.id(), b.type(), b.details(), b.travelDate(), b.total(), b.status(),
                b.paymentStatus(), b.rating() == null ? "-" : "★".repeat(b.rating())});
        }
        summary.setText(all.size() + " booking(s)   |   " + Ui.money(spent) + " spent on active bookings");
    }

    private BookingView selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Please select a booking in the table first.");
            return null;
        }
        return shown.get(table.convertRowIndexToModel(row));
    }

    private void cancel() {
        BookingView b = selected();
        if (b == null) {
            return;
        }
        if (Ui.confirm(this, "Cancel booking #" + b.id() + "?\n" + b.details() + "\n\n" + Ui.money(b.total())
                + " will be refunded to your " + b.paymentMethod() + " payment.", "Cancel booking")) {
            try {
                portal.app().bookings.cancelByCustomer(b.id(), portal.customer().id());
                Ui.info(this, "Booking #" + b.id() + " is cancelled and your payment is refunded.");
                refresh();
            } catch (IllegalArgumentException | SQLException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void feedback() {
        BookingView b = selected();
        if (b == null) {
            return;
        }
        if (!"Approved".equals(b.status())) {
            Ui.info(this, "You can give feedback once the agency has approved the booking.");
            return;
        }
        if (b.rating() != null) {
            Ui.info(this, "You have already rated this booking. Thank you!");
            return;
        }
        Stars stars = new Stars();
        JTextArea comments = Ui.area(4);
        JPanel ask = new JPanel(new BorderLayout(0, 10));
        ask.add(Ui.label("How was \"" + b.details() + "\"?", Ui.LABEL, Ui.INK), BorderLayout.NORTH);
        ask.add(stars, BorderLayout.CENTER);
        ask.add(Ui.field("Comments (optional)", comments), BorderLayout.SOUTH);
        ask.setPreferredSize(new Dimension(420, 220));
        int answer = JOptionPane.showConfirmDialog(this, ask, "Your feedback", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            portal.app().bookings.addFeedback(b.id(), portal.customer().id(), stars.rating, comments.getText());
            Ui.info(this, "Thank you for your feedback!");
            refresh();
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }

    /** Five clickable stars. */
    private static class Stars extends JPanel {
        int rating = 0;
        private int hover = 0;

        Stars() {
            setOpaque(false);
            setPreferredSize(new Dimension(260, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    rating = starAt(e.getX());
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    hover = starAt(e.getX());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = 0;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private int starAt(int x) {
            return Math.max(1, Math.min(5, x / 44 + 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            g2.setFont(new Font("Dialog", Font.PLAIN, 36));
            int show = hover > 0 ? hover : rating;
            for (int i = 0; i < 5; i++) {
                g2.setColor(i < show ? Ui.CORAL : Ui.LINE);
                g2.drawString("★", i * 44, 38);
            }
            g2.dispose();
        }
    }
}
