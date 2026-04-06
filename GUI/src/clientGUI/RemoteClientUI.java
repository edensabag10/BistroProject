package clientGUI; // Defining the package location for the client-side UI launcher

import client.ChatClient; // Importing the ChatClient class for network communication
import clientGUI.Controllers.RemoteLoginController; // Importing the controller for the landing screen
import javafx.application.Application; // Importing the core JavaFX Application class
import javafx.fxml.FXMLLoader; // Importing the loader to process FXML layout files
import javafx.scene.Parent; // Importing the root node class for the scene graph
import javafx.scene.Scene; // Importing the container for all visual content in a stage
import javafx.stage.Stage; // Importing the primary window container class

/**
 * Main entry point for the Bistro Remote Access client application.
 * This class initializes the JavaFX environment and establishes the OCSF network connection 
 * to the restaurant server.
 */
public class RemoteClientUI extends Application { 
    
    /** Persistent network client instance for server communication. */
    private ChatClient client; 

    /**
     * The standard main method which serves as the JVM entry point.
     * * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) { 
        launch(args); 
    } 

    /**
     * The start method initializes the primary UI stage and the network client.
     * * @param primaryStage The primary window (stage) for this application.
     * @throws Exception If FXML loading or server connection fails.
     */
    @Override 
    public void start(Stage primaryStage) throws Exception { 
        
        // --- STEP 1: UI LOADING AND RENDERING ---
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxmlFiles/RemoteLoginFrame.fxml")); 
        Parent root = loader.load(); 
        
        RemoteLoginController controller = loader.getController();

        Scene scene = new Scene(root); 
        
        primaryStage.setTitle("Bistro - Remote Access Portal"); 
        primaryStage.setResizable(false); 
        primaryStage.setScene(scene); 
        
        primaryStage.show(); 

        // --- STEP 2: NETWORK INITIALIZATION AND DATA INJECTION ---
        
        try { 
            
            // Initializing the ChatClient to connect to the server
            client = new ChatClient("localhost", 5555, controller); 
            
            /**
             * The Unified Controller Pipe:
             * Injecting initial session data (client, userType=null, userId=0).
             */
            controller.setClient(client, null, 0); 
            
            controller.appendLog("Connected to server successfully."); 
            
        } catch (Exception e) { 
            
            if (controller != null) { 
                controller.appendLog("Status: Offline - Connection Failed."); 
            } 
            
            e.printStackTrace(); 
            
        } 
        
    } 
    
}