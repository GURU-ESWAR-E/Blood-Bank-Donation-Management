package bloodbank.exception;

/**
 * Custom checked exception for Blood Bank domain and database operations.
 * 
 * Why checked exception?
 * It forces calling code (like the UI or Service layer) to anticipate and handle
 * business errors (such as 90-day donation interval restriction, insufficient blood stock,
 * or database connectivity issues) using try-catch blocks.
 */
public class BloodBankException extends Exception { // 'extends Exception' makes this a checked exception
    
    // Standard constructor taking an error message
    public BloodBankException(String message) {
        super(message); // Passes message up to the java.lang.Exception superclass
    }

    // Overloaded constructor taking a message and the underlying root cause exception
    public BloodBankException(String message, Throwable cause) {
        super(message, cause); // Enables exception chaining (e.g., wrapping a SQLException)
    }
}
