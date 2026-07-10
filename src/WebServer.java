import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Lost and Found Management System - Web App entry point.
 * A dependency-free web server (built on the JDK's com.sun.net.httpserver)
 * that exposes a small JSON API and serves the static frontend in web/index.html.
 * Reuses the exact same DataStore / MatchingEngine / model classes as the
 * console and desktop GUI versions of this project.
 */
public class WebServer {
    private static final int PORT = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8080;
    private final DataStore store;

    public WebServer(DataStore store) { this.store = store; }

    public static void main(String[] args) throws IOException {
        DataStore store = new DataStore();
        WebServer app = new WebServer(store);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/lost", app::handleLost);
        server.createContext("/api/found", app::handleFound);
        server.createContext("/api/matches", app::handleMatches);
        server.createContext("/api/claim", app::handleClaim);
        server.createContext("/api/records", app::handleRecords);
        server.createContext("/uploads", app::handleImage);
        server.createContext("/", app::handleStatic);
        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println(" Lost and Found Management System - Web App");
        System.out.println(" Server running at: http://localhost:" + PORT);
        System.out.println(" Press Ctrl+C to stop.");
        System.out.println("=================================================");
    }

    // ================= Static frontend =================
    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) path = "/index.html";
        Path file = Path.of("web", path.substring(1));
        if (!Files.exists(file) || Files.isDirectory(file)) {
            file = Path.of("web", "index.html");
        }
        byte[] bytes = Files.readAllBytes(file);
        String contentType = path.endsWith(".css") ? "text/css; charset=utf-8"
                : path.endsWith(".js") ? "application/javascript; charset=utf-8"
                : "text/html; charset=utf-8";
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    // ================= Uploaded item photos =================
    private void handleImage(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath(); // e.g. /uploads/L001.jpg
        String filename = path.substring(path.lastIndexOf('/') + 1);
        Path file = Path.of("data", "images", filename);
        if (filename.isEmpty() || filename.contains("..") || !Files.exists(file)) {
            ex.sendResponseHeaders(404, -1);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        String lower = filename.toLowerCase();
        String contentType = lower.endsWith(".png") ? "image/png"
                : lower.endsWith(".gif") ? "image/gif"
                : lower.endsWith(".webp") ? "image/webp"
                : "image/jpeg";
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /**
     * Decodes a "data:image/xxx;base64,...." string from the upload form and saves it to
     * data/images/<id>.<ext>. Returns the saved filename, or "" if no image was provided.
     */
    private String saveImageIfPresent(Map<String, String> f, String id) throws IOException {
        String dataUrl = f.get("image");
        if (dataUrl == null || dataUrl.isEmpty() || !dataUrl.startsWith("data:image/")) return "";
        int comma = dataUrl.indexOf(',');
        if (comma < 0) return "";
        String header = dataUrl.substring(5, dataUrl.indexOf(';')); // e.g. "image/jpeg"
        String ext = header.equals("image/png") ? "png"
                : header.equals("image/gif") ? "gif"
                : header.equals("image/webp") ? "webp"
                : "jpg";
        byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
        Path dir = Path.of("data", "images");
        Files.createDirectories(dir);
        String filename = id + "." + ext;
        Files.write(dir.resolve(filename), bytes);
        return filename;
    }

    // ================= Module 1 & partial 3: /api/lost =================
    private void handleLost(HttpExchange ex) throws IOException {
        try {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> f = parseBody(ex);
                LocalDate date = parseDate(f.get("date"));
                if (date == null) { sendJson(ex, 400, JsonUtil.obj("error", "Invalid date. Use yyyy-MM-dd.")); return; }
                if (missing(f, "name", "category", "color", "location", "reporter", "contact")) {
                    sendJson(ex, 400, JsonUtil.obj("error", "All fields are required.")); return;
                }
                String id = store.nextLostId();
                LostItem item = new LostItem(id, f.get("name"), f.get("category"), f.get("color"), date,
                        f.get("location"), f.getOrDefault("description", ""), f.get("reporter"), f.get("contact"));
                item.setImagePath(saveImageIfPresent(f, id));
                store.addLostItem(item);
                store.saveAll();
                sendJson(ex, 200, JsonUtil.obj("id", id, "status", "ok"));
            } else {
                Map<String, String> q = parseQuery(ex);
                String mode = q.getOrDefault("mode", "all");
                String query = q.getOrDefault("q", "");
                List<LostItem> results;
                switch (mode) {
                    case "keyword": results = MatchingEngine.linearSearchLost(store.getLostItems(), query); break;
                    case "category": results = store.getLostByCategory(query); break;
                    case "id": {
                        LostItem li = store.getLostById(query); // O(1) HashMap lookup
                        results = new ArrayList<>();
                        if (li != null) results.add(li);
                        break;
                    }
                    case "date": {
                        LocalDate d = parseDate(query);
                        if (d == null) { sendJson(ex, 400, JsonUtil.obj("error", "Invalid date.")); return; }
                        ArrayList<LostItem> sorted = new ArrayList<>(store.getLostItems());
                        sorted.sort(Comparator.comparing(LostItem::getDate));
                        results = MatchingEngine.binarySearchLostByDate(sorted, d);
                        break;
                    }
                    default: results = store.getLostItems();
                }
                sendJson(ex, 200, JsonUtil.arr(toItemJsonList(results)));
            }
        } catch (Exception e) {
            sendJson(ex, 500, JsonUtil.obj("error", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ================= Module 2 & partial 3: /api/found =================
    private void handleFound(HttpExchange ex) throws IOException {
        try {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> f = parseBody(ex);
                LocalDate date = parseDate(f.get("date"));
                if (date == null) { sendJson(ex, 400, JsonUtil.obj("error", "Invalid date. Use yyyy-MM-dd.")); return; }
                if (missing(f, "name", "category", "color", "location", "reporter", "contact")) {
                    sendJson(ex, 400, JsonUtil.obj("error", "All fields are required.")); return;
                }
                String id = store.nextFoundId();
                FoundItem item = new FoundItem(id, f.get("name"), f.get("category"), f.get("color"), date,
                        f.get("location"), f.getOrDefault("description", ""), f.get("reporter"), f.get("contact"));
                item.setImagePath(saveImageIfPresent(f, id));
                store.addFoundItem(item);
                store.saveAll();

                List<String> matchFragments = new ArrayList<>();
                List<Map.Entry<LostItem, Integer>> hits = new ArrayList<>();
                for (LostItem li : store.getLostItems()) {
                    if (!"ACTIVE".equals(li.getStatus())) continue;
                    int score = MatchingEngine.matchScore(li, item);
                    if (score >= 5) hits.add(Map.entry(li, score));
                }
                hits.sort((a, b) -> b.getValue() - a.getValue());
                for (Map.Entry<LostItem, Integer> h : hits) matchFragments.add(itemJsonWithScore(h.getKey(), h.getValue()));

                sendJson(ex, 200, JsonUtil.obj("id", id, "status", "ok", "matches", JsonUtil.raw(JsonUtil.arr(matchFragments))));
            } else {
                Map<String, String> q = parseQuery(ex);
                String mode = q.getOrDefault("mode", "all");
                String query = q.getOrDefault("q", "");
                List<FoundItem> results;
                switch (mode) {
                    case "keyword": results = MatchingEngine.linearSearchFound(new ArrayList<>(store.getFoundItems()), query); break;
                    case "category": results = store.getFoundByCategory(query); break;
                    case "id": {
                        FoundItem fi = store.getFoundById(query); // O(1) HashMap lookup
                        results = new ArrayList<>();
                        if (fi != null) results.add(fi);
                        break;
                    }
                    default: results = new ArrayList<>(store.getFoundItems());
                }
                sendJson(ex, 200, JsonUtil.arr(toItemJsonList(results)));
            }
        } catch (Exception e) {
            sendJson(ex, 500, JsonUtil.obj("error", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ================= Module 3: suggest matches for a lost item =================
    private void handleMatches(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = parseQuery(ex);
            String lostId = q.getOrDefault("lostId", "");
            LostItem lost = store.getLostById(lostId);
            if (lost == null) { sendJson(ex, 404, JsonUtil.obj("error", "No lost item with ID " + lostId)); return; }
            List<Map.Entry<FoundItem, Integer>> matches = MatchingEngine.suggestMatches(lost, store.getFoundItems(), 3);
            List<String> fragments = new ArrayList<>();
            for (Map.Entry<FoundItem, Integer> m : matches) fragments.add(itemJsonWithScore(m.getKey(), m.getValue()));
            sendJson(ex, 200, JsonUtil.arr(fragments));
        } catch (Exception e) {
            sendJson(ex, 500, JsonUtil.obj("error", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ================= Module 4: claim & verification =================
    private void handleClaim(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, JsonUtil.obj("error", "Use POST.")); return;
            }
            Map<String, String> f = parseBody(ex);
            LostItem lost = store.getLostById(f.getOrDefault("lostId", ""));
            if (lost == null) { sendJson(ex, 404, JsonUtil.obj("error", "Lost item ID not found.")); return; }
            if (!"ACTIVE".equals(lost.getStatus())) { sendJson(ex, 409, JsonUtil.obj("error", "This lost item is already " + lost.getStatus() + ".")); return; }
            FoundItem found = store.getFoundById(f.getOrDefault("foundId", ""));
            if (found == null) { sendJson(ex, 404, JsonUtil.obj("error", "Found item ID not found.")); return; }
            if (!"ACTIVE".equals(found.getStatus())) { sendJson(ex, 409, JsonUtil.obj("error", "This found item is already " + found.getStatus() + ".")); return; }
            String claimant = f.getOrDefault("claimant", "").trim();
            String detail = f.getOrDefault("detail", "").trim();
            if (claimant.isEmpty() || detail.isEmpty()) {
                sendJson(ex, 400, JsonUtil.obj("error", "Claimant name and identifying detail are required.")); return;
            }

            int score = MatchingEngine.matchScore(lost, found);
            boolean detailMatches = found.getDescription().toLowerCase().contains(detail.toLowerCase())
                    || lost.getDescription().toLowerCase().contains(detail.toLowerCase());
            boolean verified = score >= 5 && detailMatches;

            String claimId = store.nextClaimId();
            Claim claim = new Claim(claimId, lost.getId(), found.getId(), claimant, LocalDate.now(), verified);
            store.addClaim(claim);

            String contact = null;
            if (verified) {
                lost.setStatus("RETURNED");
                found.setStatus("RETURNED");
                store.removeFoundFromActiveList(found.getId());
                contact = found.getContactInfo();
            }
            store.saveAll();

            sendJson(ex, 200, JsonUtil.obj(
                    "claimId", claimId,
                    "score", score,
                    "detailMatches", detailMatches,
                    "verified", verified,
                    "finderContact", contact
            ));
        } catch (Exception e) {
            sendJson(ex, 500, JsonUtil.obj("error", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ================= Records dump =================
    private void handleRecords(HttpExchange ex) throws IOException {
        try {
            List<String> lostFrag = toItemJsonList(store.getLostItems());
            List<String> foundFrag = toItemJsonList(new ArrayList<>(store.getFoundItems()));
            List<String> claimFrag = new ArrayList<>();
            for (Claim c : store.getClaims()) {
                claimFrag.add(JsonUtil.obj(
                        "id", c.getId(), "lostId", c.getLostItemId(), "foundId", c.getFoundItemId(),
                        "claimant", c.getClaimantName(), "date", c.getClaimDate().toString(),
                        "verified", c.isVerified()
                ));
            }
            String json = JsonUtil.obj(
                    "lost", JsonUtil.raw(JsonUtil.arr(lostFrag)),
                    "found", JsonUtil.raw(JsonUtil.arr(foundFrag)),
                    "claims", JsonUtil.raw(JsonUtil.arr(claimFrag))
            );
            sendJson(ex, 200, json);
        } catch (Exception e) {
            sendJson(ex, 500, JsonUtil.obj("error", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ================= JSON conversion helpers =================
    private List<String> toItemJsonList(List<? extends Item> items) {
        List<String> out = new ArrayList<>();
        for (Item it : items) out.add(itemJson(it));
        return out;
    }

    private String itemJson(Item it) {
        return JsonUtil.obj(
                "id", it.getId(), "name", it.getName(), "category", it.getCategory(), "color", it.getColor(),
                "date", it.getDate().toString(), "location", it.getLocation(), "description", it.getDescription(),
                "status", it.getStatus(), "reporter", it.getReporterName(), "contact", it.getContactInfo(),
                "image", it.hasImage() ? "/uploads/" + it.getImagePath() : null
        );
    }

    private String itemJsonWithScore(Item it, int score) {
        return JsonUtil.obj(
                "id", it.getId(), "name", it.getName(), "category", it.getCategory(), "color", it.getColor(),
                "date", it.getDate().toString(), "location", it.getLocation(), "description", it.getDescription(),
                "status", it.getStatus(), "reporter", it.getReporterName(), "contact", it.getContactInfo(),
                "image", it.hasImage() ? "/uploads/" + it.getImagePath() : null,
                "score", score
        );
    }

    // ================= HTTP helpers =================
    private Map<String, String> parseBody(HttpExchange ex) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ex.getRequestBody().transferTo(buf);
        return parseFormEncoded(buf.toString(StandardCharsets.UTF_8));
    }

    private Map<String, String> parseQuery(HttpExchange ex) {
        return parseFormEncoded(ex.getRequestURI().getRawQuery());
    }

    private Map<String, String> parseFormEncoded(String data) {
        Map<String, String> map = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) return map;
        for (String pair : data.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String val = eq >= 0 ? pair.substring(eq + 1) : "";
            map.put(urlDecode(key), urlDecode(val));
        }
        return map;
    }

    private String urlDecode(String s) {
        try { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private boolean missing(Map<String, String> f, String... keys) {
        for (String k : keys) if (f.get(k) == null || f.get(k).trim().isEmpty()) return true;
        return false;
    }

    private LocalDate parseDate(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; }
    }

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
