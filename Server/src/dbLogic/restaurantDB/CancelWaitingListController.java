package dbLogic.restaurantDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import MainControllers.DBController;

/**
 * Controller class responsible for handling the cancellation of waiting list
 * entries in the database.
 */
public class CancelWaitingListController {

	/**
	 * Cancels a waiting list entry based on the user's ID. Updates the status to
	 * 'CANCELLED' only if the current status is 'WAITING'.
	 *
	 * @param userId The unique identifier of the user whose entry should be
	 *               cancelled.
	 * @return 1 if the update was successful, 0 if no matching 'WAITING' entry was
	 *         found, or -1 if a database error occurred.
	 * @throws SQLException Although caught internally, the method handles database
	 *                      access errors.
	 */
	public static int cancelWaitingEntry(int userId) {
		String query = "UPDATE waiting_list_entry SET status = 'CANCELLED' "
				+ "WHERE user_id = ? AND status = 'WAITING'";

		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			pstmt.setInt(1, userId);
			int rowsAffected = pstmt.executeUpdate();

			// If rowsAffected > 0, it means an entry was found and updated
			return rowsAffected > 0 ? 1 : 0;

		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Cancels a waiting list entry based on a unique confirmation code. This is
	 * useful for anonymous or occasional guests identified by their code. * @param
	 * confirmationCode The unique long code assigned to the waiting entry.
	 * 
	 * @return 1 if the update was successful, 0 if no matching 'WAITING' entry was
	 *         found, or -1 if a database error occurred.
	 * @throws SQLException Although caught internally, the method handles database
	 *                      access errors.
	 */
	public static int cancelWaitingEntry(long confirmationCode) { // Changed parameter to long
		// Updated query to filter by confirmation_code instead of user_id
		String query = "UPDATE waiting_list_entry SET status = 'CANCELLED' "
				+ "WHERE confirmation_code = ? AND status = 'WAITING'";

		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			pstmt.setLong(1, confirmationCode); // Use setLong for the code
			int rowsAffected = pstmt.executeUpdate();

			return rowsAffected > 0 ? 1 : 0;

		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
}