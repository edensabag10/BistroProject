package serverLogic.serverLogin;

import dbLogic.systemLogin.DBOccasionalConnection;
import ocsf.server.ConnectionToClient;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Specialized handler for the Occasional (Guest) Customer login process.
 * This class implements the server-side business logic for authenticating guests 
 * who do not possess a formal subscription.
 * * Role: It acts as an intermediary between the {@link MainControllers.ServerController} 
 * and the {@link dbLogic.systemLogin.DBOccasionalConnection}, ensuring that guest 
 * credentials are verified and session identifiers (user_id) are correctly returned.
 * * @author Software Engineering Student
 * @version 1.0
 */
public class OccasionalLoginHandler {

    /**
     * Executes the login verification process for a guest.
     * * Message Protocol:
     * - Incoming: {@link ArrayList} containing [COMMAND_STRING, USERNAME, CONTACT_INFO].
     * - Outgoing (Success): {@link ArrayList} containing [STATUS_STRING, USER_ID].
     * - Outgoing (Failure): A plain {@link String} describing the error.
     * * @param data   The encapsulated message list from the client.
     * @param client The OCSF {@link ConnectionToClient} responsible for this guest's session.
     */
    public void handle(ArrayList<Object> data, ConnectionToClient client) {
        try {
            /**
             * STEP 1: Protocol Extraction
             * Extracts the username and contact (phone/email) from the network message.
             * Explicit casting is required since the list stores objects of type 'Object'.
             */
            String username = (String) data.get(1);
            String contact = (String) data.get(2);

            /**
             * STEP 2: Database Interaction
             * Queries the DB to find a match for the provided guest credentials.
             * verifyOccasional returns the unique DB 'user_id' (int), or -1 if no match is found.
             */
            DBOccasionalConnection db = new DBOccasionalConnection();
            int userId = db.verifyOccasional(username, contact);

            /**
             * STEP 3: Response Management
             */
            if (userId != -1) {
                // Success Scenario: Encapsulate the confirmation and the retrieved UserID
                ArrayList<Object> response = new ArrayList<>();
                response.add("LOGIN_OCCASIONAL_SUCCESS");
                response.add(userId);
                client.setInfo("userId", userId);
                
                // Transmit the successful response back to the client
                client.sendToClient(response);
            } else {
                // Failure Scenario: Inform the UI of an invalid credential set
                client.sendToClient("ERROR: Invalid username or contact information.");
            }
            
        } catch (Exception e) {
            /**
             * STEP 4: Exception Safety
             * Handles potential casting errors, database connectivity issues, or OCSF interruptions.
             */
            e.printStackTrace();
            try {
                // Notifies the client about the technical failure to prevent the UI from hanging
                client.sendToClient("ERROR: Internal server error during login.");
            } catch (IOException ex) {
                // Logs failure to communicate with the specific client socket
                ex.printStackTrace();
            }
        }
    }
}