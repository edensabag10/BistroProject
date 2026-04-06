package dbLogic.restaurantDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.Duration;
import MainControllers.DBController;
import common.Visit;

import java.util.ArrayList;
import java.util.List;

/**
 * VisitController handles the arrival logic and seating transactions at the
 * Terminal. This class coordinates between reservations, waiting lists, and
 * table management.
 */
public class VisitController {

	/**
	 * Entry point for Terminal arrival. Validates codes from both reservations and
	 * waiting lists. * @param code The confirmation code entered by the customer at
	 * the terminal.
	 * 
	 * @return String status code (e.g., "SUCCESS_TABLE_X", "INVALID_CODE",
	 *         "TOO_EARLY").
	 */
	public synchronized static String processTerminalArrival(long code) {
		Connection conn = DBController.getInstance().getConnection();
		try {
			// 1. Search in confirmed reservations (Including Waiting and Notified statuses)
			String resQuery = "SELECT * FROM reservation WHERE confirmation_code = ? "
					+ "AND status IN ('ACTIVE', 'WAITING_AT_RESTAURANT', 'NOTIFIED')";
			try (PreparedStatement ps = conn.prepareStatement(resQuery)) {
				ps.setLong(1, code);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return handleReservationFlow(conn, rs, code);
				}
			}

			// 2. Search in notified/waiting list entries
			String waitQuery = "SELECT * FROM waiting_list_entry WHERE confirmation_code = ? "
					+ "AND status IN ('WAITING', 'NOTIFIED')";
			try (PreparedStatement ps = conn.prepareStatement(waitQuery)) {
				ps.setLong(1, code);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return handleWaitingListArrival(conn, rs, code);
				}
			}
			return "INVALID_CODE";
		} catch (SQLException e) {
			e.printStackTrace();
			return "DATABASE_ERROR";
		}
	}

	/**
	 * Logic for pre-booked reservations. Handles early arrivals and notified
	 * priorities. * @param conn Connection to the database.
	 * 
	 * @param rs   ResultSet containing reservation details.
	 * @param code The confirmation code.
	 * @return Status message for the UI.
	 * @throws SQLException If database access fails.
	 */
	private static String handleReservationFlow(Connection conn, ResultSet rs, long code) throws SQLException {
		String status = rs.getString("status");
		int guests = rs.getInt("number_of_guests");
		int userId = rs.getInt("user_id");

		// PRIORITY 1: Guest was already NOTIFIED. Seat them immediately.
		if (status.equals("NOTIFIED")) {
			int tableId = findSuitableTable(guests);
			if (tableId != -1) {
				return proceedToSeating(conn, code, tableId, userId, "reservation");
			}
			return "TABLE_NOT_READY_WAIT"; // Should not happen
		}

		// PRIORITY 2: Guest is already in 'WAITING_AT_RESTAURANT'. They must wait for
		// SMS.
		if (status.equals("WAITING_AT_RESTAURANT")) {
			return "TABLE_NOT_READY_WAIT";
		}

		// PRIORITY 3: First arrival (Status: ACTIVE).
		Timestamp scheduledTs = rs.getTimestamp("reservation_datetime");
		long diffMinutes = Duration.between(scheduledTs.toLocalDateTime(), LocalDateTime.now()).toMinutes();

		// Enforce the 15-minute early arrival window
		if (diffMinutes < -15) {
			return "TOO_EARLY";
		}

		// Check if a table is available and NOT "promised" to a NOTIFIED guest
		if (isSeatingSafe(conn, guests)) {
			int tableId = findSuitableTable(guests);
			if (tableId != -1) {
				return proceedToSeating(conn, code, tableId, userId, "reservation");
			}
		}

		// No table or not safe? Move to waiting status and notify to wait for SMS.
		updateStatus(conn, "reservation", "WAITING_AT_RESTAURANT", code);
		return "TABLE_NOT_READY_WAIT";
	}

	/**
	 * Logic for walk-in arrivals from the waiting list. * @param conn Database
	 * connection.
	 * 
	 * @param rs   ResultSet from waiting_list_entry.
	 * @param code Confirmation code.
	 * @return Status message.
	 * @throws SQLException If database access fails.
	 */
	private static String handleWaitingListArrival(Connection conn, ResultSet rs, long code) throws SQLException {
		String status = rs.getString("status");
		if (status.equals("NOTIFIED")) {
			int tableId = findSuitableTable(rs.getInt("number_of_guests"));
			if (tableId != -1) {
				return proceedToSeating(conn, code, tableId, rs.getInt("user_id"), "waiting_list_entry");
			}
		}
		// Customers in 'WAITING' status must wait for the notification trigger.
		return "TABLE_NOT_READY_WAIT";
	}

	/**
	 * Ensures seating a guest won't "steal" a table promised to someone already
	 * notified. * @param conn Database connection.
	 * 
	 * @param guests Party size of the guest to be seated.
	 * @return true if seating is safe, false if it conflicts with notified guests.
	 * @throws SQLException If database access fails.
	 */
	private static boolean isSeatingSafe(Connection conn, int guests) throws SQLException {
		List<Integer> tables = new ArrayList<>();
		String sqlTables = "SELECT capacity FROM `table` WHERE is_available = 1 ORDER BY capacity ASC";
		try (PreparedStatement ps = conn.prepareStatement(sqlTables); ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				tables.add(rs.getInt("capacity"));
		}

		List<Integer> groups = new ArrayList<>();
		groups.add(guests);

		String sqlGroups = "SELECT number_of_guests FROM reservation WHERE status = 'NOTIFIED' " + "UNION ALL "
				+ "SELECT number_of_guests FROM waiting_list_entry WHERE status = 'NOTIFIED'";

		try (PreparedStatement ps = conn.prepareStatement(sqlGroups); ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				groups.add(rs.getInt("number_of_guests"));
		}

		groups.sort((a, b) -> b - a);

		for (int groupSize : groups) {
			boolean foundTable = false;
			for (int i = 0; i < tables.size(); i++) {
				if (tables.get(i) >= groupSize) {
					tables.remove(i);
					foundTable = true;
					break;
				}
			}
			if (!foundTable)
				return false;
		}

		return true;
	}

	/**
	 * Executes the seating transaction: Bill creation, status update, table
	 * occupation, and visit logging. * @param conn Database connection.
	 * 
	 * @param code        Confirmation code.
	 * @param tableId     Allocated table ID.
	 * @param userId      ID of the guest.
	 * @param sourceTable The DB table name to update ('reservation' or
	 *                    'waiting_list_entry').
	 * @return Success message with table ID.
	 * @throws SQLException If any part of the transaction fails.
	 */
	private static String proceedToSeating(Connection conn, long code, int tableId, int userId, String sourceTable)
			throws SQLException {
		try {
			conn.setAutoCommit(false); // Start transaction

			// 1. Create a new Bill
			int billId = createNewBill(conn);

			// 2. Update status to 'ARRIVED'
			updateStatus(conn, sourceTable, "ARRIVED", code);

			// 3. Occupy the table
			updateTableAvailability(conn, tableId, false);

			// 4. Create Visit record
			insertVisitRecord(conn, code, tableId, userId, billId);

			conn.commit();
			return "SUCCESS_TABLE_" + tableId;

		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(true);
		}
	}

	/**
	 * Triggered when a table is freed. Checks for priority (early arrival)
	 * reservations first. * @param tableId The ID of the vacated table.
	 */
	public static void handleTableFreed(int tableId) {
		Connection conn = DBController.getInstance().getConnection();
		try {
			// 1. Get the capacity of the freed table
			int capacity = 0;
			String capQuery = "SELECT capacity FROM `table` WHERE table_id = ?";
			try (PreparedStatement ps = conn.prepareStatement(capQuery)) {
				ps.setInt(1, tableId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
					capacity = rs.getInt("capacity");
			}

			if (capacity <= 0)
				return;

			// 2. Search for Priority Guests (WAITING_AT_RESTAURANT) ordered by original
			// reservation time
			String resQuery = "SELECT confirmation_code FROM reservation "
					+ "WHERE status = 'WAITING_AT_RESTAURANT' AND number_of_guests <= ? "
					+ "ORDER BY reservation_datetime ASC LIMIT 1";

			try (PreparedStatement psRes = conn.prepareStatement(resQuery)) {
				psRes.setInt(1, capacity);
				ResultSet rsRes = psRes.executeQuery();
				if (rsRes.next()) {
					// Priority match found: Update to NOTIFIED
					updateStatus(conn, "reservation", "NOTIFIED", rsRes.getLong("confirmation_code"));
					serverLogic.scheduling.VisitScheduler.startNoShowTimer(rsRes.getLong("confirmation_code"), tableId);
					System.out.println("[VisitController] Priority reservation notified.");
					return;
				}
			}

			// 3. No priority match: Delegate to WaitingListController for general walk-ins
			WaitingListController.handleTableFreed(tableId);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the current status of a guest's entry. * @param code The unique
	 * confirmation code.
	 * 
	 * @return Status string or "NOT_FOUND".
	 */
	public static String checkCurrentStatus(long code) {
		Connection conn = DBController.getInstance().getConnection();
		// Query both tables to find the current status of the code
		String query = "SELECT status FROM reservation WHERE confirmation_code = ? " + "UNION "
				+ "SELECT status FROM waiting_list_entry WHERE confirmation_code = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setLong(1, code);
			ps.setLong(2, code);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getString("status"); // Return status like 'NOTIFIED'
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "NOT_FOUND";
	}

	/**
	 * Retrieves all active diners currently seated in the restaurant. * @return
	 * ArrayList of active Visit objects.
	 */
	public static java.util.ArrayList<Visit> getAllActiveDiners() {
		java.util.ArrayList<Visit> activeDiners = new java.util.ArrayList<>();

		String sql = "SELECT v.*, COALESCE(r.number_of_guests, w.number_of_guests) as guests " + "FROM visit v "
				+ "LEFT JOIN reservation r ON v.confirmation_code = r.confirmation_code "
				+ "LEFT JOIN waiting_list_entry w ON v.confirmation_code = w.confirmation_code "
				+ "WHERE v.status = 'ACTIVE'";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				try {
					String statusStr = rs.getString("status");
					Visit.VisitStatus vStatus = Visit.VisitStatus.valueOf(statusStr);

					Visit v = new Visit(rs.getLong("confirmation_code"), rs.getInt("table_id"), rs.getInt("user_id"),
							rs.getLong("bill_id"), rs.getString("start_time"), vStatus);

					v.setNumberOfGuests(rs.getInt("guests"));
					activeDiners.add(v);

					// הדפסה ל-Console של השרת לצורך בדיקה
				} catch (IllegalArgumentException e) {
					System.err.println("Enum Mapping Error: " + rs.getString("status") + " is not valid.");
				}
			}
		} catch (SQLException e) {
			System.err.println("Database Execution Error: " + e.getMessage());
		}
		return activeDiners;
	}

	/**
	 * Gets the status of a specific reservation. * @param confirmationCode Unique
	 * code.
	 * 
	 * @return Status string or null.
	 * @throws Exception If database access fails.
	 */
	public static String getStatusByCode(long confirmationCode) throws Exception {
		String sql = "SELECT status FROM reservation WHERE confirmation_code = ?";

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
	 * General status update method. * @param confirmationCode Unique code.
	 * 
	 * @param newStatus New status value.
	 * @throws Exception If database access fails.
	 */
	public static void updateStatus(long confirmationCode, String newStatus) throws Exception {

		String sql = "UPDATE reservation " + "SET status = ?" + "WHERE confirmation_code = ?";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, newStatus);
			ps.setLong(2, confirmationCode);
			ps.executeUpdate();
		}
	}

	// --- Database Helper Methods ---

	/**
     * Finds the smallest available table that can accommodate the party size.
     * Searches for tables marked as 'is_available = 1' and sorts them by capacity.
     *
     * @param guests The number of guests that need to be seated.
     * @return The table_id of the most suitable table, or -1 if no suitable table is found.
     * @throws SQLException If a database access error occurs.
     */
	private static int findSuitableTable(int guests) throws SQLException {
		String query = "SELECT table_id FROM `table` WHERE is_available = 1 AND capacity >= ? ORDER BY capacity ASC LIMIT 1";
		try (PreparedStatement ps = DBController.getInstance().getConnection().prepareStatement(query)) {
			ps.setInt(1, guests);
			ResultSet rs = ps.executeQuery();
			return rs.next() ? rs.getInt("table_id") : -1;
		}
	}

	/**
     * Updates the status of a record in a specified database table.
     * * @param conn   The active database connection to use for the update.
     * @param table  The name of the database table to update (e.g., 'reservation' or 'waiting_list_entry').
     * @param status The new status string to set.
     * @param code   The unique confirmation code of the record to update.
     * @throws SQLException If a database access error occurs.
     */
	private static void updateStatus(Connection conn, String table, String status, long code) throws SQLException {
		String sql = "UPDATE " + table + " SET status = ? WHERE confirmation_code = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, status);
			ps.setLong(2, code);
			ps.executeUpdate();
		}
	}

	/**
     * Updates the availability flag of a specific table in the database.
     *
     * @param conn        The active database connection.
     * @param id          The unique identifier (table_id) of the table.
     * @param isAvailable The new availability state (true for free, false for occupied).
     * @throws SQLException If a database access error occurs.
     */
	private static void updateTableAvailability(Connection conn, int id, boolean isAvailable) throws SQLException {
		String sql = "UPDATE `table` SET is_available = ? WHERE table_id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, isAvailable);
			ps.setInt(2, id);
			ps.executeUpdate();
		}
	}

	/**
     * Creates a new blank bill record in the database to be associated with a new visit.
     *
     * @param conn The active database connection.
     * @return The auto-generated bill_id (primary key) of the new bill.
     * @throws SQLException If the bill record cannot be created or keys cannot be retrieved.
     */
	private static int createNewBill(Connection conn) throws SQLException {
		String sql = "INSERT INTO bill (base_amount, discount_percent, final_amount, is_paid) VALUES (0, 0, 0, 0)";
		try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next())
				return rs.getInt(1);
		}
		throw new SQLException("Failed to create bill record.");
	}

	/**
     * Inserts a new visit record into the database to track an active dining session.
     *
     * @param conn    The active database connection.
     * @param code    The confirmation code associated with the visit.
     * @param tableId The ID of the table where the guests are seated.
     * @param userId  The ID of the user (customer).
     * @param billId  The ID of the bill created for this visit.
     * @throws SQLException If a database access error occurs.
     */
	private static void insertVisitRecord(Connection conn, long code, int tableId, int userId, int billId)
			throws SQLException {
		String sql = "INSERT INTO visit (confirmation_code, table_id, user_id, bill_id, start_time, status) "
				+ "VALUES (?, ?, ?, ?, NOW(), 'ACTIVE')";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, code);
			ps.setInt(2, tableId);
			ps.setInt(3, userId);
			ps.setInt(4, billId);
			ps.executeUpdate();
		}
	}

}