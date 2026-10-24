# BloodBank Database Entity-Relationship (ER) Diagram

The following diagram models the complete relational schema of the BloodBank donation and stock management system.

```mermaid
erDiagram
    BLOOD_GROUPS ||--o{ DONORS : "classifies"
    BLOOD_GROUPS ||--o{ RECIPIENTS : "classifies"
    BLOOD_GROUPS ||--|| BLOOD_STOCK : "tracks inventory for"
    BLOOD_GROUPS ||--o{ BLOOD_ISSUES : "issued as"
    DONORS ||--o{ DONATIONS : "makes (1:M)"
    RECIPIENTS ||--o{ BLOOD_ISSUES : "receives (1:M)"

    BLOOD_GROUPS {
        varchar(3) blood_group PK "e.g. A+, O-, etc."
    }

    BLOOD_STOCK {
        varchar(3) blood_group PK, FK "References blood_groups"
        int units "Available units (>= 0)"
    }

    DONORS {
        int donor_id PK "Auto-increment"
        varchar(60) name "Full name"
        int age "Check: 18 to 65"
        varchar(3) blood_group FK "References blood_groups"
        varchar(15) phone UK "Unique contact number"
        date registered_on "Default current date"
    }

    RECIPIENTS {
        int recipient_id PK "Auto-increment"
        varchar(60) name "Full name"
        int age "Check: 0 to 120"
        varchar(3) blood_group FK "References blood_groups"
        varchar(15) phone UK "Unique contact number"
        varchar(80) hospital "Hospital name"
    }

    DONATIONS {
        int donation_id PK "Auto-increment"
        int donor_id FK "References donors (ON DELETE CASCADE)"
        date donation_date "Date blood was drawn"
        int units "Units donated (> 0)"
    }

    BLOOD_ISSUES {
        int issue_id PK "Auto-increment"
        int recipient_id FK "References recipients (ON DELETE CASCADE)"
        varchar(3) blood_group FK "References blood_groups"
        int units "Units issued (> 0)"
        date issue_date "Date blood was issued"
    }
```

### Cardinality Summary
- **blood_groups (1) to donors (M)**: One blood group can be shared by zero or many donors. Each donor belongs to exactly one blood group.
- **blood_groups (1) to recipients (M)**: One blood group can be shared by zero or many recipients. Each recipient belongs to exactly one blood group.
- **blood_groups (1) to blood_stock (1)**: Exactly one stock entry per blood group.
- **donors (1) to donations (M)**: One donor can donate blood multiple times (1:M). `ON DELETE CASCADE` ensures donations are removed if donor record is deleted.
- **recipients (1) to blood_issues (M)**: One recipient can receive blood multiple times (1:M). `ON DELETE CASCADE` preserves referential integrity.
- **blood_groups (1) to blood_issues (M)**: Each issue transaction references the specific blood group disbursed.
