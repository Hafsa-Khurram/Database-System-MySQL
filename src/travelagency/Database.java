package travelagency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MySQL connection settings and helpers. Settings are kept in "database.properties"
 * next to the program, so they can be changed without touching the code.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class Database {

    public static final String SCHEMA_RESOURCE = "travel_agency.sql";
    private static final Path CONFIG_FILE = Paths.get("database.properties");

    private String host = "localhost";
    private int port = 3306;
    private String name = "travel_agency";
    private String user = "root";
    private String password = "";

    /** A row mapper, like a small lambda that turns the current ResultSet row into an object. */
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static Database loadFromFile() {
        Database db = new Database();
        if (Files.exists(CONFIG_FILE)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                p.load(in);
                db.host = p.getProperty("host", db.host);
                db.port = Integer.parseInt(p.getProperty("port", String.valueOf(db.port)));
                db.name = p.getProperty("database", db.name);
                db.user = p.getProperty("user", db.user);
                db.password = p.getProperty("password", db.password);
            } catch (IOException | NumberFormatException ex) {
                System.err.println("Could not read database.properties: " + ex.getMessage());
            }
        }
        return db;
    }

    public void saveToFile() throws IOException {
        Properties p = new Properties();
        p.setProperty("host", host);
        p.setProperty("port", String.valueOf(port));
        p.setProperty("database", name);
        p.setProperty("user", user);
        p.setProperty("password", password);
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            p.store(out, "MySQL connection for the Travel Agency Management System");
        }
    }

    public void configure(String host, int port, String name, String user, String password) {
        this.host = host.trim();
        this.port = port;
        this.name = name.trim();
        this.user = user.trim();
        this.password = password;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getName() { return name; }
    public String getUser() { return user; }
    public String getPassword() { return password; }

    private String url(boolean withDatabase) {
        return "jdbc:mysql://" + host + ":" + port + "/" + (withDatabase ? name : "")
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    }

    /** Connection to the travel_agency database. */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url(true), user, password);
    }

    /** Checks that the MySQL server can be reached with these settings. */
    public void testServer() throws SQLException {
        try (Connection c = DriverManager.getConnection(url(false), user, password)) {
            c.isValid(3);
        }
    }

    /** True when the database exists and already has its tables. */
    public boolean isInstalled() {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = 'bookings'")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    /**
     * Creates the database, tables, trigger, function, views and starting data by
     * running travel_agency.sql (the same file you can run in MySQL Workbench).
     */
    public void install() throws SQLException, IOException {
        List<String> statements = readScript();
        try (Connection c = DriverManager.getConnection(url(false), user, password);
             Statement st = c.createStatement()) {
            for (String sql : statements) {
                // Use the database name from the settings instead of the one in the file.
                String s = sql.replaceAll("\\btravel_agency\\b", java.util.regex.Matcher.quoteReplacement(name));
                st.execute(s);
            }
        }
    }

    /** Splits the SQL file into statements, understanding MySQL's DELIMITER command. */
    static List<String> readScript() throws IOException {
        InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE);
        if (in == null) {
            Path file = Paths.get("src", "travelagency", SCHEMA_RESOURCE);
            if (!Files.exists(file)) {
                throw new IOException("The file " + SCHEMA_RESOURCE + " is missing.");
            }
            in = Files.newInputStream(file);
        }
        List<String> statements = new ArrayList<>();
        String delimiter = ";";
        StringBuilder current = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (current.length() == 0 && (trimmed.isEmpty() || trimmed.startsWith("--"))) {
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring(10).trim();
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(delimiter)) {
                    String sql = current.toString().trim();
                    sql = sql.substring(0, sql.length() - delimiter.length()).trim();
                    if (!sql.isEmpty()) {
                        statements.add(sql);
                    }
                    current.setLength(0);
                }
            }
        }
        return statements;
    }

    // ===================== Small JDBC helpers =====================

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        }
    }

    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int update(String sql, Object... params) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Runs an INSERT and returns the new AUTO_INCREMENT id. */
    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // Tables whose key is not AUTO_INCREMENT (like package_bookings) return no key.
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public int insert(String sql, Object... params) throws SQLException {
        try (Connection c = connect()) {
            return insert(c, sql, params);
        }
    }

    static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** Turns a MySQL error into a sentence a normal user understands. */
    public static String friendly(SQLException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            return "Cannot reach the MySQL server. Make sure MySQL is running.";
        }
        if (msg.contains("Access denied")) {
            return "MySQL rejected the username or password in the database settings.";
        }
        if (ex.getErrorCode() == 1062) {
            return "This value already exists (duplicate). " + msg.replaceAll(".*for key '([^']*)'.*", "Field: $1.");
        }
        if (ex.getErrorCode() == 1451) {
            return "This record is used by existing bookings, so it cannot be deleted. You can deactivate it instead.";
        }
        if (ex.getErrorCode() == 3819) {
            return "The value breaks a database rule: " + msg.replaceAll(".*Check constraint '([^']*)'.*", "$1") + ".";
        }
        return msg;
    }
}
