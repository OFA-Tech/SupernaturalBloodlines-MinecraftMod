package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R185 to R187: the Beast of Gevaudan's numbers and rows, all pure. The Host (who calls it, on which night), the Roar (who it frightens), and the body (what
 * hurts it, what it regenerates, what stuns it).
 */
public final class BeastRules {
    /** The Roar's wind-up: the Beast rears up for this long before the fear lands. */
    public static final int ROAR_WINDUP_TICKS = 40;
    /** Shots (arrows, tridents) do this part of their damage: the Beast is a shadow pretending to be real. Melee and the pike are not reduced. */
    public static final float RANGED_FACTOR = 0.6F;
    /** What it heals every second, unless a wolfsbane strain is on it. */
    public static final float REGEN_PER_SECOND = 2.0F;
    /** A Hearing Device stuns it for a second at most (others for five). */
    public static final int STUN_CAP_TICKS = 20;
    /** The most a wolfsbane poison pulse may take from it: northern blue stops its regeneration, it does not kill it. */
    public static final float WOLFSBANE_DAMAGE_CAP = 1.0F;

    /** The fastest the Spirit goes, in blocks per tick: a sprinting player does {@link #SPRINT_SPEED}, so a player on foot gains on it. */
    public static final double SPIRIT_SPEED = 0.2;
    /** A sprinting player's speed in blocks per tick (5.612 blocks a second). */
    public static final double SPRINT_SPEED = 0.2806;
    /** Every {@value} ticks the Spirit stops... */
    public static final int SPIRIT_PAUSE_PERIOD = 80;
    /** ...for the last {@value} of them (a second), looking back at whoever follows. */
    public static final int SPIRIT_PAUSE_TICKS = 20;

    private BeastRules() {
    }

    // ---- the Spirit on the run ----

    /** Whether the Spirit stands still and looks back at this tick of its life. */
    public static boolean spiritPauses(long tick) {
        return tick % SPIRIT_PAUSE_PERIOD >= SPIRIT_PAUSE_PERIOD - SPIRIT_PAUSE_TICKS;
    }

    /** The distance it may cover in one tick: never more than {@link #SPIRIT_SPEED}. */
    public static double spiritSpeed(double blocksPerTick) {
        return Math.min(blocksPerTick, SPIRIT_SPEED);
    }

    // ---- the reward ----

    public enum Payee { TOP_ONLINE, TOP_OWED, STRIKER, GROUND }

    /**
     * Where the Mantle goes: to the player who did the most damage if they are here, kept for them if they are not (it is never given to somebody else), else to
     * the one who struck with the pike, else on the ground where the Spirit fell.
     */
    public static Payee payee(boolean topKnown, boolean topOnline, boolean strikerKnown) {
        if (topKnown) {
            return topOnline ? Payee.TOP_ONLINE : Payee.TOP_OWED;
        }
        return strikerKnown ? Payee.STRIKER : Payee.GROUND;
    }

    // ---- the Host: who calls it, when ----

    /** What the director knows about a player when it looks for somebody the Host may appear for. */
    public record Target(boolean alpha, boolean hunter, boolean overworld, boolean spectator) {
    }

    /** An Alpha (or True Alpha) or a Hunter of the Five, in the overworld, who is not a spectator. */
    public static boolean hostTarget(Target t) {
        return (t.alpha() || t.hunter()) && t.overworld() && !t.spectator();
    }

    /**
     * Whether the Host appears now: a full moon night, nothing rolled tonight yet, somebody to call it, a Host may still be made ({@link BeastPhaseRules}), and the
     * roll (0 up to, not including, 1) is under {@code chance} (clamped to 0 to 1).
     */
    public static boolean hostAppears(BeastPhase phase, int hostsMade, boolean fullMoonNight, boolean rolledTonight, boolean anyTarget, double chance, double roll) {
        return fullMoonNight && !rolledTonight && anyTarget && BeastPhaseRules.hostMayBeMade(phase, hostsMade) && roll < Math.max(0.0, Math.min(1.0, chance));
    }

    // ---- the Roar ----

    public enum Kind { PLAYER, MOB, PACK_MEMBER }

    /**
     * Whether the Roar frightens one in range. A player or a pack member of Alpha or True Alpha rank is immune (their rank is what the Roar tests); an Omega, a Beta and
     * everybody who is no wolf are not. Hostile mobs that belong to no pack flee. {@code exempt}: creative, spectator, another boss, the Beast's own Host.
     */
    public static boolean roarAffects(Kind kind, boolean alphaRank, boolean exempt) {
        if (exempt) {
            return false;
        }
        return switch (kind) {
            case PLAYER, PACK_MEMBER -> !alphaRank;
            case MOB -> true;
        };
    }

    /** Within the range, the edge included. */
    public static boolean roarInRange(double distanceSquared, double range) {
        return distanceSquared <= range * range;
    }

    /** The Roar may begin: its cooldown is over. */
    public static boolean roarReady(long now, long readyAt) {
        return now >= readyAt;
    }

    /** The ticks the fear lasts for a setting in seconds; at least one second. */
    public static int fearTicks(int seconds) {
        return Math.max(1, seconds) * 20;
    }

    /** The tick the next Roar may begin, from the tick it was let out. */
    public static long nextRoarAt(long now, int cooldownSeconds) {
        return now + Math.max(1, cooldownSeconds) * 20L;
    }

    // ---- the body ----

    /** A blow's damage after the shadow: shots are reduced, everything else is whole. */
    public static float damageTaken(boolean ranged, float amount) {
        return ranged ? amount * RANGED_FACTOR : amount;
    }

    /** What it heals in the second that is passing: nothing under a wolfsbane strain or at full health, and never above full. */
    public static float regen(boolean wolfsbane, float health, float maxHealth) {
        if (wolfsbane || health <= 0F || health >= maxHealth) {
            return 0F;
        }
        return Math.min(REGEN_PER_SECOND, maxHealth - health);
    }

    /** A stun for {@code ticks}: no longer than {@link #STUN_CAP_TICKS}. */
    public static int stunTicks(int ticks) {
        return Math.min(ticks, STUN_CAP_TICKS);
    }

    /** A pulse of wolfsbane poison on it: a pulse of one heart at most, whatever the strain wants to do (northern blue's last pulse is lethal to a wolf). */
    public static float wolfsbaneDamage(float amount) {
        return Math.min(amount, WOLFSBANE_DAMAGE_CAP);
    }
}
