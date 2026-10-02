package travelagency;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import travelagency.Models.Feedback;

/**
 * Admin: reviews that customers left on their approved bookings.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminFeedbackPage extends JPanel implements PortalFrame.Refreshable {

    private final AdminPortal portal;
    private final DefaultTableModel model = Ui.model(new String[]{"Booking", "Customer", "Booked", "Rating", "Comments",
        "Date"}, Integer.class, String.class, String.class, String.class, String.class, LocalDate.class);
    private final JTable table = Ui.table(model, "No feedback yet. Customers can rate approved bookings.");
    private final JLabel summary = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final JTextArea full = Ui.area(3);
    private List<Feedback> list = List.of();

    public AdminFeedbackPage(AdminPortal portal) {
        super(new BorderLayout(0, 16));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        Ui.widths(table, 70, 150, 260, 95, 380, 105);
        table.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                                    int row, int col) {
                java.awt.Component c = t.getDefaultRenderer(Object.class)
                        .getTableCellRendererComponent(t, v, sel, focus, row, col);
                c.setForeground(Ui.CORAL_DARK);
                return c;
            }
        });

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("Customer feedback", Ui.TITLE, Ui.INK));
        titles.add(summary);
        add(titles, BorderLayout.NORTH);

        full.setEditable(false);
        full.setBackground(Ui.TEAL_SOFT);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                Feedback f = list.get(table.convertRowIndexToModel(row));
                full.setText(f.customerName() + " about booking #" + f.bookingId() + " (" + f.bookingDetails() + "):\n"
                        + (f.comments() == null || f.comments().isBlank() ? "(no comment)" : f.comments()));
                full.setCaretPosition(0);
            }
        });
        Ui.Card card = new Ui.Card(new BorderLayout(0, 14));
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(Ui.label("Selected review", Ui.LABEL, Ui.MUTED), BorderLayout.NORTH);
        bottom.add(full, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            list = portal.app().bookings.allFeedback();
        } catch (SQLException ex) {
            Ui.error(this, ex);
            return;
        }
        model.setRowCount(0);
        double sum = 0;
        for (Feedback f : list) {
            sum += f.rating();
            model.addRow(new Object[]{f.bookingId(), f.customerName(), f.bookingDetails(),
                "★".repeat(f.rating()) + "☆".repeat(5 - f.rating()), f.comments(),
                f.createdAt() == null ? null : f.createdAt().toLocalDate()});
        }
        full.setText("Click a review to read it in full.");
        summary.setText(list.isEmpty() ? "No reviews yet"
                : list.size() + " review(s)   |   average " + String.format("%.1f", sum / list.size()) + " / 5");
    }
}
