package serverLogic.serverLogin;

import dbLogic.systemLogin.DBOccasionalConnection;
import ocsf.server.ConnectionToClient;
import java.util.ArrayList;

/**
 * The OccasionalRegistrationHandler class is responsible for managing the 
 * server-side workflow for new guest registration.
 * * Role: It acts as a bridge between the network communication layer (OCSF) 
 * and the database persistence layer, specifically handling the 
 * "REGISTER_OCCASIONAL" command.
 * * It extracts raw data from the client's message and delegates the 
 * transactional database work to the {@link DBOccasionalConnection} class.
 * * @author Software Engineering Student
 * @version 1.0
 */
public class OccasionalRegistrationHandler {

    /**
     * Executes the registration process for a new occasional customer.
     * * Message Protocol:
     * - Incoming: {@link ArrayList} containing [COMMAND_STRING, USERNAME, CONTACT_INFO].
     * - Outgoing: A {@link String} indicating success ("REGISTRATION_SUCCESS") 
     * or a specific error message (e.g., "ERROR: Username already exists.").
     * * @param data   The message payload received from the client.
     * @param client The OCSF {@link ConnectionToClient} handle for the specific requester.
     */
    public void handle(ArrayList<Object> data, ConnectionToClient client) {
        try {
            /**
             * STEP 1: Data Decapsulation
             * Extracts the username and contact (phone or email) provided by the user.
             * Explicit casting is required as the protocol uses a generic ArrayList of Objects.
             */
            String user = (String) data.get(1);
            String contact = (String) data.get(2);

            /**
             * STEP 2: Database Persistence Delegation
             * Triggers the registration logic which manages transactions across the 
             * 'user' and 'occasional_customer' tables.
             */
            DBOccasionalConnection db = new DBOccasionalConnection();
            String result = db.registerNewOccasional(user, contact);

            /**
             * STEP 3: Feedback Loop
             * Transmits the database result directly back to the client.
             * This triggers the 'display()' method in the client-side controller.
             */
            client.sendToClient(result);
            
        } catch (Exception e) {
            /**
             * STEP 4: Exception Handling
             * Catches potential casting errors or network interruptions.
             * Ensures the client is notified of the failure to prevent UI hanging.
             */
            e.printStackTrace();
            try {
                client.sendToClient("ERROR: Internal server error during registration.");
            } catch (Exception ex) {
                // Log failure to communicate error status back to the socket
                ex.printStackTrace();
            }
        }
    }
}