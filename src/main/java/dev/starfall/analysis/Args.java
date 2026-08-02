package dev.starfall.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny command-line parser: positionals, {@code --key value} options, repeatable options, flags. */
public final class Args {

    private final List<String> positionals = new ArrayList<>();
    private final Map<String, List<String>> options = new LinkedHashMap<>();

    public Args(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String a = argv[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String value = null;
                int eq = key.indexOf('=');
                if (eq >= 0) {
                    value = key.substring(eq + 1);
                    key = key.substring(0, eq);
                } else if (i + 1 < argv.length && !argv[i + 1].startsWith("--")) {
                    value = argv[++i];
                }
                options.computeIfAbsent(key, k -> new ArrayList<>()).add(value == null ? "true" : value);
            } else {
                positionals.add(a);
            }
        }
    }

    public String positional(int i) {
        return i < positionals.size() ? positionals.get(i) : null;
    }

    public String requirePositional(int i, String what) {
        String v = positional(i);
        if (v == null) {
            throw new IllegalArgumentException("missing argument: " + what);
        }
        return v;
    }

    public List<String> positionals() {
        return positionals;
    }

    public boolean has(String key) {
        return options.containsKey(key);
    }

    public boolean flag(String key) {
        return has(key) && !"false".equalsIgnoreCase(options.get(key).get(0));
    }

    public String get(String key, String fallback) {
        List<String> v = options.get(key);
        return v == null || v.isEmpty() ? fallback : v.get(v.size() - 1);
    }

    public String require(String key, String why) {
        String v = get(key, null);
        if (v == null) {
            throw new IllegalArgumentException("--" + key + " is required: " + why);
        }
        return v;
    }

    public List<String> all(String key) {
        return options.getOrDefault(key, List.of());
    }

    public int getInt(String key, int fallback) {
        String v = get(key, null);
        return v == null ? fallback : Integer.parseInt(v.trim());
    }

    public double getDouble(String key, double fallback) {
        String v = get(key, null);
        return v == null ? fallback : Double.parseDouble(v.trim());
    }

    public File getFile(String key, File fallback) {
        String v = get(key, null);
        return v == null ? fallback : new File(v);
    }

    public java.util.Set<String> keys() {
        return options.keySet();
    }
}
