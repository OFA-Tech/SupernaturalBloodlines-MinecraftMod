package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AwakeningRulesTest {
    private static final double MIN = 5_000_000;

    private static AwakeningRules.Facts good() {
        return new AwakeningRules.Facts(true, 1_000_000L, 0L, 120, 6_000_000, MIN, true, false);
    }

    @Test
    void theEpBoundaryIsAtOrAbove() {
        assertFalse(AwakeningRules.enoughEp(4_999_999, MIN));
        assertTrue(AwakeningRules.enoughEp(5_000_000, MIN), "Ascension compares with < and refuses only below the threshold");
        assertTrue(AwakeningRules.enoughEp(5_000_001, MIN));
    }

    @Test
    void aLowerMinEpOpensItLower() {
        assertTrue(AwakeningRules.enoughEp(1000, 1000), "the threshold is Ascension's live config value, never a literal");
        assertFalse(AwakeningRules.enoughEp(999, 1000));
        assertTrue(AwakeningRules.satisfied(new AwakeningRules.Facts(true, 10, 0, 120, 1000, 1000, false, true)));
    }

    @Test
    void theCooldownIsGameTicksOfMinutes() {
        assertEquals(144_000L, AwakeningRules.cooldownTicks(120), "120 minutes of 60 s of 20 ticks, as the altar computes it");
        assertEquals(0L, AwakeningRules.cooldownTicks(0));
    }

    @Test
    void aNeverAwakenedPlayerIsNotCoolingDown() {
        assertFalse(AwakeningRules.coolingDown(5L, 0L, 120), "a tag of 0 means never: the altar skips the check when last is 0");
        assertTrue(AwakeningRules.coolingDown(144_000L, 1L, 120), "a tag of 1 is a real awakening one tick into the world");
    }

    @Test
    void theCooldownEndsAtTheTick() {
        long last = 10_000L;
        assertTrue(AwakeningRules.coolingDown(last + 143_999L, last, 120), "one tick short is still cooling down");
        assertFalse(AwakeningRules.coolingDown(last + 144_000L, last, 120), "the altar refuses only while now - last < ticks");
    }

    @Test
    void aZeroCooldownNeverBlocks() {
        assertFalse(AwakeningRules.coolingDown(10L, 9L, 0));
    }

    @Test
    void eitherStatusQualifiesNeitherDoesNot() {
        assertTrue(AwakeningRules.qualifyingStatus(true, false));
        assertTrue(AwakeningRules.qualifyingStatus(false, true));
        assertTrue(AwakeningRules.qualifyingStatus(true, true));
        assertFalse(AwakeningRules.qualifyingStatus(false, false));
    }

    @Test
    void theRuleOffClosesIt() {
        AwakeningRules.Facts f = good();
        assertTrue(AwakeningRules.satisfied(f));
        assertFalse(AwakeningRules.satisfied(new AwakeningRules.Facts(false, f.now(), f.lastAwakeningTick(), f.cooldownMinutes(), f.maxEp(), f.minEp(), f.trueHero(), f.trueDemonLord())));
    }

    @Test
    void everyRequirementIsNeeded() {
        AwakeningRules.Facts f = good();
        assertTrue(AwakeningRules.satisfied(f));
        assertFalse(AwakeningRules.satisfied(new AwakeningRules.Facts(f.ruleOn(), f.now(), 999_999L, f.cooldownMinutes(), f.maxEp(), f.minEp(), f.trueHero(), f.trueDemonLord())),
                "inside the cooldown");
        assertFalse(AwakeningRules.satisfied(new AwakeningRules.Facts(f.ruleOn(), f.now(), f.lastAwakeningTick(), f.cooldownMinutes(), 4_999_999, f.minEp(), f.trueHero(), f.trueDemonLord())),
                "below the EP threshold");
        assertFalse(AwakeningRules.satisfied(new AwakeningRules.Facts(f.ruleOn(), f.now(), f.lastAwakeningTick(), f.cooldownMinutes(), f.maxEp(), f.minEp(), false, false)),
                "neither a true Hero nor a true Demon Lord");
        assertTrue(AwakeningRules.satisfied(new AwakeningRules.Facts(f.ruleOn(), f.now(), 1L, f.cooldownMinutes(), f.maxEp(), f.minEp(), false, true)),
                "a true Demon Lord who awakened long ago");
    }
}
