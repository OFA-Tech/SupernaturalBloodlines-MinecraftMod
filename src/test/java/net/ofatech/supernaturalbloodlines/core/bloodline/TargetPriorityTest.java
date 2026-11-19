package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.bloodline.TargetPriority.Candidate;
import net.ofatech.supernaturalbloodlines.core.bloodline.TargetPriority.Tier;
import org.junit.jupiter.api.Test;

/** R144, row by row: vampire, then hunter, then player, then prey. */
class TargetPriorityTest {
    private static Candidate<String> c(String who, Tier tier, double dist, boolean friend) {
        return new Candidate<>(who, tier, dist, friend);
    }

    @Test
    void theOrderIsVampireHunterPlayerPrey() {
        assertEquals(List.of(Tier.VAMPIRE, Tier.HUNTER, Tier.PLAYER, Tier.PREY), List.of(Tier.values()));
    }

    @Test
    void aVampireBeatsEverythingEvenFarther() {
        var best = TargetPriority.pick(List.of(c("sheep", Tier.PREY, 1, false), c("steve", Tier.PLAYER, 4, false), c("hunter", Tier.HUNTER, 9, false),
                c("vamp", Tier.VAMPIRE, 400, false)));
        assertEquals("vamp", best.who());
    }

    @Test
    void aHunterBeatsAPlayerAndPrey() {
        assertEquals("hunter", TargetPriority.pick(List.of(c("steve", Tier.PLAYER, 1, false), c("hunter", Tier.HUNTER, 50, false), c("cow", Tier.PREY, 1, false))).who());
    }

    @Test
    void aPlayerBeatsPrey() {
        assertEquals("steve", TargetPriority.pick(List.of(c("cow", Tier.PREY, 1, false), c("steve", Tier.PLAYER, 80, false))).who());
    }

    @Test
    void ofOneTierTheNearestWins() {
        assertEquals("near", TargetPriority.pick(List.of(c("far", Tier.PLAYER, 100, false), c("near", Tier.PLAYER, 4, false))).who());
        assertEquals("near", TargetPriority.pick(List.of(c("near", Tier.PLAYER, 4, false), c("far", Tier.PLAYER, 100, false))).who());
    }

    @Test
    void aFriendIsNeverChosen() {
        assertEquals("steve", TargetPriority.pick(List.of(c("guard", Tier.VAMPIRE, 1, true), c("steve", Tier.PLAYER, 30, false))).who());
        assertNull(TargetPriority.pick(List.of(c("guard", Tier.PLAYER, 1, true))));
    }

    @Test
    void nothingSeenIsNothingChosen() {
        assertNull(TargetPriority.pick(List.of()));
    }

    @Test
    void onlyABetterTierTakesOverAValidTarget() {
        assertTrue(TargetPriority.switchTo(Tier.PLAYER, true, Tier.VAMPIRE));
        assertTrue(TargetPriority.switchTo(Tier.PREY, true, Tier.PLAYER));
        assertFalse(TargetPriority.switchTo(Tier.PLAYER, true, Tier.PLAYER), "an equal one never takes over");
        assertFalse(TargetPriority.switchTo(Tier.VAMPIRE, true, Tier.PREY), "a worse one never does");
    }

    @Test
    void anInvalidOrMissingTargetIsReplacedByAnything() {
        assertTrue(TargetPriority.switchTo(Tier.VAMPIRE, false, Tier.PREY));
        assertTrue(TargetPriority.switchTo(null, false, Tier.PREY));
        assertFalse(TargetPriority.switchTo(Tier.PLAYER, false, null), "nothing to switch to");
    }
}