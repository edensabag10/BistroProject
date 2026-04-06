package dbLogic.restaurantDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import MainControllers.DBController;
import dbLogic.restaurantDB.TableDBController;

/**
 * Provides capacity-based availability checks for immediate seating. Determines
 * whether adding new guests would conflict with future reservations or guests
 * already notified from the waiting list.
 */
public class SeatingAvailabilityController {

	/**
	 * Determines whether incoming guests can be seated immediately without
	 * exceeding the restaurant's total capacity, taking into account active
	 * reservations and notified waiting list entries within the next 2 hours.
	 *
	 * @param incomingGuests Number of guests attempting to enter now.
	 * @param now            The current timestamp.
	 * @return true if there is enough capacity to seat the guests, false otherwise.
	 */
	public static boolean canSeatWithFutureReservations(int incomingGuests, LocalDateTime now) {

		// Define the end of the future reservation window (fixed duration)
		LocalDateTime end = now.plusHours(2);

		// Count guests from future active reservations within the window
		int futureGuests = getFutureReservedGuests(now, end);

		// Retrieve the maximum seating capacity of the restaurant
		int restaurantCapacity = TableDBController.getRestaurantMaxCapacity();

		// Debug output for capacity calculation
		System.out.println("DEBUG – futureGuests: " + futureGuests);
		System.out.println("DEBUG – incomingGuests: " + incomingGuests);
		System.out.println("DEBUG – restaurantCapacity: " + restaurantCapacity);

		// Allow seating only if total guests do not exceed capacity
		return futureGuests + incomingGuests <= restaurantCapacity;
	}

	/**
	 * Retrieves the total number of guests from ACTIVE reservations, guests waiting
	 * at the restaurant, and notified waiting list entries within a specific time
	 * window.
	 *
	 * @param start The start of the time window (usually 'now').
	 * @param end   The end of the time window (usually 'now' + 2 hours).
	 * @return The aggregated total number of guests expected to occupy seats.
	 * @throws RuntimeException If a database error occurs during the query.
	 */
	public static int getFutureReservedGuests(LocalDateTime start, LocalDateTime end) {

		String sql = "SELECT COALESCE(SUM(total_guests), 0) FROM (" + "  SELECT number_of_guests AS total_guests "
				+ "  FROM reservation " + "  WHERE reservation_datetime >= ? " + "  AND reservation_datetime < ? "
				+ "  AND status IN ('ACTIVE', 'WAITING_AT_RESTAURANT', 'NOTIFIED') " + "  UNION ALL "
				+ "  SELECT number_of_guests AS total_guests " + "  FROM waiting_list_entry "
				+ "  WHERE status = 'NOTIFIED'" + ") AS combined_data";

		// Acquire database connection
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			// Bind time window parameters
			ps.setObject(1, start);
			ps.setObject(2, end);

			// Execute query and extract aggregated result
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
				return 0;
			}

		} catch (Exception e) {
			// Escalate DB failures as runtime exceptions for upper-layer handling
			throw new RuntimeException("Failed to check future reservations", e);
		}
	}
}
