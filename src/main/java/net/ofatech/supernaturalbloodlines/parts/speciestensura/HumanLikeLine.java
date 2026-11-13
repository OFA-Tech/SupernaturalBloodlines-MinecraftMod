package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexSpec;

/**
 * Tensura's {@code human_like} race tag holds only the five base races (human, dwarf, elf, merfolk, beastfolk), so a
 * race that evolved inside its line is listed here too. No game classes. See docs/parts/species_tensura.md.
 */
public final class HumanLikeLine {
    /** The evolved races of the five lines. */
    public static final Set<String> EVOLVED = Set.of(
            "tensura:enlightened_human", "tensura:human_saint", "tensura:divine_human",
            "tensura:enlightened_dwarf", "tensura:dwarf_saint", "tensura:divine_dwarf",
            "tensura:enlightened_elf", "tensura:elf_saint", "tensura:divine_elf",
            "tensura:enlightened_merfolk", "tensura:merfolk_saint", "tensura:divine_fish",
            "tensura:beast_lord", "tensura:spirit_beast", "tensura:divine_beast");

    /** Tier 3 and 4 of a line: the "saint or higher" races that may be offered an apex race. */
    public static final Set<String> SAINT_OR_HIGHER = Set.of(
            "tensura:human_saint", "tensura:divine_human", "tensura:dwarf_saint", "tensura:divine_dwarf",
            "tensura:elf_saint", "tensura:divine_elf", "tensura:merfolk_saint", "tensura:divine_fish",
            "tensura:spirit_beast", "tensura:divine_beast");

    private HumanLikeLine() {}

    /** An evolved race of the five lines, or an apex race of ours (those are tagged human_like too). */
    public static boolean isEvolvedMember(String raceId) {
        return EVOLVED.contains(raceId) || ApexSpec.ALL.stream().anyMatch(s -> raceId.equals(ApexSpec.RACE_NAMESPACE + ":" + s.id()));
    }

    public static boolean isSaintOrHigher(String raceId) {
        return SAINT_OR_HIGHER.contains(raceId);
    }
}
