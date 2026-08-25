package edu.ug.nexusb.web;

/**
 * Minimal hand-rolled JSON response builder for {@link ApiServer} -- no
 * external JSON library, consistent with the rest of this project building
 * its own infrastructure rather than reaching for a dependency. Only
 * supports what the API actually needs: objects, arrays, strings, numbers,
 * booleans, and raw (already-JSON) fragments for nesting.
 */
final class Json {

    private final StringBuilder buffer = new StringBuilder();
    private boolean needsComma = false;

    private Json() {
    }

    static Json object() {
        Json json = new Json();
        json.buffer.append('{');
        return json;
    }

    static Json array() {
        Json json = new Json();
        json.buffer.append('[');
        return json;
    }

    Json field(String name, String value) {
        comma();
        key(name);
        buffer.append(value == null ? "null" : quote(value));
        return this;
    }

    Json field(String name, double value) {
        comma();
        key(name);
        buffer.append(Double.isFinite(value) ? formatNumber(value) : "null");
        return this;
    }

    Json field(String name, long value) {
        comma();
        key(name);
        buffer.append(value);
        return this;
    }

    Json field(String name, boolean value) {
        comma();
        key(name);
        buffer.append(value);
        return this;
    }

    /** Nests an already-built {@link Json} value (object or array) under {@code name}. */
    Json field(String name, Json nested) {
        comma();
        key(name);
        buffer.append(nested.close());
        return this;
    }

    Json element(String value) {
        comma();
        buffer.append(quote(value));
        return this;
    }

    Json element(double value) {
        comma();
        buffer.append(Double.isFinite(value) ? formatNumber(value) : "null");
        return this;
    }

    /** Appends an already-built {@link Json} value as the next array element. */
    Json element(Json nested) {
        comma();
        buffer.append(nested.close());
        return this;
    }

    /** Terminates this object/array and returns the finished JSON text. Idempotent. */
    String close() {
        char first = buffer.charAt(0);
        char expectedClose = first == '{' ? '}' : ']';
        if (buffer.charAt(buffer.length() - 1) != expectedClose) {
            buffer.append(expectedClose);
        }
        return buffer.toString();
    }

    private void comma() {
        if (needsComma) {
            buffer.append(',');
        }
        needsComma = true;
    }

    private void key(String name) {
        buffer.append(quote(name)).append(':');
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
