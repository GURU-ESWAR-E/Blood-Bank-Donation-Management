# Blood Bank Donation Management System

An enterprise-grade, console-based Java application managing blood donations, inventory stock, patient issuances, and analytical DBMS reporting.

Built using **Java 21/17**, **JDBC**, and **MySQL 8.0 (InnoDB)** adhering to **Third Normal Form (3NF)** and core **Object-Oriented Programming (OOP)** principles.

---

## 1. System Architecture & Tech Stack

- **Language & Runtime**: Java 17+ (Tested on Java 21 LTS)
- **Database**: MySQL Server 8.0+ (InnoDB Storage Engine for ACID compliance)
- **Driver**: MySQL Connector/J 8.3.0 (located in `lib/`)
- **Architecture**: 3-Tier Layered Architecture:
  1. **Presentation Layer**: `bloodbank.app.Main` (Terminal UI with Scanner and input validators)
  2. **Data Access Layer (DAO)**: `bloodbank.dao.*` (`DonorDAO`, `RecipientDAO`, `DonationDAO`, `StockDAO`)
  3. **Domain Model Layer**: `bloodbank.model.*` (`Person`, `Donor`, `Recipient`, `Donation`)
  4. **Utilities & Exceptions**: `bloodbank.util.*` (`DBConnection`, `ReportWriter`) & `bloodbank.exception.BloodBankException`

---

## 2. Directory Structure

```text
Blood-Bank-Donation-Management/
├── DBMS_NOTES.md            # Comprehensive DBMS architecture and technical notes
├── ER_diagram.md            # Entity-Relationship diagram in Mermaid syntax
├── CONCEPTS.md              # Complete OOP & DBMS concept mapping table
├── DEMO_SCRIPT.md           # Step-by-step evaluator live demonstration script
├── schema.sql               # Database schema (6 tables, 1 view, constraints, seed data)
├── db.properties.example    # Template configuration file for database credentials
├── db.properties            # Local database credentials (GITIGNORED - do not commit!)
├── lib/
│   └── mysql-connector-j-8.3.0.jar
├── reports/                 # Output directory for exported .txt audit reports
└── src/
    └── bloodbank/
        ├── app/
        │   └── Main.java                 # Interactive CLI menu application
        ├── dao/
        │   ├── Manageable.java           # Generic CRUD interface <T>
        │   ├── DonorDAO.java             # Donor data access & overloaded search
        │   ├── RecipientDAO.java         # Recipient CRUD data access
        │   ├── DonationDAO.java          # Transactional donation & 90-day check
        │   └── StockDAO.java             # Pessimistic locking issue & inventory
        ├── exception/
        │   └── BloodBankException.java   # Custom application checked exception
        ├── model/
        │   ├── Person.java               # Abstract encapsulated base class
        │   ├── Donor.java                # Extends Person (specialized donor)
        │   ├── Recipient.java            # Extends Person (hospital recipient)
        │   └── Donation.java             # Encapsulated donation record
        ├── test/
        │   └── DaoSmokeTest.java         # 12-step automated verification suite
        └── util/
            ├── DBConnection.java         # Connection manager & credential resolver
            └── ReportWriter.java         # Java I/O report exporter (.txt)
```

---

## 3. Database Setup & Configuration

### Step A: Initialize the MySQL Database
Ensure your local MySQL service (e.g. `MySQL80`) is running. Then load `schema.sql`:

```powershell
# In PowerShell (Windows):
Get-Content schema.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```
*(Enter your MySQL root password when prompted)*

This automatically creates:
- The `bloodbank` database (UTF-8).
- The 6 relational tables: `blood_groups`, `donors`, `recipients`, `donations`, `blood_stock`, and `blood_issues`.
- The relational analytical view: `donor_donation_summary`.
- Reference seed data for the 8 canonical blood groups (`A+`, `A-`, `B+`, `B-`, `O+`, `O-`, `AB+`, `AB-`) initialized to 0 stock units.

### Step B: Configure Credentials (`db.properties`)
Create a file named `db.properties` in the project root directory (next to `schema.sql`). You can copy `db.properties.example`:

```properties
url=jdbc:mysql://localhost:3306/bloodbank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
user=root
password=YOUR_MYSQL_PASSWORD_HERE
```

