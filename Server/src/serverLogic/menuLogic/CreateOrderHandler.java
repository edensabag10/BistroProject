package serverLogic.menuLogic; // Defining the package for menu-related server logic handlers

import java.io.IOException; // Importing for handling network communication errors
import java.util.ArrayList; // Importing for dynamic list structure handling

import common.Reservation; // Importing the Reservation DTO
import common.ServiceResponse; // Importing the standardized response wrapper
import dbLogic.restaurantDB.CreateOrderController; // Importing the business logic controller
import ocsf.server.ConnectionToClient; // Importing the OCSF client connection handle

/**
 * The CreateOrderHandler class manages the "CREATE_RESERVATION" command.
 * It decapsulates the DTO, triggers the business logic, and returns the result.
 */
public class CreateOrderHandler { // Start of CreateOrderHandler class definition

    /**
     * Entry point for processing a reservation creation request.
     * @param messageList The ArrayList protocol: [0] command, [1] Reservation object.
     * @param client      The specific client connection handle.
     */
    public void handle(ArrayList<Object> messageList, ConnectionToClient client) { // Start of handle method
        
        try { // Start of the main try block for processing logic
            
            // --- STEP 1: OBJECT EXTRACTION ---
            
            // Extract the Reservation DTO from the protocol list at the expected index
            Reservation reservation = (Reservation) messageList.get(1); // Casting object at index 1

            // --- STEP 2: BUSINESS LOGIC DELEGATION ---
            
            // Delegate table allocation and DB persistence to the specialized controller
            ServiceResponse result = CreateOrderController.processNewReservation(reservation); // Executing logic

            // --- STEP 3: RESPONSE TRANSMISSION ---
            
            // Transmit the final ServiceResponse (Success/Suggestion/Full) back to the client UI
            client.sendToClient(result); // Sending the result object via socket

        } // End of main processing logic
        catch (ClassCastException e) { // Start of catch block for protocol format errors
            
            // Log technical error indicating the received object was not a Reservation
            System.err.println("CreateOrderHandler Error: Expected Reservation object at index 1."); // Logging error
            
            // Inform the client about the invalid data format using a standardized error response
            sendErrorMessage(client, "INTERNAL_ERROR: Invalid reservation data format."); // Sending error feedback
            
        } // End of ClassCastException catch
        catch (IOException e) { // Start of catch block for socket communication errors
            
            // Log the network failure to the server console
            System.err.println("CreateOrderHandler Error: Failed to send response to client."); // Logging error
            
            // Print the stack trace for technical troubleshooting
            e.printStackTrace(); // Outputting technical details
            
        } // End of IOException catch
        catch (Exception e) { // Start of catch block for any other unexpected runtime issues
            
            // Print the exception details to diagnose the failure
            e.printStackTrace(); // Outputting technical trace
            
            // Send a generic internal error message to prevent the client from hanging
            sendErrorMessage(client, "INTERNAL_ERROR: Server encountered an unexpected issue."); // Sending feedback
            
        } // End of generic Exception catch
        
    } // End of the handle method

    /**
     * Helper method to send standardized error responses to the client.
     * @param client  The network connection to the client.
     * @param message The descriptive error message string.
     */
    private void sendErrorMessage(ConnectionToClient client, String message) { // Start of sendErrorMessage method
        
        try { // Start of inner try block for error reporting
            
            // Initialize a new ServiceResponse with an INTERNAL_ERROR status
            ServiceResponse errorResponse = new ServiceResponse( // Start object initialization
                ServiceResponse.ServiceStatus.INTERNAL_ERROR, // Setting status
                message // Setting payload message
            ); // End object initialization
            
            // Transmit the error response object back to the client
            client.sendToClient(errorResponse); // Execution of send
            
        } // End of reporting try block
        catch (IOException e) { // Catch block if the error report itself fails to send
            
            // Log a critical failure indicating that the client is unreachable even for error reporting
            System.err.println("Critical Error: Could not send error response to client: " + e.getMessage()); // Log failure
            
        } // End of inner catch block
        
    } // End of the sendErrorMessage helper method
    
} // End of the CreateOrderHandler class definition