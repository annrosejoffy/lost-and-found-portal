import java.io.*;
import java.util.*;

/**
 * Central data manager for the Lost and Found system.
 *
 * Data structures used (as required by the project brief):
 *  - HashMap<String, LostItem>  lostById    : O(1) direct ID lookup for lost items
 *  - HashMap<String, FoundItem> foundById   : O(1) direct ID lookup for found items
 *  - HashMap<String, List<String>> categoryIndex(lost/found) : O(1) category bucket lookup (indexing)
 *  - ArrayList<LostItem>  lostItems   : ordered, resizable list -> supports sorting + binary search by date
 *  - LinkedList<FoundItem> foundItems : demonstrates efficient sequential insert/remove (claim removal)
 *  - ArrayList<Claim> claims          : chronological claim log
 *
 * All records are persisted to plain text files in ./data so the system survives restarts.
 */
public class DataStore {
    private static final String DATA_DIR = "data";
    private static final String LOST_FILE = DATA_DIR + File.separator + "lost_items.txt";
    private static final String FOUND_FILE = DATA_DIR + File.separator + "found_items.txt";
    private static final String CLAIM_FILE = DATA_DIR + File.separator + "claims.txt";
    private static final String COUNTER_FILE = DATA_DIR + File.separator + "counters.txt";

    private final ArrayList<LostItem> lostItems = new ArrayList<>();
    private final LinkedList<FoundItem> foundItems = new LinkedList<>();
    private final ArrayList<Claim> claims = new ArrayList<>();

    private final HashMap<String, LostItem> lostById = new HashMap<>();
    private final HashMap<String, FoundItem> foundById = new HashMap<>();
    private final HashMap<String, List<String>> lostCategoryIndex = new HashMap<>();
    private final HashMap<String, List<String>> foundCategoryIndex = new HashMap<>();

    private int lostCounter = 0;
    private int foundCounter = 0;
    private int claimCounter = 0;

    public DataStore() {
        new File(DATA_DIR).mkdirs();
        loadAll();
    }

    // ---------- ID generation ----------
    public String nextLostId() { return "L" + String.format("%03d", ++lostCounter); }
    public String nextFoundId() { return "F" + String.format("%03d", ++foundCounter); }
    public String nextClaimId() { return "C" + String.format("%03d", ++claimCounter); }

    // ---------- Add operations (keep list + HashMap index + category index in sync) ----------
    public void addLostItem(LostItem item) {
        lostItems.add(item);
        lostById.put(item.getId(), item);
        lostCategoryIndex.computeIfAbsent(item.getCategory().toLowerCase(), k -> new ArrayList<>()).add(item.getId());
    }

    public void addFoundItem(FoundItem item) {
        foundItems.add(item); // LinkedList append - O(1)
        foundById.put(item.getId(), item);
        foundCategoryIndex.computeIfAbsent(item.getCategory().toLowerCase(), k -> new ArrayList<>()).add(item.getId());
    }

    public void addClaim(Claim c) {
        claims.add(c);
    }

    // ---------- Accessors ----------
    public ArrayList<LostItem> getLostItems() { return lostItems; }
    public LinkedList<FoundItem> getFoundItems() { return foundItems; }
    public ArrayList<Claim> getClaims() { return claims; }

    public LostItem getLostById(String id) { return lostById.get(id); } // O(1) HashMap lookup
    public FoundItem getFoundById(String id) { return foundById.get(id); }

    public List<LostItem> getLostByCategory(String category) {
        List<String> ids = lostCategoryIndex.getOrDefault(category.toLowerCase(), Collections.emptyList());
        List<LostItem> result = new ArrayList<>();
        for (String id : ids) result.add(lostById.get(id));
        return result;
    }

    public List<FoundItem> getFoundByCategory(String category) {
        List<String> ids = foundCategoryIndex.getOrDefault(category.toLowerCase(), Collections.emptyList());
        List<FoundItem> result = new ArrayList<>();
        for (String id : ids) result.add(foundById.get(id));
        return result;
    }

    /** Removes a found item from the LinkedList + index once it has been claimed/returned. */
    public void removeFoundFromActiveList(String id) {
        FoundItem item = foundById.get(id);
        if (item != null) foundItems.remove(item);
    }

    // ---------- Persistence ----------
    public void saveAll() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOST_FILE))) {
            for (LostItem it : lostItems) pw.println(it.toDataLine());
        } catch (IOException e) {
            System.out.println("Error saving lost items: " + e.getMessage());
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(FOUND_FILE))) {
            for (FoundItem it : foundItems) pw.println(it.toDataLine());
        } catch (IOException e) {
            System.out.println("Error saving found items: " + e.getMessage());
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(CLAIM_FILE))) {
            for (Claim c : claims) pw.println(c.toDataLine());
        } catch (IOException e) {
            System.out.println("Error saving claims: " + e.getMessage());
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(COUNTER_FILE))) {
            pw.println(lostCounter + "," + foundCounter + "," + claimCounter);
        } catch (IOException e) {
            System.out.println("Error saving counters: " + e.getMessage());
        }
    }

    private void loadAll() {
        readLines(LOST_FILE).forEach(line -> {
            if (!line.trim().isEmpty()) addLostItem(LostItem.fromDataLine(line));
        });
        readLines(FOUND_FILE).forEach(line -> {
            if (!line.trim().isEmpty()) addFoundItem(FoundItem.fromDataLine(line));
        });
        readLines(CLAIM_FILE).forEach(line -> {
            if (!line.trim().isEmpty()) claims.add(Claim.fromDataLine(line));
        });
        List<String> counterLines = readLines(COUNTER_FILE);
        if (!counterLines.isEmpty()) {
            String[] p = counterLines.get(0).split(",");
            lostCounter = Integer.parseInt(p[0]);
            foundCounter = Integer.parseInt(p[1]);
            claimCounter = Integer.parseInt(p[2]);
        } else {
            // Derive counters from existing IDs if counter file missing (defensive fallback)
            for (LostItem it : lostItems) lostCounter = Math.max(lostCounter, idNum(it.getId()));
            for (FoundItem it : foundItems) foundCounter = Math.max(foundCounter, idNum(it.getId()));
            for (Claim c : claims) claimCounter = Math.max(claimCounter, idNum(c.getId()));
        }
    }

    private int idNum(String id) {
        try { return Integer.parseInt(id.substring(1)); } catch (Exception e) { return 0; }
    }

    private List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        File f = new File(path);
        if (!f.exists()) return lines;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            System.out.println("Error reading " + path + ": " + e.getMessage());
        }
        return lines;
    }
}