> [!IMPORTANT]
> `db.properties` is listed in `.gitignore` and **must never be committed to GitHub** to protect your local credentials.

---

## 4. Compilation & Verification

### Compile All Source Files
Compile all Java source packages into the `out` directory:

```powershell
# Windows PowerShell:
$files = (Get-ChildItem -Recurse -Filter *.java src).FullName; javac -cp "lib/*" -d out $files

# Linux / macOS:
javac -cp "lib/*" -d out $(find src -name "*.java")
```

### Run the Automated Smoke Test Suite
Verify that your database schema, JDBC connectivity, transactions, and constraints are 100% operational:

```powershell
# Windows:
java -cp "out;lib/*" bloodbank.test.DaoSmokeTest

# Linux / macOS:
java -cp "out:lib/*" bloodbank.test.DaoSmokeTest
```
**Expected Output**: `SMOKE TEST RESULTS: 12 PASSED, 0 FAILED`

---

## 5. Running the Application

Launch the interactive console application:

```powershell
# Windows:
java -cp "out;lib/*" bloodbank.app.Main

# Linux / macOS:
java -cp "out:lib/*" bloodbank.app.Main
```

### Main Menu Overview

| Option | Category | Action / Academic Concept Demonstrated |
| :---: | :--- | :--- |
| **1** | Donor | Register New Donor (Validated age 18-65, phone, blood group) |
| **2** | Donor | View All Donors (Streams & Lambdas: sort by name/age, filter by group) |
| **3** | Donor | Update Donor Details |
| **4** | Donor | Delete Donor (**Demonstrates DBMS `ON DELETE CASCADE`**) |
| **5** | Donor | Search Donors by Blood Group (**Compile-time Overloaded Search #1**) |
| **6** | Donor | Search Donors by Name & Blood Group (**Compile-time Overloaded Search #2**) |
| **7** | Recipient | Register New Recipient / Patient (Validated hospital and age) |
| **8** | Recipient | View All Recipients |
| **9** | Recipient | Update Recipient Details |
| **10** | Recipient | Delete Recipient (**DBMS `ON DELETE CASCADE` on blood issues**) |
| **11** | Transaction | Record Blood Donation (**ACID Transaction + 90-Day Waiting Rule**) |
| **12** | Inventory | View Blood Stock Inventory (Canonical order & low-stock warning) |
| **13** | Transaction | Issue Blood to Recipient (**ACID Transaction + `FOR UPDATE` Row Lock & Rollback**) |
| **14** | Report | Donation History Audit Report (**SQL INNER JOIN `donors JOIN donations`**) |
| **15** | Report | Donor Summary Report (**SQL Relational VIEW `donor_donation_summary`**) |
| **16** | Report | Group-Wise Donation Summary (**SQL Aggregation `GROUP BY blood_group`**) |
| **17** | Report | Blood Issuance History Report (**SQL Relational JOIN**) |
| **18** | Export | Export Reports to `.txt` File (**Java File I/O: `ReportWriter.java`**) |
| **19** | OOP Demo | **Runtime Polymorphism Live Demo (`List<Person>` Dynamic Method Dispatch)** |
| **0** | System | Exit Application |

---

## 6. Project Documentation Links
- **[CONCEPTS.md](file:///c:/Users/Guru%20Eswar%20E/Downloads/Blood-Bank-Donation-Management/CONCEPTS.md)**: Academic mapping table of OOP and DBMS concepts with line references.
- **[DEMO_SCRIPT.md](file:///c:/Users/Guru%20Eswar%20E/Downloads/Blood-Bank-Donation-Management/DEMO_SCRIPT.md)**: Step-by-step evaluator presentation guide.
- **[DBMS_NOTES.md](file:///c:/Users/Guru%20Eswar%20E/Downloads/Blood-Bank-Donation-Management/DBMS_NOTES.md)**: Relational schema design, 3NF proofs, and transaction sequence diagrams.
- **[ER_diagram.md](file:///c:/Users/Guru%20Eswar%20E/Downloads/Blood-Bank-Donation-Management/ER_diagram.md)**: Visual Entity-Relationship diagram.
