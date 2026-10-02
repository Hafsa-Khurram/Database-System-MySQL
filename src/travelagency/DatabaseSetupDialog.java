package travelagency;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Asks for the MySQL connection details, tests them, and creates the
 * travel_agency database the first time. The details are saved in
 * database.properties so this appears only when something is wrong.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class DatabaseSetupDialog extends JDialog {

    private final Database db;
    private final JTextField host = Ui.field();
    private final JTextField port = Ui.field();
    private final JTextField name = Ui.field();
    private final JTextField user = Ui.field();
    private final JPasswordField password = Ui.password();
    private final JLabel status = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private boolean connected;

    /** Shows the dialog. Returns true once the database is ready. */
    public static boolean show(Component parent, Database db) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        DatabaseSetupDialog d = new DatabaseSetupDialog(owner, db);
        d.setVisible(true);
        return d.connected;
    }

    private DatabaseSetupDialog(Window owner, Database db) {
        super(owner, "MySQL Database Settings", ModalityType.APPLICATION_MODAL);
        this.db = db;
        host.setText(db.getHost());
        port.setText(String.valueOf(db.getPort()));
        name.setText(db.getName());
        user.setText(db.getUser());
        password.setText(db.getPassword());

        Ui.Gradient header = new Ui.Gradient(new BorderLayout(12, 0), Ui.TEAL, Ui.TEAL_DARK, false);
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        header.add(new JLabel(Ui.logo(40, Ui.WHITE, Ui.CORAL)), BorderLayout.WEST);
        JPanel titles = new JPanel(new java.awt.GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Ui.label("Connect to MySQL", Ui.TITLE, Ui.WHITE));
        titles.add(Ui.label("Enter the details you use in MySQL Workbench.", Ui.BODY, Ui.TEAL_LIGHT));
        header.add(titles, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(Ui.row(Ui.field("Host", host), Ui.field("Port", port)), c);
        form.add(Ui.row(Ui.field("MySQL username", user), Ui.field("MySQL password", password)), c);
        form.add(Ui.field("Database name (created automatically)", name), c);
        c.insets = new Insets(0, 0, 0, 0);
        form.add(status, c);

        JButton test = Ui.button("Test Connection", Ui.Style.OUTLINE);
        JButton ok = Ui.button("Connect & Continue", Ui.Style.PRIMARY);
        JButton cancel = Ui.button("Exit", Ui.Style.LIGHT);
        test.addActionListener(e -> test());
        ok.addActionListener(e -> connectAndInstall());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttons.setBackground(Ui.WHITE);
        buttons.add(cancel);
        buttons.add(test);
        buttons.add(ok);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.WHITE);
        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        setSize(new Dimension(560, 460));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private boolean readForm() {
        try {
            int p = Integer.parseInt(port.getText().trim());
            if (host.getText().isBlank() || user.getText().isBlank() || !name.getText().trim().matches("[A-Za-z0-9_]{1,64}")) {
                throw new NumberFormatException();
            }
            db.configure(host.getText(), p, name.getText(), user.getText(), new String(password.getPassword()));
            return true;
        } catch (NumberFormatException ex) {
            showStatus("Please fill in all fields. Port is a number (usually 3306); the database name uses letters, "
                    + "numbers and _ only.", true);
            return false;
        }
    }

    private void test() {
        if (!readForm()) {
            return;
        }
        run(() -> {
            db.testServer();
            showStatus(db.isInstalled() ? "Connected. The database is ready."
                    : "Connected. The database will be created when you continue.", false);
        });
    }

    private void connectAndInstall() {
        if (!readForm()) {
            return;
        }
        run(() -> {
            db.testServer();
            if (!db.isInstalled()) {
                showStatus("Creating the database and tables...", false);
                db.install();
            }
            db.saveToFile();
            connected = true;
            SwingUtilities.invokeLater(this::dispose);
        });
    }

    private interface Work {
        void run() throws SQLException, IOException;
    }

    /** Runs database work off the screen thread so the window stays responsive. */
    private void run(Work work) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        showStatus("Connecting...", false);
        new Thread(() -> {
            try {
                work.run();
            } catch (SQLException ex) {
                showStatus(Database.friendly(ex), true);
            } catch (IOException ex) {
                showStatus("Could not read or save a file: " + ex.getMessage(), true);
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    private void showStatus(String text, boolean error) {
        SwingUtilities.invokeLater(() -> {
            status.setText("<html><body style='width:470px'>" + text + "</body></html>");
            status.setForeground(error ? Ui.RED : Ui.TEAL_DARK);
        });
    }
}
