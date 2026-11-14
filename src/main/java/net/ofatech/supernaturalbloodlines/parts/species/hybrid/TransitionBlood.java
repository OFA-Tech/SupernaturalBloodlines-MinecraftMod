package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.jetbrains.annotations.Nullable;

/**
 * Whose Original's Blood may start the hybrid transition (R40, R40a), as pure rules. The bottle records the species of the one it was
 * taken from when the rite fills it, so no lookup is needed and the source can be offline.
 */
public final class TransitionBlood {
    private TransitionBlood() {
    }

    /**
     * @param anyOriginal   {@code hybrid_transition_blood = original}: blood of an Original or an Original hybrid. Otherwise
     *                      ({@code original_hybrid}, the default) only an Original hybrid's.
     * @param sourceSpecies the species name recorded on the bottle; null for a bottle made before the update, which is accepted
     *                      under either setting so that nothing a player holds stops working
     */
    public static boolean accepts(boolean anyOriginal, @Nullable String sourceSpecies) {
        if (sourceSpecies == null) {
            return true;
        }
        Species s = parse(sourceSpecies);
        return s == Species.ORIGINAL_HYBRID || anyOriginal && s == Species.ORIGINAL;
    }

    private static Species parse(String name) {
        try {
            return Species.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return Species.NONE;
        }
    }
}
