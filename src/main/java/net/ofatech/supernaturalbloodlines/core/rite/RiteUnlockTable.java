package net.ofatech.supernaturalbloodlines.core.rite;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which rites are open to everyone, which unlock by kind, which by lore (2026-11-21, plan {@code rites-unlock}): the one table the server, the book
 * generator and the tests read. A rite is its path ({@code rite_of_severance}). A gated rite is known when the player is one of its kinds or has read
 * one of its lore sources; a rite that is in neither list does not exist for this table (a test walks the rite files against both). Pure.
 */
public final class RiteUnlockTable {
    /** Where a lore id comes from, for the book's hint and the docs. */
    public static final String KOL = "kol";
    public static final String ESTHER = "esther";
    public static final String FINN = "finn";
    public static final String ELIJAH = "elijah";
    public static final String OLD_RITES = "old_rites";
    public static final String CURSES = "curses";

    /** A gated rite: the kinds that know it from the start of being one, the lore that teaches it, and the id of its one-line hint (lang {@code rites.hint.<hint>}). */
    public record Gate(Set<RiteKind> kinds, Set<String> lore, String hint) {
        public Gate {
            kinds = Set.copyOf(kinds);
            lore = Set.copyOf(lore);
        }
    }

    /** Known to everyone from the start: no advancement, no sealed entry. */
    public static final List<String> OPEN = List.of("ancestral_calling", "blood_offering", "hearing_device", "magicule_offering", "purification",
            "purification_holy_water", "rite_of_revelation", "vampire_compass");

    public static final Map<String, Gate> GATED;

    static {
        Map<String, Gate> m = new LinkedHashMap<>();
        kind(m, "cleansing_blood", "vampires", RiteKind.VAMPIRE);
        kind(m, "ward_of_daylight", "vampires", RiteKind.VAMPIRE, RiteKind.WITCH);
        kind(m, "rite_of_the_five", "hunters", RiteKind.HUNTER);
        kind(m, "alpha_trial", "werewolves", RiteKind.WEREWOLF);
        kind(m, "silver_moon", "werewolves", RiteKind.WEREWOLF);
        kind(m, "werewolf_brother", "werewolves", RiteKind.WEREWOLF);
        kind(m, "doppelganger_blood", "werewolves", RiteKind.WEREWOLF, RiteKind.HYBRID);
        kind(m, "rite_of_lineage_reveal", "werewolves", RiteKind.WEREWOLF, RiteKind.WITCH);
        kind(m, "rite_of_lineage_awaken", "werewolves", RiteKind.WEREWOLF, RiteKind.TRIBRID);
        kind(m, "ward_of_the_moon", "werewolves", RiteKind.WEREWOLF, RiteKind.WITCH);
        kind(m, "hybrid_blood", "hybrids", RiteKind.HYBRID);
        kind(m, "original_blood", "originals", RiteKind.ORIGINAL);
        kind(m, "rite_of_the_moon", "originals", RiteKind.ORIGINAL);
        lore(m, "rite_of_cloaking", "kol", KOL);
        lore(m, "rite_of_linking", "esther", ESTHER);
        both(m, "rite_of_severance", "esther", Set.of(RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID), ESTHER);
        lore(m, "rite_of_the_broken_line", "finn", FINN);
        lore(m, "rite_of_the_imprisoned_original", "finn", FINN);
        both(m, "rite_of_the_cure", "elijah", Set.of(RiteKind.WITCH), ELIJAH);
        lore(m, "immortality", "old_rites", OLD_RITES);
        both(m, "ward_of_return", "old_rites", Set.of(RiteKind.WITCH), OLD_RITES);
        lore(m, "curse_magicule_drain", "curses", CURSES);
        lore(m, "curse_skill_seal", "curses", CURSES);
        GATED = java.util.Collections.unmodifiableMap(m);
    }

    private RiteUnlockTable() {
    }

    private static void kind(Map<String, Gate> m, String rite, String hint, RiteKind first, RiteKind... more) {
        Set<RiteKind> kinds = EnumSet.of(first, more);
        m.put(rite, new Gate(kinds, Set.of(), hint));
    }

    private static void lore(Map<String, Gate> m, String rite, String hint, String lore) {
        m.put(rite, new Gate(Set.of(), Set.of(lore), hint));
    }

    private static void both(Map<String, Gate> m, String rite, String hint, Set<RiteKind> kinds, String lore) {
        m.put(rite, new Gate(kinds, Set.of(lore), hint));
    }

    public static boolean isOpen(String rite) {
        return OPEN.contains(rite);
    }

    public static boolean isGated(String rite) {
        return GATED.containsKey(rite);
    }

    /** The gated rites a player of these kinds knows from being them (not what they have read). */
    public static Set<String> knownByKinds(Set<RiteKind> kinds) {
        Set<String> out = new LinkedHashSet<>();
        GATED.forEach((rite, gate) -> {
            if (gate.kinds().stream().anyMatch(kinds::contains)) {
                out.add(rite);
            }
        });
        return out;
    }

    /** The gated rites a lore source teaches, in table order; empty for a lore id that teaches nothing. */
    public static List<String> taughtBy(String lore) {
        List<String> out = new ArrayList<>();
        GATED.forEach((rite, gate) -> {
            if (gate.lore().contains(lore)) {
                out.add(rite);
            }
        });
        return out;
    }

    /** Whether a lore source is a Torn Page (the others are the two grimoires, read where they already are). */
    public static boolean isPage(String lore) {
        return !lore.equals(KOL) && !lore.equals(ESTHER);
    }

    /** Every lore id the table names. */
    public static Set<String> loreIds() {
        Set<String> out = new LinkedHashSet<>();
        GATED.values().forEach(g -> out.addAll(g.lore()));
        return out;
    }

    /** Whether a rite is known to a player of these kinds who has read these lore sources; an open rite always is. */
    public static boolean knows(String rite, Set<RiteKind> kinds, Set<String> lore) {
        if (isOpen(rite)) {
            return true;
        }
        Gate gate = GATED.get(rite);
        return gate != null && (gate.kinds().stream().anyMatch(kinds::contains) || gate.lore().stream().anyMatch(lore::contains));
    }
}
