package bloodbank.model;

/**
 * Represents an individual blood donation transaction event.
 * 
 * Demonstrates:
 * Encapsulation: Holds transaction data (foreign key donorId, date, and units)
 * with private fields and immutable read access via public getters.
 */
public class Donation {

    // Private fields guarantee data hiding and prevent external tampering
    private int donationId;
    private int donorId;
    private String donationDate; // Date represented as ISO "yyyy-MM-dd"
    private int units;

    /**
     * Constructs a Donation instance representing a completed transaction record.
     */
    public Donation(int donationId, int donorId, String donationDate, int units) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.donationDate = donationDate;
        this.units = units;
    }

    public int getDonationId() {
        return donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public String getDonationDate() {
        return donationDate;
    }

    public int getUnits() {
        return units;
    }
}
