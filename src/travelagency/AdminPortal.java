package travelagency;

import travelagency.Models.Admin;

/**
 * Admin side: dashboard, bookings, catalogue management, customers, employees,
 * feedback, reports and settings.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class AdminPortal extends PortalFrame {

    public static final String DASHBOARD = "Dashboard";
    public static final String BOOKINGS = "Bookings";
    public static final String PACKAGES = "Packages";
    public static final String ROUTES = "Bus Routes";
    public static final String HOTELS = "Hotels";
    public static final String CUSTOMERS = "Customers";
    public static final String EMPLOYEES = "Employees";
    public static final String FEEDBACK = "Feedback";
    public static final String REPORTS = "Reports";
    public static final String SETTINGS = "Settings";

    private final Admin admin;

    public AdminPortal(App app, Admin admin) {
        super(app, "Travel Agency - Admin Portal", admin.fullName(), admin.role().toUpperCase());
        this.admin = admin;
        addPage(DASHBOARD, new AdminDashboardPage(this));
        addPage(BOOKINGS, new AdminBookingsPage(this));
        addPage(PACKAGES, new AdminPackagesPage(this));
        addPage(ROUTES, new AdminRoutesPage(this));
        addPage(HOTELS, new AdminHotelsPage(this));
        addPage(CUSTOMERS, new AdminCustomersPage(this));
        addPage(EMPLOYEES, new AdminEmployeesPage(this));
        addPage(FEEDBACK, new AdminFeedbackPage(this));
        addPage(REPORTS, new ReportsPage(this));
        addPage(SETTINGS, new AdminSettingsPage(this));
        open(DASHBOARD);
    }

    public App app() {
        return app;
    }

    public Admin admin() {
        return admin;
    }

    /** Approve a pending booking after asking the admin. Returns true when it changed. */
    boolean approve(java.awt.Component parent, Models.BookingView b) {
        if (!"Pending".equals(b.status())) {
            Ui.info(parent, "Only pending bookings can be approved. Booking #" + b.id() + " is " + b.status() + ".");
            return false;
        }
        if (!Ui.confirm(parent, "Approve booking #" + b.id() + " for " + b.customerName() + "?\n" + b.details(),
                "Approve booking")) {
            return false;
        }
        try {
            app.bookings.approve(b.id(), admin.id());
            return true;
        } catch (IllegalArgumentException | java.sql.SQLException ex) {
            Ui.error(parent, ex);
            return false;
        }
    }

    /** Cancel a booking after asking the admin; the trigger refunds the payment. */
    boolean cancel(java.awt.Component parent, Models.BookingView b) {
        if ("Cancelled".equals(b.status())) {
            Ui.info(parent, "Booking #" + b.id() + " is already cancelled.");
            return false;
        }
        if (!Ui.confirm(parent, "Cancel booking #" + b.id() + " for " + b.customerName() + "?\n"
                + "The payment of " + Ui.money(b.total()) + " will be refunded.", "Cancel booking")) {
            return false;
        }
        try {
            app.bookings.cancelByAdmin(b.id(), admin.id());
            return true;
        } catch (IllegalArgumentException | java.sql.SQLException ex) {
            Ui.error(parent, ex);
            return false;
        }
    }
}
