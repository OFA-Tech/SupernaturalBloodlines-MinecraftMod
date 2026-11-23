package net.ofatech.supernaturalbloodlines.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.slf4j.Logger;

/**
 * The versions of the linked mods this build was tested with (13d M10). The ranges in {@code neoforge.mods.toml} are wide enough to load (FML refuses to
 * start when an optional dependency is present but out of range, checked), so a version beyond the tested series loads with a warning instead.
 */
public final class LinkedMods {
    /** Mod id to the tested version series: a version equal to it, or continuing it (a dot, dash or plus), is tested. */
    public static final Map<String, String> TESTED = tested();

    private LinkedMods() {
    }

    private static Map<String, String> tested() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("tensura", "2.0.1");
        m.put("ascension", "1.0"); // the jar is named ascension-3.1.0 but its mods.toml declares 1.0.0; Deus Sanguis is its Unique, the rite reads one config switch of it (plan 19)
        m.put("manascore_race", "4.0");
        m.put("vampirism", "1.10.13");
        m.put("werewolves", "2.0.3");
        m.put("enchanted", "4.2.7");
        m.put("curios", "9.5,9.6"); // 9.6.0: every Curios class we call has the same public signatures as 9.5.1
        m.put("geckolib", "4.9");
        m.put("enhancedcelestials2core", "2.0.3");
        return Map.copyOf(m);
    }

    /** True when {@code actual} is none of the tested series in {@code tested} (comma-separated). Pure. */
    public static boolean untested(String actual, String tested) {
        for (String series : tested.split(",")) {
            if (!untestedSeries(actual, series.trim())) {
                return false;
            }
        }
        return true;
    }

    private static boolean untestedSeries(String actual, String tested) {
        if (actual.equals(tested)) {
            return false;
        }
        if (!actual.startsWith(tested) || actual.length() == tested.length()) {
            return true;
        }
        char next = actual.charAt(tested.length());
        return next != '.' && next != '-' && next != '+';
    }

    /** Warns once when the Accessories compat layer and Curios are both installed: that pair is known to crash. Returns whether it warned. */
    public static boolean warnCrashPair(Predicate<String> loaded, Logger log) {
        if (!loaded.test("accessories_compat_layer") || !loaded.test("curios")) {
            return false;
        }
        log.warn("accessories_compat_layer x Curios is a known crash pair; remove the compat layer.");
        return true;
    }

    /** Warns once per linked mod that is installed in an untested version; {@code versionOf} answers null for a mod that is not installed. */
    public static int warnUntested(Function<String, String> versionOf, Logger log) {
        int warned = 0;
        for (Map.Entry<String, String> e : TESTED.entrySet()) {
            String actual = versionOf.apply(e.getKey());
            if (actual != null && untested(actual, e.getValue())) {
                log.warn("{} {} is not a version this build was tested with ({}); it loads, but a hook may be missing (see the log lines about mixins)",
                        e.getKey(), actual, e.getValue());
                warned++;
            }
        }
        return warned;
    }
}
