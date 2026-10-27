package weatherapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, read-only JSON reader.
 *
 * <p>The app only needs to read the replies of the weather API, so a tiny parser keeps the
 * project free of external libraries (it compiles and runs with a plain JDK). A parsed value
 * is navigated with {@link #get(String)} and {@link #get(int)}. Asking for something that
 * is not there never throws; it returns a "missing" value, which makes chained lookups such
 * as {@code root.get("current").get("temperature_2m")} safe. A JSON {@code null} counts as
 * missing too, because that is how the API reports data it does not have.
 */
public final class Json {

    private static final Json MISSING = new Json(null);
    private static final int MAX_DEPTH = 64;

    /** A Map, List, String, Double, Boolean, or null. */
    private final Object value;

    private Json(Object value) {
        this.value = value;
    }

    /** Parses a complete JSON document; throws IllegalArgumentException if it is malformed. */
    public static Json parse(String text) {
        Parser parser = new Parser(text);
        Object result = parser.readValue(0);
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw parser.error("unexpected text after the end of the JSON value");
        }
        return new Json(result);
    }

    /** The value used when something is absent. */
    public static Json missing() {
        return MISSING;
    }

    /** The property with the given name, or a missing value. */
    public Json get(String key) {
        if (value instanceof Map<?, ?> map && map.containsKey(key)) {
            return new Json(map.get(key));
        }
        return MISSING;
    }

    /** The array element at the given index, or a missing value. */
    public Json get(int index) {
        if (value instanceof List<?> list && index >= 0 && index < list.size()) {
            return new Json(list.get(index));
        }
        return MISSING;
    }

    /** The number of elements of an array or properties of an object (0 for anything else). */
    public int size() {
        if (value instanceof List<?> list) {
            return list.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        return 0;
    }

    public boolean isMissing() {
        return value == null;
    }

    public boolean isNumber() {
        return value instanceof Double;
    }

    public double asDouble(double fallback) {
        return value instanceof Double number ? number : fallback;
    }

    public int asInt(int fallback) {
        return value instanceof Double number ? (int) Math.round(number) : fallback;
    }

    public String asString(String fallback) {
        return value instanceof String text ? text : fallback;
    }

    public boolean asBoolean(boolean fallback) {
        return value instanceof Boolean flag ? flag : fallback;
    }

    /** Recursive-descent parser for the JSON grammar (RFC 8259). */
    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        boolean atEnd() {
            return pos >= text.length();
        }

        IllegalArgumentException error(String problem) {
            return new IllegalArgumentException("Invalid JSON at position " + pos + ": " + problem);
        }

        void skipWhitespace() {
            while (!atEnd() && " \t\n\r".indexOf(text.charAt(pos)) >= 0) {
                pos++;
            }
        }

        /** The current character, or '\0' at the end of the input. */
        private char peek() {
            return atEnd() ? '\0' : text.charAt(pos);
        }

        private void expect(char expected) {
            if (peek() != expected) {
                throw error("expected '" + expected + "'");
            }
            pos++;
        }

        Object readValue(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("nesting is too deep");
            }
            skipWhitespace();
            return switch (peek()) {
                case '{' -> readObject(depth);
                case '[' -> readArray(depth);
                case '"' -> readString();
                case 't' -> readWord("true", Boolean.TRUE);
                case 'f' -> readWord("false", Boolean.FALSE);
                case 'n' -> readWord("null", null);
                default -> readNumber();
            };
        }

        private Object readWord(String word, Object result) {
            if (!text.startsWith(word, pos)) {
                throw error("unexpected character");
            }
            pos += word.length();
            return result;
        }

        private Map<String, Object> readObject(int depth) {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;                                   // the opening '{'
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("expected a quoted property name");
                }
                String key = readString();
                skipWhitespace();
                expect(':');
                map.put(key, readValue(depth + 1));
                skipWhitespace();
                if (peek() == ',') {
                    pos++;
                } else {
                    expect('}');
                    return map;
                }
            }
        }

        private List<Object> readArray(int depth) {
            List<Object> list = new ArrayList<>();
            pos++;                                   // the opening '['
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(readValue(depth + 1));
                skipWhitespace();
                if (peek() == ',') {
                    pos++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        private String readString() {
            pos++;                                   // the opening quote
            StringBuilder out = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw error("the string is not closed");
                }
                char c = text.charAt(pos++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw error("the string is not closed");
                }
                char escape = text.charAt(pos++);
                switch (escape) {
                    case '"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(readUnicodeEscape());
                    default -> throw error("unknown escape sequence '\\" + escape + "'");
                }
            }
        }

        private char readUnicodeEscape() {
            if (pos + 4 > text.length()) {
                throw error("incomplete \\u escape");
            }
            try {
                char c = (char) Integer.parseInt(text.substring(pos, pos + 4), 16);
                pos += 4;
                return c;
            } catch (NumberFormatException e) {
                throw error("bad \\u escape");
            }
        }

        private Double readNumber() {
            int start = pos;
            while (!atEnd() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
                pos++;
            }
            if (start == pos) {
                throw error("unexpected character");
            }
            try {
                return Double.parseDouble(text.substring(start, pos));
            } catch (NumberFormatException e) {
                pos = start;
                throw error("malformed number");
            }
        }
    }
}
