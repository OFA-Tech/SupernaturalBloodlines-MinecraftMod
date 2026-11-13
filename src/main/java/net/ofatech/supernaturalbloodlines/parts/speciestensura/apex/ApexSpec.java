package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * One apex race: the stat bonuses (placeholders, the owner tunes them) and its intrinsic skills. The species layer
 * skills are not repeated here: the layer owns and tracks them, so they go dormant when the species is lost, while the
 * race keeps its signature skill. No game classes.
 *
 * @param health  added to max health
 * @param attack  added to attack damage
 * @param speed   added to movement speed (the base is 0.1)
 * @param undead  vampire type: tagged {@code tensura:undead} too
 */
public record ApexSpec(String id, double health, double attack, double speed, List<String> intrinsicSkills, boolean undead) {
    public static final String RACE_NAMESPACE = "supernaturalbloodlines";

    public static final ApexSpec PRIMORDIAL_VAMPIRE = new ApexSpec("primordial_vampire", 1000, 20, 0.02, List.of("tensura:infinite_regeneration"), true);
    public static final ApexSpec PRIMORDIAL_HYBRID = new ApexSpec("primordial_hybrid", 1000, 25, 0.03, List.of("tensura:ultraspeed_regeneration"), true);
    public static final ApexSpec MOONBLOOD_HYBRID = new ApexSpec("moonblood_hybrid", 900, 25, 0.03, List.of("tensura:godwolf_sense"), true);
    public static final ApexSpec ANCESTRAL_WITCH = new ApexSpec("ancestral_witch", 800, 10, 0.01, List.of("tensura:magic_sense"), false);
    public static final ApexSpec VOID_SIPHON = new ApexSpec("void_siphon", 900, 15, 0.02, List.of("tensura:gluttony"), false);
    public static final ApexSpec TRIBRID_ASCENDANT = new ApexSpec("tribrid_ascendant", 1100, 25, 0.03, List.of("tensura:ultra_instinct"), true);
    public static final ApexSpec ETERNAL_HUNTER = new ApexSpec("eternal_hunter", 900, 20, 0.02, List.of("tensura:danger_sense"), false);

    public static final List<ApexSpec> ALL = List.of(PRIMORDIAL_VAMPIRE, PRIMORDIAL_HYBRID, MOONBLOOD_HYBRID, ANCESTRAL_WITCH,
            VOID_SIPHON, TRIBRID_ASCENDANT, ETERNAL_HUNTER);

    public ApexSpec {
        intrinsicSkills = List.copyOf(intrinsicSkills);
    }

    public static Optional<ApexSpec> forSpecies(Species species) {
        return Optional.ofNullable(switch (species) {
            case ORIGINAL -> PRIMORDIAL_VAMPIRE;
            case ORIGINAL_HYBRID -> PRIMORDIAL_HYBRID;
            case HYBRID -> MOONBLOOD_HYBRID;
            case WITCH -> ANCESTRAL_WITCH;
            case SIPHONER, HERETIC -> VOID_SIPHON;
            case TRIBRID -> TRIBRID_ASCENDANT;
            case HUNTER_OF_THE_FIVE -> ETERNAL_HUNTER;
            case NONE, POTENTIAL -> null;
        });
    }
}
