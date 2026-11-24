package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** The order of checks of an evolution rite (spec 6.4) and what each refusal says. Pure. */
public final class EvolutionRules {
    public static final String CLOSED = "supernaturalbloodlines.refusal.evolution.closed";
    public static final String NOT_WITCH = "supernaturalbloodlines.refusal.evolution.not_witch";
    public static final String NEEDS_FULL_MOON = "supernaturalbloodlines.rite.needs_full_moon";
    public static final String NEEDS_ECLIPSE = "supernaturalbloodlines.rite.needs_eclipse";
    public static final String SNEAK_TO_EVOLVE = "supernaturalbloodlines.rite.sneak_to_evolve";
    public static final String ALREADY = "supernaturalbloodlines.refusal.evolution.already";
    public static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";

    public enum Reason { CLOSED, NOT_WITCH, WRONG_NIGHT, NO_CONSENT, ALREADY, NOT_READY }

    /** What is true of the performer, the world and the target at the moment of the rite. */
    public record Facts(boolean enabled, boolean modLoaded, boolean probeFound, boolean roadOpen, boolean casterWitch, boolean fullMoonNight,
                        boolean eclipse, boolean casterIsTarget, boolean targetSneaking, boolean holdsGrant, boolean kindOk, boolean holdsNeeds,
                        boolean probeSatisfied, boolean ownedByOther, boolean enoughEp, boolean retired) {
    }

    private static final Set<Reason> ABOUT_TARGET = EnumSet.of(Reason.ALREADY, Reason.NOT_READY);

    private EvolutionRules() {
    }

    public static Optional<Reason> check(EvolutionRecipe recipe, Facts f) {
        if (!(f.enabled() && f.modLoaded() && f.probeFound() && f.roadOpen())) {
            return Optional.of(Reason.CLOSED);
        }
        if (!f.casterWitch()) {
            return Optional.of(Reason.NOT_WITCH);
        }
        if (!nightOk(recipe.tier(), f.fullMoonNight(), f.eclipse())) {
            return Optional.of(Reason.WRONG_NIGHT);
        }
        if (!f.casterIsTarget() && !f.targetSneaking()) {
            return Optional.of(Reason.NO_CONSENT);
        }
        if (f.holdsGrant()) {
            return Optional.of(Reason.ALREADY);
        }
        if (!f.kindOk() || !f.holdsNeeds() || !f.probeSatisfied() || f.ownedByOther() || !f.enoughEp() || f.retired()) {
            return Optional.of(Reason.NOT_READY);
        }
        return Optional.empty();
    }

    /** An eclipse is only ever on a full moon night, so it also satisfies a full-moon rite. */
    public static boolean nightOk(Tier tier, boolean fullMoonNight, boolean eclipse) {
        return tier.night() == Tier.Night.ECLIPSE ? eclipse : fullMoonNight || eclipse;
    }

    /** Whether the answer depends on the target: those go through {@code RiteTargets.refuseAbout}, never straight to the performer. */
    public static boolean aboutTarget(Reason r) {
        return ABOUT_TARGET.contains(r);
    }

    public static String keyOf(Reason r, Tier tier) {
        return switch (r) {
            case CLOSED -> CLOSED;
            case NOT_WITCH -> NOT_WITCH;
            case WRONG_NIGHT -> tier.night() == Tier.Night.ECLIPSE ? NEEDS_ECLIPSE : NEEDS_FULL_MOON;
            case NO_CONSENT -> SNEAK_TO_EVOLVE;
            case ALREADY -> ALREADY;
            case NOT_READY -> NOT_READY;
        };
    }
}
