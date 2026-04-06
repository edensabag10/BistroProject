package clientGUI.Controllers; // Define the package location for the controller interfaces

import client.ChatClient; // Import the main communication client class
import clientGUI.Controllers.MenuControlls.BaseMenuController; // Import the base controller for dependency injection
import javafx.event.ActionEvent; // Import ActionEvent to handle UI button clicks
import javafx.fxml.FXMLLoader; // Import FXMLLoader to load FXML layout files
import javafx.scene.Node; // Import Node to access UI elements within the scene graph
import javafx.scene.Parent; // Import Parent as the root for the scene
import javafx.scene.Scene; // Import Scene for window content management
import javafx.stage.Stage; // Import Stage for primary window management

/**
 * Behavioral contract for customer-related activities in the Bistro system.
 * Provides a centralized navigation engine for switching between different customer screens.
 */
public interface ICustomerActions { 

    /**
     * Navigates the user to the New Reservation screen.
     * @param client   The network client for server communication.
     * @param event    The ActionEvent that triggered the navigation.
     * @param userType The type/role of the logged-in user.
     * @param userId   The unique identifier of the user.
     */
    default void createNewReservation(ChatClient client, ActionEvent event, String userType, int userId) { 
        navigateTo(client, event, userType, userId, "/clientGUI/fxmlFiles/MenuFXML/NewReservationFrame.fxml", "Bistro - New Reservation"); 
    } 

    /**
     * Navigates the user to the Payment Code entry screen.
     * @param client   The network client for server communication.
     * @param event    The ActionEvent that triggered the navigation.
     * @param userType The type/role of the logged-in user.
     * @param userId   The unique identifier of the user.
     */
    default void payBill(ChatClient client, ActionEvent event, String userType, int userId) { 
        navigateTo(client, event, userType, userId, "/clientGUI/fxmlFiles/MenuFXML/PayBillEntryFrame.fxml", "Bistro - Enter Payment Code"); 
    } 

    /**
     * Navigates the user to the screen where they can view existing reservations.
     * @param client   The network client for server communication.
     * @param event    The ActionEvent that triggered the navigation.
     * @param userType The type/role of the logged-in user.
     * @param userId   The unique identifier of the user.
     */
    default void viewReservation(ChatClient client, ActionEvent event, String userType, int userId) { 
        navigateTo(client, event, userType, userId, "/clientGUI/fxmlFiles/MenuFXML/ViewReservationFrame.fxml", "Bistro - View & Pay"); 
    } 
    
    /**
     * Abstract method for retrieving reservation history.
     * @param client The network client for server communication.
     * @param userId The unique identifier of the user.
     */
    void viewOrderHistory(ChatClient client, int userId); 

    /**
     * Abstract method for editing personal profile details.
     * @param client The network client for server communication.
     * @param userId The unique identifier of the user.
     */
    void editPersonalDetails(ChatClient client, int userId); 
    
    /**
     * Sends a command to exit the digital waiting list.
     * @param client           The network client for server communication.
     * @param confirmationCode The reservation code to be removed from the list.
     */
    default void exitWaitingList(ChatClient client, String confirmationCode) { 
    } 

    /**
     * The Central Navigation Engine: Handles FXML loading and Controller session injection.
     * @param client   The network client for server communication.
     * @param event    The ActionEvent that initiated navigation.
     * @param userType The role of the user to be propagated.
     * @param userId   The user ID to be propagated.
     * @param path     The FXML resource path to load.
     * @param title    The title of the new window stage.
     */
    default void navigateTo(ChatClient client, ActionEvent event, String userType, int userId, String path, String title) { 
        
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path)); 
            Parent root = loader.load(); 
            Object controller = loader.getController(); 
            
            if (controller instanceof BaseMenuController) { 
                ((BaseMenuController) controller).setClient(client, userType, userId); 
            } 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            Scene scene = new Scene(root); 
            scene.getStylesheets().add(getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()); 
            
            stage.setTitle(title); 
            stage.setScene(scene); 
            stage.show(); 
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    } 
}