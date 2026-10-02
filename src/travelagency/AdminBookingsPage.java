package travelagency;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import travelagency.Models.BookingView;

/**
 * Admin: every booking with filters, plus approve and cancel.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminBookingsPage extends JPanel implements PortalFrame.Refreshable {

    private final AdminPortal portal;
    private final JComboBox<String> type = Ui.combo("All types", "Package", "Bus", "Hotel");
    private final JComboBox<String> status = Ui.combo("All statuses", "Pending", "Approved", "Cancelled");
    private final JTextField search = Ui.field();
    private final DefaultTableModel model = Ui.model(new String[]{"No.", "Customer", "Type", "Details", "Travel date",
        "Amount", "Method", "Payment", "Status"}, Integer.class, String.class, String.class, String.class,
            LocalDate.class, Double.class, String.class, String.class, String.class);
    private final JTable table = Ui.table(model, "No bookings match these filters.");
    private final JLabel summary = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final List<BookingView> shown = new ArrayList<>();

    public AdminBookingsPage(AdminPortal portal) {
        super(new BorderLayout(0, 16));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        table.getColumnModel().getColumn(7).setCellRenderer(Ui.statusRenderer());
        table.getColumnModel().getColumn(8).setCellRenderer(Ui.statusRenderer());
        Ui.widths(table, 45, 130, 75, 310, 100, 95, 115, 95, 110);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("Bookings", Ui.TITLE, Ui.INK));
        titles.add(summary);

        search.setPreferredSize(new Dimension(220, 40));
        search.setToolTipText("Customer name, details or booking number");
        search.addActionListener(e -> refresh());
        type.setPreferredSize(new Dimension(130, 40));
        status.setPreferredSize(new Dimension(145, 40));
        type.addActionListener(e -> refresh());
        status.addActionListener(e -> refresh());
        JButton find = Ui.button("Search", Ui.Style.PRIMARY);
        find.addActionListener(e -> refresh());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        filters.add(search);
        filters.add(type);
        filters.add(status);
        filters.add(find);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titles, BorderLayout.WEST);
        top.add(filters, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 14));
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton approve = Ui.button("Approve", Ui.Style.SUCCESS);
        JButton cancel = Ui.button("Cancel & refund", Ui.Style.DANGER);
        approve.addActionListener(e -> act(true));
        cancel.addActionListener(e -> act(false));
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(Ui.label("Select a booking, then approve it or cancel it. Cancelling refunds the payment automatically.",
                Ui.SMALL, Ui.MUTED), BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(approve);
        actions.add(buttons, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            List<BookingView> list = portal.app().bookings.allBookings((String) type.getSelectedItem(),
                    (String) status.getSelectedItem(), search.getText());
            shown.clear();
            shown.addAll(list);
            model.setRowCount(0);
            int pending = 0;
            double total = 0;
            for (BookingView b : list) {
                model.addRow(new Object[]{b.id(), b.customerName(), b.type(), b.details(), b.travelDate(), b.total(),
                    b.paymentMethod() == null ? "-" : b.paymentMethod(),
                    b.paymentStatus() == null ? "-" : b.paymentStatus(), b.status()});
                if ("Pending".equals(b.status())) {
                    pending++;
                }
                if (!"Cancelled".equals(b.status())) {
                    total += b.total();
                }
            }
            summary.setText(list.size() + " booking(s) shown   |   " + pending + " pending   |   "
                    + Ui.money(total) + " active value");
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
    }

    private void act(boolean approve) {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Please select a booking in the table first.");
            return;
        }
        BookingView b = shown.get(table.convertRowIndexToModel(row));
        if (approve ? portal.approve(this, b) : portal.cancel(this, b)) {
            refresh();
        }
    }
}
