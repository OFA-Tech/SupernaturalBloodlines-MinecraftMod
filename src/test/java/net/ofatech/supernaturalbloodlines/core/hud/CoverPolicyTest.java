package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoverPolicyTest {
    @Test
    void coversOnlyWhenEverythingHolds() {
        assertTrue(CoverPolicy.cover(true, true, true, true));
    }

    @Test
    void theHudOffGivesTheKeysBackEvenWithHideCoveredKeysOn() {
        assertFalse(CoverPolicy.cover(false, true, true, true));
    }

    @Test
    void theOptionOffGivesTheKeysBack() {
        assertFalse(CoverPolicy.cover(true, false, true, true));
    }

    @Test
    void aServerWithoutTheHudChannelGivesTheKeysBack() {
        assertFalse(CoverPolicy.cover(true, true, true, false));
    }

    @Test
    void leavingTheWorldGivesTheKeysBack() {
        assertFalse(CoverPolicy.cover(true, true, false, true));
        assertFalse(CoverPolicy.cover(true, true, false, false));
    }
}
