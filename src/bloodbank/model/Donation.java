// PLACEHOLDER - Member 1 owns this file, will be replaced on merge
package bloodbank.model;

/**
 * Represents a blood donation event.
 */
public class Donation {
    private int donationId;
    private int donorId;
    private String donationDate; // "yyyy-MM-dd"
    private int units;

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
