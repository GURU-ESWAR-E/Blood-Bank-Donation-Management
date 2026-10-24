// PLACEHOLDER - Member 1 owns this file, will be replaced on merge
package bloodbank.model;

/**
 * Represents a blood donor in the system.
 */
public class Donor extends Person {
    private String lastDonation; // "yyyy-MM-dd" or null

    public Donor(int id, String name, int age, String bloodGroup, String phone, String lastDonation) {
        super(id, name, age, bloodGroup, phone);
        this.lastDonation = lastDonation;
    }

    public String getLastDonation() {
        return lastDonation;
    }

    public void setLastDonation(String lastDonation) {
        this.lastDonation = lastDonation;
    }

    @Override
    public String getRole() {
        return "Donor";
    }

    @Override
    public void displayDetails() {
        System.out.printf("Donor #%d | Name: %s | Age: %d | Blood Group: %s | Phone: %s | Last Donation: %s%n",
                getId(), getName(), getAge(), getBloodGroup(), getPhone(),
                (lastDonation != null ? lastDonation : "None"));
    }
}
