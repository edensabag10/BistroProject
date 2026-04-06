package common;

import java.io.Serializable;

/**
 * Represents a registered subscriber in the Bistro system.
 * This class holds user identity details and their associated digital QR code.
 */
public class Subscriber implements Serializable {
    
    /** Serial version UID for serialization compatibility. */
    private static final long serialVersionUID = 1L;
    
    /** The unique identification number of the user. */
    private int userId;
    
    /** The specific subscription identifier. */
    private int subscriberId;
    
    /** The username of the subscriber. */
    private String username;
    
    /** The digital QR code string assigned to the subscriber. */
    private String qrCode;

    /**
     * Constructs a new Subscriber instance with identity and account details.
     * * @param userId       The unique identification number of the user.
     * @param subscriberId The specific subscription identifier.
     * @param username     The username of the subscriber.
     * @param qrCode       The digital QR code string assigned to the subscriber.
     */
    public Subscriber(int userId, int subscriberId, String username, String qrCode) {
        this.userId = userId;
        this.subscriberId = subscriberId;
        this.username = username;
        this.qrCode = qrCode;
    }

    // Getters for TableView binding

    /**
     * Retrieves the user's unique identification number.
     * * @return The user ID as an integer.
     */
    public int getUserId() { return userId; }

    /**
     * Retrieves the specific subscription identifier.
     * * @return The subscriber ID as an integer.
     */
    public int getSubscriberId() { return subscriberId; }

    /**
     * Retrieves the username of the subscriber.
     * * @return The username as a String.
     */
    public String getUsername() { return username; }

    /**
     * Retrieves the digital QR code string.
     * * @return The QR code as a String.
     */
    public String getQrCode() { return qrCode; }
}