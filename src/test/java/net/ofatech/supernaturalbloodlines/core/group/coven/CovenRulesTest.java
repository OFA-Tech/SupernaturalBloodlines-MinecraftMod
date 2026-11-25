package net.ofatech.supernaturalbloodlines.core.group.coven;

import static net.ofatech.supernaturalbloodlines.core.group.coven.CovenRules.Role.ELDER;
import static net.ofatech.supernaturalbloodlines.core.group.coven.CovenRules.Role.MEMBER;
import static net.ofatech.supernaturalbloodlines.core.group.coven.CovenRules.Role.REGENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CovenRulesTest {
    @Test
    void aCandidateNeedsTheDays() {
        assertFalse(CovenRules.mayBeElder(2, 3));
        assertTrue(CovenRules.mayBeElder(3, 3));
        assertTrue(CovenRules.mayBeElder(0, 0));
    }

    @Test
    void theElderCapFollowsTheMembers() {
        assertEquals(0, CovenRules.elderCap(2, 4));
        assertEquals(1, CovenRules.elderCap(3, 4));
        assertEquals(1, CovenRules.elderCap(7, 4));
        assertEquals(2, CovenRules.elderCap(8, 4));
    }

    @Test
    void theRegentCannotBeShunned() {
        assertFalse(CovenRules.mayCallShun(REGENT, REGENT));
        assertFalse(CovenRules.mayCallShun(ELDER, REGENT));
        assertTrue(CovenRules.mayCallShun(REGENT, MEMBER));
    }

    @Test
    void onlyAnElderOrTheRegentCallsAShun() {
        assertFalse(CovenRules.mayCallShun(MEMBER, MEMBER));
        assertTrue(CovenRules.mayCallShun(ELDER, MEMBER));
        assertTrue(CovenRules.mayCallShun(ELDER, ELDER));
    }

    @Test
    void theElectionCooldown() {
        assertTrue(CovenRules.mayCallElection(ELDER, ELDER, 0, 100));
        assertFalse(CovenRules.mayCallElection(ELDER, ELDER, 101, 100));
        assertTrue(CovenRules.mayCallElection(ELDER, ELDER, 100, 100));
        assertFalse(CovenRules.mayCallElection(REGENT, ELDER, 0, 100));
        assertFalse(CovenRules.mayCallElection(MEMBER, ELDER, 0, 100));
        assertFalse(CovenRules.mayCallElection(ELDER, MEMBER, 0, 100));
    }

    @Test
    void banArithmetic() {
        long until = CovenRules.banUntil(1000, 7);
        assertEquals(1000 + 7L * 24 * 60 * 60 * 1000, until);
        assertTrue(CovenRules.banned(until, until - 1));
        assertFalse(CovenRules.banned(until, until));
    }
}