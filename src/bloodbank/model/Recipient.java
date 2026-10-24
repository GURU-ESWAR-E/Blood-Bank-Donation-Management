package bloodbank.model;

/**
 * Represents a blood recipient / hospital patient needing transfusions.
 * 
 * Demonstrates:
 * 1. Inheritance: 'extends Person' inherits core identity attributes.
 * 2. Specialization: Adds hospital tracking field unique to recipients.
 * 3. Polymorphism: Overrides getRole() and displayDetails() specific to hospital recipients.
 */
public class Recipient extends Person { // 'extends' establishes that a Recipient IS-A Person

    // Recipient-specific attribute: the medical facility managing the transfusion
    private String hospital;

    /**
     * Constructs a Recipient instance.
     * Delegates common attributes to Person via super(...).
     */
    public Recipient(int id, String name, int age, String bloodGroup, String phone, String hospital) {
        super(id, name, age, bloodGroup, phone); // Invokes the Person superclass constructor
        setHospital(hospital);
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        if (hospital == null || hospital.trim().isEmpty()) {
            throw new IllegalArgumentException("Hospital name cannot be null or empty.");
        }
        this.hospital = hospital.trim();
    }

    @Override // @Override indicates this method replaces the abstract declaration in Person
    public String getRole() {
        return "Recipient";
    }

    @Override // @Override provides recipient-specific formatting for polymorphic display
    public void displayDetails() {
        System.out.printf("Recipient #%-4d | Name: %-20s | Age: %-2d | Group: %-3s | Phone: %-12s | Hospital: %s%n",
                getId(), getName(), getAge(), getBloodGroup(), getPhone(), hospital);
    }
}
