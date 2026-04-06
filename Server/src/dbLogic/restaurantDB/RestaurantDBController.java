package dbLogic.restaurantDB; // Defining the package for restaurant database operations

import java.sql.*; // Importing standard SQL classes for JDBC operations
import java.time.LocalDate; // Importing LocalDate for modern date handling
import java.util.ArrayList; // Importing ArrayList for dynamic list structures
import java.util.List; // Importing List interface

import common.Reservation; // Importing the Reservation domain model
import common.Restaurant; // Importing the Restaurant entity class
import common.Reservation.ReservationStatus; // Importing ReservationStatus enum
import MainControllers.DBController; // Importing the singleton database controller

/**
 * The RestaurantDBController is a specialized data access object (DAO)
 * responsible for reconstructing a complete Restaurant domain entity from the
 * database.
 */
public class RestaurantDBController { // Start of the RestaurantDBController class

	/**
	 * Performs a deep load of a restaurant's data based on its unique ID. This
	 * includes basic info, physical table counts, and both regular and special
	 * operating hours.
	 *
	 * @param restaurantId The unique identifier of the restaurant to load.
	 * @return A fully populated {@link Restaurant} object if found, or null if no
	 *         restaurant exists with the given ID.
	 * @throws SQLException If a database access error occurs during any phase of
	 *                      the loading process.
	 */
	public static Restaurant loadFullRestaurantData(int restaurantId) throws SQLException { // Start of the loading
																							// method

		// Initializing the restaurant object as null until basic metadata is retrieved
		Restaurant restaurant = null; // Declaration and initialization

		// Accessing the shared database connection from the singleton DBController
		Connection conn = DBController.getInstance().getConnection(); // Retrieving the active connection

		try { // Start of the main database access block

			// --- PHASE 1: Basic Identification ---

			// SQL query to fetch the name of the restaurant based on its ID
			String queryName = "SELECT name FROM restaurant WHERE restaurant_id = ?"; // Query definition

			// Preparing the statement to prevent SQL injection and set parameters
			try (PreparedStatement stmt = conn.prepareStatement(queryName)) { // Initializing the prepared statement
				// Binding the method parameter restaurantId to the first question mark in the
				// query
				stmt.setInt(1, restaurantId); // Parameter assignment

				// Executing the query and storing results in a ResultSet
				ResultSet rs = stmt.executeQuery(); // Execution

				// If a record is found for this restaurantId
				if (rs.next()) { // Checking for results
					// Instantiate the Restaurant object with the ID and the name retrieved from the
					// DB
					restaurant = new Restaurant(restaurantId, rs.getString("name")); // Object creation
				} // End of if block
			} // End of name query try-with-resources block

			// Guard Clause: If no restaurant was found in Phase 1, return null immediately
			if (restaurant == null) { // Null check
				return null; // Early exit
			} // End of guard clause

			// --- PHASE 2: Physical Table Inventory ---

			// SQL query to count total tables grouped by their seating capacity (Best Fit
			// Algorithm preparation)
			String queryTables = "SELECT t.capacity, COUNT(*) as total " + // Select columns
					"FROM restaurant_table rt " + // From the mapping table
					"JOIN `table` t ON rt.table_id = t.table_id " + // Join with the table entity table
					"WHERE rt.restaurant_id = ? GROUP BY t.capacity"; // Filter by restaurant and group results

			// Preparing the statement for table inventory retrieval
			try (PreparedStatement stmt = conn.prepareStatement(queryTables)) { // Initializing the statement
				// Binding the restaurantId to the query filter
				stmt.setInt(1, restaurantId); // Parameter assignment

				// Executing the query to get table capacity counts
				ResultSet rs = stmt.executeQuery(); // Execution

				// Iterate through each capacity group (e.g., 2-person tables, 4-person tables)
				while (rs.next()) { // Loop start
					// Update the restaurant object's internal inventory map with the retrieved data
					restaurant.addTablesToInventory(rs.getInt("capacity"), rs.getInt("total")); // Inventory update
				} // End of inventory loop
			} // End of inventory query try-with-resources block

			// --- PHASE 3: Regular Operating Hours ---

			// SQL query to fetch standard weekly schedule joined with specific time ranges
			String queryHours = "SELECT rh.day_of_week, tr.open_time, tr.close_time " + // Select columns
					"FROM restaurant_regular_hours rh " + // From regular hours table
					"JOIN time_range tr ON rh.time_range_id = tr.time_range_id " + // Join with time ranges
					"WHERE rh.restaurant_id = ?"; // Filter by restaurant ID

			// Preparing the statement for weekly schedule retrieval
			try (PreparedStatement stmt = conn.prepareStatement(queryHours)) { // Initializing the statement
				// Binding the restaurantId to the query filter
				stmt.setInt(1, restaurantId); // Parameter assignment

				// Executing the query to get standard opening/closing hours
				ResultSet rs = stmt.executeQuery(); // Execution

				// Iterate through the results (typically 7 rows for each day of the week)
				while (rs.next()) { // Loop start
					// Map the DB day name and time strings into the restaurant object
					restaurant.setRegularHours( // Call setter
							rs.getString("day_of_week"), // Extract day name
							rs.getString("open_time"), // Extract open time
							rs.getString("close_time") // Extract close time
					); // End of setter call
				} // End of regular hours loop
			} // End of hours query try-with-resources block

			// --- PHASE 4: Special Operating Hours Overrides ---

			// SQL query to fetch date-specific overrides (e.g., Holidays) joined with time
			// ranges
			String querySpecial = "SELECT sh.special_date, tr.open_time, tr.close_time " + // Select columns
					"FROM restaurant_special_hours sh " + // From special hours table
					"JOIN time_range tr ON sh.time_range_id = tr.time_range_id " + // Join with time ranges
					"WHERE sh.restaurant_id = ?"; // Filter by restaurant ID

			// Preparing the statement for special overrides retrieval
			try (PreparedStatement stmt = conn.prepareStatement(querySpecial)) { // Initializing the statement
				// Binding the restaurantId to the query filter
				stmt.setInt(1, restaurantId); // Parameter assignment

				// Executing the query to get holiday/event specific hours
				ResultSet rs = stmt.executeQuery(); // Execution

				// Iterate through any specific date overrides found in the DB
				while (rs.next()) { // Loop start
					// Converting the SQL Date object to a modern Java LocalDate object
					LocalDate specialDate = rs.getDate("special_date").toLocalDate(); // Date conversion

					// Populate the special hours map within the restaurant object
					restaurant.setSpecialHours( // Call setter
							specialDate, // Assign the specific date
							rs.getString("open_time"), // Assign the new opening time
							rs.getString("close_time") // Assign the new closing time
					); // End of setter call
				} // End of special hours loop
			} // End of special query try-with-resources block

		} catch (SQLException e) { // Catch block for any SQL or connectivity errors
			// Standard error handling: Print technical stack trace for server-side
			// debugging
			e.printStackTrace(); // Logging technical failure
		} // End of main try-catch block

		// Return the fully constructed and populated Restaurant domain object
		return restaurant; // Return result

	} // End of loadFullRestaurantData method

} // End of RestaurantDBController class