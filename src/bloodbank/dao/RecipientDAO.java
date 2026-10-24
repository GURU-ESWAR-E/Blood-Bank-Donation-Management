package bloodbank.dao;

import bloodbank.exception.BloodBankException;
import bloodbank.model.Recipient;
import bloodbank.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Recipient entities.
 * Implements Manageable<Recipient> providing full CRUD operations.
 */
public class RecipientDAO implements Manageable<Recipient> {

    @Override
    public void add(Recipient recipient) throws BloodBankException {
        if (recipient == null) {
            throw new BloodBankException("Cannot add a null Recipient.");
        }

        String sql = "INSERT INTO recipients (name, age, blood_group, phone, hospital) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, recipient.getName());
            stmt.setInt(2, recipient.getAge());
            stmt.setString(3, recipient.getBloodGroup());
            stmt.setString(4, recipient.getPhone());
            stmt.setString(5, recipient.getHospital());

            stmt.executeUpdate();

            // Retrieve auto-generated recipient_id
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    recipient.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            handleSQLException("Failed to add recipient", e, recipient.getPhone(), recipient.getBloodGroup());
        }
    }

    @Override
    public List<Recipient> getAll() throws BloodBankException {
        String sql = "SELECT recipient_id, name, age, blood_group, phone, hospital FROM recipients ORDER BY name ASC";
        List<Recipient> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("recipient_id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String bg = rs.getString("blood_group");
                String phone = rs.getString("phone");
                String hospital = rs.getString("hospital");

                list.add(new Recipient(id, name, age, bg, phone, hospital));
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve recipients: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public void update(Recipient recipient) throws BloodBankException {
        if (recipient == null) {
            throw new BloodBankException("Cannot update a null Recipient.");
        }

        String sql = "UPDATE recipients SET name = ?, age = ?, blood_group = ?, phone = ?, hospital = ? WHERE recipient_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, recipient.getName());
            stmt.setInt(2, recipient.getAge());
            stmt.setString(3, recipient.getBloodGroup());
            stmt.setString(4, recipient.getPhone());
            stmt.setString(5, recipient.getHospital());
            stmt.setInt(6, recipient.getId());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new BloodBankException("Recipient with ID " + recipient.getId() + " not found.");
            }

        } catch (SQLException e) {
            handleSQLException("Failed to update recipient #" + recipient.getId(), e, recipient.getPhone(), recipient.getBloodGroup());
        }
    }

    @Override
    public boolean delete(int id) throws BloodBankException {
        String sql = "DELETE FROM recipients WHERE recipient_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new BloodBankException("Failed to delete recipient #" + id + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------
    // Error Translation
    // ------------------------------------------------------------------------
    private void handleSQLException(String prefix, SQLException e, String phone, String bloodGroup)
            throws BloodBankException {
        int code = e.getErrorCode();

        if (code == 1062) {
            throw new BloodBankException("A recipient with phone number '" + phone + "' already exists.", e);
        } else if (code == 1452) {
            throw new BloodBankException("Invalid blood group '" + bloodGroup + "'. Allowed: A+, A-, B+, B-, O+, O-, AB+, AB-.", e);
        } else if (code == 3819) {
            throw new BloodBankException("Constraint violation: Recipient age must be between 0 and 120.", e);
        } else {
            throw new BloodBankException(prefix + ": " + e.getMessage(), e);
        }
    }
}
