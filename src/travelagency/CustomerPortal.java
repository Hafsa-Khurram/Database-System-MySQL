package travelagency;

import travelagency.Models.Customer;

/**
 * Customer side: explore packages, book bus tickets and hotels, see bookings and profile.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class CustomerPortal extends PortalFrame {

    public static final String PACKAGES = "Tour Packages";
    public static final String BUS = "Bus Tickets";
    public static final String HOTELS = "Hotels";
    public static final String BOOKINGS = "My Bookings";
    public static final String PROFILE = "My Profile";

    private Customer customer;

    public CustomerPortal(App app, Customer customer) {
        super(app, "Travel Agency - Customer Portal", "Hi, " + customer.firstName(), "CUSTOMER");
        this.customer = customer;
        addPage(PACKAGES, new PackagesPage(this));
        addPage(BUS, new BusPage(this));
        addPage(HOTELS, new HotelPage(this));
        addPage(BOOKINGS, new MyBookingsPage(this));
        addPage(PROFILE, new ProfilePage(this));
        open(PACKAGES);
    }

    public App app() {
        return app;
    }

    public Customer customer() {
        return customer;
    }

    void setCustomer(Customer updated) {
        this.customer = updated;
    }

    /** After a booking: tell the user and jump to My Bookings. */
    void booked(int bookingId, String what) {
        Ui.info(this, "<html><b>Booking #" + bookingId + " confirmed and paid.</b><br><br>" + what
                + "<br><br>Status: <b>Pending</b> until the agency approves it.<br>You can follow it in My Bookings.</html>");
        open(BOOKINGS);
    }
}
