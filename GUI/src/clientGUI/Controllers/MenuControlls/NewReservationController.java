package clientGUI.Controllers.MenuControlls; // Define the package for menu controllers

import java.net.URL; // Import for handling URL resources
import java.time.LocalDate; // Import for handling dates without time
import java.time.LocalDateTime; // Import for handling both date and time
import java.time.LocalTime; // Import for handling time components
import java.util.ArrayList; // Import for dynamic array list structures
import java.util.ResourceBundle; // Import for localization resources
import client.ChatClient; // Import the main client communication class
import common.ChatIF; // Import the communication interface
import common.Reservation; // Import the Reservation Data Transfer Object
import common.Restaurant; // Import the Restaurant entity class
import common.ServiceResponse; // Import the generic server response envelope
import javafx.application.Platform; // Import for running tasks on the JavaFX thread
import javafx.collections.FXCollections; // Import for creating observable collections
import javafx.collections.ObservableList; // Import for list types used in UI components
import javafx.event.ActionEvent; // Import for handling UI action events
import javafx.fxml.FXML; // Import for FXML injection annotation
import javafx.fxml.FXMLLoader; // Import for loading FXML layout files
import javafx.fxml.Initializable; // Import for initialization interface
import javafx.scene.Node; // Import for generic UI node elements
import javafx.scene.Parent; // Import for root UI elements
import javafx.scene.Scene; // Import for stage scene management
import javafx.scene.control.Alert; // Import for alert dialog boxes
import javafx.scene.control.Alert.AlertType; // Import for alert types (Info, Error, etc.)
import javafx.scene.control.Button; // Import for button components
import javafx.scene.control.ButtonType; // Import for alert button types
import javafx.scene.control.ComboBox; // Import for dropdown selection components
import javafx.scene.control.DateCell; // Import for customizing individual date cells
import javafx.scene.control.DatePicker; // Import for date selection components
import javafx.scene.control.TextArea; // Import for multi-line text display
import javafx.scene.control.TextField; // Import for single-line text input
import javafx.stage.Stage; // Import for the primary window container

/**
 * Controller for the New Reservation interface in the Bistro system.
 * This class handles the creation of new bookings, including complex validation rules, 
 * real-time availability checks, and handling alternative slot suggestions from the server.
 */
public class NewReservationController extends BaseMenuController implements ChatIF, Initializable { 

    @FXML private DatePicker dpDate; 
    @FXML private ComboBox<String> comboTime; 
    @FXML private TextField txtGuests; 
    @FXML private TextArea txtLog; 
    @FXML private Button btnConfirm; 
    @FXML private Button btnBack; 
    
    /** Internal cache for restaurant operational data. */
    private Restaurant currentRestaurant; 
    
    /**
     * Triggered when the client is ready. Requests restaurant operating hours from the server.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        ArrayList<Object> msg = new ArrayList<>(); 
        msg.add("GET_RESTAURANT_WORKTIMES"); 
        
        this.client.handleMessageFromClientUI(msg); 
        appendLog("Fetching restaurant information..."); 
    } 
   
    /**
     * Initializes UI components, configures date picker constraints, and populates time slots.
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     * @return None.
     */
    @Override 
    public void initialize(URL location, ResourceBundle resources) { 
        dpDate.setEditable(false); 

        dpDate.setDayCellFactory(picker -> new DateCell() { 
            @Override 
            public void updateItem(LocalDate date, boolean empty) { 
                super.updateItem(date, empty); 
                if (date.isBefore(LocalDate.now())) { 
                    setDisable(true); 
                    setStyle("-fx-background-color: #ffc0cb;"); 
                } 
            } 
        }); 

        ObservableList<String> hours = FXCollections.observableArrayList(); 
        LocalTime time = LocalTime.MIDNIGHT; 
        
        do { 
            hours.add(time.toString()); 
            time = time.plusMinutes(30); 
        } while (!time.equals(LocalTime.MIDNIGHT)); 

        comboTime.setItems(hours); 
        dpDate.setValue(LocalDate.now()); 
        
        appendLog("Ready to take your reservation."); 
    } 

    /**
     * Processes the confirmation action by validating inputs (future time, capacity) and sending data.
     * @param event The ActionEvent triggered by the confirm button.
     * @return None.
     */
    @FXML 
    void clickConfirm(ActionEvent event) { 
        
        boolean isMissingInput = (dpDate.getValue() == null || comboTime.getValue() == null || txtGuests.getText().isEmpty()); 
        
        if (isMissingInput) { 
            appendLog("Error: Missing input fields."); 
            return; 
        } 

        try { 
            String guestsInput = txtGuests.getText().trim(); 
            
            if (!guestsInput.matches("\\d+")) { 
                appendLog("Error: Guests field must contain only numbers (no letters or symbols)."); 
                return; 
            } 
            
            int guestCount = Integer.parseInt(guestsInput); 
            
            if (guestCount <= 0) { 
                throw new NumberFormatException(); 
            } 
            
            LocalDateTime requestedDT = LocalDateTime.of(dpDate.getValue(), LocalTime.parse(comboTime.getValue())); 
            LocalDateTime now = LocalDateTime.now(); 
            
            if (requestedDT.isBefore(now.plusHours(1))) { 
                appendLog("Error: Reservations must be made at least 1 hour in advance."); 
                return; 
            } 

            if (requestedDT.isAfter(now.plusMonths(1))) { 
                appendLog("Error: Reservations can only be made up to one month in advance."); 
                return; 
            } 

            String sqlDateTime = dpDate.getValue().toString() + " " + comboTime.getValue() + ":00"; 
            
            Reservation res = new Reservation(userId, sqlDateTime, guestCount); 
            
            ArrayList<Object> msg = new ArrayList<>(); 
            msg.add("CREATE_RESERVATION"); 
            msg.add(res); 

            if (client != null) { 
                client.setUI(this); 
                appendLog("Sending request to server..."); 
                client.handleMessageFromClientUI(msg); 
            } 
            
        } catch (NumberFormatException e) { 
            appendLog("Error: Please enter a valid number of guests."); 
        } 
    } 

