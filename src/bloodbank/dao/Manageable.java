package bloodbank.dao;

import bloodbank.exception.BloodBankException;
import java.util.List;

/**
 * Generic CRUD management interface.
 * 
 * Demonstrates:
 * 1. Interface: Defines a contract of behavior without dictating implementation.
 * 2. Generics (<T>): Allows the same interface to be reused type-safely for 
 *    different entities like Donor (Manageable<Donor>) and Recipient (Manageable<Recipient>).
 *
 * @param <T> the entity type managed by this DAO (e.g. Donor, Recipient)
 */
public interface Manageable<T> { // Generic type parameter <T> allows type-safe reuse across entities

    // Contract method to add a new entity to the persistent store
    void add(T item) throws BloodBankException;

    // Contract method to retrieve all entities of type T
    List<T> getAll() throws BloodBankException;

    // Contract method to update an existing entity
    void update(T item) throws BloodBankException;

    // Contract method to delete an entity by its numeric primary key identifier
    boolean delete(int id) throws BloodBankException;
}
