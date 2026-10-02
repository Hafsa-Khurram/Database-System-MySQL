package travelagency;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import travelagency.Models.Customer;

/**
 * "My Profile": update personal details and change the password.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class ProfilePage extends JPanel implements PortalFrame.Refreshable {

    private final CustomerPortal portal;
    private final JTextField first = Ui.field();
    private final JTextField last = Ui.field();
    private final JTextField email = Ui.field();
    private final JTextField phone = Ui.field();
    private final JComboBox<String> gender = Ui.combo("Female", "Male", "Other");
    private final JTextField address = Ui.field();
    private final JLabel username = Ui.label(" ", Ui.LABEL, Ui.TEAL_DARK);
    private final JPasswordField current = Ui.password();
    private final JPasswordField fresh = Ui.password();
    private final JPasswordField confirm = Ui.password();

    public ProfilePage(CustomerPortal portal) {
        super(new GridBagLayout());
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.6;
        c.insets = new Insets(0, 0, 0, 12);
        add(detailsCard(), c);
        c.weightx = 0.4;
        c.insets = new Insets(0, 12, 0, 0);
        add(passwordCard(), c);
    }

    private JComponent detailsCard() {
        Ui.Card card = new Ui.Card(new GridBagLayout());
        card.setPreferredSize(new java.awt.Dimension(100, 100));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(Ui.label("Personal details", Ui.TITLE, Ui.INK), c);
        c.insets = new Insets(0, 0, 20, 0);
        card.add(username, c);
        c.insets = new Insets(0, 0, 14, 0);
        card.add(Ui.row(Ui.field("First name", first), Ui.field("Last name", last)), c);
        card.add(Ui.row(Ui.field("Email", email), Ui.field("Phone", phone)), c);
        card.add(Ui.row(Ui.field("Gender", gender), Ui.field("City / address", address)), c);
        JButton save = Ui.button("Save details", Ui.Style.PRIMARY);
        save.addActionListener(e -> saveDetails());
        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(save);
        card.add(right, c);
        c.weighty = 1;
        card.add(new JLabel(), c);
        return card;
    }

    private JComponent passwordCard() {
        Ui.Card card = new Ui.Card(new GridBagLayout());
        card.setPreferredSize(new java.awt.Dimension(100, 100));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(Ui.label("Change password", Ui.TITLE, Ui.INK), c);
        c.insets = new Insets(0, 0, 20, 0);
        card.add(Ui.label("Use at least 6 characters.", Ui.BODY, Ui.MUTED), c);
        c.insets = new Insets(0, 0, 14, 0);
        card.add(Ui.field("Current password", current), c);
        card.add(Ui.field("New password", fresh), c);
        card.add(Ui.field("Confirm new password", confirm), c);
        JButton change = Ui.button("Update password", Ui.Style.ACCENT);
        change.addActionListener(e -> changePassword());
        card.add(change, c);
        c.weighty = 1;
        card.add(new JLabel(), c);
        return card;
    }

    @Override
    public void refresh() {
        Customer cu = portal.customer();
        first.setText(cu.firstName());
        last.setText(cu.lastName());
        email.setText(cu.email());
        phone.setText(cu.phone());
        gender.setSelectedItem(cu.gender());
        address.setText(cu.address() == null ? "" : cu.address());
        username.setText("Username: " + cu.username());
    }

    private void saveDetails() {
        try {
            Customer updated = portal.app().auth.updateCustomerProfile(portal.customer().id(), first.getText(),
                    last.getText(), email.getText(), phone.getText(), (String) gender.getSelectedItem(), address.getText());
            portal.setCustomer(updated);
            refresh();
            Ui.info(this, "Your details are saved.");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }

    private void changePassword() {
        try {
            portal.app().auth.changeCustomerPassword(portal.customer().id(), current.getPassword(), fresh.getPassword(),
                    confirm.getPassword());
            current.setText("");
            fresh.setText("");
            confirm.setText("");
            Ui.info(this, "Your password is changed. Use it the next time you log in.");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }
}
