package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BookGuidePageTest {
    @Test
    void aChapterWithALinkEndsBelowItsTextSoItsScrollingKnowsHowLongItIs() {
        for (int end : new int[] {0, 100, 383, 1294}) {
            assertEquals(end + 18, BookGuidePage.afterLink(end));
        }
    }

    @Test
    void theContentsGivesThePageTheSpreadsFooterShows() {
        // the Guide is the 8th page of the hub: its spread is 15 and 16, and every chapter is read on it, so the Contents says 16 and never 25
        assertEquals(16, BookGuidePage.chapterPage());
    }

    @Test
    void theContentsNumbersItsChaptersInRoman() {
        assertEquals("I", BookGuidePage.roman(1));
        assertEquals("IV", BookGuidePage.roman(4));
        assertEquals("VIII", BookGuidePage.roman(8));
        assertEquals("IX", BookGuidePage.roman(9));
        assertEquals("X", BookGuidePage.roman(10));
        assertEquals("XII", BookGuidePage.roman(12));
    }
}
