package travelagency;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Checkout: shows what is being booked, the total, and asks how the customer pays.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class PaymentDialog extends JDialog {

    private final JComboBox<String> method = Ui.combo(BookingRepository.PAYMENT_METHODS);
    private String chosen;

    /** Returns the payment method, or null if the customer cancelled. */
    public static String ask(Component parent, String title, List<String[]> lines, double total) {
        PaymentDialog d = new PaymentDialog(SwingUtilities.getWindowAncestor(parent), title, lines, total);
        d.setVisible(true);
        return d.chosen;
    }

    private PaymentDialog(Window owner, String title, List<String[]> lines, double total) {
        super(owner, "Checkout", ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.WHITE);

        Ui.Gradient header = new Ui.Gradient(new BorderLayout(), Ui.TEAL, Ui.TEAL_DARK, true);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        header.add(Ui.label("Checkout", Ui.SMALL, Ui.TEAL_LIGHT), BorderLayout.NORTH);
        header.add(Ui.label(title, Ui.TITLE, Ui.WHITE), BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Ui.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        for (String[] line : lines) {
            c.gridx = 0;
            c.weightx = 0.4;
            body.add(Ui.label(line[0], Ui.BODY, Ui.MUTED), c);
            c.gridx = 1;
            c.weightx = 0.6;
            body.add(Ui.label(line[1], Ui.LABEL, Ui.INK), c);
            c.gridy++;
        }
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(8, 0, 14, 0);
        JPanel totalRow = new Ui.Card(new BorderLayout(), Ui.CORAL_LIGHT);
        totalRow.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));
        totalRow.add(Ui.label("Total to pay", Ui.LABEL, Ui.CORAL_DARK), BorderLayout.WEST);
        totalRow.add(Ui.label(Ui.money(total), Ui.NUMBER, Ui.CORAL_DARK), BorderLayout.EAST);
        c.gridy++;
        body.add(totalRow, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(Ui.field("Payment method", method), c);
        root.add(body, BorderLayout.CENTER);

        JButton cancel = Ui.button("Cancel", Ui.Style.LIGHT);
        JButton pay = Ui.button("Pay & Book", Ui.Style.ACCENT);
        cancel.addActionListener(e -> dispose());
        pay.addActionListener(e -> {
            chosen = (String) method.getSelectedItem();
            dispose();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 16));
        buttons.setBackground(Ui.WHITE);
        buttons.add(cancel);
        buttons.add(pay);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(pay);
        pack();
        setSize(Math.max(getWidth(), 480), getHeight());
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
