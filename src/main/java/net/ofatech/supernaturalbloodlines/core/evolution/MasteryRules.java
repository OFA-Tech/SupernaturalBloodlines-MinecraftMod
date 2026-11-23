package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** The Rite of Mastery's order of checks (spec 8.5, E16) and its refusals. Pure. */
public final class MasteryRules {
    public static final String SNEAK_TO_MASTER = "supernaturalbloodlines.rite.sneak_to_master";
    public static final String KEY = "supernaturalbloodlines.refusal.mastery.";
    public static final String VETOED = KEY + "vetoed";

    public enum Reason { CLOSED, NOT_WITCH, WRONG_NIGHT, NO_CONSENT, NO_CHOICE, NOT_HELD, TEMPORARY, ALREADY, WRONG_TIER, SYSTEM, RETIRED, NO_EP, COOLDOWN }

    public record Facts(boolean enabled, boolean casterWitch, boolean fullMoonNight, boolean eclipse, boolean casterIsTarget, boolean targetSneaking,
                        boolean hasChoice, boolean held, boolean temporary, boolean alreadyDone, Tier skillTier, Tier riteTier, boolean refusedId,
                        boolean retired, boolean enoughEp, boolean onCooldown) {
    }

    /** The refusals that are about the target's own skills and state: they are told to the target, never to the performer (through {@code refuseAbout}). */
    private static final Set<Reason> ABOUT_TARGET = EnumSet.of(Reason.NO_CHOICE, Reason.NOT_HELD, Reason.TEMPORARY, Reason.ALREADY, Reason.WRONG_TIER, Reason.SYSTEM,
            Reason.RETIRED, Reason.NO_EP, Reason.COOLDOWN);

    /** Mastery points handed to {@code addMasteryPoint}: far more than any skill needs, so the method's own cap decides where mastery ends (not a number we compute). */
    public static final double POINTS = 1.0e9;

    private MasteryRules() {
    }

    /** Whether the target can pay: a cost of 0 or less is free, else the base Max EP must be strictly more than the cost. */
    public static boolean enoughEp(double baseMaxEp, double cost) {
        return cost <= 0 || baseMaxEp > cost;
    }

    public static Optional<Reason> check(Facts f) {
        if (!f.enabled()) {
            return Optional.of(Reason.CLOSED);
        }
        if (!f.casterWitch()) {
            return Optional.of(Reason.NOT_WITCH);
        }
        if (!EvolutionRules.nightOk(f.riteTier(), f.fullMoonNight(), f.eclipse())) {
            return Optional.of(Reason.WRONG_NIGHT);
        }
        if (!f.casterIsTarget() && !f.targetSneaking()) {
            return Optional.of(Reason.NO_CONSENT);
        }
        if (!f.hasChoice()) {
            return Optional.of(Reason.NO_CHOICE);
        }
        if (!f.held()) {
            return Optional.of(Reason.NOT_HELD);
        }
        if (f.temporary()) {
            return Optional.of(Reason.TEMPORARY);
        }
        if (f.alreadyDone()) {
            return Optional.of(Reason.ALREADY);
        }
        if (f.skillTier() != f.riteTier()) {
            return Optional.of(Reason.WRONG_TIER);
        }
        if (f.refusedId()) {
            return Optional.of(Reason.SYSTEM);
        }
        if (f.retired()) {
            return Optional.of(Reason.RETIRED);
        }
        if (!f.enoughEp()) {
            return Optional.of(Reason.NO_EP);
        }
        if (f.onCooldown()) {
            return Optional.of(Reason.COOLDOWN);
        }
        return Optional.empty();
    }

    /** Everything from the choice on is about the target's own skills: it goes through {@code refuseAbout}. */
    public static boolean aboutTarget(Reason r) {
        return ABOUT_TARGET.contains(r);
    }

    public static String keyOf(Reason r, Tier riteTier) {
        return switch (r) {
            case CLOSED -> EvolutionRules.CLOSED;
            case NOT_WITCH -> EvolutionRules.NOT_WITCH;
            case WRONG_NIGHT -> riteTier.night() == Tier.Night.ECLIPSE ? EvolutionRules.NEEDS_ECLIPSE : EvolutionRules.NEEDS_FULL_MOON;
            case NO_CONSENT -> SNEAK_TO_MASTER;
            default -> KEY + r.name().toLowerCase(java.util.Locale.ROOT);
        };
    }

    /** What the picker may offer: the reason a held skill cannot be chosen at all, whatever the rite file, or empty. */
    public static Optional<Reason> unchoosable(boolean temporary, boolean alreadyDone, boolean refusedId, boolean retired) {
        if (temporary) {
            return Optional.of(Reason.TEMPORARY);
        }
        if (alreadyDone) {
            return Optional.of(Reason.ALREADY);
        }
        if (refusedId) {
            return Optional.of(Reason.SYSTEM);
        }
        return retired ? Optional.of(Reason.RETIRED) : Optional.empty();
    }

    /** The cooldown in game ticks: whole in-game days of 24000 ticks. */
    public static long cooldownTicks(int days) {
        return 24000L * days;
    }

    public static boolean onCooldown(long lastRite, long now, int days) {
        return lastRite >= 0 && now - lastRite < cooldownTicks(days);
    }

    public static int daysLeft(long lastRite, long now, int days) {
        long left = cooldownTicks(days) - (now - lastRite);
        return (int) Math.max(1, (left + 23999) / 24000);
    }
}
