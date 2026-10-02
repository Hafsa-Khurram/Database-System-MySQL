package travelagency;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Input checks shared by all forms. Each method throws an IllegalArgumentException
 * with a clear message, which the screens show to the user.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public final class Validate {

    public static final DateTimeFormatter DATE_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Validate() {
    }

    public static String name(String value, String field) {
        String v = clean(value);
        if (!v.matches("[\\p{L}][\\p{L} .'-]{1,49}")) {
            throw new IllegalArgumentException(field + " must be 2-50 letters.");
        }
        return v;
    }

    public static String text(String value, String field, int min, int max) {
        String v = clean(value);
        if (v.length() < min || v.length() > max) {
            throw new IllegalArgumentException(min == 0
                    ? field + " can be at most " + max + " characters."
                    : field + " must be " + min + "-" + max + " characters.");
        }
        return v;
    }

    public static String email(String value) {
        String v = clean(value).toLowerCase();
        if (!v.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+") || v.length() > 150) {
            throw new IllegalArgumentException("Please enter a valid email address, e.g. name@gmail.com.");
        }
        return v;
    }

    public static String phone(String value) {
        String v = clean(value);
        if (!v.matches("\\+?[0-9][0-9 -]{9,17}")) {
            throw new IllegalArgumentException("Please enter a valid phone number, e.g. 0300-1234567.");
        }
        return v;
    }

    public static String username(String value) {
        String v = clean(value).toLowerCase();
        if (!v.matches("[a-z0-9_.]{3,30}")) {
            throw new IllegalArgumentException("Username must be 3-30 letters, numbers, '_' or '.'.");
        }
        return v;
    }

    public static void password(char[] password, char[] confirm) {
        if (password.length < 6) {
            throw new IllegalArgumentException("Password must have at least 6 characters.");
        }
        if (confirm != null && !java.util.Arrays.equals(password, confirm)) {
            throw new IllegalArgumentException("The two passwords do not match.");
        }
    }

    public static int positiveInt(String value, String field, int max) {
        try {
            int n = Integer.parseInt(clean(value));
            if (n <= 0 || n > max) {
                throw new IllegalArgumentException(field + " must be between 1 and " + max + ".");
            }
            return n;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
    }

    public static int nonNegativeInt(String value, String field, int max) {
        try {
            int n = Integer.parseInt(clean(value));
            if (n < 0 || n > max) {
                throw new IllegalArgumentException(field + " must be between 0 and " + max + ".");
            }
            return n;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
    }

    public static double money(String value, String field) {
        try {
            double d = Double.parseDouble(clean(value).replace(",", ""));
            if (d <= 0 || d > 10_000_000) {
                throw new IllegalArgumentException(field + " must be more than 0.");
            }
            return d;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a number, e.g. 25000.");
        }
    }

    public static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(clean(value), DATE_INPUT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must be a date like 2026-12-31.");
        }
    }

    /** Departure times like "8:00; 14:30" become "08:00;14:30", sorted and checked. */
    public static String times(String value) {
        java.util.TreeSet<String> times = new java.util.TreeSet<>();
        for (String part : clean(value).split("[;,]")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (!t.matches("\\d{1,2}:\\d{2}")) {
                throw new IllegalArgumentException("\"" + t + "\" is not a valid time. Use 24-hour times like 08:00; 14:30.");
            }
            String[] hm = t.split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            if (h > 23 || m > 59) {
                throw new IllegalArgumentException("\"" + t + "\" is not a valid time.");
            }
            times.add(String.format("%02d:%02d", h, m));
        }
        if (times.isEmpty()) {
            throw new IllegalArgumentException("Please enter at least one departure time, e.g. 08:00; 14:30.");
        }
        return String.join(";", times);
    }

    public static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
