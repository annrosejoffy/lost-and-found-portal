import java.time.LocalDate;

/** Represents a claim linking a lost item report to a found item report. */
public class Claim {
    private String id;
    private String lostItemId;
    private String foundItemId;
    private String claimantName;
    private LocalDate claimDate;
    private boolean verified;

    public Claim(String id, String lostItemId, String foundItemId, String claimantName,
                 LocalDate claimDate, boolean verified) {
        this.id = id;
        this.lostItemId = lostItemId;
        this.foundItemId = foundItemId;
        this.claimantName = claimantName;
        this.claimDate = claimDate;
        this.verified = verified;
    }

    public String getId() { return id; }
    public String getLostItemId() { return lostItemId; }
    public String getFoundItemId() { return foundItemId; }
    public String getClaimantName() { return claimantName; }
    public LocalDate getClaimDate() { return claimDate; }
    public boolean isVerified() { return verified; }

    public String toDataLine() {
        return String.join("|", id, lostItemId, foundItemId, claimantName.replace("|", "/"),
                claimDate.toString(), String.valueOf(verified));
    }

    public static Claim fromDataLine(String line) {
        String[] p = line.split("\\|", -1);
        return new Claim(p[0], p[1], p[2], p[3], LocalDate.parse(p[4]), Boolean.parseBoolean(p[5]));
    }

    @Override
    public String toString() {
        return String.format("Claim %s | Lost:%s <-> Found:%s | Claimant: %s | Date: %s | Verified: %s",
                id, lostItemId, foundItemId, claimantName, claimDate, verified ? "YES" : "NO");
    }
}
