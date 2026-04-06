package common; // Defining the package location for the class

import java.io.Serializable; // Importing for object serialization capabilities
import java.time.LocalDate; // Importing for date handling
import java.util.ArrayList; // Importing for dynamic list implementation
import java.util.Collections; // Importing for utility methods like sorting
import java.util.HashMap; // Importing for key-value pair storage
import java.util.List; // Importing for list interface
import java.util.Map; // Importing for map interface

/**
 * Core Domain Entity representing a Restaurant in the system.
 */
public class Restaurant implements Serializable { // Defining the Restaurant class
    
    // Serial version identifier to ensure object compatibility during serialization
    private static final long serialVersionUID = 1L;

    // Unique identifier for the restaurant instance in the database
    private int restaurantId;
    
    // Human-readable name of the restaurant
    private String restaurantName;
    
    // Map storing standard weekly hours (Key: Day name, Value: TimeRange object)
    private Map<String, TimeRange> regularHours;
    
    // Map storing date-specific overrides (Key: LocalDate, Value: TimeRange object)
    private Map<LocalDate, TimeRange> specialHours;
    
    // Map storing table inventory (Key: Seating capacity, Value: Total number of tables)
    private Map<Integer, Integer> tableInventory;

    /**
     * Constructs a new Restaurant and initializes internal data structures.
     */
    public Restaurant(int restaurantId, String restaurantName) { // Constructor start
        this.restaurantId = restaurantId; // Assigning the unique ID
        this.restaurantName = restaurantName; // Assigning the display name
        this.regularHours = new HashMap<>(); // Initializing the regular hours map
        this.specialHours = new HashMap<>(); // Initializing the special overrides map
        this.tableInventory = new HashMap<>(); // Initializing the inventory map
    } // Constructor end

    // --- Identification Getters ---

    public int getRestaurantId() { // Method to retrieve restaurant ID
        return restaurantId; // Returning the ID value
    } // End method
    
    public String getRestaurantName() { // Method to retrieve restaurant name
        return restaurantName; // Returning the name string
    } // End method

    // --- Operating Hours Management ---

    public void setRegularHours(String day, String open, String close) { // Method to set standard hours
        regularHours.put(day, new TimeRange(open, close)); // Creating and mapping a new TimeRange
    } // End method

    public void setSpecialHours(LocalDate date, String open, String close) { // Method to set date overrides
        specialHours.put(date, new TimeRange(open, close)); // Creating and mapping a new override
    } // End method
    
    public Map<String, TimeRange> getRegularHours() { // Method to get all regular hours
        return regularHours; // Returning the underlying map
    } // End method

    public Map<LocalDate, TimeRange> getSpecialHours() { // Method to get all special overrides
        return specialHours; // Returning the underlying map
    } // End method
    
