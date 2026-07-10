import java.time.LocalDate;

/** Represents an item reported as FOUND by a user. */
public class FoundItem extends Item {
    public FoundItem(String id, String name, String category, String color, LocalDate dateFound,
                      String foundLocation, String description, String reporterName, String contactInfo) {
        super(id, name, category, color, dateFound, foundLocation, description, reporterName, contactInfo);
    }

    public static FoundItem fromDataLine(String line) {
        String[] p = line.split("\\|", -1);
        FoundItem item = new FoundItem(p[0], p[1], p[2], p[3], LocalDate.parse(p[4]), p[5], p[6], p[7], p[8]);
        item.setStatus(p[9]);
        if (p.length > 10) item.setImagePath(p[10]); // backward-compatible: older files have no image field
        return item;
    }
}
