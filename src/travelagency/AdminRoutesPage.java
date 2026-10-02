package travelagency;

import java.sql.SQLException;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import travelagency.Models.BusRoute;

/**
 * Admin: bus routes with their company, departure times, seats and fares.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminRoutesPage extends CrudPage<BusRoute> {

    private final JTextField from = Ui.field();
    private final JTextField to = Ui.field();
    private final JTextField company = Ui.field();
    private final JTextField times = Ui.field();
    private final JTextField ecoSeats = Ui.field();
    private final JTextField bizSeats = Ui.field();
    private final JTextField ecoFare = Ui.field();
    private final JTextField bizFare = Ui.field();
    private final JCheckBox active = AdminPackagesPage.check("Active (customers can book)");

    public AdminRoutesPage(AdminPortal portal) {
        super(portal, "Bus routes", "Route",
                new String[]{"ID", "From", "To", "Company", "Departures", "Seats", "Fares E / B", "Active"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, String.class,
                    String.class, Boolean.class});
        times.setToolTipText("24-hour times separated by ; for example 08:00; 14:30; 21:00");
        build();
    }

    @Override
    protected List<BusRoute> load() throws SQLException {
        return portal.app().catalog.allRoutes();
    }

    @Override
    protected Object[] toRow(BusRoute r) {
        return new Object[]{r.id(), r.from(), r.to(), r.company(), String.join(", ", r.times()),
            r.economySeats() + " / " + r.businessSeats(),
            Ui.money(r.economyFare()) + " / " + Ui.money(r.businessFare()).substring(3), r.active()};
    }

    @Override
    protected JComponent form() {
        return formOf(Ui.row(Ui.field("From city", from), Ui.field("To city", to)),
                Ui.field("Bus company", company),
                Ui.field("Departure times (24h, separated by ;)", times),
                Ui.row(Ui.field("Economy seats", ecoSeats), Ui.field("Business seats", bizSeats)),
                Ui.row(Ui.field("Economy fare (Rs)", ecoFare), Ui.field("Business fare (Rs)", bizFare)),
                active);
    }

    @Override
    protected void fillForm(BusRoute r) {
        from.setText(r.from());
        to.setText(r.to());
        company.setText(r.company());
        times.setText(String.join("; ", r.times()));
        times.setCaretPosition(0);
        ecoSeats.setText(String.valueOf(r.economySeats()));
        bizSeats.setText(String.valueOf(r.businessSeats()));
        ecoFare.setText(String.valueOf((long) r.economyFare()));
        bizFare.setText(String.valueOf((long) r.businessFare()));
        active.setSelected(r.active());
    }

    @Override
    protected void clearForm() {
        for (JTextField f : new JTextField[]{from, to, company, ecoFare, bizFare}) {
            f.setText("");
        }
        times.setText("08:00; 14:00; 20:00");
        ecoSeats.setText("32");
        bizSeats.setText("8");
        active.setSelected(true);
    }

    @Override
    protected void save(BusRoute r) throws SQLException {
        double eco = Validate.money(ecoFare.getText(), "Economy fare");
        double biz = Validate.money(bizFare.getText(), "Business fare");
        if (biz < eco) {
            throw new IllegalArgumentException("The Business fare should not be lower than the Economy fare.");
        }
        portal.app().catalog.saveRoute(r == null ? null : r.id(), Validate.name(from.getText(), "From city"),
                Validate.name(to.getText(), "To city"), Validate.text(company.getText(), "Bus company", 2, 80),
                Validate.times(times.getText()), Validate.nonNegativeInt(ecoSeats.getText(), "Economy seats", 80),
                Validate.nonNegativeInt(bizSeats.getText(), "Business seats", 40), eco, biz, active.isSelected());
    }

    @Override
    protected void delete(BusRoute r) throws SQLException {
        portal.app().catalog.deleteRoute(r.id());
    }

    @Override
    protected String describe(BusRoute r) {
        return "the route " + r.from() + " to " + r.to();
    }
}
