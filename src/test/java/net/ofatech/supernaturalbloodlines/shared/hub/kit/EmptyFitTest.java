package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmptyFitTest {
    @Test
    void aTallPageKeepsTheHexagonAndAShortOneGivesItUpToKeepItsButtons() {
        int full = EmptyFit.height(true, true, 3, true);
        assertTrue(EmptyFit.showArt(100, 100 + full, true, 3, true), "just enough room");
        assertFalse(EmptyFit.showArt(100, 100 + full - 1, true, 3, true), "a pixel short");
        // the Grimoire's bloodline page at GUI scale 4: about 150 px under the head, text of 3 lines, two buttons
        assertFalse(EmptyFit.showArt(100, 250, true, 3, true));
        assertTrue(EmptyFit.height(false, true, 3, true) <= 150, "without the art the buttons fit: " + EmptyFit.height(false, true, 3, true));
    }

    @Test
    void aRegionWithNoHeightIsNotBounded() {
        assertTrue(EmptyFit.showArt(40, 40, true, 3, false));
        assertTrue(EmptyFit.showArt(40, 0, true, 3, false));
    }

    @Test
    void theHeightGrowsWithWhatIsOnIt() {
        int base = EmptyFit.height(false, false, 1, false);
        assertEquals(base + EmptyFit.HEX + 4, EmptyFit.height(true, false, 1, false));
        assertEquals(base + 10, EmptyFit.height(false, false, 2, false));
        assertEquals(base + 22, EmptyFit.height(false, false, 1, true));
        assertEquals(EmptyFit.height(true, false, 1, false) + 10, EmptyFit.height(true, true, 1, false));
    }
}