import java.time.LocalDate;

/** Represents an item reported as LOST by a user. */
public class LostItem extends Item {
    public LostItem(String id, String name, String category, String color, LocalDate dateLost,
                     String lastSeenLocation, String description, String reporterName, String contactInfo) {
        super(id, name, category, color, dateLost, lastSeenLocation, description, reporterName, contactInfo);
    }

    public static LostItem fromDataLine(String line) {
        String[] p = line.split("\\|", -1);
        LostItem item = new LostItem(p[0], p[1], p[2], p[3], LocalDate.parse(p[4]), p[5], p[6], p[7], p[8]);
        item.setStatus(p[9]);
        return item;
    }
}
