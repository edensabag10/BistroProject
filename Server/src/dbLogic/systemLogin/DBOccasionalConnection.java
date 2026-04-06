package dbLogic.systemLogin; // Defining the package for login database logic

import MainControllers.DBController; // Importing the singleton database controller
import dbLogic.ILoginDatabase; // Importing the login database interface
import java.sql.*; // Importing standard SQL classes for JDBC

/**
 * The DBOccasionalConnection class manages the data access logic for Guest (Occasional) customers.
 * It implements the ILoginDatabase interface to provide authentication and registration services.
 */
public class DBOccasionalConnection implements ILoginDatabase { // Class start

	/**
	 * Verifies the credentials of an occasional customer and retrieves their user ID.
	 * <p>
	 * This method performs a JOIN operation between the {@code occasional_customer} and 
	 * {@code user} tables. It checks if the provided username matches and if the 
	 * provided contact information corresponds to either the phone number or the email 
	 * stored in the system.
	 * </p>
	 *
	 * @param username The unique username of the occasional customer.
	 * @param contact  The contact information provided, which can be either a phone 
	 * number or an email address.
	 * @return The unique {@code user_id} associated with the customer if found; 
	 * returns {@code -1} if no matching record exists or if a database error occurs.
	 */
    @Override // Implementing method from ILoginDatabase interface
    public int verifyOccasional(String username, String contact) { // Method start
        // Retrieve the shared database connection from the singleton instance
        Connection conn = DBController.getInstance().getConnection(); // Get connection
        
        // SQL query: Join occasional_customer with user table to verify identity and contact info
        String sql = "SELECT oc.user_id FROM occasional_customer oc " + // Select user_id
                     "JOIN user u ON oc.user_id = u.user_id " + // Join on common ID
                     "WHERE oc.username = ? AND (u.phone_number = ? OR u.email = ?)"; // Filter by credentials
        
        // Use try-with-resources to ensure the PreparedStatement is closed automatically
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // Start try block
            // Bind the username parameter to the first placeholder
            pstmt.setString(1, username); // Set parameter 1
            // Bind the contact info (phone/email) to the second placeholder
            pstmt.setString(2, contact); // Set parameter 2
            // Bind the same contact info to the third placeholder (Email check)
            pstmt.setString(3, contact); // Set parameter 3
            
            // Execute the query and capture the results in a ResultSet
            try (ResultSet rs = pstmt.executeQuery()) { // Start inner try for ResultSet
                // Check if a matching record was found in the database
                if (rs.next()) { // Start if result exists
                    // Return the unique user_id found for this guest
                    return rs.getInt("user_id"); // Return ID
                } // End if
            } // End of ResultSet try
        } catch (SQLException e) { // Catch block for SQL errors
            // Print the technical stack trace for server-side debugging
            e.printStackTrace(); // Log error
        } // End of main try block
        