    /**
     * Generates a formatted string representing the restaurant's schedule.
     */
    public String getFormattedOpeningHours() { // Method start
        StringBuilder sb = new StringBuilder("=== RESTAURANT OPERATING HOURS ===\n\n"); // Initializing string builder with header

        sb.append("[ Standard Weekly Schedule ]\n"); // Adding section header
        
        // Check if regular hours exist or the map is empty
        boolean noHoursDefined = (regularHours == null || regularHours.isEmpty()); // Evaluation
        
        if (noHoursDefined) { // If no hours are found
            sb.append("No regular hours are currently defined.\n"); // Notify the user in the output
        } else { // If hours are defined
            String[] daysOrder = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"}; // Hardcoded order for sorting
            for (String day : daysOrder) { // Iterating through the week in order
                TimeRange hours = regularHours.get(day); // Attempting to retrieve hours for the current day
                if (hours != null) { // If hours are found for this specific day
                    sb.append("• ").append(day).append(": ").append(hours.toString()).append("\n"); // Append the day and hours
                } // End if
            } // End loop
        } // End else

        LocalDate today = LocalDate.now(); // Capturing the current system date
        LocalDate lookAheadLimit = today.plusDays(30); // Setting the limit for special alerts to 30 days
        boolean hasSpecialAlerts = false; // Flag to track if any upcoming changes exist

        for (Map.Entry<LocalDate, TimeRange> entry : specialHours.entrySet()) { // Iterating through special hours entries
            LocalDate specialDate = entry.getKey(); // Extracting the date key
            // Check if the date is within the range: Today <= specialDate <= lookAheadLimit
            boolean isWithinRange = !specialDate.isBefore(today) && !specialDate.isAfter(lookAheadLimit); // Range logic
            
            if (isWithinRange) { // If the date falls within the next 30 days
                if (!hasSpecialAlerts) { // If this is the first alert found
                    sb.append("\n[ !!! IMPORTANT: UPCOMING SCHEDULE CHANGES !!! ]\n"); // Add the alert header
                    hasSpecialAlerts = true; // Mark that alerts have been found
                } // End if alert found
                sb.append("• Date: ").append(specialDate).append(" (").append(specialDate.getDayOfWeek()).append(")\n") // Date details
                  .append("  New Hours: ").append(entry.getValue().toString()).append(" *Overrides regular schedule*\n"); // Hour details
            } // End if within range
        } // End loop

        if (!hasSpecialAlerts) { // If no special events were found in the loop
            sb.append("\nNo special holiday or event hours in the next 30 days.\n"); // Inform the user
        } // End if no alerts

        sb.append("\n================================="); // Add closing footer
        return sb.toString(); // Return the complete formatted string
    } // Method end

    /**
     * Checks if the restaurant is open at a given date and time.
     */
    public boolean isOpen(LocalDate date, String timeStr) { // Method start
        TimeRange hours = specialHours.get(date); // Step 1: Look for an override for this specific date
        
        if (hours == null) { // Step 2: If no override exists, fall back to weekly schedule
            // Convert DayOfWeek enum (e.g., MONDAY) to Title Case (e.g., Monday)
            String rawDayName = date.getDayOfWeek().name(); // Get original name
            String dayName = rawDayName.substring(0, 1) + rawDayName.substring(1).toLowerCase(); // Apply formatting logic
            hours = regularHours.get(dayName); // Fetch hours for the formatted day name
        } // End fallback check

        // Step 3: Check if hours were found and if the provided time falls within that range
        return (hours != null && hours.isWithinRange(timeStr)); // Logic validation
    } // Method end

    // --- Table Inventory Management ---
    
    public void addTablesToInventory(int capacity, int count) { // Method to add/update table counts
        int currentCount = tableInventory.getOrDefault(capacity, 0); // Get existing count or default to 0
        tableInventory.put(capacity, currentCount + count); // Sum the values and update the map
    } // End method

    public int getTableCountByCapacity(int capacity) { // Method to check how many tables exist for a size
        return tableInventory.getOrDefault(capacity, 0); // Return count or 0 if capacity is unknown
    } // End method

    /**
     * Algorithm to find the smallest table that can accommodate the group size.
     */
    public int getBestFitCapacity(int requestedSize) { // Method start
        List<Integer> capacities = new ArrayList<>(tableInventory.keySet()); // Convert keys (sizes) into a list
        
        Collections.sort(capacities); // Sort sizes in ascending order (smallest to largest)
        
        for (int capacity : capacities) { // Loop through available sorted sizes
            if (capacity >= requestedSize) { // Check if this table size is enough for the party
                return capacity; // Return the first (and therefore smallest) fit found
            } // End if
        } // End loop
        
        return -1; // Return -1 if no table in the entire inventory is large enough
    } // Method end

    public Map<Integer, Integer> getFullInventory() { // Method to retrieve the entire table setup
        return new HashMap<>(tableInventory); // Return a protective copy of the inventory map
    } // End method
} // End of Restaurant class