package travelagency;

import java.sql.SQLException;
import javax.swing.SwingUtilities;

/**
 * Travel Agency Management System - Database Systems project.
 *
 * Start-up: connect to MySQL (asking for the settings if needed), create the
 * database automatically the first time, then show the login screen.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public class TravelAgency {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            Database db = Database.loadFromFile();
            if (!isReady(db) && !DatabaseSetupDialog.show(null, db)) {
                System.exit(0);
            }
            new LoginFrame(new App(db)).setVisible(true);
        });
    }

    /** True when MySQL can be reached and the tables exist. */
    private static boolean isReady(Database db) {
        try {
            db.testServer();
            return db.isInstalled();
        } catch (SQLException ex) {
            return false;
        }
    }
}
