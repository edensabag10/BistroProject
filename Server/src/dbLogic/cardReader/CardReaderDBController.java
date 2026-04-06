package dbLogic.cardReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import MainControllers.DBController;


public class CardReaderDBController {

	/**
	 * Validates whether a subscriber has an active reservation in the system.
	 * This method queries the database by joining the 'reservation' and 'subscriber' 
	 * tables to check for an 'ACTIVE' status associated with the given subscriber ID.
	 *
	 * @param id The unique identifier of the subscriber to be validated.
	 * @return {@code true} if at least one active reservation is found for the subscriber; 
	 * {@code false} otherwise or if a database error occurs.
	 */
    public boolean validateSubscriber(String id) {
        String query = "SELECT r.status FROM reservation r " +
                       "JOIN subscriber s ON r.user_id = s.user_id " +
                       "WHERE s.subscriber_id = ? AND r.status = 'ACTIVE'";
        
        Connection conn = DBController.getInstance().getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = rs.next();
                System.out.println("Login check for Subscriber ID " + id + ": " + (found ? "SUCCESS" : "FAILED"));
                return found;
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves a list of active reservation confirmation codes associated with a given ID.
     * The method searches for matches in both the subscriber ID and the user ID fields 
     * to ensure all relevant active reservations are identified.
     *
     * @param id The identifier used to search for the subscriber or user (e.g., subscriber_id or user_id).
     * @return A {@code List<String>} containing the active confirmation codes. 
     * Returns an empty list if no active reservations are found or if a database error occurs.
     */
    public List<String> getLostConfirmationCodes(String id) {
        List<String> activeCodes = new ArrayList<>();
        // עדכון השאילתה: חיפוש כפול (גם לפי מזהה מנוי וגם לפי מזהה משתמש)
        String query = "SELECT r.confirmation_code, r.reservation_datetime FROM reservation r " +
                       "LEFT JOIN subscriber s ON r.user_id = s.user_id " +
                       "WHERE (s.subscriber_id = ? OR r.user_id = ?) AND r.status = 'ACTIVE'";

        Connection conn = DBController.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id); // יחפש 1028
            pstmt.setString(2, id); // יחפש 28
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activeCodes.add("Code: " + rs.getString("confirmation_code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeCodes;
    }
    
    
    /**
     * Verifies a reservation confirmation code for a specific subscriber and updates 
     * the reservation status to 'COMPLETED' upon successful validation.
     * <p>
     * This method first checks if an active reservation exists for the given code 
     * and subscriber ID. If found, it proceeds to update the reservation's status 
     * in the database.
     * </p>
     *
     * @param code         The confirmation code to be verified.
     * @param subscriberID The unique ID of the subscriber associated with the reservation.
     * @return A {@code String} message indicating the result: 
     * "Success" if the code is valid and updated, 
     * "Error" if the code is invalid/expired, 
     * or a "Database Error" message if an exception occurs.
     */
    public String verifyConfirmationCode(String code, String subscriberID) {
        // שאילתה לבדיקת קיום הקוד עבור המנוי הספציפי
        String checkQuery = "SELECT r.confirmation_code FROM reservation r " +
                            "JOIN subscriber s ON r.user_id = s.user_id " +
                            "WHERE r.confirmation_code = ? AND s.subscriber_id = ? AND r.status = 'ACTIVE'";
        
        // שאילתה לעדכון הסטטוס
        String updateQuery = "UPDATE reservation r " +
                             "JOIN subscriber s ON r.user_id = s.user_id " +
                             "SET r.status = 'COMPLETED' " +
                             "WHERE r.confirmation_code = ? AND s.subscriber_id = ?";

        Connection conn = DBController.getInstance().getConnection();

        try {
            try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                pstmt.setString(1, code);
                pstmt.setString(2, subscriberID);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateQuery)) {
                            updatePstmt.setString(1, code);
                            updatePstmt.setString(2, subscriberID);
                            updatePstmt.executeUpdate();
                        }
                        return "Success: Welcome to the restaurant!";
                    } else {
                        return "Error: Invalid or expired code.";
                    }
                }
            }
        } catch (SQLException e) {
            return "Database Error: " + e.getMessage();
        }
    }
}