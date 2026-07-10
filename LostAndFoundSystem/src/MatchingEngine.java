import java.time.LocalDate;
import java.util.*;

/**
 * Implements the searching and matching algorithms used by the system:
 *  - Linear search over keyword text (used for free-text search across any field)
 *  - Binary search over a date-sorted ArrayList (used for exact-date lookups)
 *  - A weighted scoring algorithm that compares a lost item against every found item
 *    to suggest likely matches (category, color, location, and keyword-overlap scoring)
 */
public class MatchingEngine {

    /** Linear search: O(n). Scans every lost item and keeps those whose text contains the keyword. */
    public static List<LostItem> linearSearchLost(List<LostItem> items, String keyword) {
        List<LostItem> results = new ArrayList<>();
        String kw = keyword.toLowerCase().trim();
        for (LostItem item : items) {
            if (item.searchableText().contains(kw)) results.add(item);
        }
        return results;
    }

    /** Linear search: O(n). Same idea, applied to found items. */
    public static List<FoundItem> linearSearchFound(List<FoundItem> items, String keyword) {
        List<FoundItem> results = new ArrayList<>();
        String kw = keyword.toLowerCase().trim();
        for (FoundItem item : items) {
            if (item.searchableText().contains(kw)) results.add(item);
        }
        return results;
    }

    /**
     * Binary search: O(log n). The caller must pass a list already sorted by date
     * (see Collections.sort with Comparator.comparing(Item::getDate)).
     * Returns every lost item that was reported lost on the exact given date.
     */
    public static List<LostItem> binarySearchLostByDate(List<LostItem> sortedByDate, LocalDate target) {
        List<LostItem> results = new ArrayList<>();
        int lo = 0, hi = sortedByDate.size() - 1;
        int foundIndex = -1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            LocalDate midDate = sortedByDate.get(mid).getDate();
            int cmp = midDate.compareTo(target);
            if (cmp == 0) { foundIndex = mid; break; }
            else if (cmp < 0) lo = mid + 1;
            else hi = mid - 1;
        }
        if (foundIndex == -1) return results; // not found
        // Expand outward since multiple items can share the same date
        int left = foundIndex;
        while (left >= 0 && sortedByDate.get(left).getDate().equals(target)) left--;
        int right = foundIndex;
        while (right < sortedByDate.size() && sortedByDate.get(right).getDate().equals(target)) right++;
        for (int i = left + 1; i < right; i++) results.add(sortedByDate.get(i));
        return results;
    }

    /**
     * Scores how well a found item matches a lost item.
     * +4 category match, +3 color match, +2 location keyword overlap, +1 per shared description/name word.
     */
    public static int matchScore(LostItem lost, FoundItem found) {
        int score = 0;
        if (lost.getCategory().equalsIgnoreCase(found.getCategory())) score += 4;
        if (lost.getColor().equalsIgnoreCase(found.getColor())) score += 3;
        if (containsEither(lost.getLocation(), found.getLocation())) score += 2;
        score += sharedWordCount(lost.getName() + " " + lost.getDescription(),
                                  found.getName() + " " + found.getDescription());
        return score;
    }

    /** Finds and ranks candidate found-item matches for a given lost item, best first. */
    public static List<Map.Entry<FoundItem, Integer>> suggestMatches(LostItem lost, Collection<FoundItem> foundPool, int minScore) {
        List<Map.Entry<FoundItem, Integer>> scored = new ArrayList<>();
        for (FoundItem f : foundPool) {
            if (!"ACTIVE".equals(f.getStatus())) continue;
            int score = matchScore(lost, f);
            if (score >= minScore) scored.add(new AbstractMap.SimpleEntry<>(f, score));
        }
        scored.sort((a, b) -> b.getValue() - a.getValue());
        return scored;
    }

    private static boolean containsEither(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.toLowerCase().trim();
        String y = b.toLowerCase().trim();
        return x.contains(y) || y.contains(x);
    }

    private static int sharedWordCount(String a, String b) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\W+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\W+")));
        wordsA.retainAll(wordsB);
        wordsA.removeIf(w -> w.length() < 3); // ignore short/common words
        return wordsA.size();
    }
}
