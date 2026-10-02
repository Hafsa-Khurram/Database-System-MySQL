package travelagency;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import travelagency.Models.Customer;
import travelagency.Models.CustomerSummary;
import travelagency.Models.Employee;

/**
 * Employees, customers, the dashboard numbers and the reports.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminRepository {

    private final Database db;

    public AdminRepository(Database db) {
        this.db = db;
    }

    // ===================== Employees =====================

    public List<Employee> employees() throws SQLException {
        return db.query("SELECT * FROM employees ORDER BY department, first_name",
                rs -> new Employee(rs.getInt("employee_id"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("phone"), rs.getString("job_title"),
                        rs.getString("department"), rs.getObject("hire_date", LocalDate.class), rs.getDouble("salary")));
    }

    public void saveEmployee(Integer id, String first, String last, String email, String phone, String job,
                             String department, LocalDate hireDate, double salary) throws SQLException {
        if (hireDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Hire date cannot be in the future.");
        }
        Integer other = db.queryOne("SELECT employee_id FROM employees WHERE email = ?", rs -> rs.getInt(1), email);
        if (other != null && !other.equals(id)) {
            throw new IllegalArgumentException("Another employee already uses this email.");
        }
        if (id == null) {
            db.insert("INSERT INTO employees (first_name, last_name, email, phone, job_title, department, hire_date, salary) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", first, last, email, phone, job, department, hireDate, salary);
        } else {
            db.update("UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?, job_title = ?, "
                            + "department = ?, hire_date = ?, salary = ? WHERE employee_id = ?",
                    first, last, email, phone, job, department, hireDate, salary, id);
        }
    }

    public void deleteEmployee(int id) throws SQLException {
        db.update("DELETE FROM employees WHERE employee_id = ?", id);
    }

    // ===================== Customers =====================

    public List<CustomerSummary> customers() throws SQLException {
        return db.query("SELECT c.*, COUNT(b.booking_id) AS bookings, "
                        + "COALESCE(SUM(CASE WHEN b.status <> 'Cancelled' THEN b.total_amount END), 0) AS spent "
                        + "FROM customers c LEFT JOIN bookings b ON b.customer_id = c.customer_id "
                        + "GROUP BY c.customer_id ORDER BY c.created_at DESC",
                rs -> new CustomerSummary(AuthRepository.mapCustomer(rs), rs.getInt("bookings"), rs.getDouble("spent"),
                        rs.getObject("created_at", LocalDateTime.class)));
    }

    /** Deleting a customer also deletes their bookings, payments and feedback (ON DELETE CASCADE). */
    public void deleteCustomer(Customer customer) throws SQLException {
        Integer upcoming = db.queryOne("SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status <> 'Cancelled' "
                + "AND travel_date >= CURDATE()", rs -> rs.getInt(1), customer.id());
        if (upcoming != null && upcoming > 0) {
            throw new IllegalArgumentException(customer.fullName() + " has " + upcoming + " upcoming booking(s). "
                    + "Cancel them first, then delete the account.");
        }
        db.update("DELETE FROM customers WHERE customer_id = ?", customer.id());
    }

    // ===================== Dashboard =====================

    /** Numbers for the dashboard cards, in a fixed order. */
    public Map<String, Number> dashboardStats() throws SQLException {
        Map<String, Number> stats = new LinkedHashMap<>();
        Object[] row = db.queryOne("SELECT "
                        + "(SELECT COUNT(*) FROM customers), "
                        + "(SELECT COUNT(*) FROM bookings WHERE status = 'Pending'), "
                        + "(SELECT COUNT(*) FROM bookings WHERE status = 'Approved' AND travel_date >= CURDATE()), "
                        + "(SELECT COALESCE(SUM(total_amount), 0) FROM bookings WHERE status <> 'Cancelled'), "
                        + "(SELECT COUNT(*) FROM travel_packages WHERE is_active AND start_date > CURDATE()), "
                        + "(SELECT COALESCE(ROUND(AVG(rating), 1), 0) FROM feedback)",
                rs -> new Object[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4), rs.getInt(5), rs.getDouble(6)});
        stats.put("customers", (Number) row[0]);
        stats.put("pending", (Number) row[1]);
        stats.put("upcoming", (Number) row[2]);
        stats.put("revenue", (Number) row[3]);
        stats.put("packages", (Number) row[4]);
        stats.put("rating", (Number) row[5]);
        return stats;
    }

    // ===================== Reports =====================

    /** Revenue per booking type, from the v_revenue_by_type view. Rows: type, bookings, revenue. */
    public List<Object[]> revenueByType() throws SQLException {
        return db.query("SELECT t.type, COALESCE(v.bookings, 0), COALESCE(v.revenue, 0) FROM "
                        + "(SELECT 'Package' AS type UNION ALL SELECT 'Bus' UNION ALL SELECT 'Hotel') t "
                        + "LEFT JOIN v_revenue_by_type v ON v.booking_type = t.type",
                rs -> new Object[]{rs.getString(1), rs.getInt(2), rs.getDouble(3)});
    }

    /** Revenue of the last 6 months (by booking date). Rows: "Sep 2026", revenue. */
    public List<Object[]> monthlyRevenue() throws SQLException {
        return db.query("SELECT DATE_FORMAT(m.month_start, '%b %Y'), COALESCE(SUM(b.total_amount), 0) FROM ("
                        + " SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n MONTH), '%Y-%m-01') AS month_start FROM ("
                        + "  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5"
                        + " ) AS nums) AS m"
                        + " LEFT JOIN bookings b ON DATE_FORMAT(b.booked_at, '%Y-%m-01') = m.month_start AND b.status <> 'Cancelled'"
                        + " GROUP BY m.month_start ORDER BY m.month_start",
                rs -> new Object[]{rs.getString(1), rs.getDouble(2)});
    }

    /** Best-selling packages. Rows: name, travellers, revenue, average rating (or null). */
    public List<Object[]> topPackages() throws SQLException {
        return db.query("SELECT tp.name, COALESCE(SUM(pb.travellers), 0) AS travellers, COALESCE(SUM(b.total_amount), 0), "
                        + "ROUND(AVG(f.rating), 1) FROM travel_packages tp "
                        + "LEFT JOIN package_bookings pb ON pb.package_id = tp.package_id "
                        + "LEFT JOIN bookings b ON b.booking_id = pb.booking_id AND b.status <> 'Cancelled' "
                        + "LEFT JOIN feedback f ON f.booking_id = b.booking_id "
                        + "WHERE b.booking_id IS NOT NULL GROUP BY tp.package_id ORDER BY travellers DESC LIMIT 5",
                rs -> new Object[]{rs.getString(1), rs.getInt(2), rs.getDouble(3),
                        rs.getObject(4) == null ? null : rs.getDouble(4)});
    }

    /** Number of bookings per status. Rows: status, count. */
    public List<Object[]> statusCounts() throws SQLException {
        return db.query("SELECT s.status, COUNT(b.booking_id) FROM (SELECT 'Pending' AS status UNION ALL "
                        + "SELECT 'Approved' UNION ALL SELECT 'Cancelled') s LEFT JOIN bookings b ON b.status = s.status "
                        + "GROUP BY s.status ORDER BY FIELD(s.status, 'Pending', 'Approved', 'Cancelled')",
                rs -> new Object[]{rs.getString(1), rs.getInt(2)});
    }

    /** Total refunded to customers (payments marked Refunded by the trigger). */
    public double totalRefunded() throws SQLException {
        Double d = db.queryOne("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'Refunded'",
                rs -> rs.getDouble(1));
        return d == null ? 0 : d;
    }
}
