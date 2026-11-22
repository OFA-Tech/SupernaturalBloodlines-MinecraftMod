package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BookGuidePageTest {
    @Test
    void aChapterWithALinkEndsBelowItsTextSoItsScrollingKnowsHowLongItIs() {
        for (int end : new int[] {0, 100, 383, 1294}) {
            assertEquals(end + 18, BookGuidePage.afterLink(end));
        }
    }

    @Test
    void aSkillsDescriptionKeyIsItsIdInLowerCaseUnderSkillDescAndTheRoarHasOne() throws Exception {
        assertEquals("supernaturalbloodlines.hub.skill_desc.s-wol-11", PageKit.skillDescKey("S-WOL-11"));
        assertEquals("supernaturalbloodlines.hub.skill_desc.s-vam-3", PageKit.skillDescKey("s-vam-3"));
        try (var in = new java.io.InputStreamReader(BookGuidePageTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            var lang = com.google.gson.JsonParser.parseReader(in).getAsJsonObject();
            assertTrue(lang.has(PageKit.skillDescKey("S-WOL-11")), "the Roar has no description on the Skills page");
        }
    }

    @Test
    void onlyTheOpenChapterSaysSoAndNoRowRepeatsAPageNumber() throws Exception {
        for (int open = 0; open < 6; open++) {
            for (int row = 0; row < 6; row++) {
                assertEquals(row == open, BookGuidePage.marksOpen(row, open));
            }
        }
        try (var in = new java.io.InputStreamReader(BookGuidePageTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            var lang = com.google.gson.JsonParser.parseReader(in).getAsJsonObject();
            assertTrue(lang.has("supernaturalbloodlines.hub.book.guide.reading"));
            assertTrue(!lang.has("supernaturalbloodlines.hub.book.guide.page"), "the same page number on every row is gone");
        }
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
