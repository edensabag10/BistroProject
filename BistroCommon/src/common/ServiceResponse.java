package common; // Define the package where the class belongs

import java.io.Serializable; // Import the Serializable interface for network transmission

/**
 * ServiceResponse serves as a generic communication envelope for messages sent 
 * from the Server to the Client in the Bistro system.
 */
public class ServiceResponse implements Serializable { 
    
    /** Serial version UID for serialization compatibility. */
    private static final long serialVersionUID = 1L;

    /**
     * Enum defining the specific outcomes of a reservation request handled by the Server.
     */
    public enum ServiceStatus { 
        
        /** Case: The reservation was successfully placed and saved in the DB. */
        RESERVATION_SUCCESS,    
        
        /** Case: Slot is full, but an alternative date/time is suggested. */
        RESERVATION_SUGGESTION, 
        
        /** Case: No availability found for requested or alternative slots. */
        RESERVATION_FULL,
        
        /** Case: The requested time falls outside of restaurant operating hours. */
        RESERVATION_OUT_OF_HOURS,
        
        /** Case: A general update operation was completed successfully. */
        UPDATE_SUCCESS,

        /** Case: A technical failure occurred (SQL error, connection loss, etc.). */
        INTERNAL_ERROR          
    } 

    /** Field to store the specific outcome category of the response. */
    private ServiceStatus status;
    
    /** Polymorphic field to hold the payload (ID, Date string, or Error message). */
    private Object data; 

    /**
     * Constructs a new ServiceResponse with the specified status and data payload.
     * * @param status The category of the response outcome.
     * @param data   The actual information or object being sent.
     */
    public ServiceResponse(ServiceStatus status, Object data) { 
        this.status = status;
        this.data = data;
    } 

    /**
     * Retrieves the status of the server response.
     * * @return The ServiceStatus associated with this response.
     */
    public ServiceStatus getStatus() { 
        return status; 
    } 

    /**
     * Retrieves the data payload associated with this response.
     * * @return The data object containing the response payload.
     */
    public Object getData() { 
        return data; 
    } 

    /**
     * Provides a human-readable string summary of the response.
     * * @return A string representation of the status and data.
     */
    @Override 
    public String toString() { 
        return "ServiceResponse [" + 
               "Status=" + status + 
               ", Data=" + data + 
               "]";
    } 
    
    /**
     * Retrieves the human-readable message carried by this response.
     * * @return A string representation of the payload, or empty string if null.
     */
    public String getMessage() { 
        return data != null ? data.toString() : "";
    } 

} // End of ServiceResponse class