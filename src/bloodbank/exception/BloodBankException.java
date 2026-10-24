// PLACEHOLDER - Member 1 owns this file, will be replaced on merge
package bloodbank.exception;

/**
 * Custom application exception for Blood Bank database and domain operations.
 */
public class BloodBankException extends Exception {
    
    public BloodBankException(String message) {
        super(message);
    }

    public BloodBankException(String message, Throwable cause) {
        super(message, cause);
    }
}
