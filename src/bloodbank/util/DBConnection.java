package bloodbank.util;

import bloodbank.exception.BloodBankException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class for establishing and managing database connections.
 * Reads connection settings from 'db.properties' in the project root,
 * falling back to environment variables (DB_URL, DB_USER, DB_PASSWORD).
 */
public class DBConnection {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/bloodbank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";

    // Ensure driver is loaded once
    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] Warning: MySQL JDBC driver class not found in classpath.");
        }
    }

    private DBConnection() {
        // Prevent instantiation
    }

    /**
     * Establishes a connection to the MySQL database.
     *
     * @return an open {@link Connection}
     * @throws BloodBankException if driver is missing, credentials are not configured,
     *                            or connection to MySQL fails
     */
    public static Connection getConnection() throws BloodBankException {
        // 1. Verify driver availability
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new BloodBankException(
                "MySQL JDBC Driver (" + DRIVER_CLASS + ") not found in classpath. " +
                "Please ensure mysql-connector-j-*.jar is placed in 'lib/' and included with -cp \"lib/*\"."
            );
        }

        // 2. Resolve credentials from db.properties or environment variables
        Properties props = loadProperties();

        String url = props.getProperty("url");
        String user = props.getProperty("user");
        String password = props.getProperty("password");

        // Fallback to environment variables if not found or empty
        if (url == null || url.trim().isEmpty()) {
            url = System.getenv("DB_URL");
        }
        if (user == null || user.trim().isEmpty()) {
            user = System.getenv("DB_USER");
        }
        if (password == null) {
            password = System.getenv("DB_PASSWORD");
        }

        // Check if configuration is missing
        if (url == null || user == null || password == null) {
            throw new BloodBankException(
                "Database credentials are not configured.\n" +
                "Action required: Copy 'db.properties.example' to 'db.properties' in the project root " +
                "and set your MySQL password, OR configure environment variables DB_URL, DB_USER, DB_PASSWORD."
            );
        }

        // 3. Connect to database
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            String sqlState = e.getSQLState();

            if (errorCode == 1045 || "28000".equals(sqlState)) {
                throw new BloodBankException(
                    "Database access denied: invalid MySQL username or password for user '" + user + "'. " +
                    "Please check 'db.properties' and verify your MySQL password.", e
                );
            } else if (errorCode == 1049 || "42000".equals(sqlState)) {
                throw new BloodBankException(
                    "Database 'bloodbank' does not exist on MySQL server. " +
                    "Please execute 'schema.sql' to initialize the database (e.g. mysql -u " + user + " -p < schema.sql).", e
                );
            } else if (errorCode == 0 || "08S01".equals(sqlState) || "08001".equals(sqlState)) {
                throw new BloodBankException(
                    "Unable to connect to MySQL server at '" + url + "'. " +
                    "Please ensure that the MySQL Server service (e.g., MySQL80) is started and running.", e
                );
            } else {
                throw new BloodBankException(
                    "Database connection failure [SQLState: " + sqlState + ", Code: " + errorCode + "]: " + e.getMessage(), e
                );
            }
        }
    }

    /**
     * Attempts to find and load 'db.properties' from project root or working directory.
     */
    private static Properties loadProperties() {
        Properties props = new Properties();

        // Search locations: current working directory, parent directory, and classpath
        File[] candidates = new File[] {
            new File("db.properties"),
            new File("../db.properties"),
            new File(System.getProperty("user.dir"), "db.properties")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                try (InputStream in = new FileInputStream(candidate)) {
                    props.load(in);
                    return props;
                } catch (Exception ignored) {
                    // Continue to next candidate
                }
            }
        }

        // Try classpath resource
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (Exception ignored) {
        }

        return props;
    }
}
