package serverLogic.managmentLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dbLogic.managmentDB.reportsDBController;
import ocsf.server.ConnectionToClient;

/**
 * Handler responsible for generating subscriber reports on the server side.
 */
public class GenerateSubReportsHandler {
    /**
     * Handles the request for generating subscriber reports for a given month.
     * @param messageList A list containing the request data, where index 1 holds the requested month.
     * @param client The OCSF client connection used to send the response.
     */
    public void handle(ArrayList<Object> messageList, ConnectionToClient client) {
        String month = (String) messageList.get(1);
        ArrayList<Object> response = new ArrayList<>();
        response.add("RECEIVE_SUBSCRIBER_REPORTS"); // Command sent back to the client

        try {
            List<Map<String, Object>> data = reportsDBController.getSubReportData(month);
            response.add(data);
            client.sendToClient(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.add(new ArrayList<Map<String, Object>>()); // Empty list in case of error
            try { client.sendToClient(response); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}