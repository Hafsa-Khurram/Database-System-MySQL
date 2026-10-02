package travelagency;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import travelagency.Models.Customer;
import travelagency.Models.CustomerSummary;

/**
 * Admin: registered customers with how much they have booked.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminCustomersPage extends JPanel implements PortalFrame.Refreshable {

    private final AdminPortal portal;
    private final JTextField search = Ui.field();
    private final DefaultTableModel model = Ui.model(new String[]{"ID", "Name", "Username", "Email", "Phone", "Gender",
        "Joined", "Bookings", "Spent"}, Integer.class, String.class, String.class, String.class, String.class,
            String.class, LocalDate.class, Integer.class, Double.class);
    private final JTable table = Ui.table(model, "No customers found.");
    private final JLabel summary = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private List<CustomerSummary> all = new ArrayList<>();
    private final List<CustomerSummary> shown = new ArrayList<>();

    public AdminCustomersPage(AdminPortal portal) {
        super(new BorderLayout(0, 16));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        Ui.widths(table, 45, 160, 110, 220, 120, 75, 105, 80, 110);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("Customers", Ui.TITLE, Ui.INK));
        titles.add(summary);
        search.setPreferredSize(new Dimension(260, 40));
        search.setToolTipText("Search by name, username, email or phone");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                render();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                render();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                render();
            }
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(Ui.label("Search", Ui.LABEL, Ui.MUTED));
        right.add(search);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titles, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        Ui.Card card = new Ui.Card(new BorderLayout(0, 14));
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton delete = Ui.button("Delete account", Ui.Style.DANGER);
        delete.addActionListener(e -> delete());
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(Ui.label("Deleting a customer also removes their past bookings, payments and feedback.",
                Ui.SMALL, Ui.MUTED), BorderLayout.WEST);
        actions.add(delete, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            all = portal.app().admin.customers();
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
        render();
    }

    private void render() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        model.setRowCount(0);
        shown.clear();
        for (CustomerSummary s : all) {
            Customer c = s.customer();
            String text = (c.fullName() + " " + c.username() + " " + c.email() + " " + c.phone()).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !text.contains(q)) {
                continue;
            }
            shown.add(s);
            model.addRow(new Object[]{c.id(), c.fullName(), c.username(), c.email(), c.phone(), c.gender(),
                s.joined() == null ? null : s.joined().toLocalDate(), s.bookings(), s.spent()});
        }
        summary.setText(all.size() + " registered customer(s)" + (q.isEmpty() ? "" : "   |   " + shown.size() + " match"));
    }

    private void delete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Please select a customer in the table first.");
            return;
        }
        Customer c = shown.get(table.convertRowIndexToModel(row)).customer();
        if (!Ui.confirm(this, "Delete the account of " + c.fullName() + " (" + c.username() + ")?\n"
                + "Their booking history will be removed too. This cannot be undone.", "Delete customer")) {
            return;
        }
        try {
            portal.app().admin.deleteCustomer(c);
            refresh();
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }
}
