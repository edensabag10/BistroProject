package serverLogic.menuLogic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import MainControllers.DBController;
import common.Visit;
import common.Visit.VisitStatus;

/**
 * The DBVisitHistoryController is a Data Access Object responsible for
 * querying visit-related data from the database.
 * It specifically handles the retrieval of historical dining records for subscribers.
 */
public class DBVisitHistoryController {

	/**
     * Retrieves a complete history of visits for a specific user from the 'visit' table.
     * The records are returned in descending order based on the start time (newest first).
     *
     * @param userId The unique identification number of the user/subscriber.
     * @return A {@link List} of {@link Visit} objects containing the confirmation code, 
     * table ID, bill ID, start time, and status for each visit.
     * Returns an empty list if no records are found or an error occurs.
     */
    public List<Visit> getVisitsForUser(int userId) {
        List<Visit> visits = new ArrayList<>();
        String sql = "SELECT confirmation_code, table_id, bill_id, start_time, status " +
                     "FROM visit WHERE user_id = ? ORDER BY start_time DESC";

        try {
            Connection conn = DBController.getInstance().getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Visit visit = new Visit(
                        rs.getLong("confirmation_code"),
                        rs.getInt("table_id"),
                        userId,
                        rs.getLong("bill_id"),
                        rs.getString("start_time"),
                        VisitStatus.valueOf(rs.getString("status"))
                    );
                    visits.add(visit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return visits;
    }
}