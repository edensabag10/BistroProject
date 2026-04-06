package serverLogic.managmentLogic; // Defining the package for management logic handlers

import java.io.IOException; // Importing for network communication error handling
import java.util.Map; // Importing for map-based data structures
import common.TimeRange; // Importing the TimeRange domain entity
import common.ServiceResponse; // Importing the standard response envelope
import common.ServiceResponse.ServiceStatus; // Importing the status enumeration
import dbLogic.managmentDB.UpdateManagementDBController; // Importing the DB controller for updates
import ocsf.server.ConnectionToClient; // Importing the OCSF client connection handle
import serverLogic.serverRestaurant.RestaurantManager; // Importing the RAM-based restaurant cache manager

/**
 * Handler class responsible for updating restaurant operating hours.
 * It ensures that changes are persisted in the Database and refreshed in the Server's RAM.
 */
public class UpdateHoursHandler { // Start of UpdateHoursHandler class definition

    /**
     * Processes the update request for regular operating hours.
     * @param restaurantId The ID of the restaurant being updated.
     * @param newHours A map containing the day names and their respective time ranges.
     * @param client The connection handle to send responses back to the UI.
     */
    public void handle(int restaurantId, Map<String, TimeRange> newHours, ConnectionToClient client) { // Start of handle method
        
        try { // Start of try block to catch network transmission exceptions
            
            // --- STEP 1: Persistent Storage Update ---
            
            // Attempt to update the regular hours in the MySQL database via the controller
            boolean isUpdateSuccessful = UpdateManagementDBController.updateRegularHours(restaurantId, newHours); // Execute DB update

            // Check if the database operation was successful
            if (isUpdateSuccessful) { // Start of success block for DB update
                
                // --- STEP 2: RAM Cache Synchronization ---
                
                // Since DB changed, we must refresh the RestaurantManager's internal RAM cache
                boolean cacheUpdated = RestaurantManager.reInitialize(restaurantId); // Refresh RAM data

                // Verify if the cache was successfully re-loaded from the new DB state
                if (cacheUpdated) { // Start of success block for cache refresh
                    
                    // Success: Both DB and RAM are synchronized. Notify the client.
                    client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS, "Hours updated successfully")); // Send success
                    
                } else { // If cache refresh failed
                    
                    // Error: Data is in DB but RAM failed to update. This is a critical state inconsistency.
                    client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "DB updated but failed to refresh server cache")); // Send cache error
                    
                } // End of cache update check
                
            } else { // If database update failed initially
                
                // Error: The database refused the update or a connection error occurred.
                client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, "Failed to update hours in Database")); // Send DB error
                
            } // End of DB update check
            
        } catch (IOException e) { // Catch block for OCSF transmission errors
            
            // Log the network exception details for server-side troubleshooting
            e.printStackTrace(); // Printing technical stack trace
            
        } // End of try-catch block
        
    } // End of handle method
    
} // End of UpdateHoursHandler class definition