package net.ofatech.supernaturalbloodlines.core.group.coven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RingRulesTest {
    @Test
    void aMoonRingNeedsAWerewolfInAPack() {
        assertTrue(RingRules.moonReceiverOk(true, true));
        assertFalse(RingRules.moonReceiverOk(true, false));
        assertFalse(RingRules.moonReceiverOk(false, true));
    }

    @Test
    void aSunRingNeedsAVampire() {
        assertTrue(RingRules.sunReceiverOk(true, false, false));
        assertFalse(RingRules.sunReceiverOk(false, true, false));
    }

    @Test
    void theDayWalkerSwitchOnNeedsARank() {
        assertFalse(RingRules.sunReceiverOk(true, false, true));
        assertTrue(RingRules.sunReceiverOk(true, true, true));
    }

    @Test
    void theCircleMinimum() {
        assertFalse(RingRules.circleOk(2, 3));
        assertTrue(RingRules.circleOk(3, 3));
        assertTrue(RingRules.circleOk(7, 3));
    }
}