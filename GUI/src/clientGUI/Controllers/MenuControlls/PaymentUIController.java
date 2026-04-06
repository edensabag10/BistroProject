package clientGUI.Controllers.MenuControlls; // Define the package for menu-related controllers

import javafx.event.ActionEvent; // Import for handling UI action events
import javafx.fxml.FXML; // Import for FXML injection annotation
import javafx.scene.control.Alert; // Import for alert dialog boxes
import javafx.scene.control.Alert.AlertType; // Import for defining alert styles
import javafx.scene.control.Button; // Import for button components
import javafx.scene.control.Label; // Import for text label components
import java.util.Random; // Import for generating random numbers
import java.util.ArrayList; // Import for dynamic array list structures
import client.ChatClient; // Import the main client communication class
import clientGUI.Controllers.ICustomerActions; // Import customer action interface
import common.Bill; // Import the Bill data transfer object
import common.Visit; // Import the Visit entity class

/**
 * Controller for the Payment UI screen in the Bistro system.
 * This class manages the final billing phase, including dynamic price generation, 
 * membership discount application, and secure transmission of payment data to the server.
 */
public class PaymentUIController extends BaseMenuController implements ICustomerActions { 

    @FXML private Label lblTableId; 
    @FXML private Label lblStartTime; 
    @FXML private Label lblBaseAmount; 
    @FXML private Label lblDiscount; 
    @FXML private Label lblFinalAmount; 
    @FXML private Button btnConfirmPay; 

    /** Cached ID for the bill record in the database. */
    private long currentBillId; 
    
    /** Cached confirmation code linking the payment to a specific reservation. */
    private long currentConfirmationCode; 

    /**
     * Initializes the payment dashboard with visit-specific data and performs price calculations.
     * Generates a random base amount and applies a 10% discount for registered subscribers.
     * @param client       The active network client instance.
     * @param userType     The role/type of the user.
     * @param userId       The unique identification of the user.
     * @param visit        The Visit object containing table and timing information.
     * @param isSubscriber Flag indicating if the user is eligible for a member discount.
     * @return None.
     */
    public void setupPayment(ChatClient client, String userType, int userId, Visit visit, boolean isSubscriber) { 
        
        this.client = client; 
        this.userType = userType; 
        this.userId = userId; 
        
        this.currentBillId = visit.getBillId(); 
        this.currentConfirmationCode = visit.getConfirmationCode(); 
        
        lblTableId.setText(String.valueOf(visit.getTableId())); 
        lblStartTime.setText(visit.getStartTime()); 

        Random random = new Random(); 
        double randomBaseAmount = 150 + (300 * random.nextDouble()); 
        lblBaseAmount.setText(String.format("%.2f ₪", randomBaseAmount)); 

        double discountPercent = 0.0; 
        
        if (isSubscriber) { 
            discountPercent = 10.0; 
            lblDiscount.setText("10% (Member)"); 
            lblDiscount.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); 
        } else { 
            discountPercent = 0.0; 
            lblDiscount.setText("0%"); 
            lblDiscount.setStyle("-fx-text-fill: red;"); 
        } 

        double finalTotal = randomBaseAmount * (1 - (discountPercent / 100)); 
        lblFinalAmount.setText(String.format("%.2f ₪", finalTotal)); 
        
    } 

    /**
     * Processes the payment transaction when the confirm button is clicked.
     * Parses the calculated values into a Bill DTO and transmits it to the server.
     * @param event The ActionEvent triggered by the payment button.
     * @return None.
     */
    @FXML 
    private void onPayClicked(ActionEvent event) { 
        
        try { 
            double base = Double.parseDouble(lblBaseAmount.getText().replace(" ₪", "")); 
            double discount = Double.parseDouble(lblDiscount.getText().replaceAll("[^0-9.]", "")); 
            double finalPrice = Double.parseDouble(lblFinalAmount.getText().replace(" ₪", "")); 

            Bill bill = new Bill(currentBillId, currentConfirmationCode, base, discount, finalPrice); 

            ArrayList<Object> message = new ArrayList<>(); 
            message.add("PROCESS_PAYMENT"); 
            message.add(bill); 
            client.handleMessageFromClientUI(message); 

            Alert alert = new Alert(AlertType.INFORMATION); 
            alert.setTitle("Payment Confirmation"); 
            alert.setHeaderText("Transaction Successful!"); 
            alert.setContentText("Payment processed and Table " + lblTableId.getText() + " is now available.\nReturning to Main Menu..."); 
            alert.showAndWait(); 

            returnToMainMenu(event); 

        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
        
    } 

    /**
     * Helper method to determine the correct navigation path and return the user to the main menu.
     * @param event The ActionEvent used to identify the current window stage.
     * @return None.
     */
    private void returnToMainMenu(ActionEvent event) {
        String path = "";
        if (userType != null) {
            switch (userType) {
                case "Terminal":
                    path = "/clientGUI/fxmlFiles/Terminal/ManageReservationFrame.fxml";
                    break;
                case "Subscriber":
                    path = "/clientGUI/fxmlFiles/SubscriberFXML/SubscriberMenuFrame.fxml";
                    break;
                default:
                    path = "/clientGUI/fxmlFiles/OccasionalFXML/OccasionalMenuFrame.fxml";
                    break;
            }
        }
        navigateTo(client, event, userType, userId, path, "Bistro - Main Menu");
    }

    /**
     * Implementation stub for viewing order history.
     * @param client The network client.
     * @param userId The unique user identifier.
     * @return None.
     */
    @Override public void viewOrderHistory(ChatClient client, int userId) {} 
    
    /**
     * Implementation stub for editing personal details.
     * @param client The network client.
     * @param userId The unique user identifier.
     * @return None.
     */
    @Override public void editPersonalDetails(ChatClient client, int userId) {} 
    
}