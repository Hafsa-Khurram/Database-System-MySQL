package travelagency;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import travelagency.Models.Customer;

/**
 * Customer sign-up form. On success the new customer is logged in straight away.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class SignupDialog extends JDialog {

    private final App app;
    private final JTextField first = Ui.field();
    private final JTextField last = Ui.field();
    private final JTextField email = Ui.field();
    private final JTextField phone = Ui.field();
    private final JComboBox<String> gender = Ui.combo("Female", "Male", "Other");
    private final JTextField address = Ui.field();
    private final JTextField username = Ui.field();
    private final JPasswordField password = Ui.password();
    private final JPasswordField confirm = Ui.password();
    private Customer created;

    public static Optional<Customer> show(JFrame owner, App app) {
        SignupDialog d = new SignupDialog(owner, app);
        d.setVisible(true);
        return Optional.ofNullable(d.created);
    }

    private SignupDialog(JFrame owner, App app) {
        super(owner, "Create your account", true);
        this.app = app;

        Ui.Gradient header = new Ui.Gradient(new BorderLayout(), Ui.TEAL, Ui.TEAL_DARK, true);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        header.add(Ui.label("Create your account", Ui.TITLE, Ui.WHITE), BorderLayout.NORTH);
        header.add(Ui.label("Book tours, buses and hotels in minutes.", Ui.BODY, Ui.TEAL_LIGHT), BorderLayout.SOUTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 24, 6, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(Ui.row(Ui.field("First name", first), Ui.field("Last name", last)), c);
        form.add(Ui.row(Ui.field("Email", email), Ui.field("Phone", phone)), c);
        form.add(Ui.row(Ui.field("Gender", gender), Ui.field("City / address", address)), c);
        form.add(Ui.field("Username", username), c);
        form.add(Ui.row(Ui.field("Password (6+ characters)", password), Ui.field("Confirm password", confirm)), c);

        JButton cancel = Ui.button("Cancel", Ui.Style.LIGHT);
        JButton create = Ui.button("Create Account", Ui.Style.ACCENT);
        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> create());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttons.setBackground(Ui.WHITE);
        buttons.add(cancel);
        buttons.add(create);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.WHITE);
        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(create);
        setSize(580, 620);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void create() {
        try {
            Validate.password(password.getPassword(), confirm.getPassword());
            created = app.auth.registerCustomer(first.getText(), last.getText(), email.getText(), phone.getText(),
                    (String) gender.getSelectedItem(), address.getText(), username.getText(), password.getPassword());
            Ui.info(this, "Welcome, " + created.firstName() + "! Your account is ready.");
            dispose();
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }
}
