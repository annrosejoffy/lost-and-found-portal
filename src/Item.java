import java.time.LocalDate;

/**
 * Abstract base class representing any reported item (lost or found).
 * Holds the fields and behaviour shared by LostItem and FoundItem.
 */
public abstract class Item {
    protected String id;
    protected String name;
    protected String category;
    protected String color;
    protected LocalDate date;
    protected String location;
    protected String description;
    protected String reporterName;
    protected String contactInfo;
    protected String status; // ACTIVE, MATCHED, RETURNED

    public Item(String id, String name, String category, String color, LocalDate date,
                String location, String description, String reporterName, String contactInfo) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.color = color;
        this.date = date;
        this.location = location;
        this.description = description;
        this.reporterName = reporterName;
        this.contactInfo = contactInfo;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getColor() { return color; }
    public LocalDate getDate() { return date; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getReporterName() { return reporterName; }
    public String getContactInfo() { return contactInfo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Concatenates the searchable text fields of the item, lower-cased. */
    public String searchableText() {
        return (name + " " + category + " " + color + " " + location + " " + description).toLowerCase();
    }

    /** Serializes the item into a pipe-delimited line for file storage. */
    public String toDataLine() {
        return String.join("|",
                id, escape(name), escape(category), escape(color), date.toString(),
                escape(location), escape(description), escape(reporterName), escape(contactInfo), status);
    }

    protected static String escape(String s) {
        return s == null ? "" : s.replace("|", "/");
    }

    @Override
    public String toString() {
        return String.format(
                "ID: %-6s | Name: %-15s | Category: %-12s | Color: %-10s | Date: %-10s | Location: %-15s | Status: %-9s%n      Desc: %s%n      Reported by: %s (%s)",
                id, name, category, color, date, location, status, description, reporterName, contactInfo);
    }
}
