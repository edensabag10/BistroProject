package dbLogic.managmentDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import MainControllers.DBController;

public class reportsDBController {

	/**
	 * Generates a statistical time report for a specified month.
	 * <p>
	 * This method retrieves data regarding visit delays (difference between reservation and start time)
	 * and visit durations (difference between start time and payment). The results are grouped by 
	 * date and hour of arrival/departure.
	 * </p>
	 * * @param monthStr A string representing the month, either as a numeric value ("1"-"12") 
	 * or a full month name (e.g., "January").
	 * @return A {@code List<Map<String, Object>>} where each map represents a report row containing:
	 * <ul>
	 * <li>"date": The date of the visits.</li>
	 * <li>"delay": Average delay in minutes.</li>
	 * <li>"duration": Average visit duration in minutes.</li>
	 * <li>"arr_hour": The hour of arrival.</li>
	 * <li>"dep_hour": The hour of departure.</li>
	 * </ul>
	 * @throws Exception If there is a database access error or an issue parsing the month.
	 */
	public static List<Map<String, Object>> getTimeReportData(String monthStr) throws Exception {
	    List<Map<String, Object>> reportList = new ArrayList<>();
	    
	    int selectedMonth;
	    
	    try { selectedMonth = Integer.parseInt(monthStr); } 
	    catch (Exception e) { selectedMonth = java.time.Month.valueOf(monthStr.toUpperCase()).getValue(); }
	    int reportYear = resolveReportYear(selectedMonth);

	    String sql =
	    	    "SELECT DATE(v.start_time) AS date, " +
	    	    "AVG(TIMESTAMPDIFF(MINUTE, r.reservation_datetime, v.start_time)) AS avg_delay, " +
	    	    "AVG(TIMESTAMPDIFF(MINUTE, v.start_time, b.payment_time)) AS avg_duration, " +
	    	    "HOUR(v.start_time) AS arrival_hour, " +
	    	    "HOUR(b.payment_time) AS departure_hour " +
	    	    "FROM visit v " +
	    	    "JOIN reservation r ON v.confirmation_code = r.confirmation_code " +
	    	    "JOIN bill b ON v.bill_id = b.bill_id " +
	    	    "WHERE MONTH(v.start_time) = ? AND YEAR(v.start_time) = ? " +
	    	    "GROUP BY date, arrival_hour, departure_hour";


	    Connection conn = MainControllers.DBController.getInstance().getConnection();
	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	    	ps.setInt(1, selectedMonth);
	    	ps.setInt(2, reportYear);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                Map<String, Object> row = new HashMap<>();
	                row.put("date", rs.getDate("date").toString());
	                row.put("delay", rs.getDouble("avg_delay"));
	                row.put("duration", rs.getDouble("avg_duration"));
	                row.put("arr_hour", rs.getInt("arrival_hour"));
	                row.put("dep_hour", rs.getInt("departure_hour"));
	                reportList.add(row);
	            }
	        }
	    }
//	    System.out.println(
//	    	    "DEBUG TimeReport | month=" + selectedMonth +
//	    	    " year=" + reportYear
//	    	);
	    return reportList;
	}
	
	
	/**
	 * Retrieves daily statistics for reservations and waiting list entries for a given month.
	 * <p>
	 * This method aggregates data by combining (UNION ALL) records from both the 'reservation' 
	 * and 'waiting_list_entry' tables. It provides a daily count of how many reservations 
	 * were made versus how many customers were placed on the waiting list.
	 * </p>
	 *
	 * @param monthStr A string representing the month, either as a numeric value ("1"-"12") 
	 * or a full month name (e.g., "January").
	 * @return A {@code List<Map<String, Object>>} containing daily report entries. Each map includes:
	 * <ul>
	 * <li>"date": The specific date of the activity.</li>
	 * <li>"reservations": Total number of reservations for that day.</li>
	 * <li>"waiting": Total number of waiting list entries for that day.</li>
	 * </ul>
	 * @throws Exception If a database access error occurs or the month string cannot be parsed.
	 */
	public static List<Map<String, Object>> getSubReportData(String monthStr) throws Exception {
	    List<Map<String, Object>> reportList = new ArrayList<>();
	    
	    // המרת שם החודש למספר
	    int selectedMonth;
	    try {
	        selectedMonth = Integer.parseInt(monthStr);
	    } catch (NumberFormatException e) {
	        selectedMonth = java.time.Month.valueOf(monthStr.toUpperCase()).getValue();
	    }

	    // לוגיקת בחירת שנה (2026 אם כבר עבר, אחרת 2025)
	    int currentMonth = java.time.LocalDate.now().getMonthValue();
	    int reportYear = resolveReportYear(selectedMonth);

	    // שאילתה שסופרת הזמנות והמתנות לכל יום בחודש שבו היו נתונים
	    String sql = "SELECT report_date, SUM(is_res) as res_count, SUM(is_wait) as wait_count FROM (" +
	                 "  SELECT DATE(reservation_datetime) as report_date, 1 as is_res, 0 as is_wait FROM reservation " +
	                 "  WHERE MONTH(reservation_datetime) = ? AND YEAR(reservation_datetime) = ? " +
	                 "  UNION ALL " +
	                 "  SELECT DATE(entry_time) as report_date, 0 as is_res, 1 as is_wait FROM waiting_list_entry " +
	                 "  WHERE MONTH(entry_time) = ? AND YEAR(entry_time) = ? " +
	                 ") AS combined GROUP BY report_date ORDER BY report_date ASC";

	    Connection conn = MainControllers.DBController.getInstance().getConnection();
	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	    	ps.setInt(1, selectedMonth);
	    	ps.setInt(2, reportYear);
	    	ps.setInt(3, selectedMonth);
	    	ps.setInt(4, reportYear);
	        
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                Map<String, Object> row = new HashMap<>();
	                row.put("date", rs.getDate("report_date").toString());
	                row.put("reservations", rs.getInt("res_count"));
	                row.put("waiting", rs.getInt("wait_count"));
	                reportList.add(row);
	            }
	        }
	    }
	    return reportList;
	}
	/**
	 * Determines the appropriate calendar year for a report based on the selected month
	 * relative to the current date.
	 * <p>
	 * If the selected month is greater than the current month, the method assumes 
	 * the report refers to the previous year (e.g., in January 2026, selecting 
	 * December will return 2025). Otherwise, it returns the current year.
	 * </p>
	 *
	 * @param selectedMonth The month number (1-12) for which the report is requested.
	 * @return The calculated year (either the current year or the previous year).
	 */
	private static int resolveReportYear(int selectedMonth) {
	    int currentYear = LocalDate.now().getYear();
	    int currentMonth = LocalDate.now().getMonthValue();

	    // אם החודש שנבחר עוד לא קרה השנה – חוזרים לשנה הקודמת
	    if (selectedMonth > currentMonth) {
	        return currentYear - 1;
	    }

	    return currentYear;
	}

}