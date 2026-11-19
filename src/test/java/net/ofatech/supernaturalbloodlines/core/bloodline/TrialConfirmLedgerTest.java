package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The server only believes an answer to a warning it sent (R147). */
class TrialConfirmLedgerTest {
    private final UUID p = UUID.randomUUID();

    @Test
    void anAnswerWithoutAWarningIsNotBelieved() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        assertFalse(l.confirm(p, 100));
        assertFalse(l.valid(p, 100));
    }

    @Test
    void anAnswerToASentWarningLetsTheRiteStartForTwoMinutes() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        l.warn(p, 100);
        assertTrue(l.confirm(p, 200));
        assertTrue(l.valid(p, 200));
        assertTrue(l.valid(p, 200 + TrialConfirmLedger.CONFIRM_TICKS));
        assertFalse(l.valid(p, 200 + TrialConfirmLedger.CONFIRM_TICKS + 1));
    }

    @Test
    void aWarningWaitsFiveMinutesForItsAnswer() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        l.warn(p, 100);
        assertFalse(l.confirm(p, 100 + TrialConfirmLedger.WARN_TICKS + 1));
        l.warn(p, 100);
        assertTrue(l.confirm(p, 100 + TrialConfirmLedger.WARN_TICKS));
    }

    @Test
    void oneAnswerIsOneAnswer() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        l.warn(p, 0);
        assertTrue(l.confirm(p, 1));
        assertFalse(l.confirm(p, 2), "the warning was used up by the first answer");
    }

    @Test
    void theRiteStartingUsesTheAnswerUp() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        l.warn(p, 0);
        l.confirm(p, 1);
        l.consume(p);
        assertFalse(l.valid(p, 2));
    }

    @Test
    void anotherPlayersAnswerIsNotMine() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        l.warn(p, 0);
        assertFalse(l.confirm(UUID.randomUUID(), 1));
        assertFalse(l.valid(UUID.randomUUID(), 1));
    }

    @Test
    void aRestartAsksAgain() {
        TrialConfirmLedger l = new TrialConfirmLedger();
        l.warn(p, 0);
        l.confirm(p, 1);
        l.clear();
        assertFalse(l.valid(p, 2));
        assertFalse(l.confirm(p, 2));
    }
}
