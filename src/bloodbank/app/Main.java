package bloodbank.app;

import bloodbank.dao.DonationDAO;
import bloodbank.dao.DonorDAO;
import bloodbank.dao.RecipientDAO;
import bloodbank.dao.StockDAO;
import bloodbank.exception.BloodBankException;
import bloodbank.model.Donation;
import bloodbank.model.Donor;
import bloodbank.model.Person;
import bloodbank.model.Recipient;
import bloodbank.util.ReportWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main console application entry point for Blood Bank Donation Management.
 * 
 * Demonstrates:
 * 1. Layered Architecture: Decouples terminal UI logic from the DAO and DBMS layers.
 * 2. Polymorphism:
 *    - Compile-Time: Overloaded search methods (DonorDAO.search(group) vs DonorDAO.search(name, group)).
 *    - Runtime: Uniform handling of List<Person> calling displayDetails() via Dynamic Method Dispatch.
 * 3. Modern Java Features: Streams, Lambdas (filtering, sorting, aggregation), and Method References.
 * 4. Robust Exception Handling: Catches BloodBankException and enforces audit logging via finally blocks.
 */
public class Main {

    // Canonical set of 8 ABO/Rh blood groups for input validation
    private static final Set<String> VALID_BLOOD_GROUPS = new HashSet<>(
            Arrays.asList("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    );

    private final DonorDAO donorDAO;
    private final RecipientDAO recipientDAO;
    private final DonationDAO donationDAO;
    private final StockDAO stockDAO;
    private final Scanner scanner;

    public Main() {
        this.donorDAO = new DonorDAO();
        this.recipientDAO = new RecipientDAO();
        this.donationDAO = new DonationDAO();
        this.stockDAO = new StockDAO();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.runMenuLoop();
    }

    /**
     * Primary application loop presenting the interactive terminal menu.
     */
    public void runMenuLoop() {
        boolean running = true;
        System.out.println("================================================================================");
        System.out.println("           WELCOME TO BLOOD BANK DONATION MANAGEMENT SYSTEM                     ");
        System.out.println("================================================================================");

        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice (0-19): ", 0, 19);
            System.out.println();

            boolean operationSuccess = false;
            long startTime = System.currentTimeMillis();

            // try/catch/finally block guarantees clean error reporting and operational timing
            try {
                switch (choice) {
                    // --- Donor Management ---
                    case 1 -> addDonor();
                    case 2 -> viewAllDonors();
                    case 3 -> updateDonor();
                    case 4 -> deleteDonor();
                    case 5 -> searchDonorsByBloodGroup();
                    case 6 -> searchDonorsByNameAndGroup();

                    // --- Recipient Management ---
                    case 7 -> addRecipient();
                    case 8 -> viewAllRecipients();
                    case 9 -> updateRecipient();
                    case 10 -> deleteRecipient();

                    // --- Donation & Stock Transactions ---
                    case 11 -> recordDonation();
                    case 12 -> viewBloodStock();
                    case 13 -> issueBlood();

                    // --- Analytical Reports (JOIN / VIEW / GROUP BY) ---
                    case 14 -> showDonationHistoryReport();
                    case 15 -> showDonorSummaryReport();
                    case 16 -> showGroupWiseSummaryReport();
                    case 17 -> showIssueHistoryReport();
                    case 18 -> exportReportToFile();

                    // --- OOP Demonstration ---
                    case 19 -> runPolymorphismDemo();

                    // --- Exit ---
                    case 0 -> {
                        running = false;
                        System.out.println("Thank you for using Blood Bank Donation Management. Goodbye!");
                    }
                    default -> System.out.println("Invalid option. Please choose between 0 and 19.");
                }
                operationSuccess = true;

            } catch (BloodBankException e) { // Catch custom checked domain/DB exceptions
                System.err.printf("[BUSINESS ERROR] %s%n", e.getMessage());
            } catch (IllegalArgumentException e) { // Catch validation errors thrown by setters
                System.err.printf("[VALIDATION ERROR] %s%n", e.getMessage());
            } catch (Exception e) { // Catch unexpected runtime errors
                System.err.printf("[UNEXPECTED ERROR] An unexpected error occurred: %s%n", e.getMessage());
            } finally {
                // Meaningful finally block: Records execution metrics after every operation
                if (choice != 0) {
                    long duration = System.currentTimeMillis() - startTime;
                    System.out.printf("%n[Audit] Operation %d finished in %d ms (Status: %s)%n",
                            choice, duration, (operationSuccess ? "SUCCESS" : "FAILED / ROLLED BACK"));
                    System.out.println("--------------------------------------------------------------------------------");
                }
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                         MAIN SELECTION MENU                                    ");
        System.out.println("================================================================================");
        System.out.println(" [DONOR MANAGEMENT]");
        System.out.println("   1. Register New Donor");
        System.out.println("   2. View All Donors (Streams & Lambdas sorting/filtering)");
        System.out.println("   3. Update Donor Details");
        System.out.println("   4. Delete Donor (Demonstrates ON DELETE CASCADE to donations)");
        System.out.println("   5. Search Donors by Blood Group (Overloaded Search #1)");
        System.out.println("   6. Search Donors by Name & Blood Group (Overloaded Search #2)");
        System.out.println(" [RECIPIENT MANAGEMENT]");
        System.out.println("   7. Register New Recipient / Patient");
        System.out.println("   8. View All Recipients");
        System.out.println("   9. Update Recipient Details");
        System.out.println("  10. Delete Recipient (Demonstrates ON DELETE CASCADE to issues)");
        System.out.println(" [TRANSACTIONS & INVENTORY]");
        System.out.println("  11. Record Blood Donation (ACID Transaction + 90-Day Waiting Rule)");
        System.out.println("  12. View Blood Stock Inventory (Canonical ABO/Rh order)");
        System.out.println("  13. Issue Blood to Recipient (ACID Transaction + Pessimistic Lock)");
        System.out.println(" [ANALYTICAL DBMS REPORTS]");
        System.out.println("  14. Donation History Audit Report (SQL INNER JOIN: donors JOIN donations)");
        System.out.println("  15. Donor Summary Report (SQL Relational VIEW: donor_donation_summary)");
        System.out.println("  16. Group-Wise Donation Summary (SQL Aggregation: GROUP BY blood_group)");
        System.out.println("  17. Blood Issuance History Report (SQL JOIN: blood_issues JOIN recipients)");
        System.out.println("  18. Export Reports to .txt File (Java I/O: ReportWriter)");
        System.out.println(" [OOP DEMONSTRATIONS]");
        System.out.println("  19. Run Polymorphism Demo (List<Person> Dynamic Method Dispatch)");
        System.out.println("   0. Exit System");
        System.out.println("================================================================================");
    }

    // ========================================================================
    // 1. Donor Management Handlers
    // ========================================================================

    private void addDonor() throws BloodBankException {
        System.out.println(">>> REGISTER NEW DONOR");
        String name = readNonEmptyString("Enter donor full name: ");
        int age = readInt("Enter donor age (18 - 65): ", 18, 65);
        String bloodGroup = readBloodGroup("Enter blood group (A+, A-, B+, B-, O+, O-, AB+, AB-): ");
        String phone = readPhone("Enter phone number (10 to 15 digits): ");

        // Constructor enforces encapsulation and setter validation
        Donor donor = new Donor(0, name, age, bloodGroup, phone, null);
        donorDAO.add(donor);
        System.out.printf("[SUCCESS] Donor registered successfully with generated ID #%d!%n", donor.getId());
    }

    private void viewAllDonors() throws BloodBankException {
        System.out.println(">>> VIEW ALL DONORS");
        List<Donor> donors = donorDAO.getAll();

        if (donors.isEmpty()) {
            System.out.println("No donors currently registered in the database.");
            return;
        }

        System.out.println("Filter / Sort Options:");
        System.out.println(" 1. Display All (Default database order)");
        System.out.println(" 2. Sort by Name Alphabetically (Java Stream + Lambda)");
        System.out.println(" 3. Sort by Age Ascending (Java Stream + Comparator)");
        System.out.println(" 4. Filter by Specific Blood Group (Java Stream .filter())");
        int subChoice = readInt("Select view option (1-4): ", 1, 4);

        List<Donor> displayList = donors;

        switch (subChoice) {
            case 2 -> {
                // Demonstrates Java Streams and Lambda Comparator
                displayList = donors.stream()
                        .sorted(Comparator.comparing(Donor::getName, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
                System.out.println("[Streams] Donors sorted by Name (A-Z):");
            }
            case 3 -> {
                // Demonstrates sorting with primitive key extractor lambda
                displayList = donors.stream()
                        .sorted(Comparator.comparingInt(Donor::getAge))
                        .collect(Collectors.toList());
                System.out.println("[Streams] Donors sorted by Age (Ascending):");
            }
            case 4 -> {
                String bg = readBloodGroup("Enter blood group to filter: ");
                // Demonstrates Stream filter() with Lambda Predicate
                displayList = donors.stream()
                        .filter(d -> d.getBloodGroup().equalsIgnoreCase(bg))
                        .collect(Collectors.toList());
                System.out.printf("[Streams] Filtered donors for blood group '%s':%n", bg);
            }
            default -> System.out.println("Displaying all donors:");
        }

        System.out.println("--------------------------------------------------------------------------------");
        if (displayList.isEmpty()) {
            System.out.println("No matching donors found.");
        } else {
            // Polymorphic method call displayDetails() on each Donor instance
            displayList.forEach(Donor::displayDetails);
        }
        System.out.printf("Total Donors Displayed: %d%n", displayList.size());
    }

    private void updateDonor() throws BloodBankException {
        System.out.println(">>> UPDATE DONOR DETAILS");
        int id = readInt("Enter ID of donor to update: ", 1, Integer.MAX_VALUE);

        // Verify donor exists
        List<Donor> all = donorDAO.getAll();
        Donor existing = all.stream().filter(d -> d.getId() == id).findFirst().orElse(null);

        if (existing == null) {
            System.out.printf("[NOT FOUND] Donor with ID #%d does not exist.%n", id);
            return;
        }

        System.out.printf("Current details: %s | Age: %d | Group: %s | Phone: %s%n",
                existing.getName(), existing.getAge(), existing.getBloodGroup(), existing.getPhone());

        String newName = readNonEmptyString("Enter updated full name: ");
        int newAge = readInt("Enter updated age (18 - 65): ", 18, 65);
        String newBloodGroup = readBloodGroup("Enter updated blood group: ");
        String newPhone = readPhone("Enter updated phone number: ");

        Donor updatedDonor = new Donor(id, newName, newAge, newBloodGroup, newPhone, existing.getLastDonation());
        donorDAO.update(updatedDonor);
        System.out.printf("[SUCCESS] Donor #%d updated successfully!%n", id);
    }

    private void deleteDonor() throws BloodBankException {
        System.out.println(">>> DELETE DONOR");
        int id = readInt("Enter ID of donor to delete: ", 1, Integer.MAX_VALUE);

        System.out.println("[NOTICE] Deleting this donor will also automatically remove all their");
        System.out.println("         associated donation records via database ON DELETE CASCADE.");
        String confirm = readNonEmptyString("Are you sure you want to delete donor #" + id + "? (yes/no): ");

        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println("Deletion cancelled by user.");
            return;
        }

        boolean deleted = donorDAO.delete(id);
        if (deleted) {
            System.out.printf("[SUCCESS] Donor #%d and related donations were deleted successfully.%n", id);
        } else {
            System.out.printf("[NOT FOUND] Donor with ID #%d was not found.%n", id);
        }
    }

    private void searchDonorsByBloodGroup() throws BloodBankException {
        System.out.println(">>> SEARCH DONORS BY BLOOD GROUP (Overloaded Method 1)");
        String bg = readBloodGroup("Enter blood group to search: ");

        // Calls overloaded search(String bloodGroup) demonstrating Compile-Time Polymorphism
        List<Donor> results = donorDAO.search(bg);

        System.out.println("--------------------------------------------------------------------------------");
        if (results.isEmpty()) {
            System.out.printf("No donors found with blood group '%s'.%n", bg);
        } else {
            results.forEach(Donor::displayDetails);
        }
        System.out.printf("Matching donors found: %d%n", results.size());
    }

    private void searchDonorsByNameAndGroup() throws BloodBankException {
        System.out.println(">>> SEARCH DONORS BY NAME & BLOOD GROUP (Overloaded Method 2)");
        String name = readNonEmptyString("Enter name (or partial name): ");
        String bg = readBloodGroup("Enter blood group: ");

        // Calls overloaded search(String name, String bloodGroup) demonstrating Compile-Time Polymorphism
        List<Donor> results = donorDAO.search(name, bg);

        System.out.println("--------------------------------------------------------------------------------");
        if (results.isEmpty()) {
            System.out.printf("No donors found matching name '%s' and blood group '%s'.%n", name, bg);
        } else {
            results.forEach(Donor::displayDetails);
        }
        System.out.printf("Matching donors found: %d%n", results.size());
    }

    // ========================================================================
    // 2. Recipient Management Handlers
    // ========================================================================

    private void addRecipient() throws BloodBankException {
        System.out.println(">>> REGISTER NEW RECIPIENT / PATIENT");
        String name = readNonEmptyString("Enter recipient/patient name: ");
        int age = readInt("Enter recipient age (0 - 120): ", 0, 120);
        String bloodGroup = readBloodGroup("Enter required blood group: ");
        String phone = readPhone("Enter contact phone number: ");
        String hospital = readNonEmptyString("Enter admitting hospital name: ");

        Recipient recipient = new Recipient(0, name, age, bloodGroup, phone, hospital);
        recipientDAO.add(recipient);
        System.out.printf("[SUCCESS] Recipient registered successfully with generated ID #%d!%n", recipient.getId());
    }

    private void viewAllRecipients() throws BloodBankException {
        System.out.println(">>> VIEW ALL RECIPIENTS");
        List<Recipient> recipients = recipientDAO.getAll();

        System.out.println("--------------------------------------------------------------------------------");
        if (recipients.isEmpty()) {
            System.out.println("No recipients currently registered in the database.");
        } else {
            // Polymorphic displayDetails() call for Recipient
            recipients.forEach(Recipient::displayDetails);
        }
        System.out.printf("Total Recipients: %d%n", recipients.size());
    }

    private void updateRecipient() throws BloodBankException {
        System.out.println(">>> UPDATE RECIPIENT DETAILS");
        int id = readInt("Enter ID of recipient to update: ", 1, Integer.MAX_VALUE);

        List<Recipient> all = recipientDAO.getAll();
        Recipient existing = all.stream().filter(r -> r.getId() == id).findFirst().orElse(null);

        if (existing == null) {
            System.out.printf("[NOT FOUND] Recipient with ID #%d does not exist.%n", id);
            return;
        }

        System.out.printf("Current details: %s | Age: %d | Group: %s | Hospital: %s%n",
                existing.getName(), existing.getAge(), existing.getBloodGroup(), existing.getHospital());

        String newName = readNonEmptyString("Enter updated full name: ");
        int newAge = readInt("Enter updated age (0 - 120): ", 0, 120);
        String newBloodGroup = readBloodGroup("Enter updated blood group: ");
        String newPhone = readPhone("Enter updated phone number: ");
        String newHospital = readNonEmptyString("Enter updated hospital name: ");

        Recipient updated = new Recipient(id, newName, newAge, newBloodGroup, newPhone, newHospital);
        recipientDAO.update(updated);
        System.out.printf("[SUCCESS] Recipient #%d updated successfully!%n", id);
    }

    private void deleteRecipient() throws BloodBankException {
        System.out.println(">>> DELETE RECIPIENT");
        int id = readInt("Enter ID of recipient to delete: ", 1, Integer.MAX_VALUE);

        System.out.println("[NOTICE] Deleting this recipient will also remove all their historical");
        System.out.println("         blood issuance records via database ON DELETE CASCADE.");
        String confirm = readNonEmptyString("Are you sure you want to delete recipient #" + id + "? (yes/no): ");

        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println("Deletion cancelled by user.");
            return;
        }

        boolean deleted = recipientDAO.delete(id);
        if (deleted) {
            System.out.printf("[SUCCESS] Recipient #%d and related issuance records deleted successfully.%n", id);
        } else {
            System.out.printf("[NOT FOUND] Recipient with ID #%d was not found.%n", id);
        }
    }

    // ========================================================================
    // 3. Transactions & Inventory Handlers
    // ========================================================================

    private void recordDonation() throws BloodBankException {
        System.out.println(">>> RECORD BLOOD DONATION (ACID Transaction)");
        int donorId = readInt("Enter registered donor ID: ", 1, Integer.MAX_VALUE);
        int units = readInt("Enter units of blood donated (1 - 5): ", 1, 5);

        System.out.println("[Transaction] Initiating donation workflow:");
        System.out.println("  1. Verifying donor existence.");
        System.out.println("  2. Checking statutory 90-day interval requirement.");
        System.out.println("  3. Inserting donation record and atomically updating blood stock.");

        // Executes single ACID transaction in DonationDAO
        int donationId = donationDAO.recordDonation(donorId, units);
        System.out.printf("[SUCCESS] Donation #%d successfully recorded! Blood stock inventory incremented by %d units.%n",
                donationId, units);
    }

    private void viewBloodStock() throws BloodBankException {
        System.out.println(">>> BLOOD STOCK INVENTORY (Canonical Order)");
        Map<String, Integer> stock = stockDAO.getStock();

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf(" %-12s | %-12s | %-15s%n", "Blood Group", "Units Available", "Status");
        System.out.println("--------------------------------------------------------------------------------");

        // Uses Java Stream mapToInt().sum() to aggregate total units across all groups
        int totalUnits = stock.values().stream().mapToInt(Integer::intValue).sum();

        stock.forEach((group, units) -> {
            String status = units == 0 ? "CRITICAL (EMPTY)" : (units < 3 ? "LOW STOCK" : "ADEQUATE");
            System.out.printf(" %-12s | %-15d | %-15s%n", group, units, status);
        });

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("TOTAL INVENTORY ACROSS ALL GROUPS: %d Units%n", totalUnits);
    }

    private void issueBlood() throws BloodBankException {
        System.out.println(">>> ISSUE BLOOD TO RECIPIENT (ACID Transaction with Pessimistic Locking)");
        int recipientId = readInt("Enter registered recipient ID: ", 1, Integer.MAX_VALUE);
        int units = readInt("Enter number of units required: ", 1, 50);

        System.out.println("[Transaction] Starting blood issuance with row-level lock (FOR UPDATE)...");
        System.out.println("  1. Checking recipient details and blood group.");
        System.out.println("  2. Locking inventory row to prevent concurrent overselling.");
        System.out.println("  3. Decrementing stock & logging issuance, OR rolling back on insufficient stock.");

        // Executes transactional issue in StockDAO
        int issueId = stockDAO.issueBlood(recipientId, units);
        System.out.printf("[SUCCESS] Blood Issue #%d processed successfully! %d units disbursed from inventory.%n",
                issueId, units);
    }

    // ========================================================================
    // 4. Analytical DBMS Reports Handlers
    // ========================================================================

    private void showDonationHistoryReport() throws BloodBankException {
        System.out.println(">>> DONATION HISTORY AUDIT REPORT");
        System.out.println("    [SQL Engine: Multi-Table INNER JOIN (donors JOIN donations)]");
        System.out.println("--------------------------------------------------------------------------------");
        List<String> report = donorDAO.getDonationHistory();

        if (report.isEmpty()) {
            System.out.println("No donations recorded yet.");
        } else {
            report.forEach(System.out::println);
        }
        System.out.printf("Total donation transaction records: %d%n", report.size());
    }

    private void showDonorSummaryReport() throws BloodBankException {
        System.out.println(">>> DONOR DONATION SUMMARY REPORT");
        System.out.println("    [SQL Engine: Relational VIEW donor_donation_summary (LEFT JOIN + GROUP BY)]");
        System.out.println("--------------------------------------------------------------------------------");
        List<String> report = donorDAO.getDonorSummary();

        if (report.isEmpty()) {
            System.out.println("No donors found in the summary view.");
        } else {
            report.forEach(System.out::println);
        }
        System.out.printf("Total donors summarized in view: %d%n", report.size());
    }

    private void showGroupWiseSummaryReport() throws BloodBankException {
        System.out.println(">>> GROUP-WISE DONATION SUMMARY");
        System.out.println("    [SQL Engine: Aggregation with GROUP BY blood_group (COUNT & SUM)]");
        System.out.println("--------------------------------------------------------------------------------");
        List<String> report = stockDAO.getDonationSummaryByGroup();

        if (report.isEmpty()) {
            System.out.println("No donation data available for group-wise aggregation.");
        } else {
            report.forEach(System.out::println);
        }
    }

    private void showIssueHistoryReport() throws BloodBankException {
        System.out.println(">>> BLOOD ISSUANCE HISTORY REPORT");
        System.out.println("    [SQL Engine: Relational JOIN (blood_issues JOIN recipients)]");
        System.out.println("--------------------------------------------------------------------------------");
        List<String> report = stockDAO.getIssueHistory();

        if (report.isEmpty()) {
            System.out.println("No blood disbursements recorded yet.");
        } else {
            report.forEach(System.out::println);
        }
        System.out.printf("Total issuance records: %d%n", report.size());
    }

    private void exportReportToFile() throws BloodBankException {
        System.out.println(">>> EXPORT REPORT TO FILE (util.ReportWriter)");
        System.out.println("Select report to export:");
        System.out.println(" 1. Donation History Audit Report (INNER JOIN)");
        System.out.println(" 2. Donor Summary Report (Relational VIEW)");
        System.out.println(" 3. Group-Wise Donation Summary (GROUP BY)");
        System.out.println(" 4. Blood Issue History Report (JOIN)");
        int choice = readInt("Enter choice (1-4): ", 1, 4);

        String title;
        String fileName;
        List<String> data;

        switch (choice) {
            case 1 -> {
                title = "Donation History Audit Report (INNER JOIN)";
                fileName = "donation_history_report.txt";
                data = donorDAO.getDonationHistory();
            }
            case 2 -> {
                title = "Donor Summary Report (Relational VIEW)";
                fileName = "donor_summary_report.txt";
                data = donorDAO.getDonorSummary();
            }
            case 3 -> {
                title = "Group-Wise Donation Summary (GROUP BY)";
                fileName = "groupwise_summary_report.txt";
                data = stockDAO.getDonationSummaryByGroup();
            }
            case 4 -> {
                title = "Blood Issuance History Report (JOIN)";
                fileName = "blood_issue_history_report.txt";
                data = stockDAO.getIssueHistory();
            }
            default -> throw new BloodBankException("Invalid report selection.");
        }

        String path = ReportWriter.writeReport(title, data, fileName);
        System.out.printf("[EXPORT COMPLETE] Report saved successfully to:%n%s%n", path);
    }

    // ========================================================================
    // 5. Polymorphism Live Demonstration
    // ========================================================================

    private void runPolymorphismDemo() throws BloodBankException {
        System.out.println(">>> RUNTIME POLYMORPHISM & DYNAMIC METHOD DISPATCH DEMONSTRATION");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Concept Explanation:");
        System.out.println("  1. We declare a single generic collection of the abstract superclass: List<Person>.");
        System.out.println("  2. We populate it with both Donor and Recipient objects (Upcasting).");
        System.out.println("  3. We iterate over the list and invoke person.displayDetails() and person.getRole().");
        System.out.println("  4. At runtime, the JVM determines the actual underlying object type and dynamically");
        System.out.println("     executes the overridden method of that specific subclass (Dynamic Method Dispatch).");
        System.out.println("--------------------------------------------------------------------------------");

        List<Donor> donors = donorDAO.getAll();
        List<Recipient> recipients = recipientDAO.getAll();

        // Unified polymorphic collection containing different subclasses of Person
        List<Person> participants = new ArrayList<>();
        participants.addAll(donors);
        participants.addAll(recipients);

        if (participants.isEmpty()) {
            System.out.println("No donors or recipients in database to demonstrate polymorphism.");
            System.out.println("Creating in-memory sample objects for demonstration:");
            participants.add(new Donor(901, "Dr. Alice Smith", 32, "O+", "9876543210", "2026-05-10"));
            participants.add(new Recipient(902, "Bob Miller", 45, "A-", "9123456789", "General Hospital"));
            participants.add(new Donor(903, "Charlie Davis", 24, "B+", "9988776655", null));
        }

        System.out.printf("Iterating through List<Person> (%d items):%n%n", participants.size());

        for (Person person : participants) { // Person reference variable
            // person.getRole() and person.displayDetails() resolve dynamically at runtime
            System.out.printf("[%s] -> ", person.getRole());
            person.displayDetails();
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("[SUCCESS] Polymorphism demonstration complete.");
    }

    // ========================================================================
    // 6. Robust Input Validation Helpers
    // ========================================================================

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Please enter a valid integer.");
            }
        }
    }

    private String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Input cannot be empty. Please enter a value.");
        }
    }

    private String readBloodGroup(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            if (VALID_BLOOD_GROUPS.contains(input)) {
                return input;
            }
            System.out.println("Invalid blood group! Must be one of: A+, A-, B+, B-, O+, O-, AB+, AB-");
        }
    }

    private String readPhone(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches("^\\+?[0-9]{10,15}$")) {
                return input;
            }
            System.out.println("Invalid phone format! Must contain 10 to 15 digits (optional leading '+').");
        }
    }
}
