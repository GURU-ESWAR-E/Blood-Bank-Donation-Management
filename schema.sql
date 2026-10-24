-- ============================================================================
-- BloodBank Database Schema (MySQL 8.0+ / InnoDB / UTF8MB4)
-- Member 2: Database Layer
-- Description: Comprehensive relational schema for Blood Bank Donation Management.
-- Design follows Third Normal Form (3NF) with strict constraints and ACID compliance.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS bloodbank
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bloodbank;

-- ----------------------------------------------------------------------------
-- 1. Reference Table: blood_groups
-- Purpose: Canonical lookup table for the 8 standard ABO/Rh blood groups.
-- Primary Key: blood_group (e.g. 'A+', 'O-')
-- Relationship: Parent table for donors, recipients, blood_stock, and blood_issues (1:M).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_groups (
    blood_group VARCHAR(3) NOT NULL,
    PRIMARY KEY (blood_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed only reference data: the 8 standard blood groups
INSERT IGNORE INTO blood_groups (blood_group) VALUES
('A+'), ('A-'),
('B+'), ('B-'),
('O+'), ('O-'),
('AB+'), ('AB-');

-- ----------------------------------------------------------------------------
-- 2. Entity Table: donors
-- Purpose: Stores registered blood donors.
-- Primary Key: donor_id (auto-incrementing surrogate key)
-- Foreign Key: blood_group -> blood_groups(blood_group)
-- Constraints:
--   - age BETWEEN 18 AND 65 (medical eligibility check)
--   - phone is UNIQUE and NOT NULL
--   - registered_on defaults to current date
-- Relationship: 1:M with donations (one donor can make multiple donations over time).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donors (
    donor_id INT AUTO_INCREMENT NOT NULL,
    name VARCHAR(60) NOT NULL,
    age INT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    registered_on DATE NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (donor_id),
    UNIQUE KEY uq_donors_phone (phone),
    CONSTRAINT chk_donors_age CHECK (age BETWEEN 18 AND 65),
    CONSTRAINT fk_donors_blood_group FOREIGN KEY (blood_group)
        REFERENCES blood_groups(blood_group)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_donors_blood_group (blood_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Entity Table: recipients
-- Purpose: Stores patients/recipients requesting blood units.
-- Primary Key: recipient_id (auto-incrementing surrogate key)
-- Foreign Key: blood_group -> blood_groups(blood_group)
-- Constraints:
--   - age BETWEEN 0 AND 120 (valid lifespan check)
--   - phone is UNIQUE and NOT NULL
--   - hospital name NOT NULL
-- Relationship: 1:M with blood_issues (one recipient can receive blood multiple times).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recipients (
    recipient_id INT AUTO_INCREMENT NOT NULL,
    name VARCHAR(60) NOT NULL,
    age INT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    hospital VARCHAR(80) NOT NULL,
    PRIMARY KEY (recipient_id),
    UNIQUE KEY uq_recipients_phone (phone),
    CONSTRAINT chk_recipients_age CHECK (age BETWEEN 0 AND 120),
    CONSTRAINT fk_recipients_blood_group FOREIGN KEY (blood_group)
        REFERENCES blood_groups(blood_group)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Transaction Table: donations
-- Purpose: Records each individual blood donation event by a registered donor.
-- Primary Key: donation_id (auto-incrementing surrogate key)
-- Foreign Key: donor_id -> donors(donor_id) ON DELETE CASCADE
-- Constraints:
--   - units > 0 (strictly positive volume)
--   - donation_date NOT NULL
-- Relationship: Child table of donors (1:M). If donor is removed, their donation history cascades.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donations (
    donation_id INT AUTO_INCREMENT NOT NULL,
    donor_id INT NOT NULL,
    donation_date DATE NOT NULL,
    units INT NOT NULL,
    PRIMARY KEY (donation_id),
    CONSTRAINT chk_donations_units CHECK (units > 0),
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id)
        REFERENCES donors(donor_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_donations_donor_id (donor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Aggregate / State Table: blood_stock
-- Purpose: Tracks current available units per blood group.
-- Primary Key: blood_group
-- Foreign Key: blood_group -> blood_groups(blood_group)
-- Constraints:
--   - units >= 0 (inventory cannot drop below zero)
-- Relationship: 1:1 inventory mapping for each blood group.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_stock (
    blood_group VARCHAR(3) NOT NULL,
    units INT NOT NULL DEFAULT 0,
    PRIMARY KEY (blood_group),
    CONSTRAINT chk_stock_units CHECK (units >= 0),
    CONSTRAINT fk_stock_blood_group FOREIGN KEY (blood_group)
        REFERENCES blood_groups(blood_group)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed initial inventory with 0 units for all 8 blood groups
INSERT IGNORE INTO blood_stock (blood_group, units) VALUES
('A+', 0), ('A-', 0),
('B+', 0), ('B-', 0),
('O+', 0), ('O-', 0),
('AB+', 0), ('AB-', 0);

-- ----------------------------------------------------------------------------
-- 6. Transaction Table: blood_issues
-- Purpose: Records blood units disbursed/issued to recipients for hospital care.
-- Primary Key: issue_id (auto-incrementing surrogate key)
-- Foreign Keys:
--   - recipient_id -> recipients(recipient_id) ON DELETE CASCADE
--   - blood_group -> blood_groups(blood_group)
-- Constraints:
--   - units > 0
--   - issue_date NOT NULL
-- Relationship: Child table of recipients (1:M).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_issues (
    issue_id INT AUTO_INCREMENT NOT NULL,
    recipient_id INT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    units INT NOT NULL,
    issue_date DATE NOT NULL,
    PRIMARY KEY (issue_id),
    CONSTRAINT chk_issues_units CHECK (units > 0),
    CONSTRAINT fk_issues_recipient FOREIGN KEY (recipient_id)
        REFERENCES recipients(recipient_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_issues_blood_group FOREIGN KEY (blood_group)
        REFERENCES blood_groups(blood_group)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- VIEW: donor_donation_summary
-- Purpose: Provides an aggregated summary of every donor, including total donations,
--          total units donated, and the date of their most recent donation.
-- Mechanics: Uses LEFT JOIN so donors with 0 donations still appear with count 0.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW donor_donation_summary AS
SELECT 
    d.donor_id,
    d.name,
    d.blood_group,
    COUNT(dn.donation_id) AS total_donations,
    COALESCE(SUM(dn.units), 0) AS total_units,
    MAX(dn.donation_date) AS last_donation
FROM donors d
LEFT JOIN donations dn ON d.donor_id = dn.donor_id
GROUP BY d.donor_id, d.name, d.blood_group;
