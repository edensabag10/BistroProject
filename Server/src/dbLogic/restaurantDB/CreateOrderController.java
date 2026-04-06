package dbLogic.restaurantDB; // Define the package for restaurant database logic

import java.sql.*; // Import SQL classes for database interactions
import java.time.LocalDate; // Import LocalDate for date manipulation
import java.time.LocalDateTime; // Import LocalDateTime for date and time manipulation
import java.time.LocalTime; // Import LocalTime for time manipulation
import java.time.format.DateTimeFormatter; // Import DateTimeFormatter for formatting date-time strings
import java.util.*; // Import utility classes like List, Map, and Collections

import MainControllers.DBController; // Import the database singleton controller
import common.Reservation; // Import the Reservation DTO
import common.Restaurant; // Import the Restaurant domain entity
import common.ServiceResponse; // Import the service response wrapper
import common.ServiceResponse.ServiceStatus; // Import the status enum for service responses
import serverLogic.serverRestaurant.RestaurantManager; // Import the manager to access restaurant data

/**
 * The CreateOrderController handles the core Server-side business logic for
 * allocating restaurant tables and managing the reservation lifecycle.
 */
public class CreateOrderController { // Start of CreateOrderController class definition

	// Static formatter to ensure SQL DATETIME strings follow the correct
	// 'YYYY-MM-DD HH:mm:00' format
	private static final DateTimeFormatter sqlFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00"); // Formatter
																												// definition

	/**
	 * Entry point for processing a new reservation request from the Client.
	 * Validates operating hours, checks availability, and provides suggestions if
	 * full.
	 *
	 * @param res The Reservation object containing requested date, time, and guest
	 *            count.
	 * @return A ServiceResponse indicating SUCCESS (with confirmation code),
	 *         SUGGESTION, FULL, or ERROR.
	 * @throws Exception if parsing or internal logic fails (caught internally).
	 */
	public static ServiceResponse processNewReservation(Reservation res) { // Start of processNewReservation method

		try { // Start of main try block to catch logic or parsing errors

			// Step 1: Parse the string from the Reservation DTO into a LocalDateTime object
			LocalDateTime requestedDT = LocalDateTime.parse(res.getReservationDateTime(), sqlFormatter); // Parsing
																											// operation

			// Step 2: Access the global restaurant instance to check hours and inventory
			Restaurant restaurant = RestaurantManager.getInstance(); // Retrieving singleton instance

			// Safety Check: Ensure the restaurant data is actually loaded in the server
			// memory
			if (restaurant == null) { // Check if restaurant instance is null
				// Return an error response if the server failed to initialize restaurant data
				return new ServiceResponse(ServiceStatus.INTERNAL_ERROR,
						"Server Error: Restaurant data not initialized."); // Error return
			} // End of null check

			// --- PHASE 0: Operating Hours Validation (2-Hour Dining Window) ---

			// Extract the date from the requested timestamp
			LocalDate date = requestedDT.toLocalDate(); // Get date part

			// Extract the starting time of the reservation
			LocalTime startTime = requestedDT.toLocalTime(); // Get time part

			// Calculate the expected end time (standard 2-hour meal duration)
			LocalTime endTime = startTime.plusHours(2); // Add 2 hours to start time

			// Format times to "HH:mm" for comparison with restaurant operating hours
			String startTimeStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm")); // Format start time
			String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm")); // Format end time

			// Logic: The restaurant must be open at the start AND at the end of the 2-hour
			// window
			boolean isStartOpen = restaurant.isOpen(date, startTimeStr); // Check opening at start
			boolean isEndOpen = restaurant.isOpen(date, endTimeStr); // Check opening at end

			if (!isStartOpen || !isEndOpen) { // If closed during any part of the window
				// Return a specific status informing the client that the time is outside
				// operational hours
				return new ServiceResponse(ServiceStatus.RESERVATION_OUT_OF_HOURS,
						"The restaurant is outside operational hours."); // Out of hours return
			} // End of hours check

			// --- PHASE 1: Direct Availability Check ---

			// Search for an available table that fits the party size for the exact
			// requested slot
			int allocatedTableSize = findAvailableTableSize(restaurant, requestedDT, res.getNumberOfGuests()); // Calling
																												// table
																												// search

			if (allocatedTableSize != -1) { // If a table was successfully found (allocatedTableSize is not -1)

				// Persist the reservation in the database and get the generated confirmation
				// code
				Long confCode = saveNewReservation(res, allocatedTableSize); // Attempting to save to DB

				if (confCode != null) { // If the database successfully returned a primary key
					// Return success with the unique confirmation code as the payload
					return new ServiceResponse(ServiceStatus.RESERVATION_SUCCESS, confCode); // Success return
				} // End of save success check

				// Handle database insertion failure
				return new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "Database failed to save the reservation."); // Save
																														// error
																														// return
			} // End of Phase 1 success check

			// --- PHASE 2: 3-Day Lookahead (Intelligent Suggestions) ---

			// If the requested slot is full, iterate through the next 3 days to find a
			// match
			for (int i = 1; i <= 3; i++) { // Start of lookahead loop

				// Calculate the timestamp for the current iteration (Day + i)
				LocalDateTime nextDT = requestedDT.plusDays(i); // Adding i days
				LocalDate nextDate = nextDT.toLocalDate(); // Extracting date

				// Define the 2-hour meal window for the suggested day
				LocalTime nextStart = nextDT.toLocalTime(); // Getting suggested start time
				LocalTime nextEnd = nextStart.plusHours(2); // Calculating suggested end time

				// Format suggested times for operational checks
				String nextStartTimeStr = nextStart.format(DateTimeFormatter.ofPattern("HH:mm")); // Formatting start
				String nextEndTimeStr = nextEnd.format(DateTimeFormatter.ofPattern("HH:mm")); // Formatting end

				// Check 1: Is the restaurant open for the full duration on this alternative
				// day?
				boolean isNextDayOpen = (restaurant.isOpen(nextDate, nextStartTimeStr)
						&& restaurant.isOpen(nextDate, nextEndTimeStr)); // Boolean check

				if (isNextDayOpen) { // If open on the alternative day

					// Check 2: Is there an available table of the required size at this suggested
					// time?
					int suggestedTableSize = findAvailableTableSize(restaurant, nextDT, res.getNumberOfGuests()); // Checking
																													// availability

					if (suggestedTableSize != -1) { // If capacity is found on this alternative day

						// Format the suggested date-time into a string for the client to display
						String suggestion = nextDT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); // Formatting
																											// suggestion

						// Return a suggestion response containing the alternative slot
						return new ServiceResponse(ServiceStatus.RESERVATION_SUGGESTION, suggestion); // Suggestion
																										// return
					} // End of capacity check
				} // End of open check
			} // End of 3-day lookahead loop

