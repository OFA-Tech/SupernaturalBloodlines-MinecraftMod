package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BookGuidePageTest {
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
