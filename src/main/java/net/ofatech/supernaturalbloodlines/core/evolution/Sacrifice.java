package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The sacrifice of a Rite of Mastery: item kinds and how many of each, from the config (spec E16). The rite file only names the kinds (Enchanted must see
 * each one thrown on the circle to start the rite); the counts are taken in {@code onStart}. Pure: {@link #plan} says what to take from what lies loose on
 * the circle and what to give back of what Enchanted already took.
 */
public final class Sacrifice {
    private Sacrifice() {
    }

    /** One config line, {@code namespace:path:count}; empty when it is not one (no colon pair, a count below 1, a word that is not a number). */
    public static Optional<Map.Entry<String, Integer>> parseLine(String line) {
        String s = line.trim();
        int last = s.lastIndexOf(':');
        if (last <= 0 || last == s.length() - 1) {
            return Optional.empty();
        }
        String id = s.substring(0, last);
        if (id.indexOf(':') <= 0 || id.indexOf(':') == id.length() - 1 || id.chars().anyMatch(Character::isWhitespace)) {
            return Optional.empty();
        }
        try {
            int count = Integer.parseInt(s.substring(last + 1));
            return count >= 1 && count <= 4096 ? Optional.of(Map.entry(id, count)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** The kinds and counts of a config list, in order; a bad line is skipped, a kind named twice counts once (the first). */
    public static Map<String, Integer> parse(List<String> lines) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String line : lines) {
            parseLine(line).ifPresent(e -> out.putIfAbsent(e.getKey(), e.getValue()));
        }
        return out;
    }

    /**
     * @param enough   whether the circle holds the whole sacrifice (taken plus loose)
     * @param take     per kind, how many to take from what lies loose on the circle (never more than is loose, never negative)
     * @param giveBack per kind, how many of what Enchanted already took are more than the sacrifice and go back to the circle
     */
    public record Plan(boolean enough, Map<String, Integer> take, Map<String, Integer> giveBack) {
    }

    public static Plan plan(Map<String, Integer> need, Map<String, Integer> taken, Map<String, Integer> loose) {
        Map<String, Integer> take = new LinkedHashMap<>();
        Map<String, Integer> back = new LinkedHashMap<>();
        boolean enough = true;
        for (Map.Entry<String, Integer> e : need.entrySet()) {
            int have = taken.getOrDefault(e.getKey(), 0);
            int missing = e.getValue() - have;
            if (missing > 0) {
                int lying = loose.getOrDefault(e.getKey(), 0);
                if (lying < missing) {
                    enough = false;
                }
                take.put(e.getKey(), Math.min(missing, lying));
            } else if (missing < 0) {
                back.put(e.getKey(), -missing);
            }
        }
        return new Plan(enough, take, back);
    }
}
