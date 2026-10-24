package bloodbank.model;

/**
 * Represents a blood donor entity.
 * 
 * Demonstrates:
 * 1. Inheritance: 'extends Person' inherits all common person attributes (id, name, age, bloodGroup, phone).
 * 2. Constructor Chaining: Uses 'super(...)' to delegate common property initialization to Person.
 * 3. Polymorphism: Implements abstract methods getRole() and displayDetails() with donor-specific behavior.
 */
public class Donor extends Person { // 'extends' establishes the IS-A relationship (a Donor IS-A Person)

    // Donor-specific attribute: ISO date string "yyyy-MM-dd" or null if never donated
    private String lastDonation;

    /**
     * Constructs a Donor instance.
     * Invokes the superclass constructor to initialize inherited fields.
     */
    public Donor(int id, String name, int age, String bloodGroup, String phone, String lastDonation) {
        super(id, name, age, bloodGroup, phone); // 'super(...)' calls the Person constructor
        this.lastDonation = lastDonation;
    }

    public String getLastDonation() {
        return lastDonation;
    }

    public void setLastDonation(String lastDonation) {
        this.lastDonation = lastDonation;
    }

    @Override // @Override confirms we are fulfilling the contract of Person's abstract getRole()
    public String getRole() {
        return "Donor";
    }

    @Override // @Override supplies concrete polymorphic behavior for displayDetails()
    public void displayDetails() {
        System.out.printf("Donor #%-4d | Name: %-20s | Age: %-2d | Group: %-3s | Phone: %-12s | Last Donation: %s%n",
                getId(), getName(), getAge(), getBloodGroup(), getPhone(),
                (lastDonation != null ? lastDonation : "None (New Donor)"));
    }
}
