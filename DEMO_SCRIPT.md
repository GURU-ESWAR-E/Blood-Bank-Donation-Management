# Live Evaluation Demonstration Script

This script provides an exact turn-by-turn sequence for presenting the Blood Bank Donation Management application to your teacher or evaluator in **5 to 10 minutes**.

Follow the steps in order:

---

## Pre-Demo Setup (Before Calling the Evaluator)

1. Open PowerShell in the project directory:
   ```powershell
   cd "C:\Users\Guru Eswar E\Downloads\Blood-Bank-Donation-Management"
   ```
2. Verify that `db.properties` exists and the automated smoke test passes:
   ```powershell
   java -cp "out;lib/*" bloodbank.test.DaoSmokeTest
   ```
   *(Ensure it prints `12 PASSED, 0 FAILED`)*
3. Launch the application:
   ```powershell
   java -cp "out;lib/*" bloodbank.app.Main
   ```

---

## Step-by-Step Demo Sequence

### Step 1: Add a Donor (Encapsulation & Validation)
- **Menu Choice**: `1`
- **Input Values**:
  - Full Name: `Alice Johnson`
  - Age: `28` *(Teacher tip: enter `15` first to show age validation error, then enter `28`)*
  - Blood Group: `O+`
  - Phone: `9876543210`
- **Expected Output**:
  `[SUCCESS] Donor registered successfully with generated ID #1!`
- **What to say to your teacher**:
  > *"Here we demonstrate Encapsulation and Data Validation. All fields in our abstract `Person` class are `private`. When `setAge()` or `setBloodGroup()` runs, it validates that the donor is within the statutory blood donation age bracket (18 to 65) and belongs to one of the 8 canonical ABO/Rh groups. If invalid input is provided, an `IllegalArgumentException` is caught defensively before any SQL query is dispatched."*

---

### Step 2: View and Update Donor (Streams, Lambdas & Overloaded Search)
1. **View Donors**:
   - **Menu Choice**: `2`
   - Select option: `2` *(Sort by Name via Java Streams)*
   - **What to say**:
     > *"Notice that we use Java Streams and Lambda expressions (`donors.stream().sorted(Comparator.comparing(Donor::getName))`) to declaratively sort and filter records without imperative loops."*
2. **Search by Blood Group (Compile-Time Polymorphism / Method Overloading)**:
   - **Menu Choice**: `5`
   - Blood Group: `O+`
   - **What to say**:
     > *"This calls `DonorDAO.search(bloodGroup)`. In Option 6, we have the overloaded method `DonorDAO.search(name, bloodGroup)`. Both methods share the same name, demonstrated compile-time polymorphism."*
3. **Update Donor**:
   - **Menu Choice**: `3`
   - Donor ID: `1`
   - Name: `Alice Johnson`
   - Age: `29` *(Updated age)*
   - Blood Group: `O+`
   - Phone: `9876543210`
   - **Expected Output**: `[SUCCESS] Donor #1 updated successfully!`

---

### Step 3: Record Blood Donation (ACID Transaction & Stock Increment)
- **Menu Choice**: `12` *(View stock before donation)*
  - Notice `O+` has `0` units available.
- **Menu Choice**: `11` *(Record Blood Donation)*
- **Input Values**:
  - Donor ID: `1`
  - Units Donated: `2`
- **Expected Output**:
  `[SUCCESS] Donation #1 successfully recorded! Blood stock inventory incremented by 2 units.`
- **Check Stock**:
  - **Menu Choice**: `12`
  - Notice `O+` inventory has now increased from `0` to `2` units.
- **What to say to your teacher**:
  > *"This demonstrates an ACID transaction in JDBC. In `DonationDAO.recordDonation()`, we execute `setAutoCommit(false)`. We insert a record into the `donations` table and update the `blood_stock` table inside a single transaction. Both operations succeed together or both roll back upon failure, guaranteeing Atomicity and Consistency."*

---

### Step 4: Immediate Second Donation (Enforcing 90-Day Medical Rule)
- **Menu Choice**: `11` *(Try to record another donation for the same donor immediately)*
- **Input Values**:
  - Donor ID: `1`
  - Units Donated: `1`
- **Expected Output**:
  `[BUSINESS ERROR] Donor #1 is not eligible to donate yet. A minimum 90-day gap is required between donations. Days elapsed: 0, days remaining: 90 (Last donation was on YYYY-MM-DD).`
- **What to say to your teacher**:
  > *"Here our domain logic prevents medical over-donation. The DAO executes an SQL aggregate query `SELECT MAX(donation_date) FROM donations WHERE donor_id = ?`, calculates the interval in days, and throws our custom checked `BloodBankException`. The UI catches it and displays a clear message without crashing."*

---

### Step 5: Add Recipient & Issue Blood (Pessimistic Locking `FOR UPDATE`)
1. **Register Recipient**:
   - **Menu Choice**: `7`
   - Recipient Name: `Robert Miller`
   - Age: `45`
   - Blood Group: `O+`
   - Phone: `8877665544`
   - Hospital: `City General Hospital`
   - **Expected Output**: `[SUCCESS] Recipient registered successfully with generated ID #1!`
