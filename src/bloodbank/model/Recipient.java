// PLACEHOLDER - Member 1 owns this file, will be replaced on merge
package bloodbank.model;

/**
 * Represents a blood recipient / hospital patient.
 */
public class Recipient extends Person {
    private String hospital;

    public Recipient(int id, String name, int age, String bloodGroup, String phone, String hospital) {
        super(id, name, age, bloodGroup, phone);
        this.hospital = hospital;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    @Override
    public String getRole() {
        return "Recipient";
    }

    @Override
    public void displayDetails() {
        System.out.printf("Recipient #%d | Name: %s | Age: %d | Blood Group: %s | Phone: %s | Hospital: %s%n",
                getId(), getName(), getAge(), getBloodGroup(), getPhone(), hospital);
    }
}
