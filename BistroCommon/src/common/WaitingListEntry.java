package common; // Defining the package location for this class

import java.io.Serializable; // Importing the interface for object transmission

/**
 * Represents a customer on the restaurant's waiting list.
 * This class tracks entries for diners who could not find an immediate reservation.
 */
public class WaitingListEntry implements Serializable { 

    /** Serial version identifier for serialization compatibility. */
    private static final long serialVersionUID = 1L;

    /** Unique confirmation code identifying this specific waiting entry. */
    private long confirmationCode;
    
    /** The timestamp when the customer was added to the waiting list. */
    private String entryTime;
    
    /** Total number of guests associated with this entry. */
    private int numberOfGuests;
    
    /** The unique ID of the user who is waiting. */
    private int userId;
    
    /** Current status of the entry (e.g., WAITING, NOTIFIED). */
    private String status; 
    
    /** The timestamp when the customer was notified about an available table. */
    private String notificationTime;

    /**
     * Enumeration of valid statuses for a waiting list entry.
     */
    public enum WaitingStatus { 
        /** The customer is currently in the queue. */
        WAITING,   
        /** The customer has been alerted that a table is ready. */
        NOTIFIED,  
        /** The customer removed the entry. */
        CANCELLED, 
        /** The customer has arrived and been seated. */
        ARRIVED,    
        /** The customer did not arrive at the table. */
        NOSHOW      
    } 

    /**
     * Constructs a new WaitingListEntry with complete details.
     * * @param confirmationCode Unique ID identifying this entry.
     * @param entryTime        Timestamp of addition to the list.
     * @param numberOfGuests   Total count of diners in the group.
     * @param userId           Unique ID of the waiting user.
     * @param status           Current operational status string.
     * @param notificationTime Timestamp when notification was sent.
     */
    public WaitingListEntry(long confirmationCode, String entryTime, int numberOfGuests, int userId, String status, String notificationTime) {
        this.confirmationCode = confirmationCode;
        this.entryTime = entryTime;
        this.numberOfGuests = numberOfGuests;
        this.userId = userId;
        this.status = status;
        this.notificationTime = notificationTime;
    }

    // =========================================================================
    // Getters and Setters Section
    // =========================================================================

    /**
     * Retrieves the confirmation code of the entry.
     * * @return The confirmation code as a long.
     */
    public long getConfirmationCode() { 
        return confirmationCode;        
    } 

    /**
     * Retrieves the user ID associated with this entry.
     * * @return The user ID as an integer.
     */
    public int getUserId() { 
        return userId;       
    } 

    /**
     * Retrieves the current status of the waiting entry.
     * * @return The status as a String.
     */
    public String getStatus() { 
        return status;          
    } 
    
    /**
     * Retrieves the number of guests for this waiting list entry.
     * * @return The guest count as an integer.
     */
    public int getNumberOfGuests() {
        return numberOfGuests;
    }
    
    /**
     * Retrieves the timestamp when the entry was created.
     * * @return The entry time as a String.
     */
    public String getEntryTime() {
        return entryTime;
    }

} // End of WaitingListEntry class