package bloodbank.dao;

import bloodbank.exception.BloodBankException;
import bloodbank.model.Donation;
import bloodbank.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donation operations.
 * Handles transactional recording of donations, 90-day interval checks,
 * and automatic blood stock inventory updates.
 */
public class DonationDAO {

    private static final int MIN_DONATION_INTERVAL_DAYS = 90;

    /**
     * Records a blood donation for a donor within a single ACID transaction:
     * 1. Verifies donor existence and reads their blood group.
     * 2. Checks last donation date and enforces a 90-day gap.
     * 3. Inserts the new donation record.
     * 4. Updates blood_stock units for that blood group.
     * 5. Commits transaction and returns the generated donation_id.
     *
     * @param donorId the donor's ID
     * @param units   number of units donated (> 0)
     * @return the generated donation_id
     * @throws BloodBankException if donor is invalid, ineligible, or if a database error occurs
     */
    public int recordDonation(int donorId, int units) throws BloodBankException {
        if (units <= 0) {
            throw new BloodBankException("Donation units must be strictly positive (greater than 0). Requested: " + units);
        }

        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = DBConnection.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Begin transaction

            // Step 1: Verify donor exists and retrieve their blood group
            String bloodGroup = null;
            String donorSql = "SELECT blood_group FROM donors WHERE donor_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(donorSql)) {
                stmt.setInt(1, donorId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        bloodGroup = rs.getString("blood_group");
                    } else {
                        throw new BloodBankException("Donor with ID " + donorId + " does not exist.");
                    }
                }
            }

            // Step 2: Enforce 90-day gap from last donation
            String dateSql = "SELECT MAX(donation_date) AS last_date FROM donations WHERE donor_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(dateSql)) {
                stmt.setInt(1, donorId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Date lastDate = rs.getDate("last_date");
                        if (lastDate != null) {
                            LocalDate lastDonationDate = lastDate.toLocalDate();
                            LocalDate today = LocalDate.now();
                            long daysElapsed = ChronoUnit.DAYS.between(lastDonationDate, today);

                            if (daysElapsed < MIN_DONATION_INTERVAL_DAYS) {
                                long daysRemaining = MIN_DONATION_INTERVAL_DAYS - daysElapsed;
                                throw new BloodBankException(
                                    "Donor #" + donorId + " is not eligible to donate yet. " +
                                    "A minimum 90-day gap is required between donations. " +
                                    "Days elapsed: " + daysElapsed + ", days remaining: " + daysRemaining + " " +
                                    "(Last donation was on " + lastDonationDate + ")."
                                );
                            }
                        }
                    }
                }
            }

            // Step 3: Insert new donation record
            int newDonationId;
            String insertSql = "INSERT INTO donations (donor_id, donation_date, units) VALUES (?, CURDATE(), ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, donorId);
                stmt.setInt(2, units);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newDonationId = rs.getInt(1);
                    } else {
                        throw new BloodBankException("Failed to retrieve generated donation ID.");
                    }
                }
            }

            // Step 4: Increment blood stock inventory
            String updateStockSql = "UPDATE blood_stock SET units = units + ? WHERE blood_group = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateStockSql)) {
                stmt.setInt(1, units);
                stmt.setString(2, bloodGroup);
                int rows = stmt.executeUpdate();
                if (rows == 0) {
                    throw new BloodBankException("Failed to update blood stock for blood group '" + bloodGroup + "'.");
                }
            }

            // Step 5: Commit transaction
            conn.commit();
            return newDonationId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // Suppress rollback error and rethrow main error
                }
            }
            throw new BloodBankException("Transaction failed during recordDonation: " + e.getMessage(), e);

        } catch (BloodBankException bbe) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            }
            throw bbe;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Retrieves all recorded donations ordered by date descending.
     */
    public List<Donation> getAll() throws BloodBankException {
        String sql = "SELECT donation_id, donor_id, donation_date, units FROM donations ORDER BY donation_date DESC, donation_id DESC";
        List<Donation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int donationId = rs.getInt("donation_id");
                int donorId = rs.getInt("donor_id");
                String date = rs.getString("donation_date");
                int units = rs.getInt("units");

                list.add(new Donation(donationId, donorId, date, units));
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve donations: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Retrieves all donations made by a specific donor.
     */
    public List<Donation> getByDonor(int donorId) throws BloodBankException {
        String sql = "SELECT donation_id, donor_id, donation_date, units FROM donations WHERE donor_id = ? ORDER BY donation_date DESC";
        List<Donation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int donationId = rs.getInt("donation_id");
                    String date = rs.getString("donation_date");
                    int units = rs.getInt("units");

                    list.add(new Donation(donationId, donorId, date, units));
                }
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve donations for donor #" + donorId + ": " + e.getMessage(), e);
        }

        return list;
    }
}
