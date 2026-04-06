package serverLogic.managmentLogic;

import java.io.IOException;
import common.ServiceResponse;
import common.ServiceResponse.ServiceStatus;
import dbLogic.managmentDB.UpdateManagementDBController;
import ocsf.server.ConnectionToClient;

/**
 * Handler responsible for the business logic of registering a new subscriber.
 */
public class CreateSubscriberHandler {

    /**
     * Handles the subscriber creation process.
     * @param phone The validated phone number from the UI.
     * @param email The validated email from the UI.
     * @param client The OCSF connection to send the result back.
     */
    public void handle(String phone, String email, ConnectionToClient client) {
        try {
            // STEP 1: Call the DB Controller to perform the transaction.
            // The DB controller will return the new Subscriber ID (long) or -1/0 if it fails.
            Object result = UpdateManagementDBController.createNewSubscriber(phone, email);

            if (result instanceof Long) {
                // SUCCESS: Send the generated Subscriber ID back to the client
                client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS, result));
            } 
            else if (result instanceof String) {
                // ERROR: Likely a duplicate phone number
                client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, result));
            } 
            else {
                // GENERIC FAILURE
                client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "Database transaction failed."));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}