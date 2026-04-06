package serverLogic.menuLogic; // Defining the package for menu-related server logic handlers

import java.util.ArrayList; // Importing ArrayList for dynamic list handling

import dbLogic.systemLogin.DBSubscriberDetails; // Importing the DB logic for subscriber updates
import ocsf.server.ConnectionToClient; // Importing OCSF connection handle for client communication

/**
 * Handles server-side logic for editing subscriber personal details.
 * It extracts incoming data from the protocol and coordinates with the database layer.
 */
public class EditDetailsHandler { // Start of EditDetailsHandler class definition

    /**
     * Entry point for processing the "UPDATE_SUBSCRIBER_DETAILS" command.
     * @param data   The ArrayList protocol: [1] userId, [2] username, [3] phone, [4] email.
     * @param client The connection handle to send responses back to.
     */
    public void handle(ArrayList<Object> data, ConnectionToClient client) { // Start of handle method

        try { // Start of the main processing try-block
            
            // --- STEP 1: Data Extraction ---
            
            // Extract the user's primary key from index 1 of the list
            int userId = (int) data.get(1); // Casting object to integer
            
            // Extract the optional new username from index 2
            String username = (String) data.get(2); // Casting object to string
            
            // Extract the optional new phone number from index 3
            String phone = (String) data.get(3); // Casting object to string
            
            // Extract the optional new email address from index 4
            String email = (String) data.get(4); // Casting object to string

            // --- STEP 2: Database Delegation ---
            
            // Initialize the database access object for subscriber details
            DBSubscriberDetails db = new DBSubscriberDetails(); // Creating DB logic instance
            
            // Invoke the update logic; returns true if at least one field was changed in the DB
            boolean updated = db.updateSubscriberDetails(userId, username, phone, email); // Executing update

            // --- STEP 3: Response Construction ---
            
            // Prepare a new list for the response following the system protocol
            ArrayList<Object> response = new ArrayList<>(); // Initializing response list
            
            // Add the command result identifier at the first index
            response.add("EDIT_DETAILS_RESULT"); // Setting command header
            
            // Use a ternary operator to decide between "SUCCESS" and "NO_CHANGES" status
            String status = updated ? "SUCCESS" : "NO_CHANGES"; // Determining status string
            
            // Add the resulting status to the second index of the response list
            response.add(status); // Setting payload

            // --- STEP 4: Network Transmission ---
            
            // Send the completed response list back to the client UI
            client.sendToClient(response); // Transmitting via OCSF

        } // End of processing try-block
        catch (Exception e) { // Start of catch block for unexpected errors
            
            // Log the technical exception to the server's standard error stream
            e.printStackTrace(); // Printing stack trace for debugging
            
            try { // Start of secondary try-block for error reporting
                
                // Inform the client that a technical error occurred during the update process
                client.sendToClient("ERROR_EDITING_DETAILS"); // Sending error string
                
            } // End of internal error-send try-block
            catch (Exception ex) { // Catch block if even the error message fails to send
                
                // Log the secondary failure (usually a network disconnection)
                ex.printStackTrace(); // Printing technical trace
                
            } // End of inner catch-block
            
        } // End of main catch-block
        
    } // End of the handle method
    
} // End of the EditDetailsHandler class definition