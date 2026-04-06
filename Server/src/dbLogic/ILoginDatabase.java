package dbLogic; // Defining the package for database-related logic interfaces

/**
 * The ILoginDatabase interface defines the formal contract for all database 
 * operations related to user authentication and guest registration.
 */
public interface ILoginDatabase { // Start of the ILoginDatabase interface definition

    /**
     * Verifies a registered subscriber's credentials.
     * @param subID The unique numeric identifier assigned to the subscriber.
     * @return The internal system 'userId' (int) or -1 if authentication fails.
     */
    int verifySubscriber(long subID); // Method signature for subscriber verification

    /**
     * Verifies an occasional guest's credentials based on their chosen identity.
     * @param username    The guest's unique username.
     * @param contactInfo The registered phone number or email associated with the guest.
     * @return The internal system 'userId' (int) or -1 if authentication fails.
     */
    int verifyOccasional(String username, String contactInfo); // Method signature for guest verification

    /**
     * Registers a new occasional customer in the database.
     * @param username The desired username for the new guest.
     * @param phone    The guest's mobile phone number.
     * @param email    The guest's email address.
     * @return true if successful; false otherwise.
     */
    boolean registerOccasional(String username, String phone, String email); // Method signature for guest registration

} // End of the ILoginDatabase interface