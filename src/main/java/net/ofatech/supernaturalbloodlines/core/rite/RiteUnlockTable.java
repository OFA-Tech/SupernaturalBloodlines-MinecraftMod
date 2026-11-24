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
    public static final String BEAST = "beast";

    /** A gated rite: the kinds that know it from the start of being one, and the lore that teaches it. */
    public record Gate(Set<RiteKind> kinds, Set<String> lore) {
        public Gate {
            kinds = Set.copyOf(kinds);
            lore = Set.copyOf(lore);
        }
    }

    /** Known to everyone from the start: no advancement, no sealed entry. */
    public static final List<String> OPEN = List.of("ancestral_calling", "blood_offering", "hearing_device", "magicule_offering", "purification",
            "purification_holy_water", "rite_of_revelation", "vampire_compass");

    public static final Map<String, Gate> GATED;

    /**
     * A rite that exists but whose road is closed (plan 19b: the Raphael rite while Great Sage's own road to Raphael is closed) is not present: nothing is
     * taught or sealed for it, so the book shows neither entry until it opens. Set by the part that owns the rite; nothing is dormant by default.
     */
    public static volatile java.util.function.Predicate<String> dormant = rite -> false;

    static {
        Map<String, Gate> m = new LinkedHashMap<>();
        kind(m, "cleansing_blood", RiteKind.VAMPIRE);
        kind(m, "augustine", RiteKind.VAMPIRE);
        kind(m, "ward_of_daylight", RiteKind.VAMPIRE, RiteKind.WITCH);
        kind(m, "rite_of_the_five", RiteKind.HUNTER);
        kind(m, "alpha_trial", RiteKind.WEREWOLF);
        kind(m, "rite_of_the_frequency", RiteKind.WEREWOLF);
        kind(m, "silver_moon", RiteKind.WEREWOLF);
        kind(m, "werewolf_brother", RiteKind.WEREWOLF);
        kind(m, "doppelganger_blood", RiteKind.WEREWOLF, RiteKind.HYBRID);
        kind(m, "rite_of_lineage_reveal", RiteKind.WEREWOLF, RiteKind.WITCH);
        kind(m, "rite_of_lineage_awaken", RiteKind.WEREWOLF, RiteKind.TRIBRID);
        kind(m, "ward_of_the_moon", RiteKind.WEREWOLF, RiteKind.WITCH);
        kind(m, "hybrid_blood", RiteKind.HYBRID);
        kind(m, "original_blood", RiteKind.ORIGINAL);
        kind(m, "rite_of_the_moon", RiteKind.ORIGINAL);
        // plan 19: the evolution rites and the Rite of Mastery, sealed in the book and known by kind (D5, D15): a witch type works them, a vampire learns that Deus Sanguis exists
        kind(m, "evolve_great_sage", RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID);
        kind(m, "evolve_deus_sanguis", RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID, RiteKind.VAMPIRE);
        // the Ultimates: sealed until Finn's Notes, "the rites of the eclipse" (E7, Q4)
        lore(m, "evolve_blood_sovereign", FINN);
        lore(m, "evolve_raphael", FINN);
        for (String tier : List.of("common", "extra", "unique", "ultimate")) {
            kind(m, "mastery_" + tier, RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID);
        }
        kind(m, "cursed_dagger", RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID);
        lore(m, "rite_of_cloaking", KOL);
        lore(m, "rite_of_linking", ESTHER);
        both(m, "rite_of_severance", Set.of(RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID), ESTHER);
        lore(m, "rite_of_the_broken_line", FINN);
        lore(m, "rite_of_the_imprisoned_original", FINN);
        both(m, "rite_of_the_cure", Set.of(RiteKind.WITCH), ELIJAH);
        both(m, "rite_of_the_valet_pike", Set.of(RiteKind.WITCH), BEAST);
        lore(m, "immortality", OLD_RITES);
        both(m, "ward_of_return", Set.of(RiteKind.WITCH), OLD_RITES);
        lore(m, "curse_magicule_drain", CURSES);
        lore(m, "curse_skill_seal", CURSES);
        GATED = java.util.Collections.unmodifiableMap(m);
    }

    private RiteUnlockTable() {
    }

    private static void kind(Map<String, Gate> m, String rite, RiteKind first, RiteKind... more) {
        Set<RiteKind> kinds = EnumSet.of(first, more);
        m.put(rite, new Gate(kinds, Set.of()));
    }

    private static void lore(Map<String, Gate> m, String rite, String lore) {
        m.put(rite, new Gate(Set.of(), Set.of(lore)));
    }

    private static void both(Map<String, Gate> m, String rite, Set<RiteKind> kinds, String lore) {
        m.put(rite, new Gate(kinds, Set.of(lore)));
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

    /** What {@code /bloodline rites learn|forget} and the Admin page take: {@code all}, each gated rite and each lore id. */
    public static List<String> choices() {
        List<String> out = new ArrayList<>();
        out.add("all");
        out.addAll(GATED.keySet());
        out.addAll(loreIds());
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