2. **Issue Blood**:
   - **Menu Choice**: `13`
   - Recipient ID: `1`
   - Units Required: `1`
- **Expected Output**:
  `[SUCCESS] Blood Issue #1 processed successfully! 1 units disbursed from inventory.`
- **Check Stock**:
  - **Menu Choice**: `12`
  - Notice `O+` inventory decreased from `2` down to `1` unit.
- **What to say to your teacher**:
  > *"When issuing blood, `StockDAO.issueBlood()` acquires a row-level lock on the `blood_stock` table using MySQL's `SELECT ... FOR UPDATE` (Pessimistic Locking). This prevents race conditions and ensures concurrent transactions cannot allocate the same blood bag twice."*

---

### Step 6: Over-Issue Attempt (Transaction Rollback Verification)
- **Menu Choice**: `13` *(Attempt to issue more units than exist)*
- **Input Values**:
  - Recipient ID: `1`
  - Units Required: `5` *(Only 1 unit is available!)*
- **Expected Output**:
  `[BUSINESS ERROR] Insufficient stock for O+. Available: 1, requested: 5`
  `[Audit] Operation 13 finished in X ms (Status: FAILED / ROLLED BACK)`
- **Check Stock**:
  - **Menu Choice**: `12`
  - Verify `O+` units are **still safely preserved at 1**!
- **What to say to your teacher**:
  > *"Because available inventory was less than requested units, the transaction immediately executed `conn.rollback()` before restoring auto-commit. The inventory was never deducted, preserving database consistency."*

---

### Step 7: Polymorphism Demonstration + Reports + File Export
1. **Runtime Polymorphism Live Demo**:
   - **Menu Choice**: `19`
   - **Expected Output**: Shows a unified `List<Person>` containing both `Donor` and `Recipient` objects iterating and invoking `displayDetails()`.
   - **What to say**:
     > *"This is Runtime Polymorphism via Dynamic Method Dispatch. We store both `Donor` and `Recipient` objects inside a single `List<Person>`. When we call `person.displayDetails()`, the JVM determines the actual object type at runtime and dispatches to the correct subclass implementation."*

2. **Donation History Report (SQL INNER JOIN)**:
   - **Menu Choice**: `14`
   - Shows donor identity joined with donation transactions:
     `SELECT d.name, d.blood_group, dn.donation_date, dn.units FROM donors d JOIN donations dn ON d.donor_id = dn.donor_id`

3. **Donor Summary Report (Relational VIEW)**:
   - **Menu Choice**: `15`
   - Queries `donor_donation_summary`, displaying aggregated donations and units using `LEFT JOIN` and `GROUP BY`.

4. **Export Report to File (Java File I/O)**:
   - **Menu Choice**: `18`
   - Choose `1` *(Donation History)*
   - **Expected Output**: `[ReportWriter] Successfully exported report 'Donation History Audit Report (INNER JOIN)' to: reports\donation_history_report.txt`
   - Show the file created inside the `reports/` folder.

---

### Step 8: Delete Donor (DBMS `ON DELETE CASCADE`)
- **Menu Choice**: `4` *(Delete Donor)*
- **Input Values**:
  - Donor ID: `1`
  - Confirm: `yes`
- **Expected Output**: `[SUCCESS] Donor #1 and related donations were deleted successfully.`
- **Verify Cascade Deletion in Reports**:
  - **Menu Choice**: `14` *(View Donation History)*
  - Notice the list is now empty!
- **What to say to your teacher**:
  > *"In our relational schema, the foreign key in `donations` was defined with `ON DELETE CASCADE`. When Donor #1 was deleted from the parent `donors` table, MySQL automatically cascade-deleted their corresponding rows in the child `donations` table. This prevents orphan records and preserves Third Normal Form (3NF) integrity."*

---

### Step 9: Exit System
- **Menu Choice**: `0`
- **Expected Output**:
  `Thank you for using Blood Bank Donation Management. Goodbye!`

---

## Summary of Evaluator Checklist

| Checkpoint | Tested In | Concept Proven |
| :--- | :--- | :--- |
| **Encapsulation & Validation** | Step 1 | `Person` private fields, validated setters, checked bounds |
| **Streams & Lambdas** | Step 2 | `.stream().sorted()`, `.filter()`, method references |
| **Overloaded Search** | Step 2 | Compile-Time Polymorphism in `DonorDAO` |
| **ACID Transaction** | Step 3 | `DonationDAO.recordDonation()` (Insert + Stock Update) |
| **Medical Rule Enforcement** | Step 4 | 90-day donation interval restriction |
| **Pessimistic Locking** | Step 5 | `SELECT ... FOR UPDATE` row lock in `StockDAO` |
| **Transaction Rollback** | Step 6 | Inventory preserved on over-issue |
| **Runtime Polymorphism** | Step 7 | `List<Person>` dynamic method dispatch |
| **Analytical Reporting** | Step 7 | SQL INNER JOIN, Relational VIEW, GROUP BY |
| **Java File I/O** | Step 7 | `ReportWriter.java` text file generation |
| **ON DELETE CASCADE** | Step 8 | Referential integrity child row cascade |
