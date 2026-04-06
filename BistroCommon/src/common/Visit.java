package common; // Defining the package where the class is located

import java.io.Serializable; // Importing the interface for object serialization

/**
 * Represents a customer's visit to the Bistro restaurant.
 * Tracks table occupancy, timing, and billing status throughout the visit lifecycle.
 */
public class Visit implements Serializable { 

    /**
     * Enumeration defining the possible lifecycle states of a restaurant visit.
     */
    public enum VisitStatus { 
        
        /** The customer is currently seated and the visit is ongoing. */
        ACTIVE,
        
        /** The visit has concluded, payment is done, and the table is free. */
        FINISHED,
        
        /** The visit has ended and the customer is awaiting the final bill. */
        BILL_PENDING
    } 

    /** Serial version UID for ensuring compatibility during OCSF network transmission. */
    private static final long serialVersionUID = 1L;

    /** Unique confirmation code derived from the initial reservation. */
    private long confirmationCode;
    
    /** The specific identifier of the table where the customer is seated. */
    private int tableId;
    
    /** The identifier of the user (customer) associated with this specific visit. */
    private int userId;
    
    /** The ID of the generated bill (null if bill isn't created yet). */
    private Long billId;
    
    /** Timestamp when the customer arrived. */
    private String startTime;
    
    /** The current status of the visit. */
    private VisitStatus status;

    /** Field to hold the count of diners (for display purposes). */
    private int numberOfGuests;

    /** Field for the original reservation date and time. */
    private String reservationDateTime;

    /**
     * Full constructor to initialize all fields of a Visit entity.
     * * @param confirmationCode Unique ID linked to the reservation.
     * @param tableId          ID of the assigned table.
     * @param userId           ID of the customer.
     * @param billId           ID of the generated bill.
     * @param startTime        Timestamp of arrival.
     * @param status           Initial lifecycle state of the visit.
     */
    public Visit(long confirmationCode, int tableId, int userId, long billId, String startTime, VisitStatus status) {
        this.confirmationCode = confirmationCode;
        this.tableId = tableId;
        this.userId = userId;
        this.billId = billId;
        this.startTime = startTime;
        this.status = status;
    }

    /**
     * Retrieves the reservation confirmation code.
     * @return The unique confirmation code as a long.
     */
    public long getConfirmationCode() {
        return confirmationCode;
    }

    /**
     * Updates the reservation confirmation code.
     * @param confirmationCode The new confirmation code to assign.
     */
    public void setConfirmationCode(long confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    /**
     * Retrieves the identifier of the seated table.
     * @return The table ID as an integer.
     */
    public int getTableId() {
        return tableId;
    }

    /**
     * Updates the table identifier.
     * @param tableId The new table ID to set.
     */
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    /**
     * Retrieves the customer's user identifier.
     * @return The user ID as an integer.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Updates the user identifier for this visit.
     * @param userId The new user ID to set.
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Retrieves the associated bill identifier.
     * @return The bill ID as a Long, or null if not yet created.
     */
    public Long getBillId() {
        return billId;
    }

    /**
     * Updates the associated bill identifier.
     * @param billId The new bill ID to assign.
     */
    public void setBillId(Long billId) {
        this.billId = billId;
    }

    /**
     * Retrieves the visit start time timestamp.
     * @return The start time as a String.
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Updates the visit start time.
     * @param startTime The arrival timestamp to set.
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Retrieves the current lifecycle status of the visit.
     * @return The current VisitStatus enum value.
     */
    public VisitStatus getStatus() {
        return status;
    }

    /**
     * Updates the current status of the visit.
     * @param status The new lifecycle state to set.
     */
    public void setStatus(VisitStatus status) {
        this.status = status;
    }

    /**
     * Retrieves the number of guests in the dining group.
     * @return The guest count as an integer.
     */
    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    /**
     * Updates the number of guests in the group.
     * @param numberOfGuests The guest count to set.
     */
    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    /**
     * Provides a summarized string representation of the Visit.
     * @return A string summary for logging and debugging.
     */
    @Override
    public String toString() {
        return "Visit [" + 
               "Code=" + confirmationCode + 
               ", Table=" + tableId + 
               ", Status=" + status + 
               "]";
    }

    /**
     * Retrieves the original reservation timestamp.
     * @return The reservation date and time as a String.
     */
    public String getReservationDateTime() {
        return reservationDateTime;
    }

    /**
     * Updates the original reservation timestamp.
     * @param reservationDateTime The reservation date and time to set.
     */
    public void setReservationDateTime(String reservationDateTime) {
        this.reservationDateTime = reservationDateTime;
    }
}