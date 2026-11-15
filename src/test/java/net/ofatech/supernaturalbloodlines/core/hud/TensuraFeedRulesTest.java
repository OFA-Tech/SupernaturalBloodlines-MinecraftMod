package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TensuraFeedRulesTest {
    @Test
    void aFeedGivesTensurasFoodValueInBlood() {
        assertEquals(TensuraFeedRules.FEED, TensuraFeedRules.credit(8, false, 20));
        assertEquals(2, TensuraFeedRules.FEED);
    }

    @Test
    void aVictimWithLessBloodGivesOnlyWhatItHas() {
        assertEquals(1, TensuraFeedRules.credit(1, false, 20));
        assertEquals(0, TensuraFeedRules.credit(0, false, 20));
        assertEquals(0, TensuraFeedRules.credit(-3, false, 20), "a negative blood value is never credited");
    }

    @Test
    void aBloodlessVictimGivesNothing() {
        assertEquals(0, TensuraFeedRules.credit(20, true, 20));
    }

    @Test
    void aFullVampireTakesNothingAndAlmostFullTakesWhatFits() {
        assertEquals(0, TensuraFeedRules.credit(8, false, 0), "full blood: the victim must not be drained");
        assertEquals(1, TensuraFeedRules.credit(8, false, 1));
        assertEquals(0, TensuraFeedRules.creditFromVampire(0));
        assertEquals(TensuraFeedRules.FEED, TensuraFeedRules.creditFromVampire(15));
    }

    @Test
    void aVampiresNeedsFoodMeansBloodNotFull() {
        assertTrue(TensuraFeedRules.needsFood(false, true, false), "a vampire's food is full but its blood is not");
        assertFalse(TensuraFeedRules.needsFood(true, true, true), "full blood: Tensura's hungry answer is ignored for a vampire");
    }

    @Test
    void everyoneElseKeepsTensurasAnswer() {
        assertTrue(TensuraFeedRules.needsFood(true, false, false));
        assertFalse(TensuraFeedRules.needsFood(false, false, true));
        assertFalse(TensuraFeedRules.needsFood(false, false, false));
    }
}
