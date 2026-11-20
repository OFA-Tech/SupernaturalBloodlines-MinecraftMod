package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CloakTest {
    @Test
    void aCloakRunsFromNowForTheSettingInMinutesAndNeverLessThanOne() {
        assertEquals(1000 + 30 * 1200L, Cloak.until(1000, 30));
        assertEquals(1000 + 1200L, Cloak.until(1000, 0), "a zero setting still hides for a minute");
        assertEquals(1000 + 1200L, Cloak.until(1000, -5));
    }

    @Test
    void aCloakIsActiveUntilItsTickAndNeverWhenNoneWasMade() {
        assertTrue(Cloak.active(5000, 4999));
        assertFalse(Cloak.active(5000, 5000), "it ends on its tick");
        assertFalse(Cloak.active(5000, 6000));
        assertFalse(Cloak.active(Cloak.NONE, 0), "no cloak at the start of the world");
        assertFalse(Cloak.active(Cloak.NONE, 12345));
    }

    @Test
    void aNewCloakNeverCutsALongerOneShort() {
        long running = Cloak.until(0, 60);
        assertEquals(running, Cloak.renewed(running, 100, 1), "a short one made later does not shorten the long one");
        assertEquals(Cloak.until(100, 120), Cloak.renewed(running, 100, 120), "a longer one extends it");
        assertEquals(Cloak.until(7000, 5), Cloak.renewed(Cloak.NONE, 7000, 5));
    }
}