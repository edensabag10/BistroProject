package serverGUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.application.Platform;
import MainControllers.ServerController;
import common.ServerIF;

public class ServerPortFrameController implements ServerIF {

    @FXML
    private Button btnStart;

    @FXML
    private Button btnExit;

    @FXML
    private TextArea txtLog;

    private ServerController server;

    
    /**
     * Event handler triggered when the "Start" button is clicked in the server GUI.
     * <p>
     * This method is responsible for initializing and launching the server instance. 
     * It includes a safety check to ensure that multiple server instances are not 
     * started simultaneously. If the server is not already running, it instantiates 
     * a new {@code ServerController} on port 5555 and begins listening for 
     * incoming client connections.
     * </p>
     *
     * @param event The {@code ActionEvent} triggered by the button click.
     */
    
    @FXML
    public void clickStart(ActionEvent event) {
        if (server != null) {
            appendLog("Server is already running!");
            return;
        }

        appendLog("Attempting to start server...");

        server = new ServerController(5555, this);

        try {
            server.listen();
            appendLog("Server started listening on port 5555");

        } catch (Exception e) {
            appendLog("Error starting server: " + e.getMessage());
        }
    }

    
    
    /**
     * Event handler triggered when the "Exit" or "Close" button is clicked in the server GUI.
     * <p>
     * This method initiates a graceful shutdown sequence:
     * <ol>
     * <li><b>Server Termination:</b> If the server is active, it stops listening for new 
     * connections via {@code stopListening()} and releases bound resources using {@code close()}.</li>
     * <li><b>Error Handling:</b> Any exceptions encountered during the server shutdown are 
     * logged to the UI console for debugging.</li>
     * <li><b>Process Exit:</b> Terminates the Java Virtual Machine (JVM) with status code 0, 
     * ensuring the application process is completely removed from memory.</li>
     * </ol>
     * </p>
     *
     * @param event The {@code ActionEvent} triggered by the exit command.
     */
    @FXML
    public void clickExit(ActionEvent event) {
        appendLog("Exiting...");

        if (server != null) {
            try {
                appendLog("Stopping server...");
                
                server.stopListening();
                
                server.close();

            } catch (Exception e) {
                appendLog("Error while closing server: " + e.getMessage());
            }
        }

        System.exit(0);
    }

    
    /**
     * Appends a message to the server's visual log in the GUI.
     * <p>
     * This method is thread-safe and can be called from background server threads. 
     * It uses {@link Platform#runLater(Runnable)} to ensure that the UI update 
     * (appending text to {@code txtLog}) is executed on the JavaFX Application Thread. 
     * Each log entry is automatically followed by a newline character.
     * </p>
     *
     * @param msg The string message to be displayed in the log.
     */
    @Override
    public void appendLog(String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtLog.appendText(msg + "\n");
            }
        });
    }
}