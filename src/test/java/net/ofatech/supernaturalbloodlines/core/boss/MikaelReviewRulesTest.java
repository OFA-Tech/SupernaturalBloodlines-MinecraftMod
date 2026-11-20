package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.BossRules.Event;
import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules.Facts;
import org.junit.jupiter.api.Test;

/** Plan 17b panel: R158 (Originals), the stranded Mikael, and a lost boss that is found or dies. */
class MikaelReviewRulesTest {
    private static Facts target(boolean original, boolean stakes) {
        return new Facts(true, 4, 3, true, true, true, false, original, stakes);
    }

    @Test
    void anOriginalIsNotHuntedByDefault() {
        assertFalse(MikaelRules.eligible(target(true, false)));
    }

    @Test
    void anOriginalIsHuntedWhenTheStakeKillsOriginals() {
        assertTrue(MikaelRules.eligible(target(true, true)));
    }

    @Test
    void aVampireWhoIsNotAnOriginalIsHuntedEitherWay() {
        assertTrue(MikaelRules.eligible(target(false, false)));
        assertTrue(MikaelRules.eligible(target(false, true)));
    }

    @Test
    void heChasesOnlyByNightAndOnlyWhatHeMay() {
        assertTrue(MikaelRules.mayChase(true, false, false));
        assertFalse(MikaelRules.mayChase(false, false, false));
        assertFalse(MikaelRules.mayChase(true, true, false));
        assertTrue(MikaelRules.mayChase(true, true, true));
    }

    @Test
    void heIsStrandedWhenThePreyIsGoneOrItIsDayWithNobodyToFight() {
        assertTrue(MikaelRules.stranded(false, true, true));
        assertTrue(MikaelRules.stranded(true, false, false));
        assertFalse(MikaelRules.stranded(true, false, true));
        assertFalse(MikaelRules.stranded(true, true, false));
    }

    @Test
    void heWithdrawsAfterAMinuteStrandedAndNotBefore() {
        assertFalse(MikaelRules.withdraws(MikaelRules.WITHDRAW_AFTER - 10));
        assertTrue(MikaelRules.withdraws(MikaelRules.WITHDRAW_AFTER));
    }

    @Test
    void aLostBossFoundAgainIsOutThereOrDownAsItIs() {
        assertEquals(BossState.ACTIVE, BossRules.next(BossState.LOST, Event.FOUND));
        assertEquals(BossState.OPEN, BossRules.next(BossState.LOST, Event.FOUND_OPEN));
    }

    @Test
    void aLostBossThatDiesIsDead() {
        assertEquals(BossState.DEAD, BossRules.next(BossState.LOST, Event.KILLED));
    }

    @Test
    void foundMeansNothingToABossThatIsNotLost() {
        assertEquals(BossState.UNSEEN, BossRules.next(BossState.UNSEEN, Event.FOUND));
        assertEquals(BossState.OPEN, BossRules.next(BossState.OPEN, Event.FOUND));
        assertEquals(BossState.DEAD, BossRules.next(BossState.DEAD, Event.FOUND_OPEN));
    }
}
