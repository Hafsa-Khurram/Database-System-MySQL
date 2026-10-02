package travelagency;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Admin: business reports built from SQL views and aggregate queries.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class ReportsPage extends JPanel implements PortalFrame.Refreshable {

    private final AdminPortal portal;
    private final BarChart monthly = new BarChart();
    private final BarChart byType = new BarChart();
    private final JPanel statusBox = new JPanel(new GridLayout(4, 1, 0, 10));
    private final DefaultTableModel top = Ui.model(new String[]{"Package", "Travellers", "Revenue", "Rating"},
            String.class, Integer.class, Double.class, String.class);
    private final JTable topTable = Ui.table(top, "No package bookings yet.");

    public ReportsPage(AdminPortal portal) {
        super(new BorderLayout(0, 16));
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        Ui.widths(topTable, 240, 90, 110, 70);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label("Reports", Ui.TITLE, Ui.INK));
        titles.add(Ui.label("Live figures from the travel_agency database (cancelled bookings are not counted as revenue).",
                Ui.SMALL, Ui.MUTED));
        add(titles, BorderLayout.NORTH);

        byType.setColors(new Color[]{Ui.TEAL, Ui.CORAL, Ui.TEAL_DARK});
        statusBox.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);
        grid.add(card("Revenue by month", monthly));
        grid.add(card("Revenue by booking type", byType));
        grid.add(card("Top packages", Ui.scroll(topTable)));
        grid.add(card("Bookings by status", statusBox));
        add(grid, BorderLayout.CENTER);
    }

    private static JComponent card(String title, JComponent body) {
        Ui.Card c = new Ui.Card(new BorderLayout(0, 12));
        c.add(Ui.label(title, Ui.HEADING, Ui.INK), BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    @Override
    public void refresh() {
        try {
            monthly.setData(portal.app().admin.monthlyRevenue());
            List<Object[]> types = new ArrayList<>();
            for (Object[] r : portal.app().admin.revenueByType()) {
                types.add(new Object[]{r[0] + " (" + r[1] + ")", r[2]});
            }
            byType.setData(types);

            top.setRowCount(0);
            for (Object[] r : portal.app().admin.topPackages()) {
                top.addRow(new Object[]{r[0], r[1], r[2], r[3] == null ? "-" : String.format("%.1f ★", (Double) r[3])});
            }

            statusBox.removeAll();
            List<Object[]> counts = portal.app().admin.statusCounts();
            int total = counts.stream().mapToInt(r -> (Integer) r[1]).sum();
            for (Object[] r : counts) {
                statusBox.add(statusRow((String) r[0], (Integer) r[1], total));
            }
            JLabel refunded = Ui.label("Refunded to customers:  " + Ui.money(portal.app().admin.totalRefunded()),
                    Ui.LABEL, Ui.RED);
            statusBox.add(refunded);
            statusBox.revalidate();
            statusBox.repaint();
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
    }

    private static JComponent statusRow(String status, int count, int total) {
        Color[] c = Ui.statusColors(status);
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        JLabel name = Ui.pill(status, c[0], c[1]);
        name.setPreferredSize(new Dimension(105, 28));
        row.add(name, BorderLayout.WEST);
        JComponent bar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.smooth(g);
                int h = 12;
                int y = (getHeight() - h) / 2;
                g2.setColor(Ui.SAND);
                g2.fillRoundRect(0, y, getWidth(), h, h, h);
                int w = total == 0 ? 0 : Math.max(h, (int) (getWidth() * (count / (double) total)));
                if (count > 0) {
                    g2.setColor(c[0]);
                    g2.fillRoundRect(0, y, w, h, h, h);
                }
                g2.dispose();
            }
        };
        row.add(bar, BorderLayout.CENTER);
        JLabel n = Ui.label(count + (total == 0 ? "" : "  (" + Math.round(100.0 * count / total) + "%)"), Ui.LABEL, Ui.INK);
        n.setPreferredSize(new Dimension(80, 28));
        row.add(n, BorderLayout.EAST);
        return row;
    }

    /** Simple vertical bar chart. Data rows are {label, amount}. */
    static class BarChart extends JComponent {
        private List<Object[]> data = List.of();
        private Color[] colors = {Ui.TEAL};

        BarChart() {
            setPreferredSize(new Dimension(300, 200));
        }

        void setColors(Color[] colors) {
            this.colors = colors;
        }

        void setData(List<Object[]> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            int w = getWidth();
            int h = getHeight();
            int top = 26;
            int bottom = 28;
            double max = 0;
            for (Object[] r : data) {
                max = Math.max(max, ((Number) r[1]).doubleValue());
            }
            g2.setColor(Ui.LINE);
            for (int i = 0; i <= 3; i++) {
                int y = top + (h - top - bottom) * i / 3;
                g2.drawLine(0, y, w, y);
            }
            if (data.isEmpty()) {
                g2.dispose();
                return;
            }
            int slot = w / data.size();
            int barW = Math.min(64, slot - 18);
            g2.setFont(Ui.SMALL);
            FontMetrics fm = g2.getFontMetrics();
            boolean shortLabels = false;
            for (Object[] r : data) {
                shortLabels |= fm.stringWidth(String.valueOf(r[0])) > slot - 6;
            }
            for (int i = 0; i < data.size(); i++) {
                String label = String.valueOf(data.get(i)[0]);
                double v = ((Number) data.get(i)[1]).doubleValue();
                int bh = max == 0 ? 0 : (int) ((h - top - bottom) * v / max);
                int x = i * slot + (slot - barW) / 2;
                int y = h - bottom - bh;
                g2.setColor(colors[i % colors.length]);
                if (bh > 0) {
                    g2.fillRoundRect(x, y, barW, bh + 8, 10, 10);
                    g2.setColor(Ui.WHITE);
                    g2.fillRect(x, h - bottom, barW, 8);
                }
                g2.setFont(Ui.SMALL);
                fm = g2.getFontMetrics();
                g2.setColor(Ui.MUTED);
                if (shortLabels && label.contains(" ")) {
                    label = label.substring(0, label.indexOf(' '));
                }
                g2.drawString(label, i * slot + (slot - fm.stringWidth(label)) / 2, h - 8);
                String value = v == 0 ? "0" : AdminDashboardPage.compact(v).replace("Rs ", "");
                g2.setFont(Ui.LABEL);
                fm = g2.getFontMetrics();
                g2.setColor(Ui.INK);
                g2.drawString(value, i * slot + (slot - fm.stringWidth(value)) / 2, y - 6);
            }
            g2.dispose();
        }
    }
}
