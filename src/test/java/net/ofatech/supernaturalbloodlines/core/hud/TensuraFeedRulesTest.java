package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TensuraFeedRulesTest {
    @Test
    void aFeedGivesTensurasFoodValueInBlood() {
        assertEquals(TensuraFeedRules.FEED, TensuraFeedRules.credit(8, false));
        assertEquals(2, TensuraFeedRules.FEED);
    }

    @Test
    void aVictimWithLessBloodGivesOnlyWhatItHas() {
        assertEquals(1, TensuraFeedRules.credit(1, false));
        assertEquals(0, TensuraFeedRules.credit(0, false));
        assertEquals(0, TensuraFeedRules.credit(-3, false), "a negative blood value is never credited");
    }

    @Test
    void aBloodlessVictimGivesNothing() {
        assertEquals(0, TensuraFeedRules.credit(20, true));
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
