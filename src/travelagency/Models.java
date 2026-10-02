package travelagency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data classes that mirror the database tables. Java records give each one a
 * constructor, getters, equals and toString automatically.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public final class Models {

    private Models() {
    }

    public record Admin(int id, String username, String fullName, String email, String role) {
    }

    public record Customer(int id, String firstName, String lastName, String email, String phone,
                           String gender, String address, String username) {
        public String fullName() {
            return firstName + " " + lastName;
        }
    }

    public record TravelPackage(int id, String name, String type, String destination, String description,
                                LocalDate startDate, LocalDate endDate, double pricePerPerson, int totalSeats,
                                boolean flight, boolean hotel, boolean transport, String mealPlan,
                                boolean active, int seatsLeft) {
        public long days() {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }

        public String includes() {
            StringBuilder sb = new StringBuilder();
            if (flight) {
                sb.append("Flight, ");
            }
            if (hotel) {
                sb.append("Hotel, ");
            }
            if (transport) {
                sb.append("Transport, ");
            }
            if (!"None".equals(mealPlan)) {
                sb.append(mealPlan).append(", ");
            }
            return sb.length() == 0 ? "Tour only" : sb.substring(0, sb.length() - 2);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public record BusRoute(int id, String from, String to, String company, String departureTimes,
                           int economySeats, int businessSeats, double economyFare, double businessFare,
                           boolean active) {
        public List<String> times() {
            return Arrays.stream(departureTimes.split(";")).map(String::trim).filter(t -> !t.isEmpty()).toList();
        }

        public int seats(String seatClass) {
            return "Business".equals(seatClass) ? businessSeats : economySeats;
        }

        public double fare(String seatClass) {
            return "Business".equals(seatClass) ? businessFare : economyFare;
        }

        @Override
        public String toString() {
            return from + " → " + to;
        }
    }

    public record Hotel(int id, String name, String city, String address, int stars, int singleRooms,
                        int doubleRooms, int suiteRooms, double singleRate, double doubleRate, double suiteRate,
                        boolean active) {
        public int rooms(String type) {
            return switch (type) {
                case "Single" -> singleRooms;
                case "Double" -> doubleRooms;
                default -> suiteRooms;
            };
        }

        public double rate(String type) {
            return switch (type) {
                case "Single" -> singleRate;
                case "Double" -> doubleRate;
                default -> suiteRate;
            };
        }

        @Override
        public String toString() {
            return name + " (" + "★".repeat(stars) + ")";
        }
    }

    public record Employee(int id, String firstName, String lastName, String email, String phone, String jobTitle,
                           String department, LocalDate hireDate, double salary) {
    }

    /** One row of the v_booking_overview view. */
    public record BookingView(int id, int customerId, String customerName, String type, String details,
                              LocalDate travelDate, double total, String status, String paymentMethod,
                              String paymentStatus, LocalDateTime bookedAt, Integer rating) {
    }

    public record Feedback(int id, int bookingId, String customerName, String bookingDetails, int rating,
                           String comments, LocalDateTime createdAt) {
    }

    public record CustomerSummary(Customer customer, int bookings, double spent, LocalDateTime joined) {
    }
}
