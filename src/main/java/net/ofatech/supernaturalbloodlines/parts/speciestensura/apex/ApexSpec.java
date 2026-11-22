package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * One apex race: the stat bonuses (final, see docs/balance.md) and its intrinsic skills. The species layer
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

    /** Plain Vampire (D1). */
    public static final ApexSpec ELDER_VAMPIRE = new ApexSpec("elder_vampire", 900, 20, 0.02, List.of("tensura:self_regeneration"), true);
    /** Plain Werewolf (D1); R103: not undead. */
    public static final ApexSpec ALPHA_PRIMEVAL = new ApexSpec("alpha_primeval", 900, 25, 0.03, List.of("tensura:godwolf_sense"), false);

    public static final List<ApexSpec> ALL = List.of(PRIMORDIAL_VAMPIRE, PRIMORDIAL_HYBRID, MOONBLOOD_HYBRID, ANCESTRAL_WITCH,
            VOID_SIPHON, TRIBRID_ASCENDANT, ETERNAL_HUNTER, ELDER_VAMPIRE, ALPHA_PRIMEVAL);

    public ApexSpec {
        intrinsicSkills = List.copyOf(intrinsicSkills);
    }

    /** The apex race of a supernatural race. Whether a given player may be offered it (a Potential is not a hunter of the Five) is {@code ApexOffer}'s. */
    public static Optional<ApexSpec> forRace(SupernaturalRace race) {
        return Optional.ofNullable(switch (race) {
            case VAMPIRE, AUGUSTINE -> ELDER_VAMPIRE; // an Augustine is offered what a plain vampire is (owner, Q3)
            case ORIGINAL -> PRIMORDIAL_VAMPIRE;
            case ORIGINAL_HYBRID -> PRIMORDIAL_HYBRID;
            case HYBRID -> MOONBLOOD_HYBRID;
            case WITCH -> ANCESTRAL_WITCH;
            case SIPHONER, HERETIC -> VOID_SIPHON;
            case TRIBRID -> TRIBRID_ASCENDANT;
            case HUNTER -> ETERNAL_HUNTER;
            case WEREWOLF -> ALPHA_PRIMEVAL;
            case HUMAN, GHOUL -> null;
        });
    }
}
