package serverLogic.managmentLogic; // Defining the package for management logic handlers

import java.io.IOException; // Importing for network communication error handling
import common.ServiceResponse; // Importing the standard response envelope
import common.ServiceResponse.ServiceStatus; // Importing the status enumeration
import dbLogic.managmentDB.UpdateManagementDBController; // Importing the DB controller for SQL execution
import ocsf.server.ConnectionToClient; // Importing the OCSF client connection handle
import serverLogic.serverRestaurant.RestaurantManager; // Importing the RAM-based restaurant cache manager

/**
 * Handler class responsible for deleting all special operating hours.
 * Ensures that the Database is cleared and the Server's RAM cache is synchronized.
 */
public class DeleteSpecialHoursHandler { // Start of the DeleteSpecialHoursHandler class

    /**
     * Processes the mass deletion request for special hours.
     * @param restaurantId The ID of the restaurant.
     * @param client The connection handle to send the response back.
     */
    public void handle(int restaurantId, ConnectionToClient client) { // Method start
        
        try { // Start of try block for network transmission
            
            // --- STEP 1: Persistent Storage Deletion ---
            // Execute the SQL delete operation through the management DB controller
            boolean isDeleted = UpdateManagementDBController.deleteAllSpecialHours(restaurantId); // Call DB logic

            if (isDeleted) { // If the database operation succeeded
                
                // --- STEP 2: RAM Cache Synchronization ---
                // Refresh the RAM cache so the changes are immediately effective for new reservations
                boolean cacheRefreshed = RestaurantManager.reInitialize(restaurantId); // Sync RAM with DB

                if (cacheRefreshed) { // If RAM was updated successfully
                    // Success: Send a positive response to the Representative Dashboard
                    client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS, "All special hours have been cleared.")); 
                } // End of inner if
                else { // If RAM refresh failed
                    // Error: Database is clean but RAM still has old data
                    client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "DB cleared but server RAM failed to refresh.")); 
                } // End of inner else
                
            } // End of outer if
            else { // If database deletion failed
                // Error: SQL execution error or connection loss
                client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "Failed to delete special hours from Database.")); 
            } // End of outer else
            
        } catch (IOException e) { // Catch OCSF transmission errors
            e.printStackTrace(); // Log technical stack trace for server debugging
        } // End of try-catch block
        
    } // End of handle method
    
} // End of class