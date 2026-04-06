package CardReader;

import java.util.List;
import javafx.geometry.Pos;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import client.ChatClient;
import CardReader.CardReaderController;

/**
 * The CardReaderView class provides a graphical simulation of a restaurant card reader terminal.
 * It implements ChatIF to handle asynchronous messages from the server and provides the user interface 
 * for subscriber login, arrival confirmation, and code recovery.
 */
public class CardReaderView extends Application implements common.ChatIF {

    /** The primary stage for this JavaFX application. */
    private Stage primaryStage;
    
    /** Controller instance for handling business logic and server requests. */
    private CardReaderController controller = new CardReaderController();
    
    /** The ID of the currently logged-in subscriber. */
    private String currentSubscriberID; 

    /** Label for displaying login-related status messages. */
    private Label loginStatusLabel = new Label(""); 
    
    /** Label for displaying verification status of arrival codes. */
    private Label verifyMessageLabel = new Label(""); 
    
    /** Container for dynamically displaying recovered confirmation codes. */
    private VBox codesContainer = new VBox(10); 

    /**
     * Starts the JavaFX application and displays the welcome screen.
     * * @param primaryStage The main window for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showWelcomeScreen();
    }

    /**
     * Displays the initial welcome screen with a connect button.
     */
    private void showWelcomeScreen() {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("main-background");

        Label title = new Label("Card Reader Simulation");
        title.getStyleClass().add("title-label");

        Button connectBtn = new Button("Connect");
        connectBtn.getStyleClass().add("connect-button");
        connectBtn.setOnAction(e -> showLoginScreen());

        layout.getChildren().addAll(title, connectBtn);
        setupAndShowScene(layout, "Card Reader - Welcome");
    }

    /**
     * Displays the login screen and registers this view as the active UI listener.
     */
    private void showLoginScreen() {
        if (CardReaderController.getClient() != null) {
            CardReaderController.getClient().setUI(this);
        } else {
            System.err.println("Error: ChatClient is not initialized!");
        }

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("main-background");
        
        Label title = new Label("Card Reader Simulation");
        title.getStyleClass().add("title-label");

        Label instruction = new Label("Please enter your Subscriber ID:");
        instruction.getStyleClass().add("instruction-label");

        TextField idInput = new TextField();
        idInput.setPromptText("Enter ID here...");
        idInput.getStyleClass().add("id-input-field");

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("login-button");

        loginBtn.setOnAction(e -> {
            String id = idInput.getText();
            this.currentSubscriberID = id; 
            controller.validateSubscriber(id); 
            loginStatusLabel.setText("Connecting...");
        });
        
        layout.getChildren().addAll(title, instruction, idInput, loginBtn, loginStatusLabel);
        setupAndShowScene(layout, "Card Reader - Login");
    }

    /**
     * Displays the screen showing active confirmation codes recovered from the server.
     */
    private void showRecoveredCodesScreen() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("main-background");

        Label title = new Label("Your Active Confirmation Codes");
        title.getStyleClass().add("title-label");

        codesContainer.getChildren().clear();
        codesContainer.setAlignment(Pos.CENTER);
        codesContainer.getChildren().add(new Label("Fetching codes from server..."));

        controller.getLostConfirmationCodes(currentSubscriberID); 

        Button backBtn = new Button("Back to Menu");
        backBtn.getStyleClass().add("connect-button");
        backBtn.setOnAction(e -> showSubscriberMenu());

