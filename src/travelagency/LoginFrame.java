package travelagency;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * One login screen for both roles. A switch at the top chooses Customer or Admin;
 * customers can also create an account from here.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class LoginFrame extends JFrame {

    private final App app;
    private final JTextField username = Ui.field();
    private final JPasswordField password = Ui.password();
    private final JLabel message = Ui.label(" ", Ui.SMALL, Ui.RED);
    private final JLabel signupLink = Ui.label("<html>New here? <u>Create a customer account</u></html>", Ui.BODY, Ui.TEAL);
    private final RoleSwitch roleSwitch = new RoleSwitch();
    private final JButton loginButton = Ui.button("Log in", Ui.Style.ACCENT);

    public LoginFrame(App app) {
        this.app = app;
        setTitle("Travel Agency Management System");
        setIconImage(iconImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(createScene());
        add(createForm());
        setSize(1040, 640);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
    }

    static java.awt.Image iconImage() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Ui.TEAL);
        g.fillRoundRect(0, 0, 64, 64, 18, 18);
        Ui.logo(48, Color.WHITE, Ui.CORAL).paintIcon(null, g, 8, 8);
        g.dispose();
        return img;
    }

    /** Left half: a drawn travel scene with the app name. */
    private JPanel createScene() {
        JPanel scene = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.smooth(g);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(19, 132, 122), 0, h, Ui.TEAL_DARK));
                g2.fillRect(0, 0, w, h);
                // Sun
                g2.setColor(new Color(255, 196, 150));
                g2.fillOval(w - 170, 70, 90, 90);
                // Mountains
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillPolygon(new int[]{-40, w / 3, w * 2 / 3 + 40, w + 40}, new int[]{h, h / 2 + 30, h, h}, 4);
                g2.setColor(new Color(9, 60, 57, 150));
                g2.fillPolygon(new int[]{w / 4, w * 3 / 4, w + 60}, new int[]{h, h / 2 + 80, h}, 3);
                g2.setColor(new Color(255, 255, 255, 190));
                g2.fillPolygon(new int[]{w * 3 / 4 - 34, w * 3 / 4, w * 3 / 4 + 38}, new int[]{h / 2 + 118, h / 2 + 80, h / 2 + 122}, 3);
                // Dashed flight path and plane
                g2.setColor(new Color(255, 255, 255, 170));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 9f}, 0f));
                g2.drawArc(40, 150, w - 150, 260, 20, 140);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Ui.CORAL);
                int px = w - 150;
                int py = 186;
                g2.fillPolygon(new int[]{px, px + 34, px + 6}, new int[]{py, py + 8, py + 18}, 3);
                g2.fillPolygon(new int[]{px + 10, px + 18, px + 24}, new int[]{py + 12, py + 30, py + 12}, 3);
                g2.dispose();
            }
        };
        JPanel text = new JPanel(new GridBagLayout());
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(40, 44, 40, 44));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 18, 0);
        JLabel brand = Ui.label("  Travel Agency", new Font("Segoe UI", Font.BOLD, 18), Color.WHITE);
        brand.setIcon(Ui.logo(34, Color.WHITE, Ui.CORAL));
        text.add(brand, c);
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.insets = new Insets(0, 0, 10, 0);
        text.add(Ui.label("<html>Plan every trip<br>in one place.</html>", Ui.DISPLAY, Color.WHITE), c);
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        text.add(Ui.label("<html>Tour packages, bus tickets and hotel stays,<br>backed by a MySQL database.</html>",
                Ui.BODY, Ui.TEAL_LIGHT), c);
        scene.add(text, BorderLayout.CENTER);
        return scene;
    }

    /** Right half: role switch and login form. */
    private JPanel createForm() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Ui.SAND);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(360, 470));

        JCheckBox show = new JCheckBox("Show password");
        show.setFont(Ui.SMALL);
        show.setOpaque(false);
        show.setForeground(Ui.MUTED);
        char echo = password.getEchoChar();
        show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : echo));

        loginButton.addActionListener(e -> login());
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SignupDialog.show(LoginFrame.this, app).ifPresent(customer -> openCustomer(customer));
            }
        });
        JLabel dbLink = Ui.label("<html><u>Database settings</u></html>", Ui.SMALL, Ui.MUTED);
        dbLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dbLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DatabaseSetupDialog.show(LoginFrame.this, app.db);
            }
        });
        roleSwitch.onChange = () -> {
            signupLink.setVisible(!roleSwitch.admin);
            message.setText(" ");
            username.requestFocusInWindow();
        };

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(Ui.label("Welcome aboard", Ui.TITLE, Ui.INK), c);
        c.insets = new Insets(0, 0, 22, 0);
        form.add(Ui.label("Log in to continue your journey.", Ui.BODY, Ui.MUTED), c);
        form.add(roleSwitch, c);
        c.insets = new Insets(0, 0, 14, 0);
        form.add(Ui.field("Username", username), c);
        c.insets = new Insets(0, 0, 2, 0);
        form.add(Ui.field("Password", password), c);
        form.add(show, c);
        c.insets = new Insets(6, 0, 10, 0);
        form.add(message, c);
        loginButton.setPreferredSize(new Dimension(100, 44));
        c.insets = new Insets(0, 0, 18, 0);
        form.add(loginButton, c);
        signupLink.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(signupLink, c);
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.NONE;
        form.add(dbLink, c);
        outer.add(form);
        return outer;
    }

    private void login() {
        String user = username.getText().trim();
        char[] pw = password.getPassword();
        if (user.isEmpty() || pw.length == 0) {
            message.setText("Please enter your username and password.");
            return;
        }
        try {
            if (roleSwitch.admin) {
                Models.Admin admin = app.auth.loginAdmin(user, pw);
                if (admin == null) {
                    failed();
                    return;
                }
                new AdminPortal(app, admin).setVisible(true);
            } else {
                Models.Customer customer = app.auth.loginCustomer(user, pw);
                if (customer == null) {
                    failed();
                    return;
                }
                openCustomer(customer);
                return;
            }
            dispose();
        } catch (SQLException ex) {
            message.setText("<html>" + Database.friendly(ex) + "</html>");
        }
    }

    private void openCustomer(Models.Customer customer) {
        new CustomerPortal(app, customer).setVisible(true);
        dispose();
    }

    private void failed() {
        password.setText("");
        message.setText("Wrong username or password.");
        password.requestFocusInWindow();
    }

    /** Two-option switch: Customer | Admin. */
    private static class RoleSwitch extends JPanel {
        boolean admin;
        Runnable onChange = () -> { };

        RoleSwitch() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    boolean clickedAdmin = e.getX() > getWidth() / 2;
                    if (clickedAdmin != admin) {
                        admin = clickedAdmin;
                        repaint();
                        onChange.run();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Ui.TEAL_LIGHT);
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(Ui.WHITE);
            int x = admin ? w / 2 : 0;
            g2.fillRoundRect(x + 4, 4, w / 2 - 8, h - 8, h - 8, h - 8);
            g2.setFont(Ui.LABEL);
            String[] labels = {"Customer", "Admin"};
            for (int i = 0; i < 2; i++) {
                boolean active = (i == 1) == admin;
                g2.setColor(active ? Ui.TEAL_DARK : Ui.MUTED);
                int tw = g2.getFontMetrics().stringWidth(labels[i]);
                g2.drawString(labels[i], i * w / 2 + (w / 2 - tw) / 2, h / 2 + 5);
            }
            g2.dispose();
        }
    }
}
