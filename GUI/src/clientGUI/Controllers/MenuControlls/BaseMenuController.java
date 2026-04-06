package clientGUI.Controllers.MenuControlls; // Defining the package for GUI menu controllers

import client.ChatClient; // Importing the ChatClient class for communication
import common.ChatIF; // Importing the ChatIF interface for UI consistency

/**
 * Abstract base controller that defines the shared session state for the Bistro UI.
 * It serves as the foundation for the "Pipe" architecture, ensuring that the 
 * network client and user identity are propagated across all menu screens.
 * 
 */
public abstract class BaseMenuController implements ChatIF { 

    /** Protected reference to the network client for sending requests. */
    protected ChatClient client;
    
    /** The role/type of the current user (e.g., "Manager", "Subscriber"). */
    protected String userType;
    
    /** The unique database identifier for the current user session. */
    protected int userId; 
    
    /** Flag indicating if a staff member (Manager/Representative) is in "Subscriber Mode". */
    protected boolean actingAsSubscriber = false;
    
    /** Stores the original user role to allow returning from "Subscriber Mode". */
    protected String originalUserType;

    // --- Public Getters ---

    /**
     * Retrieves the active network client instance.
     * @return The ChatClient used for server communication.
     */
    public ChatClient getClient() { 
        return client; 
    } 

    /**
     * Retrieves the current user's role type.
     * @return A String representing the user's role.
     */
    public String getUserType() { 
        return userType; 
    } 

    /**
     * Retrieves the unique identifier of the user.
     * @return The database user ID as an integer.
     */
    public int getUserId() { 
        return userId; 
    } 

    /**
     * Updates the session-specific data for the current screen.
     * @param userType The role type to assign to this session.
     * @param userId   The unique user ID to assign.
     * @return None.
     */
    public void setSessionData(String userType, int userId) { 
        this.userType = userType;
        this.userId = userId;

        if (userType == null) { 
            return; 
        } 
    } 
    
    /**
     * Configures whether the current controller is operating in "Acting as Subscriber" mode.
     * @param actingAsSubscriber True if staff is in customer mode, false otherwise.
     * @return None.
     */
    public void setActingAsSubscriber(boolean actingAsSubscriber) {
        this.actingAsSubscriber = actingAsSubscriber;
    }
    
    /**
     * Preserves the original user role before a role-switch occurs.
     * @param originalUserType The role string (e.g., "Manager") to be saved.
     * @return None.
     */
    public void setOriginalUserType(String originalUserType) {
        this.originalUserType = originalUserType;
    }

    /**
     * Injects the network client, applies session data, and triggers the lifecycle hook.
     * This is the primary entry point for the "Pipe" data injection.
     * @param client   The active ChatClient instance.
     * @param userType The role of the user.
     * @param userId   The unique ID of the user.
     * @return None.
     */
    public void setClient(ChatClient client, String userType, int userId) { 
        this.client = client;
        setSessionData(userType, userId);

        if (this.client != null) { 
            this.client.setUI(this); 
            onClientReady(); 
        } 
    } 

    /**
     * Lifecycle hook method that is automatically called after the client and session data are injected.
     * Subclasses should override this to perform screen-specific initialization.
     * @return None.
     */
    public void onClientReady() { 
        // Default implementation does nothing
    } 

    /**
     * Implementation of the ChatIF interface for receiving asynchronous server messages.
     * @param message The data object received from the server.
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        // Default implementation for receiving messages
    } 
    
} // End of BaseMenuController class