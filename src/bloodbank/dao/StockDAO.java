package bloodbank.dao;

import bloodbank.exception.BloodBankException;
import bloodbank.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Blood Stock Inventory and Blood Issuance operations.
 * Implements transactional blood issue with pessimistic locking (FOR UPDATE)
 * and analytical inventory reports.
 */
public class StockDAO {

    /**
     * Retrieves the current available blood units for all 8 standard blood groups,
     * maintaining a canonical display order.
     *
     * @return a LinkedHashMap mapping blood group string to unit count
     * @throws BloodBankException if a database error occurs
     */
    public Map<String, Integer> getStock() throws BloodBankException {
        // Enforce canonical order: A+, A-, B+, B-, O+, O-, AB+, AB-
        String sql = "SELECT blood_group, units FROM blood_stock " +
                     "ORDER BY FIELD(blood_group, 'A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-')";

        Map<String, Integer> stockMap = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                stockMap.put(rs.getString("blood_group"), rs.getInt("units"));
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve blood stock: " + e.getMessage(), e);
        }

        return stockMap;
    }

    /**
     * Calculates the total inventory units across all blood groups.
     *
     * @return total units
     * @throws BloodBankException if a database error occurs
     */
    public int getTotalUnits() throws BloodBankException {
        String sql = "SELECT COALESCE(SUM(units), 0) AS total FROM blood_stock";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;

        } catch (SQLException e) {
            throw new BloodBankException("Failed to calculate total blood stock: " + e.getMessage(), e);
        }
    }

    /**
     * Issues blood units to a registered recipient within a single ACID transaction:
     * 1. Reads the recipient's blood group.
     * 2. Locks the corresponding blood_stock row using SELECT ... FOR UPDATE.
     * 3. Checks if available units >= requested units.
     *    If insufficient, immediately rolls back and throws BloodBankException.
     * 4. Updates blood_stock (decrements units).
     * 5. Inserts an entry into blood_issues with CURDATE().
     * 6. Commits the transaction and returns the generated issue_id.
     *
     * @param recipientId the recipient ID
     * @param units       number of units to issue (> 0)
     * @return the generated issue_id
     * @throws BloodBankException if recipient not found, stock is insufficient, or database error occurs
     */
    public int issueBlood(int recipientId, int units) throws BloodBankException {
        if (units <= 0) {
            throw new BloodBankException("Requested units must be strictly positive (greater than 0). Requested: " + units);
        }

        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = DBConnection.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Begin transaction

            // Step 1: Read recipient's blood group
            String bloodGroup = null;
            String recSql = "SELECT blood_group FROM recipients WHERE recipient_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(recSql)) {
                stmt.setInt(1, recipientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        bloodGroup = rs.getString("blood_group");
                    } else {
                        throw new BloodBankException("Recipient with ID " + recipientId + " does not exist.");
                    }
                }
            }

            // Step 2: Acquire row lock on blood_stock for the recipient's blood group
            int currentStock = 0;
            String stockSql = "SELECT units FROM blood_stock WHERE blood_group = ? FOR UPDATE";
            try (PreparedStatement stmt = conn.prepareStatement(stockSql)) {
                stmt.setString(1, bloodGroup);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        currentStock = rs.getInt("units");
                    } else {
                        throw new BloodBankException("No stock entry found for blood group '" + bloodGroup + "'.");
                    }
                }
            }

            // Step 3: Validate inventory availability
            if (currentStock < units) {
                throw new BloodBankException(
                    "Insufficient stock for " + bloodGroup + ". Available: " + currentStock + ", requested: " + units
                );
            }

            // Step 4: Decrement blood stock inventory
            String updateSql = "UPDATE blood_stock SET units = units - ? WHERE blood_group = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, units);
                stmt.setString(2, bloodGroup);
                stmt.executeUpdate();
            }

            // Step 5: Record blood issue event
            int issueId;
            String issueSql = "INSERT INTO blood_issues (recipient_id, blood_group, units, issue_date) VALUES (?, ?, ?, CURDATE())";
            try (PreparedStatement stmt = conn.prepareStatement(issueSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, recipientId);
                stmt.setString(2, bloodGroup);
                stmt.setInt(3, units);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        issueId = rs.getInt(1);
                    } else {
                        throw new BloodBankException("Failed to retrieve generated issue ID.");
                    }
                }
            }

            // Step 6: Commit transaction
            conn.commit();
            return issueId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            }
            throw new BloodBankException("Transaction failed during issueBlood: " + e.getMessage(), e);

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
     * Generates a readable audit report of all blood issues by joining blood_issues and recipients.
     */
    public List<String> getIssueHistory() throws BloodBankException {
        String sql = "SELECT i.issue_id, i.issue_date, i.units, i.blood_group, " +
                     "r.recipient_id, r.name, r.hospital " +
                     "FROM blood_issues i " +
                     "JOIN recipients r ON i.recipient_id = r.recipient_id " +
                     "ORDER BY i.issue_date DESC, i.issue_id DESC";

        List<String> report = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String line = String.format(
                    "Issue #%d | Date: %s | Units: %d | Blood Group: %s | Recipient: %s (ID: %d) | Hospital: %s",
                    rs.getInt("issue_id"),
                    rs.getString("issue_date"),
                    rs.getInt("units"),
                    rs.getString("blood_group"),
                    rs.getString("name"),
                    rs.getInt("recipient_id"),
                    rs.getString("hospital")
                );
                report.add(line);
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve blood issue history: " + e.getMessage(), e);
        }

        return report;
    }

    /**
     * Aggregates total donations and units grouped by blood group across donors and donations.
     */
    public List<String> getDonationSummaryByGroup() throws BloodBankException {
        String sql = "SELECT d.blood_group, " +
                     "COUNT(dn.donation_id) AS total_donations, " +
                     "COALESCE(SUM(dn.units), 0) AS total_units " +
                     "FROM donors d " +
                     "JOIN donations dn ON d.donor_id = dn.donor_id " +
                     "GROUP BY d.blood_group " +
                     "ORDER BY d.blood_group ASC";

        List<String> report = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String line = String.format(
                    "Blood Group: %-3s | Total Donations: %d | Total Units: %d",
                    rs.getString("blood_group"),
                    rs.getInt("total_donations"),
                    rs.getInt("total_units")
                );
                report.add(line);
            }

        } catch (SQLException e) {
            throw new BloodBankException("Failed to retrieve donation summary by blood group: " + e.getMessage(), e);
        }

        return report;
    }
}
