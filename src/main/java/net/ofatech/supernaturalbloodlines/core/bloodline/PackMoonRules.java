package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;

/**
 * R96, the pack gift: a werewolf member of a pack whose current leader is hybrid-like has moon control, as with a Moon Ring. Pure; the
 * parts read the pack and the species and ask here.
 */
public final class PackMoonRules {
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private PackMoonRules() {
    }

    /**
     * @param member         the player follows a leader in the pack index (not the leader themselves)
     * @param leaderHybrid   that leader gives it now ({@link #leaderGives})
     * @param werewolf       the player is in the werewolf faction now
     */
    public static boolean gift(boolean enabled, WolfState wolf, boolean werewolf, boolean member, boolean leaderHybrid) {
        return enabled && member && leaderHybrid && werewolf && wolf.lineage() != Lineage.NONE
                && wolf.rank() != Rank.ALPHA && wolf.rank() != Rank.TRUE_ALPHA;
    }

    /** A hybrid, an Original hybrid or a tribrid ({@link Species#hybridType}) whose wolf is not sealed by the curse (R24a: a bound hybrid has no forms at all). The one definition of "unbound hybrid" for moon control. */
    public static boolean hybridLike(Species species, boolean curseBound) {
        return !curseBound && species.hybridType();
    }

    /**
     * Whether a leader who was hybrid-like gives the gift now: not when they have been away for {@code absentDays} real days or more
     * (the same limit after which the pack passes the lead on, {@code absent_leader_days}; 0 = never absent). A leader who is online is
     * here; one the index has no time for is not known to be away.
     *
     * @param seenMillis the wall-clock time the index last saw the leader, or a negative number when it never did
     */
    public static boolean leaderGives(boolean hybridLike, boolean online, long seenMillis, long nowMillis, int absentDays) {
        if (!hybridLike) {
            return false;
        }
        if (online || absentDays <= 0 || seenMillis < 0) {
            return true;
        }
        return nowMillis - seenMillis < absentDays * DAY_MILLIS;
    }
}