    /**
     * Navigates the user back to their respective menu based on their user role.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML 
    void clickBack(ActionEvent event) { 
        
        String fxmlPath = ""; 
        
        if (userType == null) { 
            appendLog("Error: Unknown user type for navigation."); 
            return; 
        } 

        switch (userType) { 
            case "Subscriber": 
                fxmlPath = "/clientGUI/fxmlFiles/SubscriberFXML/SubscriberMenuFrame.fxml"; 
                break; 
                
            case "Occasional": 
                fxmlPath = "/clientGUI/fxmlFiles/OccasionalFXML/OccasionalMenuFrame.fxml"; 
                break; 
                
            default: 
                appendLog("Error: Invalid role detected."); 
                return; 
        } 

        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath)); 
            Parent root = loader.load(); 
            
            BaseMenuController controller = loader.getController(); 
            controller.setClient(client, userType, userId); 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            Scene scene = new Scene(root); 
            scene.getStylesheets().add(getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()); 
            stage.setScene(scene); 
            stage.show(); 
            
        } catch (Exception e) { 
            e.printStackTrace(); 
            appendLog("Navigation Error: " + e.getMessage()); 
        } 
    } 

    /**
     * Processes server responses, including restaurant metadata and reservation status updates.
     * @param message The response object (Restaurant or ServiceResponse).
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        
        if (message instanceof Restaurant) { 
            this.currentRestaurant = (Restaurant) message; 

            Platform.runLater(() -> { 
                appendLog("--- Restaurant Information Loaded ---"); 
                appendLog(currentRestaurant.getFormattedOpeningHours()); 
            }); 
            return; 
        } 

        if (message instanceof ServiceResponse) { 
            ServiceResponse sr = (ServiceResponse) message; 
            
            Platform.runLater(() -> { 
                switch (sr.getStatus()) { 
                    case RESERVATION_SUCCESS: 
                        showPopup(AlertType.INFORMATION, "Success", "Reservation confirmed! Code: " + sr.getData()); 
                        appendLog("Reservation secured with code: " + sr.getData()); 
                        break; 
                        
                    case RESERVATION_SUGGESTION: 
                        handleSuggestion(sr.getData().toString()); 
                        break; 
                        
                    case RESERVATION_FULL: 
                        showPopup(AlertType.WARNING, "Fully Booked", "Sorry, no tables available."); 
                        appendLog("Server: No tables available."); 
                        break; 
                        
                    case INTERNAL_ERROR: 
                        showPopup(AlertType.ERROR, "Server Error", sr.getData().toString()); 
                        appendLog("Server Error: " + sr.getData()); 
                        break; 
                        
                    case RESERVATION_OUT_OF_HOURS: 
                        showPopup(AlertType.WARNING, "Restaurant Closed", sr.getData().toString()); 
                        appendLog("Server: " + sr.getData()); 
                        break; 
                        
                    default: 
                        break; 
                } 
            }); 
        } 
    } 

    /**
     * Handles the alternative time suggestion flow by prompting the user with a confirmation dialog.
     * @param suggested The recommended time slot string provided by the server.
     * @return None.
     */
    private void handleSuggestion(String suggested) { 
        Alert suggestionAlert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Requested time is full. Would you like to reserve for " + suggested + " instead?", 
            ButtonType.OK, ButtonType.CANCEL); 
            
        suggestionAlert.setTitle("Alternative Slot Found"); 
        suggestionAlert.setHeaderText("No availability for requested time."); 
        
        if (suggestionAlert.showAndWait().get() == ButtonType.OK) { 
            try { 
                int guests = Integer.parseInt(txtGuests.getText()); 
                Reservation res = new Reservation(userId, suggested + ":00", guests); 
                
                ArrayList<Object> msg = new ArrayList<>(); 
                msg.add("CREATE_RESERVATION"); 
                msg.add(res); 
                
                client.handleMessageFromClientUI(msg); 
                appendLog("Attempting to book suggested slot: " + suggested); 
                
            } catch (NumberFormatException e) { 
                appendLog("Error reading guest number for suggestion."); 
            } 
        } 
    } 

    /**
     * Internal utility for displaying JavaFX alerts.
     * @param type    The AlertType of the dialog.
     * @param title   The title of the window.
     * @param content The message body.
     * @return None.
     */
    private void showPopup(AlertType type, String title, String content) { 
        Alert alert = new Alert(type); 
        alert.setTitle(title); 
        alert.setHeaderText(null); 
        alert.setContentText(content); 
        alert.showAndWait(); 
    } 

    /**
     * Updates the status log area on the UI thread.
     * @param message The text message to append to the log.
     * @return None.
     */
    public void appendLog(String message) { 
        Platform.runLater(() -> { 
            txtLog.appendText("> " + message + "\n"); 
        }); 
    } 
}