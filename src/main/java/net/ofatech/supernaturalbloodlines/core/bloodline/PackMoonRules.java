package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;

/**
 * R96, the pack gift: a werewolf member of a pack whose current leader is hybrid-like has moon control, as with a Moon Ring. Pure; the
 * parts read the pack and the species and ask here.
 */
public final class PackMoonRules {
    private PackMoonRules() {
    }

    /**
     * @param member         the player follows a leader in the pack index (not the leader themselves)
     * @param leaderHybrid   that leader is hybrid-like now ({@link #hybridLike})
     * @param werewolf       the player is in the werewolf faction now
     */
    public static boolean gift(boolean enabled, WolfState wolf, boolean werewolf, boolean member, boolean leaderHybrid) {
        return enabled && member && leaderHybrid && werewolf && wolf.lineage() != Lineage.NONE
                && wolf.rank() != Rank.ALPHA && wolf.rank() != Rank.TRUE_ALPHA;
    }

    /** A hybrid, an Original hybrid or a tribrid whose wolf is not sealed by the curse (R24a: a bound hybrid has no forms at all). */
    public static boolean hybridLike(Species species, boolean curseBound) {
        return !curseBound && (species == Species.HYBRID || species == Species.ORIGINAL_HYBRID || species == Species.TRIBRID);
    }
}