        // Return -1 if no matching user was found or an exception occurred
        return -1; // Default failure return
    } // End of verifyOccasional method

    
    
    /**
     * Resets the username for an occasional customer identified by their contact information.
     * <p>
     * This method follows a three-step validation and update process:
     * <ol>
     * <li><b>Identification:</b> Retrieves the internal user ID using the provided phone number or email from the {@code user} table.</li>
     * <li><b>Uniqueness Check:</b> Verifies that the proposed new username is not already assigned to another record in the {@code occasional_customer} table.</li>
     * <li><b>Execution:</b> Updates the {@code username} field for the corresponding user ID.</li>
     * </ol>
     * </p>
     *
     * @param contact     The phone number or email address used to locate the user's record.
     * @param newUsername The new username to be assigned to the customer.
     * @return A status message: {@code "RESET_USERNAME_SUCCESS"} if the update was successful, 
     * or a descriptive error message starting with {@code "ERROR:"} if identification 
     * failed, the username is taken, or a database error occurred.
     */
    public String resetUsername(String contact, String newUsername) { // Method start
        // Retrieve the database connection
        Connection conn = DBController.getInstance().getConnection(); // Get connection
        
        // Query to find the internal user_id based on phone or email
        String findUserSql = "SELECT user_id FROM user WHERE phone_number = ? OR email = ?"; // SQL string
        // Query to check if the new desired username is already occupied
        String checkUsernameSql = "SELECT user_id FROM occasional_customer WHERE username = ?"; // SQL string
        // Query to perform the actual update
        String updateSql = "UPDATE occasional_customer SET username = ? WHERE user_id = ?"; // SQL string

        try { // Start of main logic try block
            
            // --- STEP 1: Identification ---
            int userId = -1; // Initialize the user ID variable
            try (PreparedStatement pstmt = conn.prepareStatement(findUserSql)) { // Start finding ID
                pstmt.setString(1, contact); // Bind contact to param 1
                pstmt.setString(2, contact); // Bind contact to param 2
                ResultSet rs = pstmt.executeQuery(); // Execute query
                if (rs.next()) { // If user found
                    userId = rs.getInt("user_id"); // Retrieve the ID
                } else { // If no user found
                    return "ERROR: Contact info not found."; // Return descriptive error
                } // End if-else
            } // End identification try

            // --- STEP 2: Uniqueness Check ---
            try (PreparedStatement pstmt = conn.prepareStatement(checkUsernameSql)) { // Start uniqueness check
                pstmt.setString(1, newUsername); // Bind new name
                if (pstmt.executeQuery().next()) { // If name already exists
                    return "ERROR: Username '" + newUsername + "' is already taken."; // Return error
                } // End if
            } // End uniqueness try

            // --- STEP 3: Database Execution ---
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) { // Start update execution
                pstmt.setString(1, newUsername); // Bind new name
                pstmt.setInt(2, userId); // Bind identifying ID
                int affectedRows = pstmt.executeUpdate(); // Execute update
                
                // Return success if at least one row was modified
                if (affectedRows > 0) { // Check affected rows
                    return "RESET_USERNAME_SUCCESS"; // Success return
                } else { // If update failed to find row
                    return "ERROR: Could not update username."; // Return error
                } // End if-else
            } // End update execution try

        } catch (SQLException e) { // Catch database failures
            e.printStackTrace(); // Print technical error
            return "ERROR: Database failure - " + e.getMessage(); // Return technical message
        } // End of main try-catch
    } // End of resetUsername method

    
    
    /**
     * Registers a new occasional customer by creating a user profile and linking it to a username.
     * <p>
     * This method follows a strict four-phase transactional process:
     * <ol>
     * <li><b>Validation:</b> Ensures both the username and contact information (phone or email) 
     * do not already exist in the database.</li>
     * <li><b>User Creation:</b> Inserts the contact info into the {@code user} table. It automatically 
     * detects if the contact string is an email (contains '@') or a phone number.</li>
     * <li><b>Linkage:</b> Maps the newly generated user ID to the provided username in the 
     * {@code occasional_customer} table.</li>
     * <li><b>Finalization:</b> Commits the transaction if all steps succeed, or performs a rollback 
     * if any error occurs.</li>
     * </ol>
     * </p>
     *
     * @param username The unique username for the new occasional customer.
     * @param contact  The contact info, which can be either a phone number or an email address.
     * @return A {@code String} indicating the result: {@code "REGISTRATION_SUCCESS"} on success, 
     * or an {@code "ERROR: ..."} message explaining the failure (e.g., duplicate data or DB error).
     */
    public String registerNewOccasional(String username, String contact) { // Method start
        // Retrieve the database connection
        Connection conn = DBController.getInstance().getConnection(); // Get connection

        // SQL commands for validation and multi-table insertion
        String checkUserSql = "SELECT * FROM occasional_customer WHERE username = ?"; // SQL string
        String checkContactSql = "SELECT * FROM user WHERE phone_number = ? OR email = ?"; // SQL string
        String insertUserSql = "INSERT INTO user (phone_number, email) VALUES (?, ?)"; // SQL string
        String insertOccSql = "INSERT INTO occasional_customer (user_id, username) VALUES (?, ?)"; // SQL string

        try { // Start of registration try block
            
            // TRANSACTION START: Disable auto-commit to ensure atomicity
            conn.setAutoCommit(false); // Begin manual transaction

            // --- Phase 1: Pre-validation of identity uniqueness ---
            try (PreparedStatement pstmt = conn.prepareStatement(checkUserSql)) { // Check username
                pstmt.setString(1, username); // Bind name
                if (pstmt.executeQuery().next()) { // If exists
                    conn.rollback(); // Undo potential changes
                    return "ERROR: Username already exists."; // Return error
                } // End if
            } // End username check try

            try (PreparedStatement pstmt = conn.prepareStatement(checkContactSql)) { // Check contact info
                pstmt.setString(1, contact); // Bind contact to param 1
                pstmt.setString(2, contact); // Bind contact to param 2
                if (pstmt.executeQuery().next()) { // If exists
                    conn.rollback(); // Undo potential changes
                    return "ERROR: Contact info already exists."; // Return error
                } // End if
            } // End contact check try

            // --- Phase 2: Metadata Creation (User Table) ---
            int userId = -1; // Initialize generated ID holder
            try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) { // Insert with key retrieval
                // Logic: Assign value to either phone or email based on content
                if (contact.contains("@")) { // Check if input is an email
                    pstmt.setNull(1, Types.VARCHAR); // Set phone to NULL
                    pstmt.setString(2, contact); // Set email to value
                } else { // Otherwise treat as phone
                    pstmt.setString(1, contact); // Set phone to value
                    pstmt.setNull(2, Types.VARCHAR); // Set email to NULL
                } // End if-else
                
                pstmt.executeUpdate(); // Execute metadata creation
                
                // Retrieve the auto-generated primary key from MySQL
                ResultSet rs = pstmt.getGeneratedKeys(); // Get keys
                if (rs.next()) { // If key returned
                    userId = rs.getInt(1); // Extract the numeric ID
                } // End if
            } // End metadata try

            // --- Phase 3: Identity Linkage (Occasional Customer Table) ---
            if (userId != -1) { // If metadata was created successfully
                try (PreparedStatement pstmt = conn.prepareStatement(insertOccSql)) { // Link ID to username
                    pstmt.setInt(1, userId); // Bind generated ID
                    pstmt.setString(2, username); // Bind desired name
                    pstmt.executeUpdate(); // Execute linkage
                } // End linkage try
            } else { // If user_id retrieval failed
                conn.rollback(); // Abort entire transaction
                return "ERROR: Failed to create user profile."; // Return failure
            } // End if-else

            // --- Phase 4: Finalization ---
            conn.commit(); // Permanently save all changes in the transaction
            return "REGISTRATION_SUCCESS"; // Return success signal

        } catch (SQLException e) { // Handle exceptions
            // TRANSACTION FAILURE: Roll back all changes to prevent corrupted data
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } // Perform rollback
            e.printStackTrace(); // Print technical error
            return "ERROR: " + e.getMessage(); // Return exception details
        } finally { // Final cleanup
            // Restoring default database behavior
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); } // Re-enable auto-commit
        } // End of main block
    } // End of registerNewOccasional method

    // --- Interface Compatibility Layer ---

    /**
     * Registers an occasional customer by selecting an available contact method and 
     * invoking the internal registration process.
     * <p>
     * This method determines which contact information to use by prioritizing the 
     * phone number. If the phone number is {@code null} or empty, it defaults to the 
     * email address. The selected contact is then passed to 
     * {@link #registerNewOccasional(String, String)}.
     * </p>
     *
     * @param username The unique username for the new occasional customer.
     * @param phone    The phone number provided by the customer (can be {@code null} or empty).
     * @param email    The email address provided by the customer (fallback if phone is unavailable).
     * @return {@code true} if the registration was successful; 
     * {@code false} if it failed (e.g., username already exists or database error).
     */
    @Override // Overriding method from interface
    public boolean registerOccasional(String username, String phone, String email) { // Method start
        // Refactored logic: Select which contact string is available
        String contact = (phone != null && !phone.isEmpty()) ? phone : email; // Determine active contact string
        // Invoke the primary registration method and compare result to success string
        return "REGISTRATION_SUCCESS".equals(registerNewOccasional(username, contact)); // Return boolean result
    } // End method

    /**
     * Verifies a subscriber's identity based on their unique subscriber ID.
     * <p>
     * <b>Note:</b> This specific implementation does not support subscriber verification. 
     * It serves as a placeholder or a restricted version of the login process, 
     * consistently returning a failure code.
     * </p>
     *
     * @param subID The unique long integer identifying the subscriber.
     * @return Always returns {@code -1} to indicate that subscriber verification 
     * is not supported in this context.
     */
    @Override // Implementing interface member
    public int verifySubscriber(long subID) { // Method start
        // Return -1 to indicate that this implementation does not support subscriber login
        return -1; // Standard failure code
    } // End method
    
} // End of DBOccasionalConnection class