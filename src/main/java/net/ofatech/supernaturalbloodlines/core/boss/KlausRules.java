package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R162: Klaus comes on a full moon night for a doppelganger (a villager that is one, or, with {@code klaus_hunts_players}, a player who has the
 * gene). A player is warned {@code klaus_warning_seconds} before he arrives; a villager is not. His bite is venom, and only in the beast form.
 */
public final class KlausRules {
    /** How long (ticks) he waits with nobody to hunt before he withdraws and may come again. */
    public static final int WITHDRAW_AFTER = 1200;
    /** A villager counts only when a player is this close to it (blocks): it is a loaded place, and somebody would see him come. */
    public static final int VILLAGER_RANGE = 64;

    /**
     * @param player        a player, not a villager
     * @param doppelganger  has the gene (player) or the roll (villager)
     * @param nearAPlayer   a player of the overworld is within {@link #VILLAGER_RANGE} blocks
     */
    public record Target(boolean player, boolean doppelganger, boolean overworld, boolean spectator, boolean nearAPlayer) {
    }

    /** The species Klaus's Blood counts as when it is judged for the hybrid transition: he is an Original hybrid, so it is taken under either {@code hybrid_transition_blood}. */
    public static final net.ofatech.supernaturalbloodlines.core.bloodline.Species BLOOD_SPECIES = net.ofatech.supernaturalbloodlines.core.bloodline.Species.ORIGINAL_HYBRID;

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

    /** R163: the bite is venom when it lands in the beast form on a vampire who is not already poisoned. */
    public static boolean venomBite(BossForm form, boolean victimIsVampire, boolean alreadyPoisoned) {
        return form == BossForm.BEAST && victimIsVampire && !alreadyPoisoned;
    }
}
