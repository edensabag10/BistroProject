package clientGUI.Controllers;

import client.ChatClient;
import clientGUI.Controllers.MenuControlls.BaseMenuController;
import clientGUI.Controllers.OccasionalControlls.OccasionalLoginController;
import clientGUI.Controllers.SubscriberControlls.SubscriberLoginController;
import common.LoginSource;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.application.Platform;

/**
 * Controller for the integrated Remote Login screen.
 * Manages the initial navigation to different login flows (Subscriber or Guest)
 * and handles the injection of session data and login sources.
 */
public class RemoteLoginController extends BaseMenuController { 

    /** TextArea component for displaying connection logs and server messages. */
    @FXML private TextArea txtLog;

    /**
     * Handles the action when the Occasional (Guest) login button is clicked.
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML 
    void clickOccasional(ActionEvent event) { 
        loadScreen(event, "OccasionalFXML/OccasionalLoginFrame.fxml", "Occasional Login"); 
    }

    /**
     * Handles the action when the Subscriber login button is clicked.
     * @param event The ActionEvent triggered by the button click.
     */
    @FXML 
    void clickSubscriber(ActionEvent event) { 
        loadScreen(event, "SubscriberFXML/SubscriberLoginFrame.fxml", "Subscriber Login"); 
    }

    /**
     * Central engine for screen navigation and dependency injection.
     * Loads the FXML, updates the stage, and propagates session data.
     * * @param event    The ActionEvent that initiated the navigation.
     * @param fxmlFile The path to the FXML resource to be loaded.
     * @param title    The title to be set on the new window stage.
     */
    private void loadScreen(ActionEvent event, String fxmlFile, String title) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/" + fxmlFile)); 
            Parent root = loader.load(); 
            Object controller = loader.getController(); 
            
            // Phase A: Inject session data via BaseMenuController hierarchy
            if (controller instanceof BaseMenuController) { 
                ((BaseMenuController) controller).setClient(client, userType, userId); 
            } 

            // Phase B: Inject LoginSource for navigation context
            if (controller instanceof SubscriberLoginController) { 
                ((SubscriberLoginController) controller).setLoginSource(LoginSource.REMOTE); 
            } 
            else if (controller instanceof OccasionalLoginController) { 
                ((OccasionalLoginController) controller).setLoginSource(LoginSource.REMOTE); 
            } 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            Scene scene = new Scene(root); 
            
            if (getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css") != null) { 
                scene.getStylesheets().add(getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()); 
            } 
            
            stage.setTitle(title); 
            stage.setScene(scene); 
            stage.show(); 
            
        } catch (Exception e) { 
            e.printStackTrace(); 
            appendLog("Error loading screen: " + e.getMessage()); 
        } 
    } 

    /**
     * Receives and processes asynchronous messages from the server.
     * @param message The message object received from the ChatClient.
     */
    @Override 
    public void display(Object message) { 
        if (message != null) { 
            appendLog(message.toString()); 
        } 
    } 

    /**
     * Appends a message to the on-screen log area in a thread-safe manner.
     * Uses Platform.runLater to ensure updates occur on the JavaFX Application Thread.
     * * @param message The text message to be added to the log.
     */
    public void appendLog(String message) { 
        Platform.runLater(() -> { 
            if (txtLog != null) { 
                txtLog.appendText("> " + message + "\n"); 
            } 
        }); 
    } 
}