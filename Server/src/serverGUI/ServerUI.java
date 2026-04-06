package serverGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerUI extends Application {
	/**
	 * The main entry point for the application.
	 * <p>
	 * This method serves as the starting point for the JVM (Java Virtual Machine). 
	 * It invokes the {@link #launch(String...)} method, which is a static method 
	 * inherited from the {@code Application} class. This call initializes the 
	 * JavaFX runtime, starts the JavaFX Application Thread, and eventually 
	 * triggers the {@code start(Stage)} method.
	 * </p>
	 *
	 * @param args Command-line arguments passed to the application during startup.
	 */
    public static void main(String[] args) {
        launch(args);
    }
    
    
    
    /**
     * Initializes and displays the primary stage of the JavaFX application.
     * <p>
     * This method is called automatically by the JavaFX runtime after the {@code init()} 
     * method has finished and the system is ready to start running. It performs 
     * the following setup steps:
     * <ol>
     * <li><b>FXML Loading:</b> Locates and loads the {@code ServerPortFrame.fxml} file.</li>
     * <li><b>Controller Retrieval:</b> Obtains the controller instance associated with the FXML.</li>
     * <li><b>Scene Creation:</b> Constructs a new {@code Scene} with the loaded FXML hierarchy as its root.</li>
     * <li><b>Stage Configuration:</b> Sets the window title, applies the scene, and makes the window visible.</li>
     * </ol>
     * </p>
     *
     * @param primaryStage The primary stage for this application, onto which the 
     * application scene can be set.
     * @throws Exception If there is an issue loading the FXML file or initializing the scene.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ServerPortFrame.fxml"));
        Parent root = loader.load();
        
        ServerPortFrameController controller = loader.getController();
        
        Scene scene = new Scene(root);

        primaryStage.setTitle("Server Management");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}