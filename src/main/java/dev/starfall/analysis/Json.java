package dev.starfall.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader/writer, so the analysis tools have no dependency outside the JDK.
 *
 * <p>The whole feedback loop is meant to be runnable by an agent with one command and no
 * setup; adding a JSON library would mean a network fetch on a machine that may not have one.
 * This covers the subset actually needed: region files in, measurement reports out.
 */
public final class Json {

    private Json() {
    }

    // ---------------------------------------------------------------- writing

    /** Streaming writer producing indented JSON. */
    public static final class Writer {
        private final StringBuilder sb = new StringBuilder();
        private final List<Boolean> first = new ArrayList<>();
        private int indent;

        public Writer beginObject() {
            prefix();
            sb.append('{');
            first.add(true);
            indent++;
            return this;
        }

        public Writer endObject() {
            indent--;
            first.remove(first.size() - 1);
            sb.append('\n').append("  ".repeat(indent)).append('}');
            return this;
        }

        public Writer beginArray() {
            prefix();
            sb.append('[');
            first.add(true);
            indent++;
            return this;
        }

        public Writer endArray() {
            indent--;
            first.remove(first.size() - 1);
            sb.append('\n').append("  ".repeat(indent)).append(']');
            return this;
        }

        public Writer name(String n) {
            prefix();
            sb.append(quote(n)).append(": ");
            pending = true;
            return this;
        }

        private boolean pending;

        private void prefix() {
            if (pending) {
                pending = false;
                return;
            }
            if (!first.isEmpty()) {
                if (first.get(first.size() - 1)) {
                    first.set(first.size() - 1, false);
                } else {
                    sb.append(',');
                }
                sb.append('\n').append("  ".repeat(indent));
            }
        }

        public Writer value(String v) {
            prefix();
            sb.append(v == null ? "null" : quote(v));
            return this;
        }

        public Writer value(double v) {
            prefix();
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                sb.append("null");
            } else if (v == Math.rint(v) && Math.abs(v) < 1e15) {
                sb.append((long) v);
            } else {
                sb.append(String.format(java.util.Locale.ROOT, "%.4f", v));
            }
            return this;
        }

        public Writer value(long v) {
            prefix();
            sb.append(v);
            return this;
        }

        public Writer value(boolean v) {
            prefix();
            sb.append(v);
            return this;
        }

        public Writer prop(String n, String v) {
            return name(n).value(v);
        }

        public Writer prop(String n, double v) {
            return name(n).value(v);
        }

        public Writer prop(String n, long v) {
            return name(n).value(v);
        }

        public Writer prop(String n, boolean v) {
            return name(n).value(v);
        }

        public Writer prop(String n, Rect r) {
            name(n).beginArray();
            value(r.x).value(r.y).value(r.w).value(r.h);
            return endArray();
        }

        public Writer prop(String n, double[] values) {
            name(n).beginArray();
            for (double v : values) {
                value(v);
            }
            return endArray();
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    public static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
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
        return out.append('"').toString();
    }

    // ---------------------------------------------------------------- reading

    /** Parses into String, Double, Boolean, null, {@code List<Object>} or {@code Map<String,Object>}. */
    public static Object parse(String text) {
        P p = new P(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i < p.s.length()) {
            throw new IllegalArgumentException("trailing content at offset " + p.i);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        return (Map<String, Object>) v;
    }

    private static final class P {
        final String s;
        int i;

        P(String s) {
            this.s = s;
        }

        void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        char peek() {
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end of JSON");
            }
            return s.charAt(i);
        }

        Object value() {
            ws();
            char c = peek();
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        Object literal(String lit, Object v) {
            if (!s.startsWith(lit, i)) {
                throw new IllegalArgumentException("bad literal at offset " + i);
            }
            i += lit.length();
            return v;
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (peek() == '}') {
                i++;
                return m;
            }
            while (true) {
                ws();
                String k = string();
                ws();
                if (peek() != ':') {
                    throw new IllegalArgumentException("expected ':' at offset " + i);
                }
                i++;
                m.put(k, value());
                ws();
                char c = peek();
                i++;
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("expected ',' or '}' at offset " + (i - 1));
                }
            }
        }

        List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            ws();
            if (peek() == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(value());
                ws();
                char c = peek();
                i++;
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("expected ',' or ']' at offset " + (i - 1));
                }
            }
        }

        String string() {
            if (peek() != '"') {
                throw new IllegalArgumentException("expected a string at offset " + i);
            }
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Double number() {
            int start = i;
            while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            if (start == i) {
                throw new IllegalArgumentException("expected a value at offset " + i);
            }
            return Double.valueOf(s.substring(start, i));
        }
    }
}
