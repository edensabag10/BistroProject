package dbLogic.restaurantDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import MainControllers.DBController;

/**
 * Database access controller for restaurant table management. * This class
 * centralizes all database operations related to tables, including availability
 * queries, capacity calculations, and table state updates.
 */
public class TableDBController {

	/**
	 * Retrieves a list of available table IDs that can accommodate the given number
	 * of guests. The result is ordered by table capacity in ascending order to
	 * enable a "Best-Fit" strategy.
	 *
	 * @param numberOfGuests The minimum required seating capacity.
	 * @return A List of candidate table IDs that are currently available.
	 * @throws SQLException If a database error occurs.
	 */
	public static List<Integer> getCandidateTables(int numberOfGuests) {

		List<Integer> tableIds = new ArrayList<>();

		String sql = "SELECT table_id " + "FROM `table` " + "WHERE capacity >= ? AND is_available = 1 "
				+ "ORDER BY capacity ASC";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, numberOfGuests);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					tableIds.add(rs.getInt("table_id"));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return tableIds;
	}

	/**
	 * Calculates the total maximum seating capacity of the restaurant by summing
	 * all table capacities.
	 *
	 * @return The total restaurant seating capacity.
	 */
	public static int getRestaurantMaxCapacity() {

		String sql = "SELECT COALESCE(SUM(capacity), 0) FROM `table`";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				return rs.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * Calculates the total seating capacity currently occupied by unavailable
	 * tables. This represents capacity taken by active diner visits.
	 *
	 * @return Total occupied seating capacity.
	 * @throws SQLException If a database error occurs.
	 */
	public static int getUnavailableCapacity() throws SQLException {

		String sql = "SELECT COALESCE(SUM(capacity), 0) " + "FROM `table` " + "WHERE is_available = 0";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				return rs.getInt(1);
			}
		}

		return 0;
	}

	/**
	 * Marks a specific table as unavailable in the database. Used when a table is
	 * assigned to an active visit.
	 *
	 * @param tableId The ID of the table to update.
	 * @throws Exception If a database error occurs.
	 */
	public static void setTableUnavailable(int tableId) throws Exception {

		String sql = "UPDATE `table` " + "SET is_available = 0 " + "WHERE table_id = ?";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, tableId);
			ps.executeUpdate();
		}
	}

	/**
	 * Retrieves the seating capacity of a specific table.
	 *
	 * @param tableId The unique identifier of the table.
	 * @return The seating capacity.
	 * @throws SQLException If the table does not exist or a DB error occurs.
	 */
	public static int getTableCapacity(int tableId) throws SQLException {

		String sql = "SELECT capacity FROM `table` WHERE table_id = ?";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, tableId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("capacity");
				}
			}
		}

		throw new SQLException("Table not found: table_id=" + tableId);
	}

	/**
	 * Retrieves all tables stored in the system, including capacity and
	 * availability status.
	 *
	 * @return A List of all {@link common.Table} objects.
	 */
	public static List<common.Table> getAllTables() {

		List<common.Table> tables = new ArrayList<>();
		String sql = "SELECT * FROM `table` ORDER BY table_id ASC";

		Connection conn = DBController.getInstance().getConnection();
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

			while (rs.next()) {
				common.Table t = new common.Table(rs.getInt("table_id"), rs.getInt("capacity"),
						rs.getBoolean("is_available"));
				tables.add(t);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return tables;
	}

	/**
	 * Determines the next available positive table ID to ensure uniqueness.
	 *
	 * @return The next available table ID.
	 */
	private static int getNextTableId() {

		String sql = "SELECT MAX(table_id) FROM `table`";
		Connection conn = DBController.getInstance().getConnection();

		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				int maxId = rs.getInt(1);
				return Math.max(1, maxId + 1);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 1;
	}

	/**
	 * Adds a new table to the restaurant using a transaction. Updates both the
	 * 'table' entity table and the 'restaurant_table' mapping table.
	 *
	 * @param capacity Seating capacity of the new table.
	 * @return true if added successfully, false otherwise.
	 * @throws SQLException Internal transaction handling.
	 */
	public static boolean addNewTable(int capacity) {

		int restaurantId = serverLogic.serverRestaurant.RestaurantManager.getInstance().getRestaurantId();

		int nextId = getNextTableId();
		String sqlTable = "INSERT INTO `table` (table_id, capacity, is_available) VALUES (?, ?, 1)";
		String sqlRestaurantTable = "INSERT INTO `restaurant_table` (restaurant_id, table_id) VALUES (?, ?)";

		Connection conn = DBController.getInstance().getConnection();

		try {
			conn.setAutoCommit(false);

			try (PreparedStatement pstmt1 = conn.prepareStatement(sqlTable)) {
				pstmt1.setInt(1, nextId);
				pstmt1.setInt(2, capacity);
				pstmt1.executeUpdate();
			}

			try (PreparedStatement pstmt2 = conn.prepareStatement(sqlRestaurantTable)) {
				pstmt2.setInt(1, restaurantId);
				pstmt2.setInt(2, nextId);
				pstmt2.executeUpdate();
			}

			conn.commit();
			serverLogic.serverRestaurant.RestaurantManager.reInitialize(restaurantId);

			// VisitController.handleTableFreed(nextId);

			System.out.println("Table " + nextId + " added successfully.");
			return true;

		} catch (SQLException e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException rollbackEx) {
				rollbackEx.printStackTrace();
			}
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	/**
	 * Deletes a table from the system. Reassigns existing visits to an archive
	 * table (-1) to maintain referential integrity.
	 *
	 * @param tableId The ID of the table to remove.
	 * @return true if deletion and cache refresh succeeded, false otherwise.
	 * @throws SQLException Internal transaction handling.
	 */
	public static boolean deleteTable(int tableId) {

		int resId = serverLogic.serverRestaurant.RestaurantManager.getInstance().getRestaurantId();
		Connection conn = DBController.getInstance().getConnection();

		try {
			conn.setAutoCommit(false);

			String updateVisitsSql = "UPDATE `visit` SET table_id = -1 WHERE table_id = ?";
			try (PreparedStatement ps1 = conn.prepareStatement(updateVisitsSql)) {
				ps1.setInt(1, tableId);
				ps1.executeUpdate();
			}

			String deleteFromRestaurantTableSql = "DELETE FROM `restaurant_table` WHERE table_id = ?";
			try (PreparedStatement ps2 = conn.prepareStatement(deleteFromRestaurantTableSql)) {
				ps2.setInt(1, tableId);
				ps2.executeUpdate();
			}

			String deleteTableSql = "DELETE FROM `table` WHERE table_id = ?";
			try (PreparedStatement ps3 = conn.prepareStatement(deleteTableSql)) {
				ps3.setInt(1, tableId);
				int affected = ps3.executeUpdate();
				conn.commit();
				if (affected > 0) {
					// REFRESH THE RAM CACHE!
					// This forces the RestaurantManager to recount tables from the DB
					serverLogic.serverRestaurant.RestaurantManager.reInitialize(resId);

					System.out.println("[Tables] Table #" + tableId + " deleted and cache synchronized.");
					return true;
				}
				return false;
			}

		} catch (SQLException e) {
			try {
				conn.rollback();
			} catch (SQLException ignored) {
			}
			e.printStackTrace();
			return false;

		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	/**
	 * Updates the seating capacity of an existing table.
	 *
	 * @param tableId     The ID of the table to update.
	 * @param newCapacity The new capacity value.
	 * @return true if the update was successful, false otherwise.
	 */
	public static boolean updateTableCapacity(int tableId, int newCapacity) {

		String sql = "UPDATE `table` SET capacity = ? WHERE table_id = ?";

		Connection conn = DBController.getInstance().getConnection();
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, newCapacity);
			pstmt.setInt(2, tableId);
			return pstmt.executeUpdate() > 0;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}
