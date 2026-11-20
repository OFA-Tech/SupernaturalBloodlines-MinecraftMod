package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RebekahRulesTest {
    @Test
    void sheAnswersACureOnlyWhenSheIsNotOutThereOrDead() {
        assertTrue(RebekahRules.answers(BossState.UNSEEN));
        assertTrue(RebekahRules.answers(BossState.LOST));
        assertFalse(RebekahRules.answers(BossState.ACTIVE));
        assertFalse(RebekahRules.answers(BossState.OPEN), "a Cure made while she lies desiccated");
        assertFalse(RebekahRules.answers(BossState.DEAD));
    }

    @Test
    void sheArrivesTheDelayAfterTheCureAndNotBefore() {
        long at = RebekahRules.arrivesAt(1000, 60);
        assertEquals(1000 + 1200, at);
        assertFalse(RebekahRules.due(at - 1, at));
        assertTrue(RebekahRules.due(at, at));
        assertFalse(RebekahRules.due(1_000_000, -1), "nobody is on the way");
        assertEquals(1000, RebekahRules.arrivesAt(1000, 0));
        assertEquals(1000, RebekahRules.arrivesAt(1000, -5));
    }

    @Test
    void sheWithdrawsAfterAMinuteWithNobodyToHunt() {
        assertTrue(RebekahRules.stranded(false, false));
        assertFalse(RebekahRules.stranded(true, false));
        assertFalse(RebekahRules.stranded(false, true));
        assertFalse(RebekahRules.withdraws(RebekahRules.WITHDRAW_AFTER - 10));
        assertTrue(RebekahRules.withdraws(RebekahRules.WITHDRAW_AFTER));
    }

    @Test
    void sheComesFromFartherThanTheNearest() {
        assertEquals(48, RebekahRules.RADIUS);
        assertTrue(RebekahRules.NEAREST < RebekahRules.RADIUS);
    }
}