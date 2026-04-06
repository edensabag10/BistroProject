package common; // Define the package name for the class

import java.io.Serializable; // Import the Serializable interface for object transmission

/**
 * The Reservation class represents a data transfer object (DTO) for restaurant bookings.
 * It encapsulates all necessary information for a reservation.
 */
public class Reservation implements Serializable { // Begin class definition implementing Serializable
    
    // Serial version ID to maintain compatibility during the serialization process
    private static final long serialVersionUID = 1L;

    /**
     * Enum representing the various states a reservation can hold.
     */
    public enum ReservationStatus { // Define internal Enum for reservation states
        ACTIVE,   // Represents an active and confirmed reservation
        CANCELLED, // Represents a reservation that has been revoked
        WAITING_AT_RESTAURANT,
        NOTIFIED,
        NOSHOW,
        FINISHED,
        ARRIVED   // Represents a state where the customer has reached the restaurant
    } // End of Enum definition

    // Unique ID for the reservation (Maps to BIGINT in DB)
    private long confirmationCode;   

    // Date and time string in 'YYYY-MM-DD HH:MM:SS' format
    private String reservationDateTime; 

    // Total number of guests for the booking
    private int numberOfGuests;      

    // The unique ID of the user who made the reservation
    private int userId;              

    // The current status of the reservation (ACTIVE, CANCELED, or ARRIVED)
    private ReservationStatus status;

    /**
     * Constructs a new Reservation instance.
     * @param userId The user ID
     * @param dateTime The reservation date and time
     * @param numberOfGuests The guest count
     */
    public Reservation(int userId, String dateTime, int numberOfGuests) { // Constructor start
        this.userId = userId;               // Initialize the userId field
        this.reservationDateTime = dateTime; // Initialize the reservationDateTime field
        this.numberOfGuests = numberOfGuests; // Initialize the numberOfGuests field
        this.status = ReservationStatus.ACTIVE; // Set the default status to ACTIVE upon creation
    } // Constructor end

    // --- Getter and Setter Methods ---

    public long getConfirmationCode() { // Method to get confirmation code
        return confirmationCode;        // Return the confirmationCode value
    } // End method

    public void setConfirmationCode(long code) { // Method to set confirmation code
        this.confirmationCode = code;            // Assign the provided code to the field
    } // End method
    
    public void setStatus(ReservationStatus status) { // Method to update reservation status
        this.status = status;                         // Assign the new status to the field
    } // End method

    public String getReservationDateTime() { // Method to get full date-time string
        return reservationDateTime;          // Return the reservationDateTime value
    } // End method
    
    /**
     * Extracts the date part (YYYY-MM-DD).
     */
    public String getReservationDate() { // Method to extract date
        // Use ternary operator to handle null check and return split string or empty string
        return (reservationDateTime == null) ? "" : reservationDateTime.split(" ")[0]; 
    } // End method

    /**
     * Extracts the time part (HH:MM:SS).
     */
    public String getReservationTime() { // Method to extract time
        // Use ternary operator to handle null check and return split string or empty string
        return (reservationDateTime == null) ? "" : reservationDateTime.split(" ")[1];
    } // End method

    public int getNumberOfGuests() { // Method to get guest count
        return numberOfGuests;       // Return the numberOfGuests value
    } // End method

    public int getUserId() { // Method to get user ID
        return userId;       // Return the userId value
    } // End method

    /**
     * Returns the status as a String.
     */
    public String getStatusString() { // Method to get status name as String
        return status.name();         // Return the string representation of the enum constant
    } // End method
} // End of Reservation class