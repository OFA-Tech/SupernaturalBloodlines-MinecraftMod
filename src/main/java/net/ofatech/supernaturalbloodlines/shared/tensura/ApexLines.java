package net.ofatech.supernaturalbloodlines.shared.tensura;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * The Tensura lines an apex race is offered from (D2), pure. The Saint-equivalent tier of each line: the saint and divine races of the
 * four human-like lines, {@code vampire_lord} and {@code divine_vampire} on the vampire line (Tensura's chain is vampire, overcomer,
 * lord, divine: the lord is the third tier, like a saint), and {@code spirit_beast} and {@code divine_beast} on Beastfolk. Which supernatural
 * races may be offered from which line is the table below. Shared by species_tensura (the offer) and supernatural_tensura (the ladders'
 * {@code tensura_tier}); no game classes. See docs/parts/species_tensura.md.
 */
public final class ApexLines {
    /** Saint and divine of human, dwarf, elf and merfolk: every race with an apex qualifies. */
    public static final Set<String> HUMAN_LIKE = Set.of(
            "tensura:human_saint", "tensura:divine_human", "tensura:dwarf_saint", "tensura:divine_dwarf",
            "tensura:elf_saint", "tensura:divine_elf", "tensura:merfolk_saint", "tensura:divine_fish");
    public static final Set<String> VAMPIRE = Set.of("tensura:vampire_lord", "tensura:divine_vampire");
    public static final Set<String> BEASTFOLK = Set.of("tensura:spirit_beast", "tensura:divine_beast");

    /** The namespace of our own apex races, which are past every line's tier. */
    public static final String OUR_NAMESPACE = "supernaturalbloodlines:";

    private ApexLines() {
    }

    /** The race is offered its apex race from this Tensura race (an apex race of ours has no further one). */
    public static boolean qualifies(SupernaturalRace race, String tensuraRaceId) {
        if (race == SupernaturalRace.HUMAN || race == SupernaturalRace.GHOUL) {
            return false;
        }
        if (HUMAN_LIKE.contains(tensuraRaceId)) {
            return true;
        }
        if (VAMPIRE.contains(tensuraRaceId)) {
            return switch (race) {
                case VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, HERETIC -> true;
                default -> false;
            };
        }
        if (BEASTFOLK.contains(tensuraRaceId)) {
            return switch (race) {
                case WEREWOLF, HYBRID, TRIBRID -> true;
                default -> false;
            };
        }
        return false;
    }

    /** The {@code tensura_tier} reading of a ladder: at the apex tier of a line the race may be on, or already on an apex race of ours. */
    public static boolean atApexTier(SupernaturalRace race, String tensuraRaceId) {
        if (race == SupernaturalRace.HUMAN || race == SupernaturalRace.GHOUL) {
            return false;
        }
        return tensuraRaceId.startsWith(OUR_NAMESPACE) || qualifies(race, tensuraRaceId);
    }
}
