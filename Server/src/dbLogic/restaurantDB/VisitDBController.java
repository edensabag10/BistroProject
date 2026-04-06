package dbLogic.restaurantDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import MainControllers.DBController;
import common.WaitingListEntry;

/**
 * Controller class for handling visit-related database operations. This
 * includes atomic transactions for seating guests and retrieving the waiting
 * list queue.
 */
public class VisitDBController {

	/**
     * Executes an atomic transaction to create a new bill and link it to a new visit record.
     * Uses manual transaction management (commit/rollback) to ensure data integrity.
     *
     * @param confirmationCode The unique identifier for the reservation or waiting entry.
     * @param tableId          The ID of the table assigned to this visit.
     * @param userId           The unique ID of the customer.
     * @return The auto-generated billId associated with this visit.
     * @throws SQLException If any part of the transaction fails, a rollback is performed.
     */
	public static int insertVisitAndCreateBill(long confirmationCode, int tableId, int userId) throws SQLException {

		Connection conn = DBController.getInstance().getConnection();

		try {
			conn.setAutoCommit(false);

			// 1️⃣ יצירת bill
			int billId;
			String billSql = "INSERT INTO bill (base_amount, discount_percent, final_amount, is_paid) "
					+ "VALUES (0, 0, 0, 0)";

			try (PreparedStatement ps = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {

				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				if (!rs.next()) {
					throw new SQLException("Failed to create bill");
				}
				billId = rs.getInt(1);
			}

			// 2️⃣ יצירת visit עם bill_id
			String visitSql = "INSERT INTO visit (confirmation_code, table_id, user_id, bill_id, start_time, status) "
					+ "VALUES (?, ?, ?, ?, NOW(), 'ACTIVE')";

			try (PreparedStatement ps = conn.prepareStatement(visitSql)) {
				ps.setLong(1, confirmationCode);
				ps.setInt(2, tableId);
				ps.setInt(3, userId);
				ps.setInt(4, billId);
				ps.executeUpdate();
			}

			conn.commit(); // ✅ הצלחה
			return billId;

		} catch (SQLException e) {
			conn.rollback(); // ❌ rollback מלא
			throw e;
		} finally {
			conn.setAutoCommit(true);
		}
	}

	/**
     * Retrieves all waiting list entries currently in 'WAITING' status.
     * Results are ordered by entry time in ascending order (First-In, First-Out).
     *
     * @return A {@link List} of {@link WaitingListEntry} objects representing the active queue.
     * @throws SQLException If a database access error occurs.
     */
	public static List<WaitingListEntry> getWaitingEntriesOrderedByEntryTime() throws SQLException {
		List<WaitingListEntry> waitingList = new ArrayList<>();
		String sql = "SELECT confirmation_code, entry_time, number_of_guests, user_id, status, notification_time "
				+ "FROM waiting_list_entry WHERE status = 'WAITING' ORDER BY entry_time ASC";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				WaitingListEntry entry = new WaitingListEntry(rs.getLong("confirmation_code"),
						rs.getString("entry_time"), rs.getInt("number_of_guests"), rs.getInt("user_id"),
						rs.getString("status"), rs.getString("notification_time"));
				waitingList.add(entry);
			}
		}
		return waitingList;
	}

}
