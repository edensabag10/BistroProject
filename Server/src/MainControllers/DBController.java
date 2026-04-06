package MainControllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The DBController class serves as the central database manager for the server-side application.
 * It encapsulates the JDBC connection logic and provides a unified gateway to the 
 * MySQL database.
 * * <p>Design Pattern: <b>Singleton</b>.
 * This ensures that only one database connection instance exists across the entire 
 * server application, preventing resource exhaustion and maintaining data consistency.</p>
 * * @author Software Engineering Student
 * @version 1.0
 */
public class DBController {

    /** * The single, static instance of DBController.
     */
    private static DBController instance;
    
    /** * The persistent JDBC {@link Connection} object used for all SQL operations.
     */
    private Connection conn;
    
    /**
     * Returns the single, shared instance of the {@code DBController}.
     * <p>
     * This method implements the <b>Singleton design pattern</b> using lazy initialization. 
     * If the instance does not exist, it is created; otherwise, the existing instance 
     * is returned to ensure a single point of access to the database connection logic.
     * </p>
     *
     * @return The singleton instance of {@code DBController}.
     */
    public static DBController getInstance() {
        if (instance == null) {
            instance = new DBController();
        }
        return instance;
    }

    /**
     * Establishes a physical connection to the MySQL database using the JDBC driver.
     * * <p>Connection Parameters:
     * <ul>
     * <li><b>Port:</b> 3307 (Custom MySQL port)</li>
     * <li><b>Schema:</b> prototypedb</li>
     * <li><b>Timezone:</b> Asia/Jerusalem</li>
     * <li><b>SSL:</b> Disabled</li>
     * </ul>
     * </p>
     * * @throws SQLException if the connection attempt fails or the driver is not found.
     */
    public void connectToDB() throws SQLException {
        // Prevent re-connecting if an active connection already exists
        if (conn != null) {
            return; 
        }

        /**
         * Establishes a secure connection to the local MySQL database.
         * <p>
         * This method initializes the {@code conn} object using JDBC. It includes a safety 
         * check to prevent redundant connection attempts if an active connection already exists.
         * </p>
         * <p><b>Configuration Details:</b></p>
         * <ul>
         * <li><b>Database URL:</b> {@code jdbc:mysql://localhost:3306/prototypedb}</li>
         * <li><b>Timezone:</b> Set to {@code Asia/Jerusalem} to ensure consistency between 
         * application logic and stored timestamps.</li>
         * <li><b>Local Infile:</b> Enabled to allow bulk data loading.</li>
         * <li><b>Security:</b> SSL is disabled for the local environment, and public key 
         * retrieval is permitted.</li>
         * </ul>
         *
         * @throws SQLException If the connection fails due to invalid credentials, 
         * network issues, or database unavailability.
         */
        conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/prototypedb?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true",
            "root",
            "Eden2701@"

        );}

    
    /**
     * Provides access to the current database connection object.
     * <p>
     * This method returns the active {@code Connection} instance managed by the 
     * {@code DBController}. It is essential for creating {@code PreparedStatement} 
     * or {@code Statement} objects in other parts of the application.
     * </p>
     *
     * @return The current {@link Connection} instance; may be {@code null} if 
     * {@link #connectToDB()} has not been successfully called yet.
     */
    public Connection getConnection() {
        return conn;
    }
    
    /**
     * Gracefully terminates the active database connection and releases associated resources.
     * <p>
     * This method performs a safety check to ensure that the connection instance 
     * is not {@code null} and has not been closed already. If the connection is active, 
     * it is closed to free up memory and database-side resources (such as active locks).
     * </p>
     *
     * @throws SQLException If a database access error occurs while attempting to close 
     * the connection.
     */
    public void closeConnection() throws SQLException {
        // Verify that the connection exists and is still open before attempting to close
        if (conn != null && !conn.isClosed()) {
            conn.close();
            System.out.println("SQL connection closed successfully.");
        }
    }
}