// PLACEHOLDER - Member 1 owns this file, will be replaced on merge
package bloodbank.dao;

import bloodbank.exception.BloodBankException;
import java.util.List;

/**
 * Generic CRUD management interface.
 *
 * @param <T> entity type
 */
public interface Manageable<T> {
    void add(T item) throws BloodBankException;
    List<T> getAll() throws BloodBankException;
    void update(T item) throws BloodBankException;
    boolean delete(int id) throws BloodBankException;
}
