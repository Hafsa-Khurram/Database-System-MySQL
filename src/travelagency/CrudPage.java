package travelagency;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Shared layout for admin screens that add, edit and delete records:
 * a table on the left and a form on the right. Subclasses describe the
 * columns and the form; this class handles selection, saving and deleting.
 *
 * @param <T> the record type shown in the table
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public abstract class CrudPage<T> extends JPanel implements PortalFrame.Refreshable {

    protected final AdminPortal portal;
    protected final DefaultTableModel model;
    protected final JTable table;
    private final JLabel formTitle = Ui.label(" ", Ui.HEADING, Ui.INK);
    private final JLabel count = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final String singular;
    private final List<T> rows = new ArrayList<>();
    private T editing;

    protected CrudPage(AdminPortal portal, String title, String singular, String[] columns, Class<?>[] types) {
        super(new BorderLayout(0, 16));
        this.portal = portal;
        this.singular = singular;
        this.model = Ui.model(columns, types);
        this.table = Ui.table(model, "No " + title.toLowerCase() + " yet. Use the form to add one.");
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(Ui.label(title, Ui.TITLE, Ui.INK));
        titles.add(count);
        add(titles, BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                editing = rows.get(table.convertRowIndexToModel(table.getSelectedRow()));
                formTitle.setText("Edit " + singular.toLowerCase());
                fillForm(editing);
            }
        });
    }

    /** Call at the end of the subclass constructor, after its fields exist. */
    protected void build() {
        Ui.Card tableCard = new Ui.Card(new BorderLayout(0, 10));
        tableCard.add(Ui.scroll(table), BorderLayout.CENTER);
        tableCard.add(Ui.label("Click a row to edit it.", Ui.SMALL, Ui.MUTED), BorderLayout.SOUTH);

        Ui.Card formCard = new Ui.Card(new BorderLayout(0, 12));
        formCard.add(formTitle, BorderLayout.NORTH);
        formCard.add(Ui.clearScroll(form()), BorderLayout.CENTER);
        JButton add = Ui.button("New", Ui.Style.LIGHT);
        JButton save = Ui.button("Save", Ui.Style.PRIMARY);
        JButton delete = Ui.button("Delete", Ui.Style.DANGER);
        add.addActionListener(e -> clear());
        save.addActionListener(e -> save());
        delete.addActionListener(e -> delete());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(delete);
        buttons.add(save);
        formCard.add(buttons, BorderLayout.SOUTH);
        formCard.setPreferredSize(new Dimension(formWidth(), 100));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 12);
        tableCard.setPreferredSize(new Dimension(100, 100));
        center.add(tableCard, c);
        c.weightx = 0;
        c.insets = new Insets(0, 12, 0, 0);
        center.add(formCard, c);
        add(center, BorderLayout.CENTER);
        clear();
    }

    /** Builds a vertical form from rows of fields. */
    protected static JPanel formOf(JComponent... rowsOfFields) {
        JPanel p = new FitWidth();
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 12, 0);
        for (JComponent r : rowsOfFields) {
            p.add(r, c);
        }
        c.weighty = 1;
        p.add(new JLabel(), c);
        keepHeights(p);
        return p;
    }

    /**
     * When the form is narrower than it would like, GridBagLayout falls back to minimum
     * sizes. Giving inputs a minimum height equal to their normal height keeps them
     * from being squashed; only their width shrinks.
     */
    private static void keepHeights(java.awt.Container parent) {
        for (java.awt.Component child : parent.getComponents()) {
            if (child instanceof JPanel panel) {
                keepHeights(panel);
            } else if (child instanceof JComponent jc) {
                jc.setMinimumSize(new Dimension(40, jc.getPreferredSize().height));
            }
        }
    }

    /** A panel that always takes the width of its scroll pane, so fields shrink instead of being cut off. */
    static class FitWidth extends JPanel implements javax.swing.Scrollable {
        FitWidth() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return r.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getPreferredSize().height <= getParent().getHeight();
        }
    }

    protected int formWidth() {
        return 380;
    }

    protected abstract List<T> load() throws SQLException;

    protected abstract Object[] toRow(T item);

    protected abstract JComponent form();

    protected abstract void fillForm(T item);

    protected abstract void clearForm();

    /** Insert when item is null, otherwise update it. */
    protected abstract void save(T item) throws SQLException;

    protected abstract void delete(T item) throws SQLException;

    protected abstract String describe(T item);

    @Override
    public void refresh() {
        try {
            List<T> loaded = load();
            rows.clear();
            rows.addAll(loaded);
            model.setRowCount(0);
            for (T item : rows) {
                model.addRow(toRow(item));
            }
            if (!rows.isEmpty()) {
                Ui.fitColumns(table, 190);
            }
            count.setText(rows.size() + " record(s) in the database");
        } catch (SQLException ex) {
            Ui.error(this, ex);
        }
    }

    protected void clear() {
        editing = null;
        table.clearSelection();
        formTitle.setText("Add new " + singular.toLowerCase());
        clearForm();
    }

    private void save() {
        boolean isNew = editing == null;
        try {
            save(editing);
            refresh();
            clear();
            Ui.info(this, singular + (isNew ? " added." : " updated."));
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }

    private void delete() {
        if (editing == null) {
            Ui.info(this, "Click a row in the table first.");
            return;
        }
        if (!Ui.confirm(this, "Delete " + describe(editing) + "?\nThis cannot be undone.", "Delete " + singular.toLowerCase())) {
            return;
        }
        try {
            delete(editing);
            refresh();
            clear();
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.error(this, ex);
        }
    }
}
