package travelagency;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Window shared by the customer and admin portals: a teal header with the user's
 * name, a row of tabs underneath, and the page for the selected tab.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public abstract class PortalFrame extends JFrame {

    /** Pages that reload their data from MySQL every time they are opened. */
    public interface Refreshable {
        void refresh();
    }

    protected final App app;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final Map<String, Tab> tabs = new LinkedHashMap<>();
    private final Map<String, JComponent> pageByName = new LinkedHashMap<>();

    protected PortalFrame(App app, String title, String userName, String roleBadge) {
        this.app = app;
        setTitle(title);
        setIconImage(LoginFrame.iconImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Ui.Gradient header = new Ui.Gradient(new BorderLayout(), Ui.TEAL_DARK, Ui.TEAL, false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 26, 14, 22));
        JLabel brand = Ui.label("  Travel Agency", new Font("Segoe UI", Font.BOLD, 18), Color.WHITE);
        brand.setIcon(Ui.logo(32, Color.WHITE, Ui.CORAL));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(brand);
        left.add(Ui.pill(roleBadge, Ui.TEAL_DARK, Ui.TEAL_LIGHT));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(Ui.label(userName, Ui.LABEL, Color.WHITE));
        JButton logout = Ui.button("Log out", Ui.Style.ACCENT);
        logout.addActionListener(e -> {
            if (Ui.confirm(this, "Do you want to log out?", "Log out")) {
                dispose();
                new LoginFrame(app).setVisible(true);
            }
        });
        right.add(logout);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        tabBar.setBackground(Ui.WHITE);
        tabBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(0, 18, 0, 18)));
        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH);
        top.add(tabBar, BorderLayout.SOUTH);

        pages.setBackground(Ui.SAND);
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(pages, BorderLayout.CENTER);
        setSize(1320, 820);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
    }

    protected void addPage(String name, JComponent page) {
        page.setBackground(Ui.SAND);
        pages.add(page, name);
        pageByName.put(name, page);
        Tab tab = new Tab(name);
        tabs.put(name, tab);
        tabBar.add(tab);
    }

    public void open(String name) {
        cards.show(pages, name);
        tabs.forEach((n, t) -> t.setActive(n.equals(name)));
        if (pageByName.get(name) instanceof Refreshable r) {
            r.refresh();
        }
    }

    /** One tab: text with a coral bar under the active one. */
    private class Tab extends JLabel {
        private boolean active;
        private boolean hover;

        Tab(String text) {
            super(text);
            setFont(Ui.LABEL);
            setForeground(Ui.MUTED);
            setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    open(text);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Ui.TEAL_DARK : Ui.MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hover) {
                Graphics2D g2 = Ui.smooth(g);
                g2.setColor(active ? Ui.CORAL : Ui.TEAL_LIGHT);
                g2.fillRoundRect(10, getHeight() - 4, getWidth() - 20, 4, 4, 4);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
