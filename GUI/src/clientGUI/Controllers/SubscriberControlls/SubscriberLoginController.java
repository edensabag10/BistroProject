package clientGUI.Controllers.SubscriberControlls; // Defining the package for subscriber-related controllers

import java.util.ArrayList; // Importing ArrayList for dynamic message lists
import client.ChatClient; // Importing the main client communication class
import clientGUI.Controllers.MenuControlls.BaseMenuController; // Importing the parent controller for inheritance
import common.LoginSource;
import clientGUI.Controllers.RemoteLoginController; // Importing reference to the portal login controller
import javafx.event.ActionEvent; // Importing ActionEvent for UI interaction handling
import javafx.fxml.FXML; // Importing FXML annotation for UI element injection
import javafx.fxml.FXMLLoader; // Importing FXMLLoader for loading layout files
import javafx.scene.Parent; // Importing Parent for the scene graph root
import javafx.scene.Scene; // Importing Scene for window content management
import javafx.scene.Node; // Importing Node for identifying UI elements in events
import javafx.scene.control.Button; // Importing Button component
import javafx.scene.control.TextArea; // Importing TextArea component
import javafx.scene.control.TextField; // Importing TextField component
import javafx.stage.Stage; // Importing Stage for window management
import terminalGUI.Controllers.TerminalControllers.TerminalLoginController; // New: Importing Terminal login controller
import terminalGUI.Controllers.TerminalControllers.TerminalMenuController; // New: Importing Terminal menu controller
import javafx.application.Platform; // Importing Platform for thread-safe UI updates

/**
 * Integrated controller for the Subscriber Login screen.
 * This class handles authentication requests and orchestrates navigation to various 
 * dashboards based on the user's role and the login source (Terminal vs. Remote).
 */
public class SubscriberLoginController extends BaseMenuController { 

    // --- FXML UI Components ---
    @FXML private TextField txtSubscriberID; 
    @FXML private Button btnLogin; 
    @FXML private TextArea txtLog; 

    // --- Eden's Logic Integration ---
    private LoginSource loginSource = LoginSource.REMOTE; 

    /**
     * Sets the origin of the login request to distinguish between Terminal and Remote flows.
     * @param source The login source (REMOTE or TERMINAL).
     * @return None.
     */
    public void setLoginSource(LoginSource source) { 
        this.loginSource = source; 
    } 

    /**
     * Hook method triggered when the client connection is established.
     * Logs the current session status and identity.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        appendLog("Connected to Portal. Waiting for login..."); 
        appendLog("Identity: " + userType + " | Source: " + loginSource); 
    } 

    /**
     * Processes the login attempt by sending the subscriber ID to the server.
     * @param event The ActionEvent triggered by the login button.
     * @return None.
     */
    @FXML 
    void clickLogin(ActionEvent event) { 
        String subID = txtSubscriberID.getText(); 
        
        if (subID.isEmpty()) { 
            appendLog("Error: Please enter a Subscriber ID."); 
            return; 
        } 

        if (client != null) { 
            appendLog("Attempting login for ID: " + subID); 
            ArrayList<String> msg = new ArrayList<>(); 
            msg.add("LOGIN_SUBSCRIBER"); 
            msg.add(subID); 
            client.handleMessageFromClientUI(msg); 
        } else { 
            appendLog("Fatal Error: No server connection!"); 
        } 
    } 

