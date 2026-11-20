package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoticeRulesTest {
    @Test
    void theFirstHintIsAlwaysDue() {
        assertTrue(NoticeRules.due(-1L, 0L));
    }

    @Test
    void aHintInsideTheGapIsNot() {
        assertFalse(NoticeRules.due(100L, 100L));
        assertFalse(NoticeRules.due(100L, 100L + NoticeRules.GAP - 1));
    }

    @Test
    void aHintAfterTheGapIs() {
        assertTrue(NoticeRules.due(100L, 100L + NoticeRules.GAP));
    }

    @Test
    void aClockThatWentBackwardsIsNotStuckForever() {
        assertTrue(NoticeRules.due(10_000L, 50L));
    }
}
