package serverLogic.managmentLogic; // Defining the package for management-related logic handlers

import java.io.IOException; // Importing for network communication error handling
import java.time.LocalDate; // Importing for modern date handling
import common.ServiceResponse; // Importing the standard service response wrapper
import common.ServiceResponse.ServiceStatus; // Importing the status enumeration for responses
import dbLogic.managmentDB.UpdateManagementDBController; // Importing the database controller for updates
import ocsf.server.ConnectionToClient; // Importing the OCSF connection handle for client communication
import serverLogic.serverRestaurant.RestaurantManager; // Importing the RAM-based restaurant manager

/**
 * Handler class responsible for processing special operating hours updates.
 * Ensures data consistency between the MySQL database and the Server's RAM cache.
 */
public class UpdateSpecialHoursHandler { // Start of the UpdateSpecialHoursHandler class definition

    /**
     * Processes a request to update special hours for a specific date.
     * @param restaurantId The unique ID of the restaurant.
     * @param date The specific date for the hours override.
     * @param open The opening time string (HH:mm).
     * @param close The closing time string (HH:mm).
     * @param client The connection object to respond to the client.
     */
    public void handle(int restaurantId, LocalDate date, String open, String close, ConnectionToClient client) { // Start of handle method
        
        try { // Start of the main try block to manage network communication
            
            // --- STEP 1: Update Persistent Storage ---
            
            // Execute the database update for the special hours using the management controller
            boolean isUpdateSuccessful = UpdateManagementDBController.updateSpecialHours(restaurantId, date, open, close); // Performing DB update

            // Conditional Logic: Check if the database update was successful
            if (isUpdateSuccessful) { // Start of block for successful DB update
                
                // --- STEP 2: Synchronize RAM Cache ---
                
                // Refresh the server's RAM by re-loading restaurant data from the updated database
                boolean cacheUpdated = RestaurantManager.reInitialize(restaurantId); // Refreshing RAM cache

                // Conditional Logic: Verify if the RAM refresh was successful
                if (cacheUpdated) { // Start of block for successful cache refresh
                    
                    // Respond to the client with a success status and a descriptive message
                    client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS, "Special hours updated successfully")); // Send success response
                    
                } // End of successful cache refresh block
                else { // If the cache refresh failed
                    
                    // Respond with an internal error indicating the cache failed to sync despite DB success
                    client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "DB updated but failed to refresh server cache")); // Send cache error
                    
                } // End of cache refresh failure block
                
            } // End of successful DB update block
            else { // If the database update itself failed
                
                // Respond with an internal error indicating the database update operation failed
                client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "Failed to update special hours in Database")); // Send DB error
                
            } // End of DB update failure block
            
        } // End of try block
        catch (IOException e) { // Catching exceptions related to network transmission
            
            // Output the technical stack trace to the system console for debugging
            e.printStackTrace(); // Printing technical trace
            
        } // End of catch block
        
    } // End of the handle method
    
} // End of the UpdateSpecialHoursHandler class definition