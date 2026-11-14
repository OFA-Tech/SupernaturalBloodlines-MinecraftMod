package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexSpec;

/**
 * R103: hunters and werewolves can't be undead or daemon races; the apex vampire-type races are the exception, for
 * vampire-type species only. An apex race is one of ours: its race id is in this mod's namespace. No game classes.
 * <p>
 * It matches by namespace only, not by the list in {@code ApexSpec}: any race another mod or a datapack registers under
 * {@code supernaturalbloodlines:} would be exempt too, and so would an apex race of a non-undead kind (harmless, it is
 * not undead). Only the apex races of this part live in that namespace.
 */
public final class ApexRule {
    private ApexRule() {}

    /** The species whose members count as vampires for R103 (what the species is, not what the config gives it). */
    public static boolean isVampireType(Species species) {
        return switch (species) {
            case ORIGINAL, ORIGINAL_HYBRID, HYBRID, HERETIC, TRIBRID -> true;
            default -> false;
        };
    }

    public static boolean isApexVampire(Species species, String raceNamespace) {
        return isVampireType(species) && ApexSpec.RACE_NAMESPACE.equals(raceNamespace);
    }
}
