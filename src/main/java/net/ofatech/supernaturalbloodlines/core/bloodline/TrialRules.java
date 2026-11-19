package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;

/**
 * The Alpha Trial (R147): who may start it and what its boss fight counts. A werewolf Beta or Omega at or above {@code alpha_trial_min_level},
 * transformed, on a full-moon night, with no trial running, who has seen the True Alpha warning when their path is still open. The boss rules
 * (phases, the leash, who may hurt it) are here too. Pure.
 */
public final class TrialRules {
    public enum Refusal { NONE, OFF, NOT_WEREWOLF, RANK, LEVEL, NOT_NIGHT, NOT_TRANSFORMED, ALREADY, NEEDS_WARNING }

    public enum Fail { NONE, CHALLENGER_DEAD, TOO_FAR, OTHER_DIMENSION, NIGHT_OVER }

    /** What Home says about the trial: nothing, that one could be started tonight, or that one is running. */
    public enum Notice { NONE, POSSIBLE, RUNNING }

    /**
     * The Home notice for a player: {@code RUNNING} while their trial runs; {@code POSSIBLE} when the gate would let them begin if they stood in beast form (the form
     * and the warning are things they do next, not reasons to say nothing); else {@code NONE}.
     *
     * @param running       asked only when the player could have a trial at all
     * @param refusalInForm the gate asked with {@code transformed = true}, no trial running and the warning not an obstacle
     */
    public static Notice notice(java.util.function.BooleanSupplier running, Refusal refusalInForm) {
        if (refusalInForm == Refusal.NOT_WEREWOLF) {
            return Notice.NONE; // the cheap answer first: most players are not werewolves, and the running lookup is not asked for them
        }
        if (running.getAsBoolean()) {
            return Notice.RUNNING;
        }
        return refusalInForm == Refusal.NONE || refusalInForm == Refusal.NEEDS_WARNING ? Notice.POSSIBLE : Notice.NONE;
    }

    /** The boss leaves its challenger alone this long (30 s) before the trial is lost, and 48 blocks is too far. */
    public static final int LEASH_TICKS = 600;
    public static final double LEASH_BLOCKS = 48;
    /** Two beasts join the fight when the boss falls under two thirds and again under one third of its health. */
    public static final int ADDS_PER_PHASE = 2;

    private TrialRules() {
    }

    /**
     * @param warnApplies {@code TrueAlphaWarning.applies} for this player: a Beta or Omega whose path to True Alpha is still open
     * @param confirmed   they saw the warning and chose to take the spark
     */
    public static Refusal gate(boolean enabled, WolfState w, boolean werewolfFaction, int level, int minLevel, boolean fullMoonNight, boolean transformed,
                               boolean trialRunning, boolean warnApplies, boolean confirmed) {
        if (!enabled) {
            return Refusal.OFF;
        }
        if (!werewolfFaction || w.lineage() == Lineage.NONE) {
            return Refusal.NOT_WEREWOLF;
        }
        if (w.rank() != Rank.BETA && w.rank() != Rank.OMEGA) {
            return Refusal.RANK;
        }
        if (level < minLevel) {
            return Refusal.LEVEL;
        }
        if (!fullMoonNight) {
            return Refusal.NOT_NIGHT;
        }
        if (!transformed) {
            return Refusal.NOT_TRANSFORMED;
        }
        if (trialRunning) {
            return Refusal.ALREADY;
        }
        return warnApplies && !confirmed ? Refusal.NEEDS_WARNING : Refusal.NONE;
    }

    /** Whether the warning can be shown to a player: one with no connection (a test player) is let through, one whose client has no channel for it is not. */
    public static boolean canShowWarning(boolean hasConnection, boolean hasChannel) {
        return !hasConnection || hasChannel;
    }

    /** The phase of the fight: 0 above two thirds of the health, 1 above one third, 2 below. */
    public static int phase(float health, float max) {
        if (max <= 0) {
            return 2;
        }
        float f = health / max;
        return f > 2f / 3f ? 0 : f > 1f / 3f ? 1 : 2;
    }

    /** How many beasts join when the fight moves from phase {@code stored} to the phase of {@code health}: two for each phase passed. */
    public static int addsFor(int stored, float health, float max) {
        return Math.max(0, phase(health, max) - stored) * ADDS_PER_PHASE;
    }

    /** The away counter after one check of {@code step} ticks: it grows while the challenger is farther than the leash (or absent), else starts again. */
    public static int away(int away, boolean challengerNear, int step) {
        return challengerNear ? 0 : away + step;
    }

    /** Why the trial is lost now, or NONE. */
    public static Fail failure(boolean challengerAlive, boolean sameDimension, int away, boolean nightOver) {
        if (!challengerAlive) {
            return Fail.CHALLENGER_DEAD;
        }
        if (!sameDimension) {
            return Fail.OTHER_DIMENSION;
        }
        if (nightOver) {
            return Fail.NIGHT_OVER;
        }
        return away >= LEASH_TICKS ? Fail.TOO_FAR : Fail.NONE;
    }

    /**
     * Whether a hit counts: only the challenger and their pack (and allies) can hurt the boss, so a stranger cannot steal the win. A hit that is no
     * player's (a fall, a fire) counts, so the boss is not immortal to the world.
     */
    public static boolean mayHurt(UUID attackerPlayer, UUID challenger, boolean samePackOrAlly) {
        return attackerPlayer == null || attackerPlayer.equals(challenger) || samePackOrAlly;
    }
}
