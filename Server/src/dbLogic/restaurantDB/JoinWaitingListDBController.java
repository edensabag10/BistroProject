package dbLogic.restaurantDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

import MainControllers.DBController;

/**
 * Handles database operations related to the waiting_list_entry table. Provides
 * utilities for inserting entries, updating status, and querying waiting list
 * and restaurant availability data.
 */
public class JoinWaitingListDBController {

	/**
	 * Inserts a new entry into the waiting list table. Used when no immediate table
	 * is available for the guest.
	 *
	 * @param confirmationCode The unique long code assigned to this waiting list
	 *                         entry.
	 * @param userId           The ID of the user (subscriber or guest).
	 * @param numberOfGuests   The party size.
	 * @param status           The initial status (e.g., 'WAITING' or 'ARRIVED').
	 * @throws SQLException If a database access error occurs.
	 */
	public static void insertWaitingListEntry(long confirmationCode, int userId, int numberOfGuests, String status)
			throws SQLException {

		String sql = "INSERT INTO waiting_list_entry "
				+ "(confirmation_code, entry_time, number_of_guests, user_id, status, notification_time) "
				+ "VALUES (?, NOW(), ?, ?, ?, NULL)";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, confirmationCode);
			ps.setInt(2, numberOfGuests);
			ps.setInt(3, userId);
			ps.setString(4, status);

			ps.executeUpdate();
		}
	}

	/**
	 * Updates the status of an existing waiting list entry and records the
	 * notification time.
	 *
	 * @param confirmationCode The unique identifier for the entry.
	 * @param newStatus        The new status string (e.g., 'NOTIFIED',
	 *                         'CANCELLED').
	 * @throws Exception If a database error occurs or the connection fails.
	 */
	public static void updateStatus(long confirmationCode, String newStatus) throws Exception {

		String sql = "UPDATE waiting_list_entry " + "SET status = ?, notification_time = NOW() "
				+ "WHERE confirmation_code = ?";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, newStatus);
			ps.setLong(2, confirmationCode);
			ps.executeUpdate();
		}
	}

	/**
	 * Retrieves the current status of a waiting list entry.
	 *
	 * @param confirmationCode The unique identifier for the entry.
	 * @return A String representing the current status, or null if no entry is
	 *         found.
	 * @throws Exception If a database access error occurs.
	 */
	public static String getStatusByCode(long confirmationCode) throws Exception {
		String sql = "SELECT status FROM waiting_list_entry WHERE confirmation_code = ?";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, confirmationCode);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString("status");
				}
			}
		}
		return null;
	}

	/**
	 * Retrieves the current status of a waiting list entry.
	 *
	 * @param confirmationCode The unique identifier for the entry.
	 * @return A String representing the current status, or null if no entry is
	 *         found.
	 * @throws Exception If a database access error occurs.
	 */
	public static boolean isUserAlreadyActive(int userId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM waiting_list_entry "
				+ "WHERE user_id = ? AND (status = 'WAITING' OR status = 'ARRIVED')";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1) > 0;
				}
			}
		}
		return false;
	}

	/**
	 * Determines if the restaurant is currently open based on special or regular
	 * hours. Special hours take precedence over regular weekly hours.
	 *
	 * @return true if the current time falls within the restaurant's operating
	 *         hours, false otherwise.
	 * @throws Exception If a database error occurs during the check.
	 */
	public static boolean isRestaurantOpenNow() throws Exception {

		Connection conn = DBController.getInstance().getConnection();
		LocalTime now = LocalTime.now();

		// --- Check special opening hours (override regular hours) ---
		String specialSql = "SELECT tr.open_time, tr.close_time " + "FROM restaurant_special_hours sh "
				+ "JOIN time_range tr ON sh.time_range_id = tr.time_range_id " + "WHERE sh.special_date = CURDATE()";

		try (PreparedStatement ps = conn.prepareStatement(specialSql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				LocalTime open = rs.getTime("open_time").toLocalTime();
				LocalTime close = rs.getTime("close_time").toLocalTime();

				return !now.isBefore(open) && !now.isAfter(close);
			}
		}

		// --- Fallback to regular weekly opening hours ---
		String dayName = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

		String regularSql = "SELECT tr.open_time, tr.close_time " + "FROM restaurant_regular_hours rrh "
				+ "JOIN time_range tr ON rrh.time_range_id = tr.time_range_id " + "WHERE rrh.day_of_week = ?";

		try (PreparedStatement ps = conn.prepareStatement(regularSql)) {

			ps.setString(1, dayName);

			try (ResultSet rs = ps.executeQuery()) {

				if (!rs.next()) {
					return false;
				}

				LocalTime open = rs.getTime("open_time").toLocalTime();
				LocalTime close = rs.getTime("close_time").toLocalTime();

				// Closed day
				if (open.equals(close)) {
					return false;
				}

				return !now.isBefore(open) && !now.isAfter(close);
			}
		}

	}

	/**
	 * Checks if there are any guests currently with a 'WAITING' status. Used to
	 * maintain the order of the queue and prevent skipping.
	 *
	 * @return true if there is at least one active waiting entry, false otherwise.
	 * @throws SQLException If a database error occurs.
	 */
	public static boolean hasWaitingGuests() throws SQLException {

		String sql = "SELECT COUNT(*) " + "FROM waiting_list_entry " + "WHERE status = 'WAITING'";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
		}
		return false;
	}
}
