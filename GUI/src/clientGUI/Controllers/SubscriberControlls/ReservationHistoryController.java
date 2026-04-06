package clientGUI.Controllers.SubscriberControlls; // Defining the package for subscriber-related controllers

import java.util.ArrayList; // Importing ArrayList for data list management
import common.Reservation; // Importing the Reservation entity class
import client.ChatClient; // Importing the main client communication class
import common.ChatIF; // Importing the communication interface for server responses
import javafx.application.Platform; // Importing Platform for UI thread safety
import javafx.event.ActionEvent; // Importing ActionEvent for UI interaction handling
import javafx.scene.control.Alert; // Importing Alert class for displaying dialog boxes
import javafx.scene.control.Button; // Importing Button component
import javafx.scene.control.TableColumn; // Importing TableColumn for table configuration
import javafx.scene.control.TableView; // Importing TableView for displaying data grids
import javafx.scene.control.cell.PropertyValueFactory; // Importing factory for mapping object properties to columns
import javafx.fxml.FXML; // Importing FXML annotation for UI element injection
import javafx.scene.Node; // Importing Node for accessing the scene graph
import javafx.stage.Stage; // Importing Stage for window management

/**
 * Controller for the Reservation History screen in the Bistro system.
 * Fetches and displays past and future reservations for a specific subscriber using a TableView.
 */
public class ReservationHistoryController implements ChatIF { 
	
	private ChatClient client;

    /**
     * Injects the network client and registers this controller as the UI listener.
     * @param client The active ChatClient instance.
     * @return None.
     */
	public void setClient(ChatClient client) { 
	    this.client = client; 
	    client.setUI(this); 
	} 
	
	// --- FXML UI Components ---
	
	@FXML private Button btnBack; 
	@FXML private TableView<Reservation> reservationsTable; 
	@FXML private TableColumn<Reservation, Long> codeCol; 
	@FXML private TableColumn<Reservation, String> dateCol; 
	@FXML private TableColumn<Reservation, String> timeCol; 
	@FXML private TableColumn<Reservation, Integer> guestCol; 
	@FXML private TableColumn<Reservation, String> statusCol; 
	
    /**
     * Initializes the TableView columns by mapping them to Reservation properties.
     * Called automatically by JavaFX.
     * @return None.
     */
	@FXML 
	private void initialize() { 
        codeCol.setCellValueFactory(new PropertyValueFactory<>("confirmationCode")); 
	    dateCol.setCellValueFactory(new PropertyValueFactory<>("reservationDate")); 
	    timeCol.setCellValueFactory(new PropertyValueFactory<>("reservationTime")); 
	    guestCol.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests")); 
	    statusCol.setCellValueFactory(new PropertyValueFactory<>("statusString")); 
	} 

	/**
     * Closes the reservation history window.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
	@FXML 
	private void clickBack(ActionEvent event) { 
	    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
	    stage.close(); 
	} 
	
    /**
     * Sends a request to the server to fetch history for a specific subscriber.
     * @param userId The unique identification of the subscriber.
     * @return None.
     */
	public void loadReservationsForUser(int userId) { 
	    if (client == null) { 
	        System.out.println("Error: client is null"); 
	        return; 
	    } 

	    ArrayList<Object> msg = new ArrayList<>(); 
	    msg.add("GET_RESERVATIONS_HISTORY"); 
	    msg.add(userId); 

	    client.handleMessageFromClientUI(msg); 
	} 

    /**
     * Processes server responses and populates the TableView on the UI thread.
     * @param message The server response containing a list of reservations or an error.
     * @return None.
     */
	@Override 
	public void display(Object message) { 
	    if (message instanceof ArrayList) { 
	        ArrayList<?> data = (ArrayList<?>) message; 
	        String command = data.get(0).toString(); 

            switch (command) { 
                case "RESERVATION_HISTORY": 
                    @SuppressWarnings("unchecked")
                    ArrayList<Reservation> reservations = (ArrayList<Reservation>) data.get(1); 

                    Platform.runLater(() -> { 
                        if (reservations == null || reservations.isEmpty()) { 
                            showNoReservationsAndClose(); 
                            return; 
                        } 
                        reservationsTable.getItems().clear(); 
                        reservationsTable.getItems().addAll(reservations); 
                    }); 
                    break; 

                default: 
                    break; 
            } 
	    } 
	    else { 
	    	Platform.runLater(() -> showUnexpectedErrorAndClose()); 
	    } 
	} 
	
	/**
	 * Displays an alert when no history is found and closes the window.
	 * @return None.
	 */
	private void showNoReservationsAndClose() { 
	    Alert alert = new Alert(Alert.AlertType.INFORMATION); 
	    alert.setTitle("Reservation History"); 
	    alert.setHeaderText(null); 
	    alert.setContentText("You have no reservations in your history."); 
	    alert.showAndWait(); 

	    Stage stage = (Stage) reservationsTable.getScene().getWindow(); 
	    stage.close(); 
	} 
	
	/**
	 * Displays an error alert for unexpected server responses and closes the window.
	 * @return None.
	 */
    private void showUnexpectedErrorAndClose() { 
        Alert alert = new Alert(Alert.AlertType.ERROR); 
        alert.setTitle("Unexpected Error"); 
        alert.setHeaderText(null); 
        alert.setContentText( 
            "An unexpected error occurred while loading your reservation history.\n" +
            "Please try again later."
        ); 
        alert.showAndWait(); 

        Stage stage = (Stage) reservationsTable.getScene().getWindow(); 
        stage.close(); 
    } 
}