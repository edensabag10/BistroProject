package dbLogic.managmentDB; // Defining the package for database management logic

import java.sql.Connection; // Importing Connection for database connectivity
import java.sql.PreparedStatement; // Importing PreparedStatement for parameterized SQL queries
import java.sql.ResultSet; // Importing ResultSet to handle query results
import java.sql.SQLException; // Importing SQLException for database error handling
import java.sql.Statement; // Importing Statement to retrieve generated keys
import java.time.LocalDate; // Importing LocalDate for modern date handling
import java.util.ArrayList;
import java.util.Date; // Importing Date for legacy support if needed
import java.util.Map; // Importing Map for storing day-to-range associations
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import MainControllers.DBController; // Importing the singleton DB controller
import MainControllers.ServerController;
import common.TimeRange; // Importing the TimeRange domain model
import dbLogic.restaurantDB.WaitingListController;

/**
 * Controller for managing database updates for the restaurant management system.
 * This class handles regular and special operating hours updates using SQL transactions.
 */
public class UpdateManagementDBController { // Start of the UpdateManagementDBController class
	
	// Tracks reservations that already received a "2 hours before arrival" reminder
    private static final Set<String> notifiedReservations = ConcurrentHashMap.newKeySet();
    // Tracks visits that already received a "2 hours after start" alert
    private static final Set<Long> notifiedVisits = ConcurrentHashMap.newKeySet();


	/**
	 * Updates the regular operating hours for a specific restaurant across multiple days.
	 * <p>
	 * This method performs a transactional update to ensure data integrity. For each day provided
	 * in the map, it:
	 * <ol>
	 * <li>Ensures the specific time range exists in the {@code time_range} table.</li>
	 * <li>Retrieves the unique ID for that time range.</li>
	 * <li>Performs an "Upsert" (Insert or Update) into the {@code restaurant_regular_hours} table.</li>
	 * </ol>
	 * If any part of the process fails, the entire transaction is rolled back to maintain 
	 * database consistency.
	 * </p>
	 *
	 * @param restaurantId The unique identifier of the restaurant to update.
	 * @param newHours     A {@code Map} where keys are days of the week (e.g., "Monday") 
	 * and values are {@code TimeRange} objects containing start and end times.
	 * @return {@code true} if the operation was successful and committed; 
	 * {@code false} if a database error occurred and changes were rolled back.
	 */
    public static boolean updateRegularHours(int restaurantId, Map<String, TimeRange> newHours) { // Start method
        // Retrieve the active database connection from the singleton controller
        Connection conn = DBController.getInstance().getConnection(); // Get connection
        
        // SQL to insert a time range only if it does not already exist (ignoring duplicates)
        String insertRangeSql = "INSERT IGNORE INTO time_range (open_time, close_time) VALUES (?, ?)"; // SQL string
        
        // SQL to find the unique ID of a specific time range
        String findIdSql = "SELECT time_range_id FROM time_range WHERE open_time = ? AND close_time = ?"; // SQL string
        
        // SQL to perform an UPSERT: Insert new hours or update if the day already exists for this restaurant
        String updateDaySql = "INSERT INTO restaurant_regular_hours (restaurant_id, day_of_week, time_range_id) " +
                             "VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE time_range_id = ?"; // SQL string

        try { // Start of the main database transaction block
            // Disable auto-commit to manually manage the transaction boundaries
            conn.setAutoCommit(false); // Set auto-commit to false

            // Iterate through the map containing days of the week and their respective time ranges
            for (Map.Entry<String, TimeRange> entry : newHours.entrySet()) { // Start of loop
                String day = entry.getKey(); // Extract the day name (key)
                TimeRange range = entry.getValue(); // Extract the TimeRange object (value)

                // STEP A: Ensure the time range exists in the time_range table
                try (PreparedStatement pstmt = conn.prepareStatement(insertRangeSql)) { // Prepare statement
                    pstmt.setString(1, range.getOpenTime()); // Bind opening time
                    pstmt.setString(2, range.getCloseTime()); // Bind closing time
                    pstmt.executeUpdate(); // Execute the insert (ignored if exists)
                } // End of inner try

                // STEP B: Retrieve the ID of the time range (either the existing one or the one just inserted)
                int timeRangeId = -1; // Initialize the ID variable
                try (PreparedStatement pstmt = conn.prepareStatement(findIdSql)) { // Prepare statement
                    pstmt.setString(1, range.getOpenTime()); // Bind opening time
                    pstmt.setString(2, range.getCloseTime()); // Bind closing time
                    try (ResultSet rs = pstmt.executeQuery()) { // Execute query
                        if (rs.next()) { // If a result is found
                            timeRangeId = rs.getInt("time_range_id"); // Capture the ID
                        } // End if
                    } // End result set try
                } // End find ID try

                // STEP C: Perform the Upsert into the regular hours table
                if (timeRangeId != -1) { // If a valid time range ID was acquired
                    try (PreparedStatement pstmt = conn.prepareStatement(updateDaySql)) { // Prepare statement
                        pstmt.setInt(1, restaurantId); // Bind restaurant ID
                        pstmt.setString(2, day); // Bind day of the week
                        pstmt.setInt(3, timeRangeId); // Bind time range ID for insert
                        pstmt.setInt(4, timeRangeId); // Bind time range ID for update (on duplicate)
                        pstmt.executeUpdate(); // Execute the UPSERT
                    } // End upsert try
                } // End valid ID check
            } // End of the days iteration loop

            // Commit all changes to the database as a single atomic unit
            conn.commit(); // Execute commit
            return true; // Return success

        } catch (SQLException e) { // Catch any database errors during the process
            try { // Attempt to rollback in case of an error
                if (conn != null) { // Check if connection is alive
                    conn.rollback(); // Undo all changes in this transaction
                } // End connection check
            } catch (SQLException ex) { // Catch rollback failure
                ex.printStackTrace(); // Log the rollback exception
            } // End rollback try
            e.printStackTrace(); // Log the original SQL exception
            return false; // Return failure
        } finally { // Finalize block to restore connection state
            try { // Attempt to reset auto-commit
                conn.setAutoCommit(true); // Re-enable auto-commit for future operations
            } catch (SQLException e) { // Catch reset failure
                e.printStackTrace(); // Log the error
            } // End reset try
        } // End of finally block
    } // End of updateRegularHours method

