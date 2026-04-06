package clientGUI.Controllers.OccasionalControlls; // Defining the package for occasional user controllers

import java.util.ArrayList; // Importing ArrayList for data list management
import client.ChatClient; // Importing the ChatClient for communication
import clientGUI.Controllers.MenuControlls.BaseMenuController; // Importing the base controller for inheritance
import common.LoginSource;
import clientGUI.Controllers.RemoteLoginController; // Importing the remote login controller reference
import javafx.application.Platform; // Importing Platform for UI thread safety
import javafx.event.ActionEvent; // Importing ActionEvent for UI interaction
import javafx.fxml.FXML; // Importing FXML annotation for UI injection
import javafx.fxml.FXMLLoader; // Importing FXMLLoader to load new scenes
import javafx.scene.Node; // Importing Node for generic UI elements
import javafx.scene.Parent; // Importing Parent for the scene graph root
import javafx.scene.Scene; // Importing Scene for window content
import javafx.scene.control.Button; // Importing Button control
import javafx.scene.control.TextArea; // Importing TextArea control
import javafx.scene.control.TextField; // Importing TextField control
import javafx.scene.layout.VBox; // Importing VBox layout container
import javafx.stage.Stage; // Importing Stage for window management
import terminalGUI.Controllers.TerminalControllers.TerminalLoginController; // New: Importing Terminal login controller
import terminalGUI.Controllers.TerminalControllers.TerminalMenuController; // New: Importing Terminal menu controller

/**
 * Integrated controller for the Occasional (Guest) Login screen.
 * This class handles guest authentication, username recovery/reset, and role-based 
 * navigation within the Bistro system.
 * * @author Software Engineering Student
 */
public class OccasionalLoginController extends BaseMenuController { 

    // --- FXML Injected Components ---
    @FXML private TextField txtUsername, txtContact, txtForgotContact, txtNewUsername; 
    @FXML private TextArea txtLog; 
    @FXML private VBox paneLogin, paneForgot; 
    @FXML private Button btnLogin; 

    // --- Eden's Logic Integration ---
    private LoginSource loginSource = LoginSource.REMOTE; 

    /**
     * Sets the origin of the login request to ensure correct navigation flow.
     * @param source The origin of the login (REMOTE or TERMINAL).
     * @return None.
     */
    public void setLoginSource(LoginSource source) { 
        this.loginSource = source; 
    } 

    /**
     * Hook method triggered when the network client is initialized.
     * Logs connection status and session identity.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        appendLog("Connected to Portal. Waiting for occasional login..."); 
        appendLog("Identity: " + userType + " | Source: " + loginSource); 
    } 

    /**
     * Handles the login action for occasional users by validating input and sending to server.
     * @param event The ActionEvent triggered by the login button.
     * @return None.
     */
    @FXML 
    void clickLogin(ActionEvent event) { 
        String username = txtUsername.getText(); 
        String contact = txtContact.getText(); 

        if (username.isEmpty() || contact.isEmpty()) { 
            appendLog("Error: Both fields are required."); 
            return; 
        } 

        if (client != null) { 
            appendLog("Attempting login for Guest: " + username); 
            ArrayList<String> msg = new ArrayList<>(); 
            msg.add("LOGIN_OCCASIONAL"); 
            msg.add(username); 
            msg.add(contact); 
            client.handleMessageFromClientUI(msg); 
        } 
    } 

    /**
     * Processes incoming server responses and manages transitions to the correct dashboard.
     * @param message The response message object from the server.
     * @return None.
     */
    @Override 
    @SuppressWarnings("unchecked") 
    public void display(Object message) { 
        Platform.runLater(() -> { 
            if (message instanceof ArrayList) { 
                ArrayList<Object> res = (ArrayList<Object>) message; 
                String status = res.get(0).toString(); 

                if (status.equals("LOGIN_OCCASIONAL_SUCCESS")) { 
                    appendLog("Welcome! Navigating to dashboard..."); 
                    int userIdFromDB = (int) res.get(1); 
                    
                    if (loginSource == LoginSource.TERMINAL) { 
                        navigateToTerminal(userIdFromDB); 
                    } else { 
                        navigateToMenu(userIdFromDB); 
                    } 
                } else { 
                    appendLog("Server Response: " + status); 
                } 
            } else if (message != null) { 
                handleStringResponse(message.toString()); 
            } 
        }); 
    } 

    /**
     * Helper method to process simple string messages, such as username reset feedback.
     * @param response The string response received from the server.
     * @return None.
     */
    private void handleStringResponse(String response) { 
        if (response.equals("RESET_USERNAME_SUCCESS")) { 
            appendLog("SUCCESS: Your username has been updated!"); 
            txtForgotContact.clear(); 
            txtNewUsername.clear(); 
        } else { 
            appendLog("Server Message: " + response); 
        } 
    } 

    /**
     * Navigates the guest user to the standard Remote Menu.
     * @param userIdFromDB The unique database ID assigned to the guest.
     * @return None.
     */
    private void navigateToMenu(int userIdFromDB) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/OccasionalFXML/OccasionalMenuFrame.fxml")); 
            Parent root = loader.load(); 
            
