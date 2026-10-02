package travelagency;

import java.sql.SQLException;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import travelagency.Models.Hotel;

/**
 * Admin: hotels with their rooms and nightly rates.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminHotelsPage extends CrudPage<Hotel> {

    private final JTextField name = Ui.field();
    private final JTextField city = Ui.field();
    private final JTextField address = Ui.field();
    private final JComboBox<Integer> stars = Ui.combo(1, 2, 3, 4, 5);
    private final JTextField singleRooms = Ui.field();
    private final JTextField doubleRooms = Ui.field();
    private final JTextField suiteRooms = Ui.field();
    private final JTextField singleRate = Ui.field();
    private final JTextField doubleRate = Ui.field();
    private final JTextField suiteRate = Ui.field();
    private final JCheckBox active = AdminPackagesPage.check("Active (customers can book)");

    public AdminHotelsPage(AdminPortal portal) {
        super(portal, "Hotels", "Hotel",
                new String[]{"ID", "Hotel", "City", "Stars", "Rooms", "Rs per night S / D / Su", "Active"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, String.class,
                    Boolean.class});
        build();
    }

    @Override
    protected List<Hotel> load() throws SQLException {
        return portal.app().catalog.allHotels();
    }

    @Override
    protected Object[] toRow(Hotel h) {
        return new Object[]{h.id(), h.name(), h.city(), "★".repeat(h.stars()),
            h.singleRooms() + " / " + h.doubleRooms() + " / " + h.suiteRooms(),
            Ui.money(h.singleRate()).substring(3) + " / " + Ui.money(h.doubleRate()).substring(3) + " / "
                    + Ui.money(h.suiteRate()).substring(3), h.active()};
    }

    @Override
    protected JComponent form() {
        return formOf(Ui.field("Hotel name", name),
                Ui.row(Ui.field("City", city), Ui.field("Star rating", stars)),
                Ui.field("Address", address),
                Ui.row(Ui.field("Singles", singleRooms), Ui.field("Doubles", doubleRooms),
                        Ui.field("Suites", suiteRooms)),
                Ui.row(Ui.field("Single rate", singleRate), Ui.field("Double rate", doubleRate),
                        Ui.field("Suite rate", suiteRate)),
                active);
    }

    @Override
    protected void fillForm(Hotel h) {
        name.setText(h.name());
        city.setText(h.city());
        address.setText(h.address());
        stars.setSelectedItem(h.stars());
        singleRooms.setText(String.valueOf(h.singleRooms()));
        doubleRooms.setText(String.valueOf(h.doubleRooms()));
        suiteRooms.setText(String.valueOf(h.suiteRooms()));
        singleRate.setText(String.valueOf((long) h.singleRate()));
        doubleRate.setText(String.valueOf((long) h.doubleRate()));
        suiteRate.setText(String.valueOf((long) h.suiteRate()));
        active.setSelected(h.active());
    }

    @Override
    protected void clearForm() {
        for (JTextField f : new JTextField[]{name, city, address, singleRate, doubleRate, suiteRate}) {
            f.setText("");
        }
        stars.setSelectedItem(3);
        singleRooms.setText("10");
        doubleRooms.setText("15");
        suiteRooms.setText("2");
        active.setSelected(true);
    }

    @Override
    protected void save(Hotel h) throws SQLException {
        double r1 = Validate.money(singleRate.getText(), "Single rate");
        double r2 = Validate.money(doubleRate.getText(), "Double rate");
        double r3 = Validate.money(suiteRate.getText(), "Suite rate");
        int s = Validate.nonNegativeInt(singleRooms.getText(), "Single rooms", 500);
        int d = Validate.nonNegativeInt(doubleRooms.getText(), "Double rooms", 500);
        int su = Validate.nonNegativeInt(suiteRooms.getText(), "Suites", 200);
        if (s + d + su == 0) {
            throw new IllegalArgumentException("The hotel needs at least one room.");
        }
        portal.app().catalog.saveHotel(h == null ? null : h.id(), Validate.text(name.getText(), "Hotel name", 3, 100),
                Validate.name(city.getText(), "City"), Validate.text(address.getText(), "Address", 0, 255),
                (Integer) stars.getSelectedItem(), s, d, su, r1, r2, r3, active.isSelected());
    }

    @Override
    protected void delete(Hotel h) throws SQLException {
        portal.app().catalog.deleteHotel(h.id());
    }

    @Override
    protected String describe(Hotel h) {
        return "the hotel \"" + h.name() + "\"";
    }
}
