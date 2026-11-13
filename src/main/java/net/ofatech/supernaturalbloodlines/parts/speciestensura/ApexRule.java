package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R103: hunters and werewolves can't be undead or daemon races; the apex vampire-type races are the exception, for
 * vampire-type species only. An apex race is one of ours: its race id is in this mod's namespace. No game classes.
 */
public final class ApexRule {
    public static final String APEX_NAMESPACE = "supernaturalbloodlines";

    private ApexRule() {}

    /** The species whose members count as vampires for R103 (what the species is, not what the config gives it). */
    public static boolean isVampireType(Species species) {
        return switch (species) {
            case ORIGINAL, ORIGINAL_HYBRID, HYBRID, HERETIC, TRIBRID -> true;
            default -> false;
        };
    }

    public static boolean isApexVampire(Species species, String raceNamespace) {
        return isVampireType(species) && APEX_NAMESPACE.equals(raceNamespace);
    }
}
