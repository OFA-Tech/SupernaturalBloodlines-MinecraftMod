package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

/** R96, the pack gift: a werewolf member of a pack whose current leader is hybrid-like has moon control. */
class PackMoonRulesTest {
    private static WolfState wolf(Lineage lineage, Rank rank) {
        return WolfState.NONE.toBuilder().lineage(lineage).rank(rank).build();
    }

    private static boolean gift(boolean enabled, WolfState w, boolean werewolf, boolean member, boolean leaderHybrid) {
        return PackMoonRules.gift(enabled, w, werewolf, member, leaderHybrid);
    }

    @Test
    void aBetaOfAHybridLedPackHasIt() {
        assertTrue(gift(true, wolf(Lineage.BITTEN, Rank.BETA), true, true, true));
        assertTrue(gift(true, wolf(Lineage.BORN, Rank.BETA), true, true, true));
    }

    @Test
    void aNormalLeaderGivesNothing() {
        assertFalse(gift(true, wolf(Lineage.BITTEN, Rank.BETA), true, true, false));
    }

    @Test
    void noPackNoGift() {
        assertFalse(gift(true, wolf(Lineage.BITTEN, Rank.BETA), true, false, true));
    }

    @Test
    void theConfigOffGivesNothing() {
        assertFalse(gift(false, wolf(Lineage.BITTEN, Rank.BETA), true, true, true));
    }

    @Test
    void onlyWerewolvesBenefit() {
        assertFalse(gift(true, wolf(Lineage.NONE, Rank.NONE), true, true, true), "a Packmate has no wolf");
        assertFalse(gift(true, wolf(Lineage.BITTEN, Rank.BETA), false, true, true), "a cured wolf outside the faction");
    }

    @Test
    void anAlphaIsNotAMemberHere() {
        assertFalse(gift(true, wolf(Lineage.BITTEN, Rank.ALPHA), true, true, true));
        assertFalse(gift(true, wolf(Lineage.BITTEN, Rank.TRUE_ALPHA), true, true, true));
    }

    @Test
    void anOmegaAloneIsNoMember() {
        assertFalse(gift(true, wolf(Lineage.BITTEN, Rank.OMEGA), true, false, true));
    }

    @Test
    void hybridLikeMeansAnUnboundHybridType() {
        assertTrue(PackMoonRules.hybridLike(Species.HYBRID, false));
        assertTrue(PackMoonRules.hybridLike(Species.ORIGINAL_HYBRID, false));
        assertTrue(PackMoonRules.hybridLike(Species.TRIBRID, false));
        assertFalse(PackMoonRules.hybridLike(Species.HYBRID, true), "a bound hybrid has no forms to give");
        assertFalse(PackMoonRules.hybridLike(Species.ORIGINAL, false));
        assertFalse(PackMoonRules.hybridLike(Species.NONE, false));
    }
}
