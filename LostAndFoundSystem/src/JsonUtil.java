import java.util.List;

/**
 * Minimal hand-written JSON helpers (no external library required).
 * Only covers what this project needs: escaping strings and building
 * flat/nested JSON objects and arrays from already-formatted fragments.
 */
public class JsonUtil {

    public static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String str(String s) {
        return s == null ? "null" : "\"" + esc(s) + "\"";
    }

    /** Builds a JSON object from alternating key,value pairs. Values may be
     *  String (quoted+escaped), Integer/Boolean (raw), or a JsonRaw wrapper
     *  (already-built JSON, inserted verbatim - used for nested objects/arrays). */
    public static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(",");
            String key = (String) kv[i];
            Object val = kv[i + 1];
            sb.append(str(key)).append(":").append(toJsonValue(val));
        }
        return sb.append("}").toString();
    }

    public static String arr(List<String> jsonFragments) {
        return "[" + String.join(",", jsonFragments) + "]";
    }

    private static String toJsonValue(Object val) {
        if (val == null) return "null";
        if (val instanceof JsonRaw) return ((JsonRaw) val).raw;
        if (val instanceof Integer || val instanceof Long) return val.toString();
        if (val instanceof Boolean) return val.toString();
        return str(val.toString());
    }

    /** Wrapper marking a string as already-valid JSON (object/array) to be inlined as-is. */
    public static class JsonRaw {
        final String raw;
        public JsonRaw(String raw) { this.raw = raw; }
    }

    public static JsonRaw raw(String json) { return new JsonRaw(json); }
}
