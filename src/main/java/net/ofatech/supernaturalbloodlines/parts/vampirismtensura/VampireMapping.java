package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.jetbrains.annotations.Nullable;

/** Pure Tensura-race to Vampirism-level mapping. No game classes, so it is unit-testable. */
public record VampireMapping(String cureRace, Map<String, Integer> floors, int maxLevel) {
    public static final String GHOUL = "tensura:ghoul";
    public static final String DEFAULT_CURE_RACE = "tensura:human";
    public static final List<String> DEFAULT_FLOORS = List.of(
            "tensura:vampire=1",
            "tensura:vampire_overcomer=7",
            "tensura:vampire_lord=7",
            "tensura:divine_vampire=-1");

    /** A mapping plus the config entries that could not be used. */
    public record Parsed(VampireMapping mapping, List<String> rejected) {}

    /** Empty when the race has no faction level (not in the line, or ghoul); else max(floor, current), at most maxLevel. */
    public OptionalInt levelForRace(String raceId, int currentLevel) {
        Integer floor = floors.get(raceId);
        if (floor == null) {
            return OptionalInt.empty();
        }
        int f = floor == -1 ? maxLevel : floor;
        return OptionalInt.of(Math.min(Math.max(f, currentLevel), maxLevel));
    }

    public Optional<String> raceOnCure(@Nullable String currentRace) {
        if (currentRace != null && (GHOUL.equals(currentRace) || floors.containsKey(currentRace))) {
            return Optional.of(cureRace);
        }
        return Optional.empty();
    }

    public static VampireMapping defaults(int maxLevel) {
        return parse(DEFAULT_CURE_RACE, DEFAULT_FLOORS, maxLevel).mapping();
    }

    /** Entries are "race=level"; level -1 means max. Malformed entries (or level 0 or below -1) are rejected. */
    public static Parsed parse(String cureRace, List<String> entries, int maxLevel) {
        Map<String, Integer> floors = new LinkedHashMap<>();
        List<String> rejected = new ArrayList<>();
        for (String e : entries) {
            int eq = e.lastIndexOf('=');
            String race = eq > 0 ? e.substring(0, eq).trim() : "";
            try {
                int level = eq > 0 ? Integer.parseInt(e.substring(eq + 1).trim()) : 0;
                if (!race.isEmpty() && (level == -1 || level > 0)) {
                    floors.put(race, level);
                    continue;
                }
            } catch (NumberFormatException ignored) {
                // falls through to rejected
            }
            rejected.add(e);
        }
        return new Parsed(new VampireMapping(cureRace, Map.copyOf(floors), maxLevel), List.copyOf(rejected));
    }
}
