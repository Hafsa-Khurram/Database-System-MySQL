package travelagency;

/**
 * Holds the database connection settings and one repository of each kind,
 * so every screen can reach the data without creating its own objects.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class App {

    public final Database db;
    public final AuthRepository auth;
    public final CatalogRepository catalog;
    public final BookingRepository bookings;
    public final AdminRepository admin;

    public App(Database db) {
        this.db = db;
        this.auth = new AuthRepository(db);
        this.catalog = new CatalogRepository(db);
        this.bookings = new BookingRepository(db);
        this.admin = new AdminRepository(db);
    }
}
