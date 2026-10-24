package bloodbank.dao;

import bloodbank.exception.BloodBankException;
import bloodbank.model.Donor;
import bloodbank.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donor entities.
 * Implements Manageable<Donor> and demonstrates method overloading for search.
 */
public class DonorDAO implements Manageable<Donor> {

    @Override
    public void add(Donor donor) throws BloodBankException {
        if (donor == null) {
            throw new BloodBankException("Cannot add a null Donor.");
        }

        String sql = "INSERT INTO donors (name, age, blood_group, phone) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, donor.getName());
            stmt.setInt(2, donor.getAge());
            stmt.setString(3, donor.getBloodGroup());
            stmt.setString(4, donor.getPhone());

            stmt.executeUpdate();

            // Retrieve auto-generated donor_id
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    donor.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            handleSQLException("Failed to add donor", e, donor.getPhone(), donor.getBloodGroup());
        }
    }

    @Override
    public List<Donor> getAll() throws BloodBankException {
        // Retrieve all donors with their most recent donation date (if any) via LEFT JOIN and GROUP BY
        String sql = "SELECT d.donor_id, d.name, d.age, d.blood_group, d.phone, " +
                     "MAX(dn.donation_date) AS last_donation " +
                     "FROM donors d " +
                     "LEFT JOIN donations dn ON d.donor_id = dn.donor_id " +
                     "GROUP BY d.donor_id, d.name, d.age, d.blood_group, d.phone " +
                     "ORDER BY d.name ASC";

        List<Donor> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("donor_id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String bg = rs.getString("blood_group");
                String phone = rs.getString("phone");
                String lastDonation = rs.getString("last_donation"); // null if no donations

                list.add(new Donor(id, name, age, bg, phone, lastDonation));
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve donors: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public void update(Donor donor) throws BloodBankException {
        if (donor == null) {
            throw new BloodBankException("Cannot update a null Donor.");
        }

        String sql = "UPDATE donors SET name = ?, age = ?, blood_group = ?, phone = ? WHERE donor_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, donor.getName());
            stmt.setInt(2, donor.getAge());
            stmt.setString(3, donor.getBloodGroup());
            stmt.setString(4, donor.getPhone());
            stmt.setInt(5, donor.getId());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new BloodBankException("Donor with ID " + donor.getId() + " not found.");
            }

        } catch (SQLException e) {
            handleSQLException("Failed to update donor #" + donor.getId(), e, donor.getPhone(), donor.getBloodGroup());
        }
    }

    @Override
    public boolean delete(int id) throws BloodBankException {
        String sql = "DELETE FROM donors WHERE donor_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new BloodBankException("Failed to delete donor #" + id + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------
    // Method Overloading: Search demonstrating Compile-Time Polymorphism
    // ------------------------------------------------------------------------

    /**
     * Search donors by blood group alone.
     */
    public List<Donor> search(String bloodGroup) throws BloodBankException {
        return search(null, bloodGroup);
    }

    /**
     * Search donors by name (case-insensitive substring) and/or blood group.
     * Empty/null parameters match all.
     */
    public List<Donor> search(String name, String bloodGroup) throws BloodBankException {
        StringBuilder sql = new StringBuilder(
            "SELECT d.donor_id, d.name, d.age, d.blood_group, d.phone, " +
            "MAX(dn.donation_date) AS last_donation " +
            "FROM donors d " +
            "LEFT JOIN donations dn ON d.donor_id = dn.donor_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (name != null && !name.trim().isEmpty()) {
            sql.append("AND LOWER(d.name) LIKE ? ");
            params.add("%" + name.trim().toLowerCase() + "%");
        }

        if (bloodGroup != null && !bloodGroup.trim().isEmpty() && !bloodGroup.equalsIgnoreCase("ALL")) {
            sql.append("AND d.blood_group = ? ");
            params.add(bloodGroup.trim());
        }

        sql.append("GROUP BY d.donor_id, d.name, d.age, d.blood_group, d.phone ORDER BY d.name ASC");

        List<Donor> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("donor_id");
                    String donorName = rs.getString("name");
                    int age = rs.getInt("age");
                    String bg = rs.getString("blood_group");
                    String phone = rs.getString("phone");
                    String lastDonation = rs.getString("last_donation");

                    list.add(new Donor(id, donorName, age, bg, phone, lastDonation));
                }
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to search donors: " + e.getMessage(), e);
        }

        return list;
    }

    // ------------------------------------------------------------------------
    // Analytical Reports (JOIN & VIEW)
    // ------------------------------------------------------------------------

    /**
     * Retrieves readable audit report by performing an INNER JOIN between donors and donations.
     */
    public List<String> getDonationHistory() throws BloodBankException {
        String sql = "SELECT d.donor_id, d.name, d.blood_group, d.phone, " +
                     "dn.donation_id, dn.donation_date, dn.units " +
                     "FROM donors d " +
                     "JOIN donations dn ON d.donor_id = dn.donor_id " +
                     "ORDER BY dn.donation_date DESC, dn.donation_id DESC";

        List<String> report = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String line = String.format(
                    "Donation #%d | Date: %s | Units: %d | Donor: %s (ID: %d, Blood: %s, Phone: %s)",
                    rs.getInt("donation_id"),
                    rs.getString("donation_date"),
                    rs.getInt("units"),
                    rs.getString("name"),
                    rs.getInt("donor_id"),
                    rs.getString("blood_group"),
                    rs.getString("phone")
                );
                report.add(line);
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to load donation history: " + e.getMessage(), e);
        }

        return report;
    }

    /**
     * Reads the database VIEW 'donor_donation_summary'.
     */
    public List<String> getDonorSummary() throws BloodBankException {
        String sql = "SELECT donor_id, name, blood_group, total_donations, total_units, last_donation " +
                     "FROM donor_donation_summary " +
                     "ORDER BY donor_id ASC";

        List<String> report = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String lastDate = rs.getString("last_donation");
                String line = String.format(
                    "Donor #%d: %s | Blood: %s | Total Donations: %d | Total Units: %d | Last Donation: %s",
                    rs.getInt("donor_id"),
                    rs.getString("name"),
                    rs.getString("blood_group"),
                    rs.getInt("total_donations"),
                    rs.getInt("total_units"),
                    (lastDate != null ? lastDate : "None")
                );
                report.add(line);
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to load donor summary view: " + e.getMessage(), e);
        }

        return report;
    }

    // ------------------------------------------------------------------------
    // Error Translation
    // ------------------------------------------------------------------------
    private void handleSQLException(String prefix, SQLException e, String phone, String bloodGroup)
            throws BloodBankException {
        int code = e.getErrorCode();

        if (code == 1062) {
            throw new BloodBankException("A donor with phone number '" + phone + "' already exists.", e);
        } else if (code == 1452) {
            throw new BloodBankException("Invalid blood group '" + bloodGroup + "'. Allowed: A+, A-, B+, B-, O+, O-, AB+, AB-.", e);
        } else if (code == 3819) {
            throw new BloodBankException("Constraint violation: Donor age must be between 18 and 65.", e);
        } else {
            throw new BloodBankException(prefix + ": " + e.getMessage(), e);
        }
    }
}
