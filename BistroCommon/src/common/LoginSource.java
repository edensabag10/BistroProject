package common;

/**
 * Defines the possible origins of a login request within the Bistro system.
 * This helps the server determine which UI or logic flow to apply based on the source.
 */
public enum LoginSource {
    
    /** Indicates the login was initiated from a remote management or client application. */
    REMOTE,
    
    /** Indicates the login was initiated from a physical restaurant terminal or kiosk. */
    TERMINAL
}