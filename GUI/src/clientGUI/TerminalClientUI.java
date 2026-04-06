package clientGUI;

import client.ChatClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import terminalGUI.Controllers.TerminalControllers.TerminalLoginController;

/**
 * Main entry point for the Bistro Customer Service Terminal application.
 * This class orchestrates the JavaFX lifecycle and establishes the initial 
 * network connection for the self-service terminal.
 */
public class TerminalClientUI extends Application {

    /** Persistent network client instance for OCSF communication. */
    private ChatClient client;

    /**
     * The main entry point for the application. Launches the JavaFX environment.
     * * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes the primary stage, loads the FXML UI, and connects to the server.
     * * @param primaryStage The main window for this JavaFX application.
     * @throws Exception If FXML loading or server connection fails.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // --- UI Initialization Phase ---

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("fxmlFiles/TerminalLoginFrame.fxml")
        );
        Parent root = loader.load();

        // Get controller
        TerminalLoginController controller = loader.getController();

        // Scene setup
        Scene scene = new Scene(root);
        primaryStage.setTitle("Bistro - Customer Service Terminal");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        // --- Network Initialization Phase ---
        try {
            // Establish OCSF connection to the server
            client = new ChatClient("localhost", 5555, controller);
            controller.setClient(client);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}