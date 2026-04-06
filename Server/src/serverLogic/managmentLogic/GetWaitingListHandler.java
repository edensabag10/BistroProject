package serverLogic.managmentLogic;

import java.io.IOException;
import java.util.ArrayList;
import common.WaitingListEntry;
import dbLogic.managmentDB.UpdateManagementDBController;
import ocsf.server.ConnectionToClient;

/**
 * Handler responsible for fetching the current waiting list.
 */
public class GetWaitingListHandler {

    /**
     * Handles the request to retrieve waiting list entries with status WAITING or NOTIFIED.
     * @param client The OCSF connection to send the list back.
     */
    public void handle(ConnectionToClient client) {
        try {
            // STEP 1: Fetch filtered entries from the DB
            ArrayList<WaitingListEntry> waitingList = UpdateManagementDBController.getWaitingListEntries();

            // STEP 2: Send the ArrayList directly to the client
            client.sendToClient(waitingList);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}