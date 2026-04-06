package dbLogic.restaurantDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import MainControllers.DBController;
import common.Bill;
import common.Visit;

/**
 * Controller class responsible for handling payment-related database
 * operations. Manages visit retrieval and the finalization of the billing
 * process.
 */
public class PaymentController {

	/**
	 * Retrieves active visit details based on a confirmation code.
	 *
	 * @param code The unique confirmation code for the visit.
	 * @return A {@link Visit} object if an active visit is found, null otherwise.
	 * @throws SQLException caught internally, logs error to stderr.
	 */
	public static Visit getVisitDetails(long code) {
		// Query to find an active visit by its confirmation code
		String query = "SELECT * FROM visit WHERE confirmation_code = ? AND status = ?";
		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			pstmt.setLong(1, code);
			pstmt.setString(2, Visit.VisitStatus.ACTIVE.name());

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return new Visit(rs.getLong("confirmation_code"), rs.getInt("table_id"), rs.getInt("user_id"),
							rs.getLong("bill_id"), rs.getString("start_time"), Visit.VisitStatus.ACTIVE);
				}
			}
		} catch (SQLException e) {
			System.err.println("Database error in getVisitDetails: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Finalizes the payment process by updating the bill, closing the visit, and
	 * releasing the table back to available status.
	 * <p>
	 * This method uses a SQL Transaction (commit/rollback) to ensure that all
	 * database updates succeed together or none at all.
	 * </p>
	 *
	 * @param bill The {@link Bill} object containing final amounts, IDs, and
	 *             confirmation code.
	 * @return true if the transaction was successfully committed, false if it
	 *         failed or was rolled back.
	 * @throws SQLException caught internally, handles transaction rollback on
	 *                      failure.
	 */
	public static boolean finalizePayment(Bill bill) {
		Connection conn = DBController.getInstance().getConnection();

		try {
			if (conn == null || conn.isClosed()) {
				System.err.println("Database connection is closed. Payment failed.");
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		// 1. Update bill details and set as paid
		String updateBill = "UPDATE bill SET base_amount = ?, discount_percent = ?, final_amount = ?, is_paid = 1, payment_time = NOW() WHERE bill_id = ?";
		// 2. Set visit status to FINISHED
		String updateVisit = "UPDATE visit SET status = 'FINISHED' WHERE confirmation_code = ?";
		// 3. Reset table availability. Note: `table` is a reserved keyword in SQL,
		// requiring backticks
		String updateTable = "UPDATE `table` SET is_available = ? WHERE table_id = ?";

		String updateRes = "UPDATE reservation SET status = 'FINISHED' WHERE confirmation_code = ?";

		String getTableIdSql = "SELECT table_id FROM visit WHERE confirmation_code = ?";
		try {
			// Disable auto-commit to manage the transaction manually
			conn.setAutoCommit(false);
			int tableId = -1;

			try (PreparedStatement psCap = conn.prepareStatement(getTableIdSql)) {
				psCap.setLong(1, bill.getConfirmationCode());
				ResultSet rs = psCap.executeQuery();
				if (rs.next())
					tableId = rs.getInt("table_id");
			}

			try (PreparedStatement psBill = conn.prepareStatement(updateBill);
					PreparedStatement psVisit = conn.prepareStatement(updateVisit);
					PreparedStatement psTable = conn.prepareStatement(updateTable);
					PreparedStatement psRes = conn.prepareStatement(updateRes)) {

				// Execute Bill Update
				psBill.setDouble(1, bill.getBaseAmount());
				psBill.setDouble(2, bill.getDiscountPercent());
				psBill.setDouble(3, bill.getFinalAmount());
				psBill.setLong(4, bill.getBillId());
				psBill.executeUpdate();

				// Execute Visit Update
				psVisit.setLong(1, bill.getConfirmationCode());
				psVisit.executeUpdate();

				// Execute Table Release
				if (tableId != -1) {
					psTable.setBoolean(1, true);
					psTable.setInt(2, tableId);
					psTable.executeUpdate();
				}

				// 4. Update Reservation to FINISHED (if exists)
				psRes.setLong(1, bill.getConfirmationCode());
				psRes.executeUpdate();

				// Commit all changes if no exceptions occurred
				conn.commit();

				// TRIGGER: Notify the seating engine that resources are free
				if (tableId != -1) {
					VisitController.handleTableFreed(tableId);
				}

				return true;
			} catch (SQLException e) {
				if (conn != null)
					conn.rollback();
				e.printStackTrace();
				return false;
			} finally {
				// Restore default auto-commit behavior
				if (conn != null && !conn.isClosed()) {
					conn.setAutoCommit(true);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Retrieves visit details and checks if the visiting user is a registered
	 * subscriber. This is used to determine if a discount should be applied to the
	 * bill.
	 *
	 * @param code The unique confirmation code for the visit.
	 * @return An {@link ArrayList} containing: index 0: The {@link Visit} object,
	 *         index 1: A {@link Boolean} (true if the user is a subscriber).
	 *         Returns null if the visit is not found.
	 */
	public static ArrayList<Object> getVisitWithSubscriberStatus(long code) {
		Visit v = getVisitDetails(code);
		if (v == null)
			return null;

		boolean isSub = false;
		String subQuery = "SELECT COUNT(*) FROM subscriber WHERE user_id = ?";
		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(subQuery)) {
			ps.setInt(1, v.getUserId());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next() && rs.getInt(1) > 0)
					isSub = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		ArrayList<Object> result = new ArrayList<>();
		result.add(v);
		result.add(isSub);
		return result;
	}
}
