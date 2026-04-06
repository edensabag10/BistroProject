package serverLogic.serverLogin;

import dbLogic.systemLogin.DBOccasionalConnection;
import ocsf.server.ConnectionToClient;
import java.util.ArrayList;

/**
 * The OccasionalRestUsernameHandler class manages the server-side process for 
 * resetting a guest customer's username.
 * * This handler is triggered by the "RESET_OCCASIONAL_USERNAME" command and 
 * coordinates with the database to verify contact details before applying the change.
 * * It follows the Command Pattern, decoupling the message routing from the 
 * specific business logic.
 * * @author Software Engineering Student
 * @version 1.0
 */
public class OccasionalRestUsernameHandler {

    /**
     * Processes the username reset request.
     * * Message Protocol:
     * - Incoming: {@link ArrayList} containing [COMMAND, CONTACT_INFO, NEW_USERNAME].
     * - Outgoing: A {@link String} indicating success ("RESET_USERNAME_SUCCESS") 
     * or a descriptive error message starting with "ERROR:".
     * * @param data   The data list received from the OCSF client.
     * @param client The specific connection handle for the requester.
     */
	public void handle(ArrayList<Object> data, ConnectionToClient client) {
	    try {
	        /**
	         * STEP 1: Data Decapsulation
	         * Retrieves the contact identifier (phone/email) and the desired new 
	         * username from the network packet.
	         */
	        String contact = (String) data.get(1);
	        String newUsername = (String) data.get(2);

	        /**
	         * STEP 2: Database Logic Invocation
	         * Delegates the validation and update process to the DBOccasionalConnection.
	         * This involves checking if the contact exists and if the new username 
	         * is unique.
	         */
	        DBOccasionalConnection db = new DBOccasionalConnection();
	        String result = db.resetUsername(contact, newUsername);


	        /**
	         * STEP 3: Client Notification
	         * Returns the result string to the client to update the UI (e.g., showing a popup).
	         */
	        client.sendToClient(result);
	        
	    } catch (Exception e) {
	        /**
	         * STEP 5: Robust Exception Handling
	         * Ensures that internal server failures are logged and the client is notified, 
	         * preventing the UI from remaining in a "pending" state.
	         */
	        e.printStackTrace();
	        try {
	            client.sendToClient("ERROR: Internal server error.");
	        } catch (Exception ex) { 
	            ex.printStackTrace(); 
	        }
	    }
	}
}