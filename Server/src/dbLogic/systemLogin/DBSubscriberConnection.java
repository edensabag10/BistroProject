package dbLogic.systemLogin; // Defining the package for login-related database logic

import MainControllers.DBController; // Importing the singleton database controller to manage connections
import dbLogic.ILoginDatabase; // Importing the login database interface for architectural consistency
import java.sql.*; // Importing standard Java SQL classes for JDBC interaction

/**
 * The DBSubscriberConnection class provides the database access logic specifically 
 * for the Subscriber authentication flow.
 */
public class DBSubscriberConnection implements ILoginDatabase { // Beginning of class definition implementing ILoginDatabase

	/**
	 * Verifies a subscriber's identity and checks for authorized roles within the system.
	 * <p>
	 * This method searches the {@code subscriber} table for a matching ID and ensures 
	 * the user has one of the following valid statuses: 'subscriber', 'manager', 
	 * or 'representative'. If a match is found, it retrieves the internal user ID.
	 * </p>
	 *
	 * @param subID The unique numeric identifier of the subscriber.
	 * @return The associated {@code user_id} if the subscriber is found and holds an 
	 * authorized role; returns {@code -1} if the ID is invalid, the status is 
	 * unauthorized, or a database error occurs.
	 */
    @Override // Indicating that this method overrides an interface definition
    public int verifySubscriber(long subID) { // Start of verifySubscriber method
        
        // Accessing the shared database connection instance from the DBController singleton
        Connection conn = DBController.getInstance().getConnection(); // Retrieving the connection
        
        // Defining the SQL query to check for a valid subscriber ID with an 'subscriber' or 'manager' or 'representative' status
        String sql = "SELECT user_id FROM subscriber WHERE subscriber_id = ? AND (status = 'subscriber' OR status = 'manager' OR status = 'representative')"; // SQL query string
        
        // Using try-with-resources to ensure the PreparedStatement is automatically closed
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // Initializing the statement
            
            // Binding the provided subID parameter to the first placeholder (?) in the query
            pstmt.setLong(1, subID); // Assigning the numeric ID
            
            // Executing the query and storing the outcome in a ResultSet object
            try (ResultSet rs = pstmt.executeQuery()) { // Start of ResultSet try-block
                
                // Checking if the database returned at least one matching row
                if (rs.next()) { // Start of result processing
                    
                    // Retrieve and return the internal user_id associated with this subscriber
                    return rs.getInt("user_id"); // Returning the ID from the DB
                    
                } // End of if-condition for matching results
                
            } // End of inner try-block for ResultSet
            
        } catch (SQLException e) { // Handling any database-specific exceptions (e.g., connectivity issues)
            
            // Outputting the technical stack trace to the system log for server-side debugging
            e.printStackTrace(); // Printing error details
            
        } // End of outer catch-block for SQLException
        
        // Returning -1 as a default value to indicate that authentication failed or an error occurred
        return -1; // Default failure response
        
    } // End of verifySubscriber method
    
    
    
    /**
     * Retrieves the specific status or role associated with a given user ID.
     * <p>
     * This method searches the {@code subscriber} table for a record matching the 
     * provided user identifier. It is primarily used to identify whether a user 
     * is a regular subscriber, a manager, or a representative.
     * </p>
     *
     * @param userId The unique internal identifier of the user to be verified.
     * @return A {@code String} representing the user's status (e.g., 'subscriber', 
     * 'manager', 'representative') if a matching record is found; returns 
     * {@code null} if no status is found or if a database error occurs.
     */
    public String verifyStatus(int userId) { // Start of verifyStatus method

        // Accessing the shared database connection
        Connection conn = DBController.getInstance().getConnection();

        // SQL query to retrieve the status for the given user_id
        String sql = "SELECT status FROM subscriber WHERE user_id = ?";

        // Using try-with-resources for safe resource management
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Binding the userId parameter
            pstmt.setInt(1, userId);

            // Executing the query
            try (ResultSet rs = pstmt.executeQuery()) {

                // If a matching row exists, return the status
                if (rs.next()) {
                    return rs.getString("status");
                }

            }

        } catch (SQLException e) {
            // Logging SQL errors for debugging
            e.printStackTrace();
        }

        // Return null if no status was found or an error occurred
        return null;
    }


    /**
     * Verifies the identity of an occasional customer (guest) in the system.
     * <p>
     * <b>Note:</b> This specific implementation does not support guest/occasional 
     * customer verification. It acts as a placeholder or a restricted connector 
     * and consistently returns a failure code.
     * </p>
     *
     * @param username    The unique username provided by the occasional customer.
     * @param contactInfo The contact information (phone or email) provided by the customer.
     * @return Always returns {@code -1} to indicate that verification is not 
     * supported by this connector.
     */
    @Override // Implementing interface method
    public int verifyOccasional(String username, String contactInfo) { // Start of verifyOccasional method
        
        // This specific connector does not handle guests, so it returns a failure code
        return -1; // Standard failure return
        
    } // End of verifyOccasional method

    
    
    /**
     * Attempts to register a new occasional customer (guest) in the system.
     * <p>
     * <b>Note:</b> This specific implementation does not support guest registration. 
     * It serves as a placeholder or a restricted version of the registration logic 
     * and consistently returns {@code false} to indicate failure.
     * </p>
     *
     * @param username The desired username for the occasional customer.
     * @param phone    The customer's phone number.
     * @param email    The customer's email address.
     * @return Always returns {@code false} to indicate that registration 
     * is not supported in this specific connector.
     */
    @Override // Implementing interface method
    public boolean registerOccasional(String username, String phone, String email) { // Start of registerOccasional method
        
        // This specific connector does not handle guest registration
        return false; // Standard failure return
        
    } // End of registerOccasional method
    
} // End of DBSubscriberConnection class definition