package clientGUI.Controllers.MenuControlls; // Defining the package for menu controllers

import java.util.ArrayList; // Importing ArrayList for message construction
import java.util.List; // Importing List interface for data handling
import common.Reservation; // Importing the Reservation entity
import client.ChatClient; // יבוא הלקוח - הוספתי כדי לוודא תקינות
import clientGUI.Controllers.ICustomerActions; // Importing the customer actions interface
import javafx.application.Platform; // Importing Platform for UI thread safety
import javafx.collections.FXCollections; // Importing for observable list creation
import javafx.event.ActionEvent; // Importing for UI action event handling
import javafx.fxml.FXML; // Importing FXML annotation for injection
import javafx.scene.control.*; // Importing standard JavaFX controls
import javafx.scene.control.cell.PropertyValueFactory; // Importing for table cell mapping
import javafx.beans.binding.Bindings; // Importing for dynamic UI binding
import javafx.stage.Stage; // Importing Stage for window management

/**
 * Controller class for the View Reservation screen in the Bistro system.
 * Displays a list of active reservations and provides cancellation options with a confirmation flow.
 */
public class ViewReservationController extends BaseMenuController implements ICustomerActions { 

    // FXML Table components for displaying reservation data
    @FXML private TableView<Reservation> tableReservations; 
    @FXML private TableColumn<Reservation, Long> colCode; 
    @FXML private TableColumn<Reservation, String> colDate; 
    @FXML private TableColumn<Reservation, Integer> colGuests; 
    @FXML private TableColumn<Reservation, Void> colAction; 

    /**
     * Triggered when the client is fully initialized. Requests active reservations for the user.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        System.out.println("DEBUG: Entering screen. UserID: " + userId); 
        
        if (client != null && userId != 0) { 
            client.setUI(this); 
            
            Platform.runLater(() -> { 
                tableReservations.getItems().clear(); 
                Stage stage = (Stage) tableReservations.getScene().getWindow(); 
                stage.setTitle("Bistro - view & cancel"); 
            }); 

            ArrayList<Object> message = new ArrayList<>(); 
            message.add("GET_ACTIVE_RESERVATIONS"); 
            message.add(userId); 
            
            client.handleMessageFromClientUI(message); 
        } 
    } 

    /**
     * Initializes the table columns, applies visual styling, and sets up dynamic height binding.
     * @return None.
     */
    @FXML 
    public void initialize() { 
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode")); 
        colDate.setCellValueFactory(new PropertyValueFactory<>("reservationDateTime")); 
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests")); 

        tableReservations.setFixedCellSize(75.0); 
        
        tableReservations.setStyle( 
            "-fx-background-radius: 20; " + 
            "-fx-border-radius: 20; " + 
            "-fx-border-color: #444444; " + 
            "-fx-border-width: 2;" 
        ); 
        
        tableReservations.prefHeightProperty().bind( 
            tableReservations.fixedCellSizeProperty().multiply(Bindings.size(tableReservations.getItems()).add(1.1)) 
        ); 

        String cellStyle = "-fx-alignment: CENTER; -fx-text-fill: black; -fx-font-size: 16px; -fx-font-weight: bold;"; 
        colCode.setStyle(cellStyle); 
        colDate.setStyle(cellStyle); 
        colGuests.setStyle(cellStyle); 
        
        Label placeholderLabel = new Label("NO ACTIVE RESERVATIONS"); 
        placeholderLabel.setStyle( 
            "-fx-text-fill: #3498db; " + 
            "-fx-font-size: 28px; " + 
            "-fx-font-weight: bold; " + 
            "-fx-font-family: 'Segoe UI'; " + 
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);" 
        ); 

        tableReservations.setPlaceholder(placeholderLabel); 
        setupCancelButton(); 
    } 

    /**
     * Configures the Action column to display a stylized "Cancel" button for each reservation.
     * @return None.
     */
    private void setupCancelButton() { 
        colAction.setStyle("-fx-alignment: CENTER;"); 
        colAction.setCellFactory(param -> new TableCell<>() { 
            
            private final Button btnCancel = new Button("Cancel"); 
            
            { 
                btnCancel.setStyle( 
                    "-fx-background-color: #e74c3c; " + 
                    "-fx-text-fill: white; " + 
                    "-fx-background-radius: 15; " + 
                    "-fx-font-weight: bold; " + 
                    "-fx-cursor: hand;" 
                ); 
                
                btnCancel.setPrefHeight(35); 
                btnCancel.setPrefWidth(100); 

                btnCancel.setOnAction(event -> { 
                    Reservation res = getTableView().getItems().get(getIndex()); 
                    handleCancelAction(res); 
                }); 
            } 

            @Override 
            protected void updateItem(Void item, boolean empty) { 
                super.updateItem(item, empty); 
                setGraphic(empty ? null : btnCancel); 
            } 
        }); 
    } 

    /**
     * Displays a confirmation alert and sends a cancellation request if confirmed.
     * @param res The Reservation object to cancel.
     * @return None.
     */
    private void handleCancelAction(Reservation res) { 
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION); 
        confirmAlert.setTitle("Cancel Reservation"); 
        confirmAlert.setHeaderText("Are you sure you want to cancel?"); 

        confirmAlert.showAndWait().ifPresent(response -> { 
            if (response == ButtonType.OK) { 
                ArrayList<Object> message = new ArrayList<>(); 
                message.add("CANCEL_RESERVATION"); 
                message.add(res.getConfirmationCode()); 
                client.handleMessageFromClientUI(message); 
            } 
        }); 
    } 

    /**
     * Processes server responses (refreshing the table or showing success messages).
     * @param message The server response (List of reservations or String signal).
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        if (message instanceof List) { 
            Platform.runLater(() -> { 
                tableReservations.setItems(FXCollections.observableArrayList((List<Reservation>) message)); 
                tableReservations.refresh(); 
            }); 
        } 
        else if (message instanceof String && ((String) message).equals("CANCEL_SUCCESS")) { 
            Platform.runLater(() -> { 
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION); 
                successAlert.setTitle("Success"); 
                successAlert.setHeaderText(null); 
                successAlert.setContentText("The reservation has been successfully canceled."); 
                successAlert.showAndWait(); 
                onClientReady(); 
            }); 
        } 
    } 

    /**
     * Navigates back to the relevant main menu based on the user's role.
     * @param event The ActionEvent from the back button.
     * @return None.
     */
    @FXML 
    void clickBack(ActionEvent event) { 
        String fxmlPath = ""; 
        if (userType != null) { 
            switch (userType) { 
                case "Subscriber": 
                    fxmlPath = "/clientGUI/fxmlFiles/SubscriberFXML/SubscriberMenuFrame.fxml"; 
                    break; 
                default: 
                    fxmlPath = "/clientGUI/fxmlFiles/OccasionalFXML/OccasionalMenuFrame.fxml"; 
                    break; 
            } 
        } 
        navigateTo(client, event, userType, userId, fxmlPath, "Bistro - Main Menu"); 
    } 

    // Interface requirement implementations (Stubs)
    /**
     * Interface stub for viewing order history.
     * @param client The network client.
     * @param userId The unique user identifier.
     * @return None.
     */
    @Override public void viewOrderHistory(ChatClient client, int userId) {} 

    /**
     * Interface stub for editing personal details.
     * @param client The network client.
     * @param userId The unique user identifier.
     * @return None.
     */
    @Override public void editPersonalDetails(ChatClient client, int userId) {} 
}