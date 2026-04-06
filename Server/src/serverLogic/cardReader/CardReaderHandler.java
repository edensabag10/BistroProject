package serverLogic.cardReader;
import dbLogic.restaurantDB.VisitController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import dbLogic.cardReader.CardReaderDBController;
import ocsf.server.ConnectionToClient;

/**
 * Handler that manages the Card Reader logic on the server.
 */
public class CardReaderHandler {

    /**
     * Receives a message from the server and routes it to the appropriate action.
     * @param data A list containing: [0] command, [1...] data.
     * @param client The client connection.
     */
	public void handle(ArrayList<Object> data, ConnectionToClient client) {
	    String command = (String) data.get(0); 
	    CardReaderDBController db = new CardReaderDBController(); 

	    try {
	        switch (command) {
	            case "CARD_READER_LOGIN":
	                String subIDForLogin = (String) data.get(1);
	                boolean isValid = db.validateSubscriber(subIDForLogin);
	                client.sendToClient(isValid); 
	                break;

	            case "CARD_READER_GET_CODES": {
	                String idFromTerminal = (String) data.get(1);
	                
                    // Instead of calling viewReservationController (which may close the connection),
                    // call the Card Reader DBController method defined above
	                List<String> codesList = db.getLostConfirmationCodes(idFromTerminal);
	                
	                client.sendToClient(codesList); 
	                break;
	            }
	            
	            case "CARD_READER_VERIFY_CODE":
	                String codeStr = (String) data.get(1);
	                long code = Long.parseLong(codeStr);
	                String result = dbLogic.restaurantDB.VisitController.processTerminalArrival(code);
	                client.sendToClient(result); 
	                break;
	        }
	    } catch (Exception e) {
	        System.err.println("Error in CardReaderHandler: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
    
    
    
}