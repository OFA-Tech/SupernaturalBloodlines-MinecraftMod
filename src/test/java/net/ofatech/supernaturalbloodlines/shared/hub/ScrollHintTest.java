package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollHintTest {
    @Test
    void aRegionThatHasMoreBelowSaysSoUntilItIsScrolledToTheEnd() {
        assertTrue(ScrollHint.moreBelow(0, 120), "at the top of a long page");
        assertTrue(ScrollHint.moreBelow(119, 120), "one line from the end");
        assertFalse(ScrollHint.moreBelow(120, 120), "at the end");
        assertFalse(ScrollHint.moreBelow(0, 0), "a page that fits has nothing below");
        assertFalse(ScrollHint.moreBelow(5, 0));
    }

    @Test
    void aRegionThatOverflowsKeepsAStripUnderItsContentForTheMarker() {
        assertFalse(ScrollHint.overflows(100, 100), "content that just fits");
        assertTrue(ScrollHint.overflows(101, 100));
        assertEquals(100, ScrollHint.clipHeight(100, false));
        assertEquals(90, ScrollHint.clipHeight(100, true));
        assertEquals(0, ScrollHint.clipHeight(4, true), "never negative");
    }

    @Test
    void aRowThatCrossesTheClipIsNotWholeAndOneThatEndsAtItIs() {
        org.junit.jupiter.api.Assertions.assertTrue(ScrollHint.rowFits(100, 20, 120));
        org.junit.jupiter.api.Assertions.assertFalse(ScrollHint.rowFits(101, 20, 120));
        org.junit.jupiter.api.Assertions.assertFalse(ScrollHint.rowFits(110, 20, 120));
    }}
