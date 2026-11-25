package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CurseExtendTest {
    @Test
    void anUnmarkedBrothersCurseEndsAfterTheHours() {
        assertEquals(OptionalLong.of(1000 + 48 * 3_600_000L), CurseExtend.endAt(false, false, 1000, 48));
    }

    @Test
    void aMarkedBrothersCurseGetsNoTimer() {
        assertTrue(CurseExtend.endAt(false, true, 1000, 48).isEmpty());
    }

    @Test
    void anAlreadyCursedKillerGetsNothingNew() {
        assertTrue(CurseExtend.endAt(true, false, 1000, 48).isEmpty());
        assertTrue(CurseExtend.endAt(true, true, 1000, 48).isEmpty());
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