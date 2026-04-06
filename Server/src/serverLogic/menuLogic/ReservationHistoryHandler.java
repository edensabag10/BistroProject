package serverLogic.menuLogic; // Defining the package for menu-related logic handlers

import ocsf.server.ConnectionToClient; // Importing the OCSF class for managing client connections
import java.util.ArrayList; // Importing ArrayList for dynamic list structures
import java.util.List; // Importing List interface for generic list handling

import dbLogic.restaurantDB.DBReservationsHistoryController; // Importing the DB controller for history retrieval
import common.Reservation; // Importing the Reservation domain model

/**
 * Handles server-side logic for fetching the reservation history of a specific subscriber.
 * This class decapsulates the user ID and coordinates with the database layer.
 */
public class ReservationHistoryHandler { // Start of the ReservationHistoryHandler class definition

    /**
     * Processes the "GET_RESERVATIONS_HISTORY" command.
     * @param data   The incoming protocol list: [0] command string, [1] userId integer.
     * @param client The active connection handle to the requesting client.
     */
    public void handle(ArrayList<Object> data, ConnectionToClient client) { // Start of the handle method

        try { // Start of the main processing try-block
            
            // --- STEP 1: Data Extraction ---
            
            // Extract the user's primary key from index 1 of the incoming message list
            int userId = (int) data.get(1); // Casting the generic object to an integer

            // --- STEP 2: Database Delegation ---
            
            // Initialize the database access object responsible for history queries
            DBReservationsHistoryController db = new DBReservationsHistoryController(); // Creating DB instance
            
            // Query the database to retrieve all reservations associated with the given user ID
            List<Reservation> reservations = db.getReservationsForUser(userId); // Executing the query

            // --- STEP 3: Response Construction ---
            
            // Initialize a new ArrayList to serve as the outgoing protocol message
            ArrayList<Object> response = new ArrayList<>(); // Creating the response container
            
            // Add the standardized command header so the client knows how to parse the result
            response.add("RESERVATION_HISTORY"); // Setting the response identifier
            
            // Add the list of retrieved reservation objects as the message payload
            response.add(reservations); // Attaching the data list

            // --- STEP 4: Network Transmission ---
            
            // Transmit the formatted response list back to the client UI via OCSF
            client.sendToClient(response); // Sending data through the socket

        } // End of the processing try-block
        catch (Exception e) { // Start of the catch block for unexpected runtime errors
            
            // Log the detailed exception trace to the server's console for debugging
            e.printStackTrace(); // Printing the technical stack trace
            
            try { // Start of inner try-block for error reporting
                
                // Send a descriptive error string back to the client to prevent the UI from hanging
                client.sendToClient("ERROR: Failed to load order history."); // Transmitting error message
                
            } // End of error-send try-block
            catch (Exception ex) { // Catch block if the network report itself fails
                
                // Log the secondary exception (usually due to a broken connection)
                ex.printStackTrace(); // Printing secondary trace
                
            } // End of inner catch-block
            
        } // End of main catch-block
        
    } // End of the handle method
    
} // End of the ReservationHistoryHandler class definition