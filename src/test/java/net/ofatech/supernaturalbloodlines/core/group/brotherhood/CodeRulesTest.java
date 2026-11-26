package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeRules.Kind;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeRules.Victim;
import org.junit.jupiter.api.Test;

class CodeRulesTest {
    private static final long DAY = 24L * 3_600_000L;
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID V = new UUID(0, 3);
    private static final UUID W = new UUID(0, 4);

    @Test
    void breaksTable() {
        assertTrue(CodeRules.breaks(new Victim(Kind.VILLAGER, false, false)), "a villager");
        assertTrue(CodeRules.breaks(new Victim(Kind.PLAYER, false, false)), "a player who is not prey and did not fight (a witch or siphoner too)");
        assertFalse(CodeRules.breaks(new Victim(Kind.PLAYER, false, true)), "a fighting player");
        assertFalse(CodeRules.breaks(new Victim(Kind.PLAYER, true, false)), "a prey player");
        assertFalse(CodeRules.breaks(new Victim(Kind.PLAYER, true, true)));
        assertFalse(CodeRules.breaks(new Victim(Kind.OTHER, false, false)), "a mob or an animal");
        assertFalse(CodeRules.breaks(new Victim(Kind.OTHER, true, false)));
    }

    @Test
    void theThirdStrikeInTheWindowExpels() {
        long now = 10 * DAY;
        assertTrue(CodeRules.expels(CodeRules.strikesIn(List.of(now - 6 * DAY, now - DAY, now), now, 7 * DAY).size(), 3));
        assertFalse(CodeRules.expels(CodeRules.strikesIn(List.of(now - DAY, now), now, 7 * DAY).size(), 3));
    }

    @Test
    void theOldestStrikeLeavesTheWindowAtExactlyTheDays() {
        long now = 20 * DAY;
        assertEquals(1, CodeRules.strikesIn(List.of(now - 7 * DAY + 1), now, 7 * DAY).size());
        assertEquals(0, CodeRules.strikesIn(List.of(now - 7 * DAY), now, 7 * DAY).size());
    }

    @Test
    void theWindowIsTheDaysInMillis() {
        assertEquals(7 * DAY, CodeRules.windowMillis(7));
        assertEquals(DAY, CodeRules.windowMillis(1));
    }

    @Test
    void aLimitOfFiveNeedsFive() {
        assertFalse(CodeRules.expels(4, 5));
        assertTrue(CodeRules.expels(5, 5));
    }

    @Test
    void breakerUntilIsNowPlusHours() {
        assertEquals(1000 + 24 * 3_600_000L, CodeRules.breakerUntil(1000, 24));
        assertEquals(1000 + 720 * 3_600_000L, CodeRules.breakerUntil(1000, 720));
    }

    @Test
    void isBreakerEndsExactlyAtUntil() {
        assertTrue(CodeRules.isBreaker(500, 499));
        assertFalse(CodeRules.isBreaker(500, 500));
        assertFalse(CodeRules.isBreaker(500, 501));
    }

    @Test
    void theLedgerCountsASecondBreakOnTheSameVictimOnce() {
        CodeLedger ledger = new CodeLedger();
        assertTrue(ledger.countsOnce(A, V, 1_000));
        assertFalse(ledger.countsOnce(A, V, 1_000));
        assertFalse(ledger.countsOnce(A, V, 60_999));
    }

    @Test
    void aBreakAfterAMinuteCountsAgain() {
        CodeLedger ledger = new CodeLedger();
        assertTrue(ledger.countsOnce(A, V, 1_000));
        assertTrue(ledger.countsOnce(A, V, 61_000));
        assertFalse(ledger.countsOnce(A, V, 61_001));
    }

    @Test
    void anotherVictimCounts() {
        CodeLedger ledger = new CodeLedger();
        assertTrue(ledger.countsOnce(A, V, 1_000));
        assertTrue(ledger.countsOnce(A, W, 1_000));
    }

    @Test
    void anotherBreakerCounts() {
        CodeLedger ledger = new CodeLedger();
        assertTrue(ledger.countsOnce(A, V, 1_000));
        assertTrue(ledger.countsOnce(B, V, 1_000));
    }

    @Test
    void aClockThatGoesBackwardsForgetsNothing() {
        CodeLedger ledger = new CodeLedger();
        assertTrue(ledger.countsOnce(A, V, 100_000));
        assertFalse(ledger.countsOnce(A, V, 50_000));
        assertFalse(ledger.countsOnce(A, V, 100_001));
    }
}