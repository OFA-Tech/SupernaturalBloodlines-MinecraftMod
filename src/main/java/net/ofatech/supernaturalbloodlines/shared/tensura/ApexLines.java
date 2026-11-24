package net.ofatech.supernaturalbloodlines.shared.tensura;

import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * The Tensura lines an apex race is offered from (D2), pure. The Saint-equivalent tier of each line: the saint and divine races of the
 * four human-like lines (and Tensura Horizons' rung between them for the elf, dwarf and merfolk lines), {@code vampire_lord} and {@code divine_vampire} on the vampire line (Tensura's chain is vampire, overcomer,
 * lord, divine: the lord is the third tier, like a saint), and {@code spirit_beast} and {@code divine_beast} on Beastfolk. Which supernatural
 * races may be offered from which line is the table below. Shared by species_tensura (the offer) and supernatural_tensura (the ladders'
 * {@code tensura_tier}); no game classes. See docs/parts/species_tensura.md.
 */
public final class ApexLines {
    /** Saint and divine of human, dwarf, elf and merfolk: every race with an apex qualifies. */
    public static final Set<String> HUMAN_LIKE = Set.of(
            "tensura:human_saint", "tensura:divine_human", "tensura:dwarf_saint", "tensura:divine_dwarf",
            "tensura:elf_saint", "tensura:divine_elf", "tensura:merfolk_saint", "tensura:divine_fish",
            // Tensura Horizons puts one more rung between the saint and the divine race of the elf, dwarf and merfolk lines; a player on it is past the saint tier
            "tensurahorizons:ascendant_elf", "tensurahorizons:runeforged_dwarf", "tensurahorizons:tide_sovereign");
    /** Ascension's Bloodfiend chain is fledgling, kindred, blood noble, elder, progenitor: the blood noble is its third tier, like the vampire lord (plan 19d). */
    public static final Set<String> VAMPIRE = Set.of("tensura:vampire_lord", "tensura:divine_vampire",
            "ascension:blood_noble", "ascension:elder_bloodfiend", "ascension:progenitor_bloodfiend");
    public static final Set<String> BEASTFOLK = Set.of("tensura:spirit_beast", "tensura:divine_beast");

    /** The namespace of our own apex races, which are past every line's tier. */
    public static final String OUR_NAMESPACE = "supernaturalbloodlines:";

    private ApexLines() {
    }

    /** {@link #qualifies(SupernaturalRace, String, List)} for a player whose only race is {@code race}. */
    public static boolean qualifies(SupernaturalRace race, String tensuraRaceId) {
        return qualifies(race, tensuraRaceId, List.of(race));
    }

    /**
     * The race is offered its apex race from this Tensura race (an apex race of ours has no further one). {@code races} is all the player's
     * races: a witch or siphoner is on the Beastfolk line only as a werewolf (human werewolf, Beastfolk), and the magic ladder stacks on it.
     */
    public static boolean qualifies(SupernaturalRace race, String tensuraRaceId, List<SupernaturalRace> races) {
        if (race == SupernaturalRace.HUMAN || race == SupernaturalRace.GHOUL) {
            return false;
        }
        if (HUMAN_LIKE.contains(tensuraRaceId)) {
            return true;
        }
        if (VAMPIRE.contains(tensuraRaceId)) {
            return switch (race) {
                case VAMPIRE, AUGUSTINE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, HERETIC -> true;
                default -> false;
            };
        }
        if (BEASTFOLK.contains(tensuraRaceId)) {
            return switch (race) {
                case WEREWOLF, HYBRID, TRIBRID -> true;
                case WITCH, SIPHONER -> races.contains(SupernaturalRace.WEREWOLF);
                default -> false;
            };
        }
        return false;
    }

    public static boolean atApexTier(SupernaturalRace race, String tensuraRaceId) {
        return atApexTier(race, tensuraRaceId, List.of(race));
    }

    /** The {@code tensura_tier} reading of a ladder: at the apex tier of a line the race may be on, or already on an apex race of ours. */
    public static boolean atApexTier(SupernaturalRace race, String tensuraRaceId, List<SupernaturalRace> races) {
        if (race == SupernaturalRace.HUMAN || race == SupernaturalRace.GHOUL) {
            return false;
        }
        return tensuraRaceId.startsWith(OUR_NAMESPACE) || qualifies(race, tensuraRaceId, races);
    }
}
