package travelagency;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import travelagency.Models.Admin;
import travelagency.Models.Customer;

/**
 * Login, sign-up and account changes for customers and admins.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AuthRepository {

    private final Database db;

    public AuthRepository(Database db) {
        this.db = db;
    }

    static Customer mapCustomer(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("customer_id"), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("email"), rs.getString("phone"), rs.getString("gender"), rs.getString("address"),
                rs.getString("username"));
    }

    private static Admin mapAdmin(ResultSet rs) throws SQLException {
        return new Admin(rs.getInt("admin_id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("role"));
    }

    /** Returns the customer, or null when the username or password is wrong. */
    public Customer loginCustomer(String username, char[] password) throws SQLException {
        try {
            List<Customer> found = db.query("SELECT * FROM customers WHERE username = ? AND password_hash = SHA2(CONCAT(salt, ?), 256)",
                    AuthRepository::mapCustomer, Validate.clean(username).toLowerCase(), new String(password));
            return found.isEmpty() ? null : found.get(0);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public Admin loginAdmin(String username, char[] password) throws SQLException {
        try {
            List<Admin> found = db.query("SELECT * FROM admins WHERE username = ? AND password_hash = SHA2(CONCAT(salt, ?), 256)",
                    AuthRepository::mapAdmin, Validate.clean(username).toLowerCase(), new String(password));
            return found.isEmpty() ? null : found.get(0);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /** Creates a customer account. Throws IllegalArgumentException for taken usernames or emails. */
    public Customer registerCustomer(String first, String last, String email, String phone, String gender,
                                     String address, String username, char[] password) throws SQLException {
        String user = Validate.username(username);
        String mail = Validate.email(email);
        if (db.queryOne("SELECT 1 FROM customers WHERE username = ?", rs -> 1, user) != null) {
            throw new IllegalArgumentException("The username \"" + user + "\" is already taken.");
        }
        if (db.queryOne("SELECT 1 FROM customers WHERE email = ?", rs -> 1, mail) != null) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        String salt = Security.newSalt();
        int id = db.insert("INSERT INTO customers (first_name, last_name, email, phone, gender, address, username, "
                        + "password_hash, salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Validate.name(first, "First name"), Validate.name(last, "Last name"), mail, Validate.phone(phone),
                gender, Validate.text(address, "Address", 0, 255), user, Security.hash(salt, password), salt);
        Arrays.fill(password, '\0');
        return findCustomer(id);
    }

    public Customer findCustomer(int id) throws SQLException {
        return db.queryOne("SELECT * FROM customers WHERE customer_id = ?", AuthRepository::mapCustomer, id);
    }

    public Customer updateCustomerProfile(int id, String first, String last, String email, String phone,
                                          String gender, String address) throws SQLException {
        String mail = Validate.email(email);
        if (db.queryOne("SELECT 1 FROM customers WHERE email = ? AND customer_id <> ?", rs -> 1, mail, id) != null) {
            throw new IllegalArgumentException("Another account already uses this email.");
        }
        db.update("UPDATE customers SET first_name = ?, last_name = ?, email = ?, phone = ?, gender = ?, address = ? "
                        + "WHERE customer_id = ?",
                Validate.name(first, "First name"), Validate.name(last, "Last name"), mail, Validate.phone(phone),
                gender, Validate.text(address, "Address", 0, 255), id);
        return findCustomer(id);
    }

    public void changeCustomerPassword(int id, char[] current, char[] fresh, char[] confirm) throws SQLException {
        changePassword("customers", "customer_id", id, current, fresh, confirm);
    }

    public void changeAdminPassword(int id, char[] current, char[] fresh, char[] confirm) throws SQLException {
        changePassword("admins", "admin_id", id, current, fresh, confirm);
    }

    private void changePassword(String table, String key, int id, char[] current, char[] fresh, char[] confirm)
            throws SQLException {
        try {
            Validate.password(fresh, confirm);
            boolean ok = db.queryOne("SELECT 1 FROM " + table + " WHERE " + key + " = ? AND password_hash = SHA2(CONCAT(salt, ?), 256)",
                    rs -> 1, id, new String(current)) != null;
            if (!ok) {
                throw new IllegalArgumentException("The current password is not correct.");
            }
            String salt = Security.newSalt();
            db.update("UPDATE " + table + " SET password_hash = ?, salt = ? WHERE " + key + " = ?",
                    Security.hash(salt, fresh), salt, id);
        } finally {
            Arrays.fill(current, '\0');
            Arrays.fill(fresh, '\0');
            if (confirm != null) {
                Arrays.fill(confirm, '\0');
            }
        }
    }

    public List<Admin> listAdmins() throws SQLException {
        return db.query("SELECT * FROM admins ORDER BY admin_id", AuthRepository::mapAdmin);
    }

    /** Only a Super Admin may add other admins (checked by the caller's role). */
    public void addAdmin(Admin creator, String username, String fullName, String email, char[] password,
                         char[] confirm, String role) throws SQLException {
        if (!"Super Admin".equals(creator.role())) {
            throw new IllegalArgumentException("Only the Super Admin can add new admins.");
        }
        String user = Validate.username(username);
        String mail = Validate.email(email);
        Validate.password(password, confirm);
        if (db.queryOne("SELECT 1 FROM admins WHERE username = ? OR email = ?", rs -> 1, user, mail) != null) {
            throw new IllegalArgumentException("An admin with this username or email already exists.");
        }
        String salt = Security.newSalt();
        db.insert("INSERT INTO admins (username, full_name, email, password_hash, salt, role) VALUES (?, ?, ?, ?, ?, ?)",
                user, Validate.text(fullName, "Full name", 3, 100), mail, Security.hash(salt, password), salt, role);
        Arrays.fill(password, '\0');
        Arrays.fill(confirm, '\0');
    }
}