			// --- PHASE 3: Capacity Reached ---

			// If no slots were found in the requested time or the 3-day window, inform the
			// client
			return new ServiceResponse(ServiceStatus.RESERVATION_FULL, "No tables available for the next 3 days."); // Full
																													// return

		} catch (Exception e) { // Catch block for any unexpected runtime exceptions
			// Print technical error details for server-side debugging
			e.printStackTrace(); // Logging exception
			// Return an internal error response with the exception message
			return new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "Server Exception: " + e.getMessage()); // Exception
																												// return
		} // End of try-catch block
	} // End of processNewReservation method

	/**
	 * Finds the smallest available table size that can accommodate the party size.
	 * Uses a "Best Fit" logic by sorting available capacities.
	 *
	 * @param restaurant    The restaurant instance containing the physical
	 *                      inventory.
	 * @param dt            The requested date and time for the reservation.
	 * @param requestedSize The number of guests in the party.
	 * @return The capacity of the allocated table size, or -1 if no table is
	 *         available.
	 */
	private static int findAvailableTableSize(Restaurant restaurant, LocalDateTime dt, int requestedSize) { // Start
																											// method
		// Retrieve a copy of the physical table inventory from the restaurant entity
		Map<Integer, Integer> inventory = restaurant.getFullInventory(); // Key: Size, Value: Count

		// Extract and sort table sizes (KeySet) to ensure the "Best Fit" logic
		List<Integer> sortedCapacities = new ArrayList<>(inventory.keySet()); // Create list of sizes
		Collections.sort(sortedCapacities); // Sort ascending (e.g., 2, 4, 6, 8)

		for (int capacity : sortedCapacities) { // Iterate through each available table size
			// Skip sizes that are physically too small for the requested group
			if (capacity < requestedSize) { // If capacity is less than group size
				continue; // Move to the next larger table size
			} // End check

			// Calculate how many tables of this specific size are already booked at this
			// time
			int currentReservedCount = getReservedTablesCount(dt, capacity); // Query database for occupancy

			// Fetch the total number of tables of this size the restaurant actually owns
			int totalPhysicalTables = inventory.get(capacity); // Get inventory count

			// Availability Rule: A table is free if current bookings < total inventory
			if (currentReservedCount < totalPhysicalTables) { // If slot is not full
				return capacity; // Return this table size as the best fit
			} // End of availability check
		} // End of capacities loop

		return -1; // Return -1 if no tables of any valid size are available
	} // End of findAvailableTableSize method

	/**
	 * Queries the database to count existing reservations that overlap with the
	 * requested slot. A slot is considered occupied if another reservation exists
	 * within a +/- 2-hour window.
	 *
	 * @param dt       The requested date and time.
	 * @param capacity The table capacity being checked.
	 * @return The count of currently occupied tables of that size.
	 * @throws SQLException If a database access error occurs (caught internally).
	 */
	private static int getReservedTablesCount(LocalDateTime dt, int capacity) { // Start method

		// Query Logic: A table is occupied if a reservation exists within a 4-hour
		// window (+/- 2 hours)
		String query = "SELECT COUNT(*) FROM reservation " + "WHERE number_of_guests = ? "
				+ "AND status NOT IN ('CANCELLED', 'FINISHED', 'NOSHOW') "
				+ "AND reservation_datetime > DATE_SUB(?, INTERVAL 2 HOUR) "
				+ "AND reservation_datetime < DATE_ADD(?, INTERVAL 2 HOUR)";

		try (PreparedStatement pstmt = DBController.getInstance().getConnection().prepareStatement(query)) { // Prepare
																												// statement
			// Bind the table capacity being checked to the first parameter
			pstmt.setInt(1, capacity); // Setting capacity

			// Bind the requested time to the window boundaries (using DATE_SUB and DATE_ADD
			// in SQL)
			pstmt.setString(2, dt.format(sqlFormatter)); // Setting lower boundary
			pstmt.setString(3, dt.format(sqlFormatter)); // Setting upper boundary

			try (ResultSet rs = pstmt.executeQuery()) { // Execute count query
				// If a result is found, return the count from the first column
				if (rs.next()) { // If row exists
					return rs.getInt(1); // Return the count
				} // End if
			} // End result set try
		} catch (SQLException e) { // Catch block for SQL errors
			// Log technical details
			e.printStackTrace(); // Printing stack trace
		} // End of try-catch block

		// Safety Policy: If database fails, return a high number to prevent accidental
		// overbooking
		return 999; // Error fallback value
	} // End of getReservedTablesCount method

	/**
	 * Persists a new reservation record in the database and retrieves the
	 * confirmation code.
	 *
	 * @param res            The Reservation DTO containing customer and booking
	 *                       details.
	 * @param finalTableSize The actual table capacity allocated for this booking.
	 * @return The generated confirmation code (Long), or null if the operation
	 *         fails.
	 * @throws SQLException If a database access error occurs (caught internally).
	 */
	private static Long saveNewReservation(Reservation res, int finalTableSize) { // Start method

		// Insert query for the reservation table
		String query = "INSERT INTO reservation (reservation_datetime, number_of_guests, user_id, status) VALUES (?, ?, ?, ?)"; // SQL
																																// string

		// Obtain connection from the DB singleton
		Connection conn = DBController.getInstance().getConnection(); // Getting connection

		try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) { // Prepare with
																										// key retrieval

			// Bind parameters from the DTO and the calculated table size
			pstmt.setString(1, res.getReservationDateTime()); // Bind date-time string
			pstmt.setInt(2, finalTableSize); // Bind the capacity allocated (might be larger than guest count)
			pstmt.setInt(3, res.getUserId()); // Bind the customer ID
			pstmt.setString(4, res.getStatusString()); // Bind the initial status (ACTIVE)

			// Execute the insert and check if it affected any rows
			if (pstmt.executeUpdate() > 0) { // If insertion was successful

				// Retrieve the auto-incremented primary key (Confirmation Code)
				try (ResultSet gk = pstmt.getGeneratedKeys()) { // Getting generated keys
					if (gk.next()) { // If key exists
						return gk.getLong(1); // Return the unique long ID
					} // End if
				} // End result set try
			} // End of update check
		} catch (SQLException e) { // Catch database exceptions
			// Print technical error details
			e.printStackTrace(); // Logging error
		} // End of try-catch block

		return null; // Return null if the save operation failed
	} // End of saveNewReservation method

} // End of CreateOrderController class definition