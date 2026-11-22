package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyringeConfirmTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void theFirstUseArmsAndTheSecondConfirms() {
        SyringeConfirm c = new SyringeConfirm();
        assertFalse(c.confirm(USER, A, 0), "the first use only arms");
        assertTrue(c.confirm(USER, A, 40), "the second, on the same player in time, confirms");
        assertFalse(c.confirm(USER, A, 41), "a confirmation is spent: the next use arms again");
    }

    @Test
    void itRunsOutAfterFiveSeconds() {
        SyringeConfirm c = new SyringeConfirm();
        c.confirm(USER, A, 0);
        assertFalse(c.confirm(USER, A, SyringeConfirm.ARM_TICKS + 1), "too late: that is a first use again");
        assertTrue(c.confirm(USER, A, SyringeConfirm.ARM_TICKS + 2), "and it armed the next one");
    }

    @Test
    void anotherTargetOrAnotherUserDoesNotConfirm() {
        SyringeConfirm c = new SyringeConfirm();
        c.confirm(USER, A, 0);
        assertFalse(c.confirm(USER, B, 10), "another player is a first use");
        assertFalse(c.confirm(UUID.randomUUID(), A, 20), "another user is a first use");
        assertTrue(c.confirm(USER, B, 30), "the arming moved to B");
    }
}
