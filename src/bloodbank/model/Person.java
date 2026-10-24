package bloodbank.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class representing a human participant in the Blood Bank system.
 * 
 * Demonstrates:
 * 1. Abstraction: Declared 'abstract' because a generic "Person" cannot exist on its own;
 *    only concrete specializations like Donor or Recipient make domain sense.
 * 2. Encapsulation: All fields are marked 'private', accessible only via public getters
 *    and validated setters that protect object integrity.
 */
public abstract class Person { // 'abstract' prevents direct instantiation: cannot do 'new Person(...)'

    // A static set of the 8 canonical ABO/Rh blood groups for domain validation
    private static final Set<String> VALID_BLOOD_GROUPS = new HashSet<>(
            Arrays.asList("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    );

    // Private fields enforce Encapsulation (data hiding)
    private int id;
    private String name;
    private int age;
    private String bloodGroup;
    private String phone;

    /**
     * Parameterized constructor initialized via validated setters.
     * Note: Constructors do not throw checked exceptions to preserve compatibility with DAO layers.
     * Validation failures throw unchecked IllegalArgumentException instead.
     */
    public Person(int id, String name, int age, String bloodGroup, String phone) {
        this.id = id;
        setName(name);
        setAge(age);
        setBloodGroup(bloodGroup);
        setPhone(phone);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        // Enforces the standard statutory blood donation / eligibility bracket (18 to 65)
        if (age < 18 || age > 65) {
            throw new IllegalArgumentException("Age must be between 18 and 65. Provided: " + age);
        }
        this.age = age;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        if (bloodGroup == null || !VALID_BLOOD_GROUPS.contains(bloodGroup.trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid blood group: '" + bloodGroup +
                    "'. Must be one of: A+, A-, B+, B-, O+, O-, AB+, AB-");
        }
        this.bloodGroup = bloodGroup.trim().toUpperCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        // Validates phone number format: 10 to 15 digits with optional leading '+'
        if (phone == null || !phone.trim().matches("^\\+?[0-9]{10,15}$")) {
            throw new IllegalArgumentException("Phone must be 10 to 15 digits (optional leading '+'). Provided: " + phone);
        }
        this.phone = phone.trim();
    }

    // Abstract methods: Subclasses MUST provide concrete implementations
    public abstract String getRole();

    public abstract void displayDetails();
}
