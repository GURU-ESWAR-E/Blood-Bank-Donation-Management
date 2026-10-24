package bloodbank.test;

import bloodbank.dao.DonationDAO;
import bloodbank.dao.DonorDAO;
import bloodbank.dao.RecipientDAO;
import bloodbank.dao.StockDAO;
import bloodbank.exception.BloodBankException;
import bloodbank.model.Donor;
import bloodbank.model.Recipient;
import bloodbank.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * End-to-end Smoke Test for BloodBank Database Layer.
 * Tests full CRUD lifecycle, overloaded search, transactional donation recording,
 * 90-day donation interval restriction, transactional blood issue with rollback,
 * analytical JOIN/VIEW/GROUP BY reports, and leaves zero leftover test data.
 */
public class DaoSmokeTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("         BLOOD BANK DATABASE LAYER - REAL SMOKE TEST           ");
        System.out.println("================================================================");

        DonorDAO donorDAO = new DonorDAO();
        RecipientDAO recipientDAO = new RecipientDAO();
        DonationDAO donationDAO = new DonationDAO();
        StockDAO stockDAO = new StockDAO();

        String testBloodGroup = "O+";
        int initialStockForGroup = 0;

        int tempDonorId = -1;
        int tempRecipientId = -1;

        // Unique phone suffixes to prevent conflicts
        long timestampSuffix = System.currentTimeMillis() % 100000000L;
        String donorPhone = "9" + String.format("%09d", timestampSuffix);
        String recipientPhone = "8" + String.format("%09d", timestampSuffix);

        try {
            // ----------------------------------------------------------------
            // Step 1: Connect to Database
            // ----------------------------------------------------------------
            System.out.print("[TEST 01] Verifying MySQL Database Connection... ");
            try (Connection conn = DBConnection.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    pass("Connected successfully to " + conn.getMetaData().getDatabaseProductName() +
                         " " + conn.getMetaData().getDatabaseProductVersion());
                } else {
                    fail("Connection was null or closed.");
                    return;
                }
            } catch (SQLException | BloodBankException e) {
                fail("Database connection failed: " + e.getMessage());
                System.err.println("\n[ABORT] Cannot proceed without an active MySQL connection.");
                return;
            }

            // Record initial stock for teardown accuracy
            try {
                Map<String, Integer> initialStock = stockDAO.getStock();
                initialStockForGroup = initialStock.getOrDefault(testBloodGroup, 0);
            } catch (BloodBankException e) {
                fail("Failed to read initial stock: " + e.getMessage());
                return;
            }

            // ----------------------------------------------------------------
            // Step 2: Add Temporary Donor
            // ----------------------------------------------------------------
            System.out.print("[TEST 02] Adding temporary donor ('TEMP_TEST_DONOR')... ");
            Donor newDonor = new Donor(0, "TEMP_TEST_DONOR", 28, testBloodGroup, donorPhone, null);
            try {
                donorDAO.add(newDonor);
                tempDonorId = newDonor.getId();
                if (tempDonorId > 0) {
                    pass("Donor created with generated ID: " + tempDonorId);
                } else {
                    fail("Donor created but ID was not populated.");
                }
            } catch (BloodBankException e) {
                fail("Error adding donor: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 3: Read Donor
            // ----------------------------------------------------------------
            System.out.print("[TEST 03] Reading all donors and verifying created donor... ");
            try {
                List<Donor> allDonors = donorDAO.getAll();
                Donor found = null;
                for (Donor d : allDonors) {
                    if (d.getId() == tempDonorId) {
                        found = d;
                        break;
                    }
                }
                if (found != null && "TEMP_TEST_DONOR".equals(found.getName()) &&
                    testBloodGroup.equals(found.getBloodGroup())) {
                    pass("Donor retrieved and verified: " + found.getName() + " (" + found.getBloodGroup() + ")");
                } else {
                    fail("Donor with ID " + tempDonorId + " not found or data mismatched.");
                }
            } catch (BloodBankException e) {
                fail("Error reading donors: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 4: Update Donor
            // ----------------------------------------------------------------
            System.out.print("[TEST 04] Updating donor details (name & age)... ");
            try {
                Donor updated = new Donor(tempDonorId, "TEMP_TEST_DONOR_UPDATED", 29, testBloodGroup, donorPhone, null);
                donorDAO.update(updated);

                List<Donor> searchList = donorDAO.search("TEMP_TEST_DONOR_UPDATED", testBloodGroup);
                if (!searchList.isEmpty() && searchList.get(0).getAge() == 29) {
                    pass("Donor updated successfully to 'TEMP_TEST_DONOR_UPDATED', age 29.");
                } else {
                    fail("Donor update verification failed.");
                }
            } catch (BloodBankException e) {
                fail("Error updating donor: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 5: Overloaded Search Methods (Polymorphism)
            // ----------------------------------------------------------------
            System.out.print("[TEST 05] Testing overloaded search methods... ");
            final int donorIdCheck = tempDonorId;
            try {
                // Overload 1: search(bloodGroup)
                List<Donor> byGroup = donorDAO.search(testBloodGroup);
                boolean foundInGroup = byGroup.stream().anyMatch(d -> d.getId() == donorIdCheck);

                // Overload 2: search(name, bloodGroup)
                List<Donor> byNameAndGroup = donorDAO.search("UPDATED", testBloodGroup);
                boolean foundInNameGroup = byNameAndGroup.stream().anyMatch(d -> d.getId() == donorIdCheck);

                if (foundInGroup && foundInNameGroup) {
                    pass("Both overloaded search methods returned the expected donor.");
                } else {
                    fail("Search overload verification failed. Group search: " + foundInGroup +
                         ", Name+Group search: " + foundInNameGroup);
                }
            } catch (BloodBankException e) {
                fail("Error during search: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 6: Record Donation (ACID Transaction & Stock Increment)
            // ----------------------------------------------------------------
            System.out.print("[TEST 06] Transaction 1: Recording donation of 2 units... ");
            try {
                int initialStockBeforeDonation = stockDAO.getStock().getOrDefault(testBloodGroup, 0);
                int donationId = donationDAO.recordDonation(tempDonorId, 2);
                int stockAfter = stockDAO.getStock().getOrDefault(testBloodGroup, 0);

                if (donationId > 0 && stockAfter == initialStockBeforeDonation + 2) {
                    pass("Donation #" + donationId + " recorded. Stock increased from " +
                         initialStockBeforeDonation + " to " + stockAfter);
                } else {
                    fail("Donation recorded but stock count invalid. Expected: " +
                         (initialStockBeforeDonation + 2) + ", Actual: " + stockAfter);
                }
            } catch (BloodBankException e) {
                fail("Transaction failed: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 7: Enforce 90-Day Waiting Period (Must FAIL)
            // ----------------------------------------------------------------
            System.out.print("[TEST 07] Enforcing 90-day donation interval restriction... ");
            try {
                donationDAO.recordDonation(tempDonorId, 1);
                fail("Violation! Second immediate donation was allowed when it should have been blocked.");
            } catch (BloodBankException e) {
                if (e.getMessage().contains("90-day")) {
                    pass("Expected rejection caught: " + e.getMessage());
                } else {
                    fail("Exception occurred but did not mention 90-day restriction: " + e.getMessage());
                }
            }

            // ----------------------------------------------------------------
            // Step 8: Add Temporary Recipient
            // ----------------------------------------------------------------
            System.out.print("[TEST 08] Adding temporary recipient ('TEMP_TEST_RECIPIENT')... ");
            Recipient newRecipient = new Recipient(0, "TEMP_TEST_RECIPIENT", 35, testBloodGroup, recipientPhone, "Metro Hospital");
            try {
                recipientDAO.add(newRecipient);
                tempRecipientId = newRecipient.getId();
                if (tempRecipientId > 0) {
                    pass("Recipient created with generated ID: " + tempRecipientId);
                } else {
                    fail("Recipient created but ID not populated.");
                }
            } catch (BloodBankException e) {
                fail("Error adding recipient: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 9: Issue More Units Than Stock (Must FAIL & Rollback)
            // ----------------------------------------------------------------
            System.out.print("[TEST 09] Transaction 2: Attempting over-issue (insufficient stock rollback)... ");
            try {
                int stockBeforeOverIssue = stockDAO.getStock().getOrDefault(testBloodGroup, 0);
                try {
                    stockDAO.issueBlood(tempRecipientId, stockBeforeOverIssue + 100);
                    fail("Violation! Over-issue succeeded when stock was insufficient.");
                } catch (BloodBankException e) {
                    int stockAfterAttempt = stockDAO.getStock().getOrDefault(testBloodGroup, 0);
                    if (stockAfterAttempt == stockBeforeOverIssue && e.getMessage().contains("Insufficient stock")) {
                        pass("Transaction safely rolled back. Stock preserved at " + stockAfterAttempt +
                             ". Reason: " + e.getMessage());
                    } else {
                        fail("Rollback failed or stock was altered. Stock before: " + stockBeforeOverIssue +
                             ", after: " + stockAfterAttempt);
                    }
                }
            } catch (BloodBankException e) {
                fail("Error during over-issue test: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 10: Issue Valid Units (Stock Decreases)
            // ----------------------------------------------------------------
            System.out.print("[TEST 10] Transaction 2: Issuing 1 valid unit to recipient... ");
            try {
                int stockBeforeValidIssue = stockDAO.getStock().getOrDefault(testBloodGroup, 0);
                int issueId = stockDAO.issueBlood(tempRecipientId, 1);
                int stockAfterValidIssue = stockDAO.getStock().getOrDefault(testBloodGroup, 0);

                if (issueId > 0 && stockAfterValidIssue == stockBeforeValidIssue - 1) {
                    pass("Issue #" + issueId + " processed. Stock decreased from " +
                         stockBeforeValidIssue + " to " + stockAfterValidIssue);
                } else {
                    fail("Issue succeeded but stock count invalid. Expected: " +
                         (stockBeforeValidIssue - 1) + ", Actual: " + stockAfterValidIssue);
                }
            } catch (BloodBankException e) {
                fail("Valid issue failed: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 11: Execute JOIN, VIEW, and GROUP BY Queries
            // ----------------------------------------------------------------
            System.out.println("[TEST 11] Executing Analytical Reports:");
            try {
                System.out.println("  --- 11.a Inner Join (Donation History):");
                List<String> history = donorDAO.getDonationHistory();
                history.stream().limit(2).forEach(line -> System.out.println("      " + line));

                System.out.println("  --- 11.b Database View (donor_donation_summary):");
                List<String> summary = donorDAO.getDonorSummary();
                summary.stream().limit(2).forEach(line -> System.out.println("      " + line));

                System.out.println("  --- 11.c Join (Blood Issue History):");
                List<String> issues = stockDAO.getIssueHistory();
                issues.stream().limit(2).forEach(line -> System.out.println("      " + line));

                System.out.println("  --- 11.d Group By Query (Donation Summary by Blood Group):");
                List<String> groupSummary = stockDAO.getDonationSummaryByGroup();
                groupSummary.stream().limit(2).forEach(line -> System.out.println("      " + line));

                pass("All 4 analytical queries (JOIN, VIEW, GROUP BY) executed successfully.");
            } catch (BloodBankException e) {
                fail("Analytical report query failed: " + e.getMessage());
            }

        } finally {
            // ----------------------------------------------------------------
            // Step 12: Complete Teardown and Cleanup (Leaves ZERO Leftover Rows)
            // ----------------------------------------------------------------
            boolean cleanPass = true;
            if (tempDonorId > 0 || tempRecipientId > 0) {
                System.out.print("[TEST 12] Teardown: Deleting temporary data and restoring initial stock... ");

                try {
                    // Delete recipient (cascades blood_issues)
                    if (tempRecipientId > 0) {
                        recipientDAO.delete(tempRecipientId);
                    }

                    // Delete donor (cascades donations)
                    if (tempDonorId > 0) {
                        donorDAO.delete(tempDonorId);
                    }

                    // Restore stock back to initial value
                    try (Connection conn = DBConnection.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("UPDATE blood_stock SET units = ? WHERE blood_group = ?")) {
                        stmt.setInt(1, initialStockForGroup);
                        stmt.setString(2, testBloodGroup);
                        stmt.executeUpdate();
                    }

                    // Verify clean state
                    int finalStock = stockDAO.getStock().getOrDefault(testBloodGroup, -1);
                    List<Donor> leftoverDonors = donorDAO.search("TEMP_TEST_DONOR", null);

                    if (finalStock == initialStockForGroup && leftoverDonors.isEmpty()) {
                        pass("Teardown verified! Temporary donor/recipient removed, stock restored to " +
                             finalStock + " units. Zero leftover rows.");
                    } else {
                        fail("Teardown incomplete. Stock: " + finalStock + " (expected " +
                             initialStockForGroup + "), Leftover donors: " + leftoverDonors.size());
                        cleanPass = false;
                    }

                } catch (Exception e) {
                    fail("Error during teardown: " + e.getMessage());
                    cleanPass = false;
                }

                if (!cleanPass) {
                    testsFailed++;
                }
            }

            System.out.println("================================================================");
            System.out.printf("SMOKE TEST RESULTS: %d PASSED, %d FAILED%n", testsPassed, testsFailed);
            System.out.println("================================================================");

            if (testsFailed > 0 || !cleanPass) {
                System.exit(1);
            }
        }
    }

    private static void pass(String msg) {
        testsPassed++;
        System.out.println("PASS -> " + msg);
    }

    private static void fail(String msg) {
        testsFailed++;
        System.out.println("FAIL -> " + msg);
    }
}
