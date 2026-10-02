package travelagency;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import travelagency.Models.Employee;

/**
 * Admin: agency employees.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminEmployeesPage extends CrudPage<Employee> {

    private final JTextField first = Ui.field();
    private final JTextField last = Ui.field();
    private final JTextField email = Ui.field();
    private final JTextField phone = Ui.field();
    private final JTextField job = Ui.field();
    private final JComboBox<String> department = Ui.combo("Sales", "Operations", "Finance", "Customer Support",
            "Marketing", "Management");
    private final JSpinner hired = Ui.datePicker(LocalDate.now());
    private final JTextField salary = Ui.field();

    public AdminEmployeesPage(AdminPortal portal) {
        super(portal, "Employees", "Employee",
                new String[]{"ID", "Name", "Job title", "Department", "Email", "Hired", "Salary"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class,
                    LocalDate.class, Double.class});
        department.setEditable(false);
        build();
    }

    @Override
    protected List<Employee> load() throws SQLException {
        return portal.app().admin.employees();
    }

    @Override
    protected Object[] toRow(Employee e) {
        return new Object[]{e.id(), e.firstName() + " " + e.lastName(), e.jobTitle(), e.department(), e.email(),
            e.hireDate(), e.salary()};
    }

    @Override
    protected JComponent form() {
        return formOf(Ui.row(Ui.field("First name", first), Ui.field("Last name", last)),
                Ui.field("Email", email),
                Ui.row(Ui.field("Phone", phone), Ui.field("Hire date", hired)),
                Ui.row(Ui.field("Job title", job), Ui.field("Department", department)),
                Ui.field("Monthly salary (Rs)", salary));
    }

    @Override
    protected void fillForm(Employee e) {
        first.setText(e.firstName());
        last.setText(e.lastName());
        email.setText(e.email());
        phone.setText(e.phone());
        job.setText(e.jobTitle());
        department.setSelectedItem(e.department());
        Ui.setDate(hired, e.hireDate());
        salary.setText(String.valueOf((long) e.salary()));
    }

    @Override
    protected void clearForm() {
        for (JTextField f : new JTextField[]{first, last, email, phone, job, salary}) {
            f.setText("");
        }
        department.setSelectedIndex(0);
        Ui.setDate(hired, LocalDate.now());
    }

    @Override
    protected void save(Employee e) throws SQLException {
        portal.app().admin.saveEmployee(e == null ? null : e.id(), Validate.name(first.getText(), "First name"),
                Validate.name(last.getText(), "Last name"), Validate.email(email.getText()), Validate.phone(phone.getText()),
                Validate.text(job.getText(), "Job title", 2, 100), (String) department.getSelectedItem(), Ui.date(hired),
                Validate.money(salary.getText(), "Salary"));
    }

    @Override
    protected void delete(Employee e) throws SQLException {
        portal.app().admin.deleteEmployee(e.id());
    }

    @Override
    protected String describe(Employee e) {
        return e.firstName() + " " + e.lastName();
    }
}