    /**
     * Updates or inserts special operating hours for a restaurant on a specific date.
     * <p>
     * This method ensures that special hours (e.g., for holidays or one-time events) are 
     * recorded in the database. It uses a transactional approach to:
     * <ol>
     * <li>Retrieve or create a unique ID for the given time range using a helper method.</li>
     * <li>Perform an "Upsert" operation on the {@code restaurant_special_hours} table.</li>
     * </ol>
     * If any database error occurs during the process, all changes are rolled back to 
     * maintain data consistency.
     * </p>
     *
     * @param restaurantId The unique identifier of the restaurant.
     * @param date         The specific {@code LocalDate} for which the special hours apply.
     * @param open         The opening time as a string (e.g., "08:00").
     * @param close        The closing time as a string (e.g., "22:00").
     * @return {@code true} if the update was successfully committed; 
     * {@code false} if a database error occurred or the transaction was rolled back.
     */
    public static boolean updateSpecialHours(int restaurantId, LocalDate date, String open, String close) { // Start method
        // Retrieve the database connection
        Connection conn = DBController.getInstance().getConnection(); // Get connection
        
        try { // Start transaction block
            // Disable auto-commit to start a manual transaction
            conn.setAutoCommit(false); // Begin transaction

            // STEP 1: Use helper method to get existing ID or create a new time range
            int timeRangeId = getOrCreateTimeRange(open, close); // Call helper
            
            // Logic validation: Check if the ID retrieval was successful
            if (timeRangeId == -1) { // If ID is invalid
                throw new SQLException("Failed to retrieve or create time_range_id"); // Trigger exception
            } // End ID check

            // STEP 2: Execute Upsert logic for the special hours table
            String sql = "INSERT INTO restaurant_special_hours (restaurant_id, special_date, time_range_id) " +
                         "VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE time_range_id = ?"; // SQL string

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // Prepare statement
                pstmt.setInt(1, restaurantId); // Bind restaurant ID
                pstmt.setDate(2, java.sql.Date.valueOf(date)); // Bind the specific date
                pstmt.setInt(3, timeRangeId); // Bind time range ID for insert
                pstmt.setInt(4, timeRangeId); // Bind time range ID for update
                pstmt.executeUpdate(); // Execute the update
            } // End statement try

            // STEP 3: Finalize the transaction
            conn.commit(); // Save all changes
            return true; // Return success

        } catch (SQLException e) { // Handle errors
            try { // Rollback logic
                if (conn != null) { // If connection exists
                    conn.rollback(); // Rollback changes
                } // End check
            } catch (SQLException ex) { // Rollback error
                ex.printStackTrace(); // Print trace
            } // End rollback catch
            e.printStackTrace(); // Print original trace
            return false; // Return failure
        } finally { // Cleanup
            try { // Restoration
                conn.setAutoCommit(true); // Re-enable auto-commit
            } catch (SQLException e) { // Restoration error
                e.printStackTrace(); // Print trace
            } // End restore try
        } // End finally
    } // End of updateSpecialHours method

    /**
     * Retrieves the unique identifier for a specific time range, creating it if it does not exist.
     * <p>
     * This helper method performs a two-step operation:
     * <ol>
     * <li>Attempts to find an existing record in the {@code time_range} table that matches 
     * the provided opening and closing times.</li>
     * <li>If no record is found, it inserts a new time range into the table and retrieves 
     * the automatically generated primary key.</li>
     * </ol>
     * </p>
     *
     * @param open  The opening time string (e.g., "HH:mm").
     * @param close The closing time string (e.g., "HH:mm").
     * @return The {@code time_range_id} of the existing or newly created range; 
     * returns -1 if the operation fails.
     * @throws SQLException If a database access error occurs during the lookup or insertion.
     */
    private static int getOrCreateTimeRange(String open, String close) throws SQLException { // Start method
        // Access the connection
        Connection conn = DBController.getInstance().getConnection(); // Get connection
        
        // Search query to check for existing identical ranges
        String selectSql = "SELECT time_range_id FROM time_range WHERE open_time = ? AND close_time = ?"; // SQL string
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) { // Prepare statement
            pstmt.setString(1, open); // Bind open time
            pstmt.setString(2, close); // Bind close time
            ResultSet rs = pstmt.executeQuery(); // Execute search
            if (rs.next()) { // If found
                return rs.getInt(1); // Return the existing ID immediately
            } // End if
        } // End select try

        // Insertion query to create the range if it wasn't found
        String insertSql = "INSERT INTO time_range (open_time, close_time) VALUES (?, ?)"; // SQL string
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) { // Prepare statement
            pstmt.setString(1, open); // Bind open time
            pstmt.setString(2, close); // Bind close time
            if (pstmt.executeUpdate() > 0) { // If row was inserted
                ResultSet rs = pstmt.getGeneratedKeys(); // Get the new ID
                if (rs.next()) { // If ID exists
                    return rs.getInt(1); // Return the newly generated ID
                } // End ID if
            } // End update if
        } // End insert try
        
        return -1; // Return -1 if both lookup and insertion failed
    } // End of getOrCreateTimeRange method
    
    /**
     * Deletes all special operating hours associated with a specific restaurant.
     * <p>
     * This method removes every record in the {@code restaurant_special_hours} table 
     * that matches the given restaurant ID. The operation is wrapped in a transaction 
     * to ensure that the deletion is atomic; if a database error occurs, the 
     * transaction is rolled back.
     * </p>
     *
     * @param restaurantId The unique identifier of the restaurant whose special hours 
     * should be removed.
     * @return {@code true} if the records were successfully deleted and the transaction 
     * committed; {@code false} if a database error occurred.
     */
    public static boolean deleteAllSpecialHours(int restaurantId) { // Start of the method
        // Retrieve the active database connection from the singleton controller
        Connection conn = DBController.getInstance().getConnection(); // Get connection

        // SQL command to remove all records matching the restaurant ID from the special hours table
        String sql = "DELETE FROM restaurant_special_hours WHERE restaurant_id = ?"; //

        try { // Start transaction block
            // Disable auto-commit to manually control the transaction boundary
            conn.setAutoCommit(false); //

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // Prepare statement
                // Bind the restaurant ID to the placeholder
                pstmt.setInt(1, restaurantId); 

                // Execute the deletion query
                pstmt.executeUpdate(); 
            } // End statement try

            // Commit the transaction to persist the changes in the database
            conn.commit(); //
            return true; // Return success

        } catch (SQLException e) { // Handle potential database errors
            try { // Rollback logic in case of failure to maintain data integrity
                if (conn != null) { 
                    conn.rollback(); //
                } 
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            } 
            e.printStackTrace(); // Log the original exception details
            return false; // Return failure
        } finally { // Cleanup block
            try { // Restore the connection state for future operations
                conn.setAutoCommit(true); //
            } catch (SQLException e) { 
                e.printStackTrace(); 
            } 
        } // End finally
    } // End of deleteAllSpecialHours method

    /**
    * Checks for active visits that have been ongoing for at least
    * two hours and logs a single alert for each such visit.
    * <p>
    * The method queries the {@code visit} table for records with
    * status {@code 'ACTIVE'} whose start time occurred {@code 120}
    * minutes or more in the past. For each eligible visit, an alert
    * message is logged to the server.
    * </p>
    * <p>
    * To prevent repeated alerts, an in-memory tracking mechanism
    * ({@code notifiedVisits}) is used to ensure that each visit
    * triggers the alert only once during the server runtime.
    * </p>
    *
    * <p><b>Threshold:</b> 120 minutes (2 hours after visit start).</p>
    */
    public static void checkStayDurationAlerts() {

        String selectSql =
            "SELECT v.table_id, v.user_id, v.confirmation_code " +
            "FROM visit v " +
            "WHERE v.status = 'ACTIVE' " +
            "AND TIMESTAMPDIFF(MINUTE, v.start_time, NOW()) >= 120";


        Connection conn = DBController.getInstance().getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {

                long confCode = rs.getLong("confirmation_code");

                // אם כבר שלחנו התראה – מדלגים
                if (notifiedVisits.contains(confCode)) {
                    continue;
                }

                int tableId = rs.getInt("table_id");
                int userId = rs.getInt("user_id");

                String alertMsg = String.format("[STAY ALERT] Table %d (User %d) has exceeded 2 hours. Visit %d requires attention.",tableId, userId, confCode);

                ServerController.log(alertMsg);

                // סימון בזיכרון – לא נשלח שוב
                notifiedVisits.add(confCode);
            }

        } catch (SQLException e) {
            ServerController.log("Error in stay duration automation: " + e.getMessage());
        }
    }



    /**
     * Updates the status of an active visit in the database.
     * <p>
     * This method attempts to change the status of a visit identified by its 
     * confirmation code, provided that the current status is 'ACTIVE'.
     * It returns {@code true} only if exactly one record was updated.
     * </p>
     *
     * @param confCode  The unique confirmation code associated with the visit.
     * @param newStatus The new status to assign to the visit (e.g., 'BILL_PENDING', 'COMPLETED').
     * @return {@code true} if the update was successful and one row was affected; 
     * {@code false} if the visit was not found, was not 'ACTIVE', or if a database error occurred.
     */
    private static boolean updateVisitStatus(long confCode, String newStatus) {

        String updateSql =
            "UPDATE visit SET status = ? " +
            "WHERE confirmation_code = ? AND status = 'ACTIVE'";

        try (PreparedStatement pstmt =DBController.getInstance().getConnection().prepareStatement(updateSql)) {

            pstmt.setString(1, newStatus);
            pstmt.setLong(2, confCode);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows == 1;

        } catch (SQLException e) {ServerController.log("Failed to update visit status for code " + confCode + ": " + e.getMessage());
            return false;
        }
    }

    
    /**
     * Checks for active reservations that are scheduled to occur
     * at least two hours from the current time and logs a reminder
     * for each eligible reservation.
     *
     * <p>
     * The method queries the {@code reservation} table for records
     * with status {@code 'ACTIVE'} whose reservation time is
     * {@code 120} minutes or more in the future. To prevent duplicate
     * reminders, an in-memory tracking mechanism
     * ({@code notifiedReservations}) is used to ensure that each
     * reservation is logged only once during the server runtime.
     * </p>
     *
     * <p>
     * This approach avoids missed reminders caused by strict time
     * windows while preventing repeated notifications without
     * modifying the database state.
     * </p>
     */
    public static void checkReservationReminders() {
        // השאילתה המעודכנת:
        // 1. משתמשת ב-reservation_datetime המאוחד
        // 2. בודקת הפרש דקות בין עכשיו לבין זמן ההזמנה
        String selectSql =
                "SELECT r.confirmation_code, r.user_id, r.reservation_datetime " +
                "FROM reservation r " +
                "WHERE r.status = 'ACTIVE' " +
                "AND TIMESTAMPDIFF(MINUTE, NOW(), r.reservation_datetime) <= 120";

            Connection conn = DBController.getInstance().getConnection();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {

                while (rs.next()) {

                    String confCode = rs.getString("confirmation_code");

                    // אם כבר שלחנו התראה – מדלגים
                    if (notifiedReservations.contains(confCode)) {
                        continue;
                    }

                    int userId = rs.getInt("user_id");
                    String fullDateTime = rs.getString("reservation_datetime");

                    String alertMsg = String.format("[REMINDER] Notification for User %d (Code: %s) for reservation at %s.",userId, confCode, fullDateTime);

                    ServerController.log(alertMsg);

                    // סימון בזיכרון – לא נשלח שוב
                    notifiedReservations.add(confCode);
                }

            } catch (SQLException e) {
                ServerController.log("Error in reservation reminder: " + e.getMessage());
            }
    }

    /**
     * Updates the status of a reservation in the database based on its confirmation code.
     * <p>
     * This method is typically used to transition a reservation between different lifecycle 
     * states, such as moving from 'ACTIVE' to 'NOTIFIED' or 'COMPLETED'.
     * </p>
     *
     * @param confCode  The unique confirmation code identifying the reservation to be updated.
     * @param newStatus The new status string to be applied to the reservation record.
     */
    private static void updateReservationStatus(String confCode, String newStatus) {
        // עדכון לפי confirmation_code כפי שמופיע בטבלה שצילמת
        String updateSql = "UPDATE reservation SET status = ? WHERE confirmation_code = ?";
        try (PreparedStatement pstmt = DBController.getInstance().getConnection().prepareStatement(updateSql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, confCode);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update status for " + confCode + ": " + e.getMessage());
        }
    }

    /**
     * Automatically cancels active reservations where the customer failed to arrive within 
     * the permitted grace period.
     * <p>
     * This method identifies reservations that are still marked as 'ACTIVE' but have exceeded 
     * their scheduled time by more than 15 minutes without a corresponding entry in the 
     * {@code visit} table (meaning the customer never checked in). Such reservations 
     * are updated to a 'NOSHOW' status.
     * </p>
     * <p><b>Grace Period:</b> 15 minutes.</p>
     */
    public static void cancelLateReservations() {
        // שאילתה שמוצאת הזמנות שזמנן עבר ואין להן ביקור תואם בטבלת visit
        String findLateSql = "SELECT r.confirmation_code FROM reservation r " +
                             "LEFT JOIN visit v ON r.confirmation_code = v.confirmation_code " +
                             "WHERE r.status = 'ACTIVE' " +
                             "AND v.confirmation_code IS NULL " + 
                             "AND TIMESTAMPDIFF(MINUTE, r.reservation_datetime, NOW()) > 15";

        String cancelSql = "UPDATE reservation SET status = 'NOSHOW' WHERE confirmation_code = ?";

        Connection conn = DBController.getInstance().getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(findLateSql)) {

            while (rs.next()) {
                String confCode = rs.getString("confirmation_code");

                // עדכון הסטטוס ל-NOSHOW
                try (PreparedStatement pstmt = conn.prepareStatement(cancelSql)) {
                    pstmt.setString(1, confCode);
                    int affected = pstmt.executeUpdate();

                    if (affected > 0) {
                    	ServerController.log("[AUTO-CANCEL] Reservation " + confCode + " marked as NOSHOW (15+ min late).");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during auto-cancel process: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registers a new subscriber in the system by creating records in both 'user' and 'subscriber' tables.
     * <p>
     * This method follows an atomic transactional process:
     * <ol>
     * <li>Checks if the provided phone number already exists in the system to prevent duplicates.</li>
     * <li>Inserts basic contact information into the {@code user} table and retrieves the generated user ID.</li>
     * <li>Generates a unique 6-digit subscriber ID and creates a corresponding entry in the {@code subscriber} table.</li>
     * </ol>
     * If any step fails, the transaction is rolled back to maintain database integrity.
     * </p>
     *
     * @param phone The phone number of the new subscriber (used as a unique identifier check).
     * @param email The email address of the new subscriber.
     * @return An {@code Object} which is either:
     * <ul>
     * <li>A {@code Long} representing the newly generated subscriber ID upon success.</li>
     * <li>A {@code String} containing an error message if the phone exists or a database error occurs.</li>
     * </ul>
     */
    public static Object createNewSubscriber(String phone, String email) { // Method start
        // Get connection from the singleton controller
        Connection conn = DBController.getInstance().getConnection(); 
        
        // SQL 1: Check if phone already exists
        String checkSql = "SELECT user_id FROM user WHERE phone_number = ?";
        
        // SQL 2: Insert into user table and get back the user_id
        String insertUserSql = "INSERT INTO user (phone_number, email) VALUES (?, ?)";
        
        // SQL 3: Insert into subscriber table
        // username and qr_code remain NULL as requested. status is set to 'Active'.
        String insertSubSql = "INSERT INTO subscriber (user_id, subscriber_id, status) VALUES (?, ?, 'subscriber')";

        try { // Start transaction block
            conn.setAutoCommit(false); // Disable auto-commit for atomicity

            // --- STEP 1: Check for existing phone number ---
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, phone);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) { // If a record is found
                        return "Phone number already exists in the system."; // Return error message
                    }
                }
            }

            // --- STEP 2: Insert into 'user' table ---
            int newUserId = -1;
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, phone);
                userStmt.setString(2, email);
                userStmt.executeUpdate();
                
                try (ResultSet keys = userStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        newUserId = keys.getInt(1); // Capture the auto-generated user_id
                    }
                }
            }

            if (newUserId == -1) throw new SQLException("Failed to generate User ID.");

            // --- STEP 3: Insert into 'subscriber' table ---
            // We will generate the subscriber_id based on a simple timestamp/random logic or DB sequence
            long generatedSubId = (long)(Math.random() * 900000) + 100000; // Example 6-digit subscriber ID

            try (PreparedStatement subStmt = conn.prepareStatement(insertSubSql)) {
                subStmt.setInt(1, newUserId); // Foreign key to user table
                subStmt.setLong(2, generatedSubId); // The unique subscriber_id
                subStmt.executeUpdate();
            }

            // --- STEP 4: Finalize Transaction ---
            conn.commit(); // Save changes
            return generatedSubId; // Return the ID to the handler

        } catch (SQLException e) { // Catch any SQL errors
            try {
                if (conn != null) conn.rollback(); // Undo changes on failure
            } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return "Critical database error occurred.";
        } finally { // Restore connection state
            try {
                conn.setAutoCommit(true); //
            } catch (SQLException e) { e.printStackTrace(); }
        } // End finally
    } // End method
    
    /**
     * Retrieves all active waiting list entries from the database.
     * <p>
     * This method queries the {@code waiting_list_entry} table to fetch records with a 
     * status of either 'WAITING' or 'NOTIFIED'. Each row in the result set is mapped 
     * to a {@code WaitingListEntry} object and added to the returned list.
     * </p>
     *
     * @return An {@code ArrayList<common.WaitingListEntry>} containing the retrieved 
     * waiting list entries. Returns an empty list if no matching records are found 
     * or if a database error occurs.
     */
    public static ArrayList<common.WaitingListEntry> getWaitingListEntries() {
        ArrayList<common.WaitingListEntry> list = new ArrayList<>();
        
        // שאילתה המסננת רשומות שהן בסטטוס המתנה או שכבר קיבלו הודעה
        String sql = "SELECT * FROM waiting_list_entry WHERE status = 'WAITING' OR status = 'NOTIFIED'";
        
        // שימוש בחיבור הקיים דרך ה-DBController של השרת
        Connection conn = MainControllers.DBController.getInstance().getConnection();
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                // יצירת אובייקט WaitingListEntry עבור כל שורה ב-DB
                // סדר הפרמטרים: confirmationCode, entryTime, numberOfGuests, userId, status, notificationTime
                common.WaitingListEntry entry = new common.WaitingListEntry(
                    rs.getLong("confirmation_code"),
                    rs.getString("entry_time"),
                    rs.getInt("number_of_guests"),
                    rs.getInt("user_id"),
                    rs.getString("status"),
                    rs.getString("notification_time")
                );
                list.add(entry);
            }
        } catch (SQLException e) {
            // תיעוד השגיאה במקרה של בעיה בשליפה מה-DB
            e.printStackTrace();
        }
        
        return list;
    }
 
} // End of class