        layout.getChildren().addAll(title, codesContainer, backBtn);
        setupAndShowScene(layout, "Recover Codes");
    }

    /**
     * Displays the main menu for a successfully logged-in subscriber.
     */
    private void showSubscriberMenu() {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("main-background");

        Label welcomeMsg = new Label("Subscriber Menu");
        welcomeMsg.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        Button enterCodeBtn = new Button("Enter Confirmation Code");
        enterCodeBtn.getStyleClass().add("arrival-code-button");
        enterCodeBtn.setOnAction(e -> showEnterCodeScreen());

        Button lostCodeBtn = new Button("I lost my confirmation code");
        lostCodeBtn.getStyleClass().add("lost-code-button");
        lostCodeBtn.setOnAction(e -> showRecoveredCodesScreen());

        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.getStyleClass().add("disconnect-button"); 
        disconnectBtn.setOnAction(e -> showWelcomeScreen());

        layout.getChildren().addAll(welcomeMsg, enterCodeBtn, lostCodeBtn, disconnectBtn);
        setupAndShowScene(layout, "Subscriber Menu");
    }

    /**
     * Helper method to configure the scene with CSS and display it on the primary stage.
     * * @param layout The VBox container for the scene.
     * @param title  The title to display in the window header.
     */
    private void setupAndShowScene(VBox layout, String title) {
        Scene scene = new Scene(layout, 450, 450);
        String cssPath = "/CardReader/CardReader.css";
        if (getClass().getResource(cssPath) != null) {
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        }
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Displays the screen where subscribers can input their arrival confirmation code.
     */
    private void showEnterCodeScreen() {
        try {
            VBox layout = new VBox(20);
            layout.setAlignment(Pos.CENTER);
            layout.getStyleClass().add("main-background");

            Label title = new Label("Enter Confirmation Code");
            title.getStyleClass().add("title-label");

            TextField codeInput = new TextField();
            codeInput.setPromptText("Type your code here (e.g. 12345)");
            codeInput.getStyleClass().add("id-input-field");

            verifyMessageLabel.setText("");
            verifyMessageLabel.setStyle("-fx-text-fill: black;");

            Button submitBtn = new Button("Confirm Arrival");
            submitBtn.getStyleClass().add("login-button");
            submitBtn.setOnAction(e -> {
                verifyMessageLabel.setText("Processing...");
                controller.verifyConfirmationCode(codeInput.getText(), currentSubscriberID); 
            });

            Button backBtn = new Button("Back");
            backBtn.getStyleClass().add("connect-button");
            backBtn.setOnAction(e -> showSubscriberMenu());

            layout.getChildren().addAll(title, codeInput, submitBtn, verifyMessageLabel, backBtn);
            setupAndShowScene(layout, "Enter Code");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Receives messages from the server and updates the UI components on the JavaFX thread.
     * * @param message The object received from the server (e.g., List of codes, success String).
     */
    @Override
    public void display(Object message) {
        System.out.println(">>> MESSAGE RECEIVED FROM SERVER: " + message);

        Platform.runLater(() -> {
            try {
                if (message instanceof List && !((List<?>) message).isEmpty()) {
                    List<?> res = (List<?>) message;
                    if ("LOGIN_SUCCESS".equals(res.get(0))) {
                        showSubscriberMenu();
                        return;
                    }
                    
                    if (res.get(0) instanceof String && !res.get(0).toString().startsWith("LOGIN")) {
                        @SuppressWarnings("unchecked")
                        List<String> codes = (List<String>) message;
                        codesContainer.getChildren().clear(); 
                        if (codes.isEmpty()) {
                            codesContainer.getChildren().add(new Label("No active codes found."));
                        } else {
                            for (String codeStr : codes) {
                                Label codeLabel = new Label(codeStr);
                                codeLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #1565c0; -fx-font-weight: bold;");
                                codesContainer.getChildren().add(codeLabel);
                            }
                        }
                        return;
                    }
                }

                if (message instanceof String) {
                    String response = (String) message;
                    if (response.contains("not found") || response.contains("ERROR")) {
                        loginStatusLabel.setText("Login Error: Subscriber ID not found.");
                        loginStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (response.startsWith("SUCCESS_TABLE_")) {
                        String tableId = response.split("_")[2];
                        verifyMessageLabel.setText("Success! Table #" + tableId);
                        verifyMessageLabel.setStyle("-fx-text-fill: green;");
                    } else {
                        verifyMessageLabel.setText(response);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Prints a standardized log message to the system console.
     * * @param message The log message string.
     */
    private void appendLog(String message) {
        System.out.println("[LOG]: " + message);
    } 

    /**
     * Main entry point for the Card Reader simulation. Initializes the client and launches the UI.
     * * @param args Command line arguments.
     */
    public static void main(String[] args) {
        try {
            ChatClient chatClient = new ChatClient("localhost", 5555, null); 
            CardReaderController.setClient(chatClient);
            launch(args);
        } catch (Exception e) {
            System.err.println("Connection failed! Displaying UI anyway...");
            launch(args); 
        }
    }
}