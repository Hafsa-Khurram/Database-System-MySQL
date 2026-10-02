package travelagency;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import travelagency.Models.Admin;

/**
 * Admin: change own password; the Super Admin can also add admins.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminSettingsPage extends JPanel implements PortalFrame.Refreshable {

    private final AdminPortal portal;
    private final JPasswordField current = Ui.password();
    private final JPasswordField fresh = Ui.password();
    private final JPasswordField confirm = Ui.password();
    private final JTextField username = Ui.field();
    private final JTextField fullName = Ui.field();
    private final JTextField email = Ui.field();
    private final JPasswordField newPass = Ui.password();
    private final JPasswordField newConfirm = Ui.password();
    private final JComboBox<String> role = Ui.combo("Admin", "Super Admin");
    private final DefaultTableModel admins = Ui.model(new String[]{"ID", "Username", "Name", "Email", "Role"},
            Integer.class, String.class, String.class, String.class, String.class);
    private final JTable adminTable = Ui.table(admins, "No admins.");

    public AdminSettingsPage(AdminPortal portal) {
        super(new GridBagLayout());
        this.portal = portal;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        Ui.widths(adminTable, 40, 100, 150, 200, 100);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.38;
        c.insets = new Insets(0, 0, 0, 9);
        add(passwordCard(), c);
        c.weightx = 0.62;
        c.insets = new Insets(0, 9, 0, 0);
        add(adminsCard(), c);
    }

    private JComponent passwordCard() {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 14));
        Admin a = portal.admin();
        JPanel head = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(Ui.label("My account", Ui.TITLE, Ui.INK));
        head.add(Ui.label(a.fullName() + "  ·  " + a.username() + "  ·  " + a.role(), Ui.SMALL, Ui.MUTED));
        card.add(head, BorderLayout.NORTH);
        JButton change = Ui.button("Update password", Ui.Style.ACCENT);
        change.addActionListener(e -> changePassword());
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btn.setOpaque(false);
        btn.add(change);
        card.add(CrudPage.formOf(Ui.label("Change password", Ui.HEADING, Ui.INK),
                Ui.field("Current password", current), Ui.field("New password (at least 6 characters)", fresh),
                Ui.field("Confirm new password", confirm), btn), BorderLayout.CENTER);
        return card;
    }

    private JComponent adminsCard() {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 14));
        card.add(Ui.label("Administrators", Ui.TITLE, Ui.INK), BorderLayout.NORTH);
        adminTable.setPreferredScrollableViewportSize(new java.awt.Dimension(400, 130));
        card.add(Ui.scroll(adminTable), BorderLayout.CENTER);

        boolean superAdmin = "Super Admin".equals(portal.admin().role());
        JButton add = Ui.button("Add admin", Ui.Style.PRIMARY);
        add.addActionListener(e -> addAdmin());
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btn.setOpaque(false);
        btn.add(add);
        JPanel form = CrudPage.formOf(Ui.label("Add a new admin", Ui.HEADING, Ui.INK),
                Ui.row(Ui.field("Username", username), Ui.field("Full name", fullName)),
                Ui.row(Ui.field("Email", email), Ui.field("Role", role)),
                Ui.row(Ui.field("Password", newPass), Ui.field("Confirm password", newConfirm)), btn);
        if (!superAdmin) {
            for (JComponent f : new JComponent[]{username, fullName, email, newPass, newConfirm, role, add}) {
                f.setEnabled(false);
            }
            add.setToolTipText("Only the Super Admin can add admins.");
        }
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.LINE),
                BorderFactory.createEmptyBorder(14, 2, 0, 2)));
        card.add(form, BorderLayout.SOUTH);
        return card;
    }

    @Override
    public void refresh() {
        try {
            admins.setRowCount(0);
            for (Admin a : portal.app().auth.listAdmins()) {
                admins.addRow(new Object[]{a.id(), a.username(), a.fullName(), a.email(), a.role()});
            }
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
    }

    private void changePassword() {
        try {
            portal.app().auth.changeAdminPassword(portal.admin().id(), current.getPassword(), fresh.getPassword(),
                    confirm.getPassword());
            current.setText("");
            fresh.setText("");
            confirm.setText("");
            Ui.info(this, "Your password is changed. Use it the next time you log in.");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }

    private void addAdmin() {
        try {
            portal.app().auth.addAdmin(portal.admin(), username.getText(), fullName.getText(), email.getText(),
                    newPass.getPassword(), newConfirm.getPassword(), (String) role.getSelectedItem());
            Ui.info(this, "Admin \"" + username.getText().trim() + "\" added. They can log in now.");
            for (JTextField f : new JTextField[]{username, fullName, email, newPass, newConfirm}) {
                f.setText("");
            }
            role.setSelectedIndex(0);
            refresh();
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }
}
