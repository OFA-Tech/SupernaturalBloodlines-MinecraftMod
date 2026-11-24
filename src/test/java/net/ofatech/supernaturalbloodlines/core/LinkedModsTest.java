package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class LinkedModsTest {
    @Test
    void theTestedSeriesAndItsPatchesAreTested() {
        assertFalse(LinkedMods.untested("2.0.1", "2.0.1"));
        assertFalse(LinkedMods.untested("2.0.1.5", "2.0.1"));
        assertFalse(LinkedMods.untested("9.5.1+1.21.1", "9.5"));
        assertFalse(LinkedMods.untested("4.0.7-beta", "4.0"));
        assertFalse(LinkedMods.untested("9.6.0+1.21.1", LinkedMods.TESTED.get("curios")));
    }

    @Test
    void theRowsOfPlan19dAreTested() {
        assertFalse(LinkedMods.untested("2.2.1", LinkedMods.TESTED.get("mysticism")));
        assertTrue(LinkedMods.untested("2.3.0", LinkedMods.TESTED.get("mysticism")));
        assertFalse(LinkedMods.untested("2.1.4", LinkedMods.TESTED.get("stextras")));
        assertFalse(LinkedMods.untested("2.1.3", LinkedMods.TESTED.get("stextras")));
        assertTrue(LinkedMods.untested("2.2.0", LinkedMods.TESTED.get("stextras")));
        assertFalse(LinkedMods.untested("0.31", LinkedMods.TESTED.get("tensurahorizons")));
        assertFalse(LinkedMods.untested("2.0.3-1.21.1-HOTFIX2", LinkedMods.TESTED.get("tensura_kumodesu")));
    }

    @Test
    void aLaterOrDifferentSeriesIsUntested() {
        assertTrue(LinkedMods.untested("2.0.2", "2.0.1"));
        assertTrue(LinkedMods.untested("2.0.10", "2.0.1"), "2.0.10 is not 2.0.1.x");
        assertTrue(LinkedMods.untested("2.1.0", "2.0.1"));
        assertTrue(LinkedMods.untested("1.10.14", "1.10.13"));
        assertTrue(LinkedMods.untested("2.0", "2.0.1"));
        assertTrue(LinkedMods.untested("9.7.0", LinkedMods.TESTED.get("curios")));
    }

    @Test
    void accessoriesLayerWithCuriosWarnsOnlyForThePair() {
        assertTrue(LinkedMods.warnCrashPair(Set.of("accessories_compat_layer", "curios")::contains, NOPLogger.NOP_LOGGER));
        assertFalse(LinkedMods.warnCrashPair(Set.of("curios")::contains, NOPLogger.NOP_LOGGER));
        assertFalse(LinkedMods.warnCrashPair(Set.of("accessories_compat_layer")::contains, NOPLogger.NOP_LOGGER));
    }

    @Test
    void onlyInstalledModsAreChecked() {
        int warned = LinkedMods.warnUntested(id -> id.equals("tensura") ? "2.2.0" : id.equals("curios") ? "9.5.1+1.21.1" : null, NOPLogger.NOP_LOGGER);
        assertEquals(1, warned);
        assertTrue(LinkedMods.TESTED.keySet().containsAll(Map.of("tensura", 1, "enchanted", 1).keySet()));
    }
}
