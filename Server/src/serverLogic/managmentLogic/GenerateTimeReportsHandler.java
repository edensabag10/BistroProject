package serverLogic.managmentLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import common.ServiceResponse;
import common.ServiceResponse.ServiceStatus;
import dbLogic.managmentDB.reportsDBController;
import ocsf.server.ConnectionToClient;

/**
 * Handler responsible for generating time reports
 * (average delays per day).
 */
public class GenerateTimeReportsHandler {

    /**
     * Handles the GET_TIME_REPORTS command.
     * Extracts the requested month, retrieves the report data from the database,
     * and sends the result back to the client.
     *
     * @param messageList A list containing the request data, where index 2 holds the selected month.
     * @param client The OCSF client connection used to send the response.
     */
	public void handle(ArrayList<Object> messageList, ConnectionToClient client) {
        try {
        	// 1. Extract the month from the message
            String month = (String) messageList.get(2);
            
            // 2. Call the DB to retrieve the report data
            List<Map<String, Object>> reportData = reportsDBController.getTimeReportData(month);
            
            // Create the response list for the client
            ArrayList<Object> response = new ArrayList<>();
            
            if (reportData == null || reportData.isEmpty()) {
                response.add("REPORT_ERROR");
                response.add("No data found for the selected month.");
            } else {
                // Add a tag so the client knows which report this is
                response.add("REPORT_TIME_DATA_SUCCESS"); 
                response.add(reportData); // The report data itself 
            }

            // 4. Send the ArrayList directly to the client
            client.sendToClient(response);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                ArrayList<Object> err = new ArrayList<>();
                err.add("REPORT_ERROR");
                err.add("Server failed to generate report.");
                client.sendToClient(err);
            } catch (Exception ignored) {}
        }
    }
}