    /**
     * Navigates back to the appropriate previous screen based on the login source.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML 
    void clickBack(ActionEvent event) { 
        try { 
            FXMLLoader loader; 
            Parent root; 

            if (loginSource == LoginSource.TERMINAL) { 
                loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/TerminalLoginFrame.fxml")); 
                root = loader.load(); 
                
                Object controller = loader.getController(); 
                if (controller instanceof TerminalLoginController) { 
                    ((TerminalLoginController) controller).setClient(client); 
                } 
            } else { 
                loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/RemoteLoginFrame.fxml")); 
                root = loader.load(); 
                
                Object controller = loader.getController(); 
                if (controller instanceof BaseMenuController) { 
                    ((BaseMenuController) controller).setClient(client, userType, userId); 
                } 
            } 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            stage.setScene(new Scene(root)); 
            stage.show(); 
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    } 

    /**
     * Handles server responses and routes the user to the correct dashboard on the UI thread.
     * @param message The server response object.
     * @return None.
     */
    @Override 
    @SuppressWarnings("unchecked") 
    public void display(Object message) { 
        if (message instanceof ArrayList) { 
            ArrayList<Object> res = (ArrayList<Object>) message; 
            String status = res.get(0).toString(); 

            if (status.equals("LOGIN_SUCCESS")) { 
                appendLog("Login confirmed! Loading dashboard..."); 
                int userIdFromDB = (int) res.get(1); 
                String userStatusFromDB = (String)res.get(2);
                
                Platform.runLater(() -> { 
                    if (loginSource == LoginSource.TERMINAL) { 
                        navigateToTerminal(userIdFromDB); 
                    } else {  
                        if ("subscriber".equalsIgnoreCase(userStatusFromDB)) {
                            navigateToMenu(userIdFromDB);
                        } else if ("manager".equalsIgnoreCase(userStatusFromDB)) {
                            navigateToManagerMenu(userIdFromDB);
                        } else if ("representative".equalsIgnoreCase(userStatusFromDB)) {
                            navigateToRepresentativeMenu(userIdFromDB);
                        }
                    } 
                }); 
            } else { 
                appendLog("Server Response: " + res.toString()); 
            } 
        } else if (message != null) { 
            appendLog(message.toString()); 
        } 
    } 

    /**
     * Navigates the user to the standard Subscriber Menu.
     * @param userIdFromDB The unique database ID of the subscriber.
     * @return None.
     */
    private void navigateToMenu(int userIdFromDB) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/SubscriberFXML/SubscriberMenuFrame.fxml")); 
            Parent root = loader.load(); 
            
            if (loader.getController() instanceof BaseMenuController) { 
                ((BaseMenuController) loader.getController()).setClient(client, "Subscriber", userIdFromDB); 
            } 
            
            updateStage(root, "Subscriber Dashboard"); 
        } catch (Exception e) { 
            e.printStackTrace(); 
            appendLog("UI Error: Could not load Menu."); 
        } 
    } 

    /**
     * Navigates the user to the Physical Terminal Menu.
     * @param userIdFromDB The unique database ID of the subscriber.
     * @return None.
     */
    private void navigateToTerminal(int userIdFromDB) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/TerminalMenuFrame.fxml"));
            Parent root = loader.load();
            TerminalMenuController controller = loader.getController();
            
            controller.setClient(client, "Subscriber", userIdFromDB);

            updateStage(root, "Customer Service Terminal");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Navigates the user to the Manager dashboard.
     * @param userIdFromDB The unique database ID of the manager.
     * @return None.
     */
    private void navigateToManagerMenu(int userIdFromDB) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/managmentGUI/ManagerDashboard.fxml"));
            Parent root = loader.load();

            if (loader.getController() instanceof BaseMenuController) {
                ((BaseMenuController) loader.getController()).setClient(client, "Manager", userIdFromDB);
            }

            updateStage(root, "Manager Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("UI Error: Could not load Manager Menu.");
        }
    }
    
    /**
     * Navigates the user to the Representative dashboard.
     * @param userIdFromDB The unique database ID of the representative.
     * @return None.
     */
    private void navigateToRepresentativeMenu(int userIdFromDB) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/managmentGUI/RepresentativeDashboard.fxml"));
            Parent root = loader.load();

            if (loader.getController() instanceof BaseMenuController) {
                ((BaseMenuController) loader.getController()).setClient(client, "Representative", userIdFromDB);
            }

            updateStage(root, "Representative Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("UI Error: Could not load Representative Menu.");
        }
    }

    /**
     * Updates the primary stage with a new scene and sets the window title.
     * @param root  The root node of the new scene.
     * @param title The title for the stage.
     * @return None.
     */
    private void updateStage(Parent root, String title) {
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        Scene scene = new Scene(root);

        if (!title.contains("Manager") && !title.contains("Representative")) {
            if (getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css") != null) {
                scene.getStylesheets().add(
                    getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()
                );
            }
        }

        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Appends a status message to the UI log area in a thread-safe manner.
     * @param message The text message to display.
     * @return None.
     */
    public void appendLog(String message) { 
        Platform.runLater(() -> { 
            if (txtLog != null) { 
                txtLog.appendText("> " + message + "\n"); 
            } 
        }); 
    } 
}