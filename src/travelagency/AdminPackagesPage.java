package travelagency;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import travelagency.Models.TravelPackage;

/**
 * Admin: create, edit, deactivate and delete travel packages.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminPackagesPage extends CrudPage<TravelPackage> {

    private final JTextField name = Ui.field();
    private final JComboBox<String> type = Ui.combo("Adventure", "Family", "Economy", "Standard", "Luxury", "Honeymoon");
    private final JTextField destination = Ui.field();
    private final JTextArea description = Ui.area(3);
    private final JSpinner start = Ui.datePicker(LocalDate.now().plusDays(14));
    private final JSpinner end = Ui.datePicker(LocalDate.now().plusDays(18));
    private final JTextField price = Ui.field();
    private final JTextField seats = Ui.field();
    private final JComboBox<String> meal = Ui.combo("None", "Breakfast", "Half Board", "Full Board");
    private final JCheckBox flight = check("Flight");
    private final JCheckBox hotel = check("Hotel");
    private final JCheckBox transport = check("Transport");
    private final JCheckBox active = check("Active (visible to customers)");

    public AdminPackagesPage(AdminPortal portal) {
        super(portal, "Travel packages", "Package",
                new String[]{"ID", "Package", "Type", "Starts", "Days", "Price", "Seats left", "Active"},
                new Class<?>[]{Integer.class, String.class, String.class, LocalDate.class, Long.class,
                    Double.class, String.class, Boolean.class});
        build();
    }

    static JCheckBox check(String text) {
        JCheckBox c = new JCheckBox(text);
        c.setFont(Ui.BODY);
        c.setOpaque(false);
        c.setForeground(Ui.INK);
        return c;
    }

    @Override
    protected List<TravelPackage> load() throws SQLException {
        return portal.app().catalog.allPackages();
    }

    @Override
    protected Object[] toRow(TravelPackage p) {
        return new Object[]{p.id(), p.name(), p.type(), p.startDate(), p.days(), p.pricePerPerson(),
            p.seatsLeft() + " / " + p.totalSeats(), p.active()};
    }

    @Override
    protected JComponent form() {
        JPanel includes = Ui.row(flight, hotel, transport);
        return formOf(Ui.field("Package name", name),
                Ui.row(Ui.field("Type", type), Ui.field("Destination", destination)),
                Ui.field("Description", description),
                Ui.row(Ui.field("Start date", start), Ui.field("End date", end)),
                Ui.row(Ui.field("Price / person (Rs)", price), Ui.field("Total seats", seats)),
                Ui.field("Meal plan", meal),
                Ui.field("Included", includes),
                active);
    }

    @Override
    protected void fillForm(TravelPackage p) {
        name.setText(p.name());
        type.setSelectedItem(p.type());
        destination.setText(p.destination());
        description.setText(p.description());
        Ui.setDate(start, p.startDate());
        Ui.setDate(end, p.endDate());
        price.setText(String.valueOf((long) p.pricePerPerson()));
        seats.setText(String.valueOf(p.totalSeats()));
        meal.setSelectedItem(p.mealPlan());
        flight.setSelected(p.flight());
        hotel.setSelected(p.hotel());
        transport.setSelected(p.transport());
        active.setSelected(p.active());
    }

    @Override
    protected void clearForm() {
        name.setText("");
        destination.setText("");
        description.setText("");
        Ui.setDate(start, LocalDate.now().plusDays(14));
        Ui.setDate(end, LocalDate.now().plusDays(18));
        price.setText("");
        seats.setText("20");
        meal.setSelectedItem("Breakfast");
        flight.setSelected(false);
        hotel.setSelected(true);
        transport.setSelected(true);
        active.setSelected(true);
    }

    @Override
    protected void save(TravelPackage p) throws SQLException {
        portal.app().catalog.savePackage(p == null ? null : p.id(),
                Validate.text(name.getText(), "Package name", 3, 100), (String) type.getSelectedItem(),
                Validate.name(destination.getText(), "Destination"),
                Validate.text(description.getText(), "Description", 0, 1000), Ui.date(start), Ui.date(end),
                Validate.money(price.getText(), "Price per person"), Validate.positiveInt(seats.getText(), "Total seats", 500),
                flight.isSelected(), hotel.isSelected(), transport.isSelected(), (String) meal.getSelectedItem(),
                active.isSelected(), portal.admin().id());
    }

    @Override
    protected void delete(TravelPackage p) throws SQLException {
        portal.app().catalog.deletePackage(p.id());
    }

    @Override
    protected String describe(TravelPackage p) {
        return "the package \"" + p.name() + "\"";
    }
}
