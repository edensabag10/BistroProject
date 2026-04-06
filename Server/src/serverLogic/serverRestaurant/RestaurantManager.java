package serverLogic.serverRestaurant;

import java.sql.SQLException;

import common.Restaurant;
import dbLogic.restaurantDB.RestaurantDBController;

/**
 * The RestaurantManager class acts as a global access point and an In-Memory Cache 
 * for the restaurant's configuration data.
 * * <p><b>Design Strategy: RAM Caching</b><br>
 * To ensure high performance during peak reservation times, the server loads 
 * static restaurant data (name, table inventory, and regular hours) into RAM 
 * during startup. This prevents the system from performing expensive SQL JOIN 
 * operations for every single availability check.</p>
 * * <p>This class follows the <b>Singleton-like</b> pattern, acting as the 
 * "Single Source of Truth" for the restaurant entity throughout the server's lifecycle.</p>
 * * @author Software Engineering Student
 * @version 1.0
 */
public class RestaurantManager {

    /** * The cached restaurant instance stored in memory. 
     * Declared static to ensure global accessibility across different logic handlers.
     */
    private static Restaurant currentRestaurant;

    /**
     * Bootstraps the restaurant data by fetching it from the persistence layer.
     * * <p>This method should be invoked during the server's <code>serverStarted()</code> 
     * hook, immediately after the database connection is verified. It bridges the 
     * {@link dbLogic.restaurantDB.RestaurantDBController} and the RAM state.</p>
     * * @param restaurantId The unique primary key of the restaurant to be cached.
     * @return <b>true</b> if the data was successfully loaded and mapped; 
     * <b>false</b> if the database returned null or an error occurred.
     */
    public static boolean initialize(int restaurantId) {
        System.out.println("Initializing restaurant data in RAM for ID: " + restaurantId + "...");
        
        try {
            /**
             * Delegation: Calling the DB logic. 
             * Since loadFullRestaurantData now throws SQLException, we must wrap it in try-catch.
             */
            currentRestaurant = RestaurantDBController.loadFullRestaurantData(restaurantId);
            
            if (currentRestaurant != null) {
                System.out.println("Restaurant data loaded successfully: " + currentRestaurant.getRestaurantName());
                return true;
            } else {
                System.err.println("Error: Failed to load restaurant data from database.");
                return false;
            }
        } catch (SQLException e) {
            // Here we catch the exception that was thrown from RestaurantDBController
            System.err.println("Database Error during initialization: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Re-loads the restaurant data from the database into the memory cache.
     * Useful after management updates (like changing opening hours).
     * * @param restaurantId The ID of the restaurant to reload.
     * @return true if re-initialization succeeded, false otherwise.
     */
    public static boolean reInitialize(int restaurantId) {
        try {
            // Load the full data using the existing DB controller
            Restaurant updatedRestaurant = RestaurantDBController.loadFullRestaurantData(restaurantId);
            
            if (updatedRestaurant != null) {
                currentRestaurant = updatedRestaurant;
                System.out.println("Restaurant cache re-initialized successfully for ID: " + restaurantId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error during restaurant re-initialization: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Provides global access to the cached restaurant instance.
     * * <p>Logic handlers like the reservation engine use this to retrieve 
     * table capacities and operating hours without querying the MySQL DB.</p>
     * * @return The currently cached {@link Restaurant} object.
     */
    public static Restaurant getInstance() {
        return currentRestaurant;
    }

    /**
     * Manually updates the in-memory restaurant instance.
     * * <p>This can be used during runtime updates (e.g., if a manager changes hours) 
     * or during <b>Unit Testing</b> to inject a Mock or Stub restaurant object 
     * for verification purposes.</p>
     * * @param restaurant The new {@link Restaurant} instance to be cached.
     */
    public static void setInstance(Restaurant restaurant) {
        currentRestaurant = restaurant;
    }
}