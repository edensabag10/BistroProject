package dbLogic.restaurantDB; // Defining the package for restaurant-related database controllers

import java.sql.Connection; // Importing SQL connection interface
import java.sql.PreparedStatement; // Importing prepared statement for parameterized queries
import java.sql.ResultSet; // Importing result set for query results
import java.util.ArrayList; // Importing ArrayList for result aggregation
import java.util.List; // Importing List interface

import MainControllers.DBController; // Importing the central DB connection manager
import common.Reservation; // Importing the Reservation domain entity
import common.Reservation.ReservationStatus; // Importing reservation status enum

/**
 * Database controller responsible for retrieving reservation history data.
 * Provides read-only access to reservation records for a specific user.
 */
public class DBReservationsHistoryController {

	/**
	 * Retrieves all reservations associated with a given user ID from the database.
	 * The results are sorted by the reservation date and time in descending order
	 * (most recent first).
	 *
	 * @param userId The unique identifier of the subscriber whose history is being
	 *               fetched.
	 * @return A List of {@link Reservation} objects representing the user's
	 *         complete history. Returns an empty list if no records are found or if
	 *         an error occurs.
	 * @throws Exception Although exceptions are caught internally, this method
	 *                   interacts with the database and handles SQL-related issues.
	 */
	public List<Reservation> getReservationsForUser(int userId) {

		// Container for the retrieved reservation records
		List<Reservation> reservations = new ArrayList<>();

		// SQL query for fetching reservation history for a specific user
		String sql = """
				    SELECT confirmation_code,
				           reservation_datetime,
				           number_of_guests,
				           status
				    FROM reservation
				    WHERE user_id = ?
				    ORDER BY reservation_datetime DESC
				""";

		try {
			// Obtain an active database connection from the central controller
			Connection conn = DBController.getInstance().getConnection();
			// Prepare a parameterized SQL statement to prevent SQL injection
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

				pstmt.setInt(1, userId); // Bind the user ID to the query parameter
				ResultSet rs = pstmt.executeQuery(); // Execute the query and retrieve the result set

				// Iterate over all matching reservation records
				while (rs.next()) {
					// Construct a Reservation object using core reservation fields
					Reservation reservation = new Reservation(userId, rs.getString("reservation_datetime"),
							rs.getInt("number_of_guests"));
					// Assign the confirmation code retrieved from the database
					reservation.setConfirmationCode(rs.getLong("confirmation_code"));

					// Convert the stored status string into a ReservationStatus enum
					try {
						ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
						reservation.setStatus(status);
					} catch (Exception e) {
						// If status parsing fails, default status remains unchanged
					}

					// Add the fully populated reservation object to the result list
					reservations.add(reservation);
				}
			}

		} catch (Exception e) {
			// Log database or connection errors for server-side diagnostics
			e.printStackTrace();
		}

		// Return the complete reservation history list
		return reservations;
	}
}
