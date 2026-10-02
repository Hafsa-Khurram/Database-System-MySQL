package travelagency;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import travelagency.Models.BookingView;

/**
 * Admin home: key numbers, revenue of the last months and the bookings waiting for approval.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminDashboardPage extends JPanel implements PortalFrame.Refreshable {

    private final AdminPortal portal;
    private final JLabel customers = number();
    private final JLabel pending = number();
    private final JLabel upcoming = number();
    private final JLabel revenue = number();
    private final JLabel packages = number();
    private final JLabel rating = number();
    private final ReportsPage.BarChart chart = new ReportsPage.BarChart();
    private final DefaultTableModel model = Ui.model(new String[]{"No.", "Customer", "Type", "Details", "Travel date",
        "Amount"}, Integer.class, String.class, String.class, String.class, LocalDate.class, Double.class);
    private final JTable table = Ui.table(model, "Nothing to approve. All bookings are handled.");
    private final List<BookingView> pendingList = new ArrayList<>();

    public AdminDashboardPage(AdminPortal portal) {
        super(new BorderLayout(0, 18));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        Ui.widths(table, 45, 125, 75, 270, 100, 100);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("Welcome back, " + portal.admin().fullName(), Ui.TITLE, Ui.INK));
        titles.add(Ui.label("Here is what is happening at the agency today, " + Ui.date(LocalDate.now()) + ".",
                Ui.SMALL, Ui.MUTED));

        JPanel stats = new JPanel(new GridLayout(1, 6, 14, 0));
        stats.setOpaque(false);
        stats.add(stat("Customers", customers, Ui.TEAL, Ui.TEAL_SOFT));
        stats.add(stat("Pending approval", pending, Ui.AMBER, Ui.AMBER_LIGHT));
        stats.add(stat("Upcoming trips", upcoming, Ui.GREEN, Ui.GREEN_LIGHT));
        stats.add(stat("Revenue", revenue, Ui.CORAL_DARK, Ui.CORAL_LIGHT));
        stats.add(stat("Open packages", packages, Ui.TEAL_DARK, Ui.TEAL_LIGHT));
        stats.add(stat("Avg. rating", rating, Ui.AMBER, Ui.AMBER_LIGHT));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(titles, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        Ui.Card pendingCard = new Ui.Card(new BorderLayout(0, 12));
        pendingCard.add(Ui.label("Waiting for approval", Ui.HEADING, Ui.INK), BorderLayout.NORTH);
        pendingCard.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton approve = Ui.button("Approve", Ui.Style.SUCCESS);
        JButton cancel = Ui.button("Cancel & refund", Ui.Style.OUTLINE);
        JButton all = Ui.button("All bookings", Ui.Style.LIGHT);
        approve.addActionListener(e -> act(true));
        cancel.addActionListener(e -> act(false));
        all.addActionListener(e -> portal.open(AdminPortal.BOOKINGS));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(all);
        buttons.add(cancel);
        buttons.add(approve);
        pendingCard.add(buttons, BorderLayout.SOUTH);

        Ui.Card chartCard = new Ui.Card(new BorderLayout(0, 12));
        chartCard.add(Ui.label("Revenue, last 6 months", Ui.HEADING, Ui.INK), BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.setPreferredSize(new Dimension(390, 100));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 9);
        pendingCard.setPreferredSize(new Dimension(100, 100));
        center.add(pendingCard, c);
        c.weightx = 0;
        c.insets = new Insets(0, 9, 0, 0);
        center.add(chartCard, c);
        add(center, BorderLayout.CENTER);
    }

    private static JLabel number() {
        return Ui.label("0", Ui.NUMBER, Ui.INK);
    }

    private static JPanel stat(String title, JLabel value, Color accent, Color tint) {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(34, 5));
        JPanel barRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barRow.setOpaque(false);
        barRow.add(bar);
        card.add(barRow, BorderLayout.NORTH);
        value.setForeground(accent.darker());
        card.add(value, BorderLayout.CENTER);
        JLabel t = Ui.label(title, Ui.SMALL, Ui.MUTED);
        card.add(t, BorderLayout.SOUTH);
        return card;
    }

    @Override
    public void refresh() {
        try {
            Map<String, Number> s = portal.app().admin.dashboardStats();
            customers.setText(String.valueOf(s.get("customers").intValue()));
            pending.setText(String.valueOf(s.get("pending").intValue()));
            upcoming.setText(String.valueOf(s.get("upcoming").intValue()));
            revenue.setText(compact(s.get("revenue").doubleValue()));
            revenue.setToolTipText(Ui.money(s.get("revenue").doubleValue()));
            packages.setText(String.valueOf(s.get("packages").intValue()));
            double r = s.get("rating").doubleValue();
            rating.setText(r == 0 ? "-" : String.format("%.1f ★", r));

            chart.setData(portal.app().admin.monthlyRevenue());

            pendingList.clear();
            pendingList.addAll(portal.app().bookings.allBookings(null, "Pending", null));
            model.setRowCount(0);
            for (BookingView b : pendingList) {
                model.addRow(new Object[]{b.id(), b.customerName(), b.type(), b.details(), b.travelDate(), b.total()});
            }
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
    }

    /** 1,250,000 becomes "Rs 1.25M" so it fits in a small card. */
    static String compact(double amount) {
        if (amount >= 1_000_000) {
            return String.format("Rs %.2fM", amount / 1_000_000);
        }
        if (amount >= 10_000) {
            return String.format("Rs %.1fK", amount / 1_000);
        }
        return Ui.money(amount);
    }

    private void act(boolean approve) {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Please select a booking in the table first.");
            return;
        }
        BookingView b = pendingList.get(table.convertRowIndexToModel(row));
        if (approve ? portal.approve(this, b) : portal.cancel(this, b)) {
            refresh();
        }
    }
}
