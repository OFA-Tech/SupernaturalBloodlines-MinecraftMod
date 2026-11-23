package net.ofatech.supernaturalbloodlines.core.boss;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;

/**
 * R162: Klaus comes on a full moon night for a doppelganger (a villager that is one, or, with {@code klaus_hunts_players}, a player who has the
 * gene). A player is warned {@code klaus_warning_seconds} before he arrives; a villager is not. His bite is venom, and only in the beast form.
 */
public final class KlausRules {
    /** How long (ticks) he waits with nobody to hunt before he withdraws and may come again. */
    public static final int WITHDRAW_AFTER = 1200;
    /** How long (ticks, 4 minutes) he hunts without landing a blow on his prey before he gives up: a prey he cannot reach (behind a wall, in a ring) does not keep him forever. */
    public static final int GIVE_UP_AFTER = 4800;
    /** The seconds at which a hunted player is reminded on the action bar, nearest first. */
    public static final int[] REMINDERS = {10, 30};
    /** A villager counts only when a player is this close to it (blocks): it is a loaded place, and somebody would see him come. */
    public static final int VILLAGER_RANGE = 64;

    /**
     * @param player        a player, not a villager
     * @param doppelganger  has the gene (player) or the roll (villager)
     * @param nearAPlayer   a player of the overworld is within {@link #VILLAGER_RANGE} blocks
     */
    public record Target(boolean player, boolean doppelganger, boolean overworld, boolean spectator, boolean nearAPlayer) {
    }

    private KlausRules() {
    }

    /** Whether this one may be hunted (before the roll). */
    public static boolean eligible(Target t, boolean huntsPlayers) {
        if (!t.doppelganger() || !t.overworld() || t.spectator()) {
            return false;
        }
        return t.player() ? huntsPlayers : t.nearAPlayer();
    }

    /** The roll against the chance: a roll of exactly the chance does not pass. Only on a full moon night, and only when there is somebody to hunt. */
    public static boolean picks(boolean fullMoonNight, boolean anyTarget, double chance, double roll) {
        return fullMoonNight && anyTarget && roll < chance;
    }

    /** The warning, in ticks: nothing for a villager, {@code klaus_warning_seconds} for a player. */
    public static long warningTicks(boolean playerTarget, int warningSeconds) {
        return playerTarget ? Math.max(0, warningSeconds) * 20L : 0L;
    }

    /** Whether the arrival that was put off is now due; {@code arriveAt} is -1 when none waits. */
    public static boolean arrivalDue(long now, long arriveAt) {
        return arriveAt >= 0 && now >= arriveAt;
    }

    /** He is stranded when his prey is gone (dead, or not in his level) and he is fighting nobody. */
    public static boolean stranded(boolean preyHere, boolean hasTarget) {
        return !preyHere && !hasTarget;
    }

    public static boolean withdraws(int strandedTicks) {
        return strandedTicks >= WITHDRAW_AFTER;
    }

    /** Dawn: the time of day is outside the full-moon night window ({@link MoonRule}); he came for the night and leaves with it. */
    public static boolean dawn(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        return !(tick > MoonRule.NIGHT_START && tick < MoonRule.NIGHT_END);
    }

    public static boolean givesUp(int ticksWithoutHit) {
        return ticksWithoutHit >= GIVE_UP_AFTER;
    }

    /**
     * The reminder to show now, in seconds (10 or 30), or 0 for none: the nearest one that is due ({@code remaining} ticks or fewer to go) and not yet shown
     * ({@code shown} is the seconds of the last one shown, 0 for none), and only when the whole warning was longer than it, so a short warning is not nagged.
     */
    public static int reminder(long remainingTicks, long totalTicks, int shown) {
        for (int seconds : REMINDERS) {
            boolean longEnough = seconds * 20L < totalTicks;
            boolean due = remainingTicks <= seconds * 20L;
            boolean notShown = shown == 0 || shown > seconds;
            if (longEnough && due && notShown) {
                return seconds;
            }
        }
        return 0;
    }

    /**
     * Which of the wolf's animations to play (the client's controller maps the name to {@code animation.klaus_hybrid.<name>}): the death pose is held
     * both when he is dying and while he lies desiccated; then the howl of the shift; then by how fast he moves.
     */
    public static String animation(boolean deadOrDying, boolean desiccated, boolean howling, boolean moving, double speed) {
        return animation(deadOrDying, desiccated, false, howling, moving, speed);
    }

    /**
     * The same with the arrival of the shift (UI overhaul phase 7): after the death pose and before the howl, the wolf arrives with {@code shift_in} for
     * the second half of the shift ({@link MorphRules#arriving}), although the howl has already begun by the rules ({@link ShiftRules#howling}).
     */
    public static String animation(boolean deadOrDying, boolean desiccated, boolean arriving, boolean howling, boolean moving, double speed) {
        if (deadOrDying || desiccated) {
            return "death";
        }
        if (arriving) {
            return "shift_in";
        }
        if (howling) {
            return "howl";
        }
        if (moving) {
            return speed > RUN_SPEED ? "run" : "walk";
        }
        return "idle";
    }

    /** Blocks per tick above which the wolf runs instead of walks. */
    public static final double RUN_SPEED = 0.2;

    /** R163: the bite is venom when it lands in the beast form on a vampire who is not already poisoned. */
    public static boolean venomBite(BossForm form, boolean victimIsVampire, boolean alreadyPoisoned) {
        return form == BossForm.BEAST && victimIsVampire && !alreadyPoisoned;
    }
}
