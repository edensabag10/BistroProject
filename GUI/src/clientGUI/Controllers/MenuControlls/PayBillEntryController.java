package clientGUI.Controllers.MenuControlls; // Define the package for menu-related controllers

import java.util.ArrayList; // Import for using dynamic list structures
import clientGUI.Controllers.ICustomerActions; // Import the interface for customer-specific actions
import common.Visit; // Import the Visit entity class
import javafx.application.Platform; // Import for executing code on the JavaFX application thread
import javafx.event.ActionEvent; // Import for handling UI action events
import javafx.fxml.FXML; // Import for FXML field injection
import javafx.fxml.FXMLLoader; // Import for loading FXML layout files
import javafx.scene.Parent; // Import for representing the root of the scene graph
import javafx.scene.Scene; // Import for managing the stage scene
import javafx.scene.control.Alert; // Import for showing alert dialogs
import javafx.scene.control.Alert.AlertType; // Import for defining alert categories
import javafx.scene.control.Button; // Import for button components
import javafx.scene.control.TextField; // Import for text input fields
import javafx.stage.Stage; // Import for the primary window container

/**
 * Controller for the initial payment entry screen in the Bistro system.
 * This class handles the validation and verification of the confirmation code 
 * provided by the customer to initiate the billing process.
 */
public class PayBillEntryController extends BaseMenuController implements ICustomerActions { // Class definition start

	// FXML injected UI components
	@FXML
	private TextField txtCode; // Input field for the reservation confirmation code
	@FXML
	private Button btnBack; // Button to return to the previous screen
	@FXML
	private Button btnVerify; // Button to initiate the code verification process

	/**
	 * Lifecycle hook called when the client connection is ready.
	 * Registers this controller as the active UI listener for server messages.
	 * @return None.
	 */
	@Override // Overriding method from BaseMenuController
	public void onClientReady() { // Start of onClientReady method
		// Check if the client instance is not null before setting the UI listener
		if (client != null) { // Null check for client
			client.setUI(this); // Register this controller to receive server messages
		} // End of null check
	} // End of onClientReady method

	/**
	 * Handles the "Verify" button click event.
	 * Validates numeric input and sends a request to the server to fetch visit data.
	 * @param event The ActionEvent triggered by the verification button.
	 * @return None.
	 */
	@FXML // Link method to FXML action
	void onVerifyClicked(ActionEvent event) { // Start of verify click handler

		// Extract the code from the text field and remove leading/trailing spaces
		String codeStr = txtCode.getText().trim(); // Get and trim input

		// Validation: Check if the input is empty
		if (codeStr.isEmpty()) { // Start of empty check
			// Display a warning message to the user
			showAlert("Error", "Please enter a confirmation code.", AlertType.WARNING); // Show warning
			return; // Exit method
		} // End of empty check

		try { // Start of numeric parsing block

			// Convert the string input into a long value
			long code = Long.parseLong(codeStr); // Parse to long

			// Prepare a message container to send to the server
			ArrayList<Object> message = new ArrayList<>(); // Initialize message list

			// Add the command and the code payload to the list
			message.add("GET_VISIT_BY_CODE"); // Add command header
			message.add(code); // Add the parsed code

			// Transmit the message list to the server
			client.handleMessageFromClientUI(message); // Send to server

		} catch (NumberFormatException e) { // Catch block for non-numeric input
			// Display an error popup if parsing fails
			showAlert("Error", "Code must be a number.", AlertType.ERROR); // Show error popup
		} // End of catch block

	} // End of onVerifyClicked method

	/**
	 * Processes responses from the server regarding visit verification.
	 * If successful, transitions the UI to the detailed payment screen.
	 * @param message The response object from the server (ArrayList or Error String).
	 * @return None.
	 */
	@Override
	public void display(Object message) {
		if (message instanceof ArrayList) {
			ArrayList<Object> data = (ArrayList<Object>) message;
			Visit visit = (Visit) data.get(0);
			boolean isSubscriberFromDB = (Boolean) data.get(1); 

			Platform.runLater(() -> {
				try {
					FXMLLoader loader = new FXMLLoader(
							getClass().getResource("/clientGUI/fxmlFiles/MenuFXML/PayBillFrame.fxml"));
					Parent root = loader.load();
					PaymentUIController paymentController = loader.getController();

					paymentController.setupPayment(client, userType, userId, visit, isSubscriberFromDB);

					Stage stage = (Stage) btnVerify.getScene().getWindow();
					stage.setScene(new Scene(root));
					stage.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} else if (message instanceof String && message.equals("VISIT_NOT_FOUND")) {
			Platform.runLater(() -> showAlert("Not Found", "No active visit found.", AlertType.ERROR));
		}
	}

	/**
	 * Determines the correct destination and navigates back to the main menu based on the user's role.
	 * @return None.
	 */
	private void returnToMainMenu() { // Start of returnToMainMenu method

		// Variable to store the resulting FXML file path
		String path = ""; // Initialize empty path

		// Refactored: Using switch-case for role-based navigation logic
		if (userType != null) { // Guard check for null userType

			switch (userType) { // Start of switch block
			case "Terminal":
				path = "/clientGUI/fxmlFiles/Terminal/ManageReservationFrame.fxml";
				break;

			case "Subscriber": // Handle registered subscribers
				path = "/clientGUI/fxmlFiles/SubscriberFXML/SubscriberMenuFrame.fxml"; // Set subscriber path
				break; // Exit switch

			default: // Handle Occasional users or any other types
				path = "/clientGUI/fxmlFiles/OccasionalFXML/OccasionalMenuFrame.fxml"; // Set occasional path
				break; // Exit switch

			} // End of switch block

		} // End of null guard

		// Execute navigation using the base class helper method
		navigateTo(client, new ActionEvent(btnBack, null), userType, userId, path, "Bistro - Main Menu"); // Trigger
																											// navigation

	} // End of returnToMainMenu method

	/**
	 * Handles the "Back" button click event.
	 * @param event The ActionEvent triggered by the back button.
	 * @return None.
	 */
	@FXML // Link to FXML action
	void onBackClicked(ActionEvent event) { // Start of back click handler
		returnToMainMenu(); // Call the shared navigation logic
	} // End of onBackClicked method

	/**
	 * Internal helper utility for displaying standardized JavaFX alert dialogs.
	 * @param title   The title of the alert window.
	 * @param content The main body text of the alert.
	 * @param type    The AlertType (Information, Warning, Error).
	 * @return None.
	 */
	private void showAlert(String title, String content, AlertType type) { // Start of showAlert method
		Alert alert = new Alert(type); // Instantiate a new alert with specified type
		alert.setTitle(title); // Set window title
		alert.setHeaderText(null); // Remove header for clean look
		alert.setContentText(content); // Set body text
		alert.showAndWait(); // Display and block execution until closed
	} // End of showAlert method

	/**
	 * Interface stub for viewing order history.
	 * @param client The network client.
	 * @param userId The unique user identifier.
	 * @return None.
	 */
	@Override
	public void viewOrderHistory(client.ChatClient client, int userId) {
	} // Empty implementation stub

	/**
	 * Interface stub for editing personal details.
	 * @param client The network client.
	 * @param userId The unique user identifier.
	 * @return None.
	 */
	@Override
	public void editPersonalDetails(client.ChatClient client, int userId) {
	} // Empty implementation stub

} // End of PayBillEntryController class