            Object nextController = loader.getController(); 
            if (nextController instanceof BaseMenuController) { 
                ((BaseMenuController) nextController).setClient(client, "Occasional", userIdFromDB); 
            } 
            
            updateStage(root, "Guest Dashboard"); 
        } catch (Exception e) { 
            appendLog("Navigation Error: " + e.getMessage()); 
        } 
    } 

    /**
     * Navigates the guest user to the Physical Terminal Menu.
     * @param userId The unique database ID assigned to the guest.
     * @return None.
     */
    private void navigateToTerminal(int userId) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/TerminalMenuFrame.fxml")); 
            Parent root = loader.load(); 

            TerminalMenuController controller = loader.getController(); 
            controller.setClient(client); 
            
            updateStage(root, "Customer Service Terminal"); 
        } catch (Exception e) { 
            appendLog("Error navigating to Terminal menu."); 
        } 
    } 

    /**
     * Handles navigation back to the previous login portal based on the source.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML 
    void clickBack(ActionEvent event) { 
        try { 
            FXMLLoader loader; 
            if (loginSource == LoginSource.TERMINAL) { 
                loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/TerminalLoginFrame.fxml")); 
            } else { 
                loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/RemoteLoginFrame.fxml")); 
            } 
            
            Parent root = loader.load(); 
            Object nextController = loader.getController(); 
            
            if (nextController instanceof BaseMenuController) { 
                ((BaseMenuController) nextController).setClient(client, userType, userId); 
            } else if (nextController instanceof TerminalLoginController) { 
                ((TerminalLoginController) nextController).setClient(client); 
            } 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            stage.setScene(new Scene(root)); 
            stage.show(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    } 

    /**
     * Utility method to render a new scene on the primary stage.
     * @param root  The root node of the FXML layout.
     * @param title The title for the stage window.
     * @return None.
     */
    private void updateStage(Parent root, String title) { 
        Stage stage = (Stage) btnLogin.getScene().getWindow(); 
        Scene scene = new Scene(root); 
        if (getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css") != null) { 
            scene.getStylesheets().add(getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()); 
        } 
        stage.setTitle(title); 
        stage.setScene(scene); 
        stage.show(); 
    } 

    /**
     * Navigates the user to the guest registration screen.
     * @param event The ActionEvent triggered by the register button.
     * @return None.
     */
    @FXML void clickRegister(ActionEvent event) {  
        try {
            // טעינת ה-FXML של מסך ההרשמה
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/OccasionalFXML/OccasionalRegistrationFrame.fxml"));
            Parent root = loader.load();

            // הזרקת הנתונים (הצינור - Pipe) לקונטרולר החדש
            Object nextController = loader.getController();
            if (nextController instanceof BaseMenuController) {
                ((BaseMenuController) nextController).setClient(client, userType, userId);
            }

            // עדכון המסך (שימוש במתודה הקיימת אצלך updateStage)
            updateStage(root, "Guest Registration");
            
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("Error loading registration screen: " + e.getMessage());
        }
    } 

    /**
     * Sends a request to the server to reset a guest's username based on contact info.
     * @param event The ActionEvent triggered by the submit button in the forgot area.
     * @return None.
     */
    @FXML 
    void clickSubmitForgot(ActionEvent event) {
        // 1. שליפת הנתונים מהשדות שהמשתמש מילא ב-paneForgot
        String contact = txtForgotContact.getText().trim();
        String newName = txtNewUsername.getText().trim();

        // 2. בדיקה שהשדות לא ריקים לפני ששולחים לשרת
        if (contact.isEmpty() || newName.isEmpty()) {
            appendLog("Error: Please fill in both contact info and new username.");
            return;
        }

        // 3. בניית ההודעה לשרת לפי הפורמט שהגדרת ב-Switch Case בשרת
        if (client != null) {
            appendLog("Sending reset request for contact: " + contact);
            
            ArrayList<String> message = new ArrayList<>();
            message.add("RESET_OCCASIONAL_USERNAME"); // הפקודה שהשרת שלך מצפה לה
            message.add(contact);  // נתון ראשון (זיהוי)
            message.add(newName);  // נתון שני (שם חדש)
            
            // שליחה לשרת
            client.handleMessageFromClientUI(message);
        } else {
            appendLog("Connection error: Client is null.");
        }
    }

    /**
     * Toggles the UI to display the username recovery area.
     * @param event The ActionEvent triggered by the forgot username link.
     * @return None.
     */
    @FXML void showForgotArea(ActionEvent event) { paneLogin.setVisible(false); paneLogin.setManaged(false); paneForgot.setVisible(true); paneForgot.setManaged(true); } 

    /**
     * Toggles the UI to hide the username recovery area and return to login.
     * @param event The ActionEvent triggered by the cancel/back button in the forgot area.
     * @return None.
     */
    @FXML void hideForgotArea(ActionEvent event) { paneForgot.setVisible(false); paneForgot.setManaged(false); paneLogin.setVisible(true); paneLogin.setManaged(true); } 

    /**
     * Appends a message to the UI log in a thread-safe manner.
     * @param message The text message to append.
     * @return None.
     */
    public void appendLog(String message) { 
        Platform.runLater(() -> { if (txtLog != null) txtLog.appendText("> " + message + "\n"); }); 
    } 
}