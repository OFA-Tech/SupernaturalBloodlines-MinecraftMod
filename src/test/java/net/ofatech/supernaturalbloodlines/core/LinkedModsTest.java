package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class LinkedModsTest {
    @Test
    void theTestedSeriesAndItsPatchesAreTested() {
        assertFalse(LinkedMods.untested("2.0.1", "2.0.1"));
        assertFalse(LinkedMods.untested("2.0.1.5", "2.0.1"));
        assertFalse(LinkedMods.untested("9.5.1+1.21.1", "9.5"));
        assertFalse(LinkedMods.untested("4.0.7-beta", "4.0"));
    }

    @Test
    void aLaterOrDifferentSeriesIsUntested() {
        assertTrue(LinkedMods.untested("2.0.2", "2.0.1"));
        assertTrue(LinkedMods.untested("2.0.10", "2.0.1"), "2.0.10 is not 2.0.1.x");
        assertTrue(LinkedMods.untested("2.1.0", "2.0.1"));
        assertTrue(LinkedMods.untested("1.10.14", "1.10.13"));
        assertTrue(LinkedMods.untested("2.0", "2.0.1"));
    }

    @Test
    void onlyInstalledModsAreChecked() {
        int warned = LinkedMods.warnUntested(id -> id.equals("tensura") ? "2.2.0" : id.equals("curios") ? "9.5.1+1.21.1" : null, NOPLogger.NOP_LOGGER);
        assertEquals(1, warned);
        assertTrue(LinkedMods.TESTED.keySet().containsAll(Map.of("tensura", 1, "enchanted", 1).keySet()));
    }
}
