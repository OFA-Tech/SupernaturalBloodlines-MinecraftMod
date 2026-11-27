package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CurseExtendTest {
    @Test
    void anUnmarkedBrothersCurseEndsAfterTheHours() {
        assertEquals(1000 + 48 * 3_600_000L, CurseExtend.endAt(1000, 48));
    }

    @Test
    void expiresExactlyAtUntil() {
        assertTrue(CurseExtend.expired(500, 500));
        assertTrue(CurseExtend.expired(500, 501));
    }

    @Test
    void notBeforeUntil() {
        assertFalse(CurseExtend.expired(500, 499));
    }
}
