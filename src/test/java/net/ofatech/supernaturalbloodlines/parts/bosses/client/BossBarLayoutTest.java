package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossBarLayout.Geometry;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class BossBarLayoutTest {
    /** The default font's advances: capitals, digits and the slash 6 px, the narrow I 4, a space 4, a comma 2. */
    private static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += c == ' ' || c == 'I' ? 4 : c == ',' ? 2 : 6;
        }
        return w;
    }

    private static final int[] SHIFTER = {width("HUMAN"), width("SHIFTING"), width("BEAST")};

    @Test
    void everyPartOfACardIsInsideItAndNothingOverlapsThatShouldNot() {
        for (int cardW : new int[] {250, 240, 210, 200}) {
            Geometry g = BossBarLayout.geometry(cardW, SHIFTER, 1);
            Rect card = new Rect(0, 0, cardW, BossBarLayout.CARD_H);
            Rect plate = new Rect(g.plateX(), g.plateY(), BossBarLayout.PLATE, BossBarLayout.PLATE);
            Rect art = new Rect(g.artX(), g.artY(), BossBarLayout.ART, BossBarLayout.ART);
            Rect name = new Rect(g.nameX(), g.nameY(), g.nameW(), BossBarLayout.NAME_H);
            Rect track = new Rect(g.trackX(), g.trackY(), g.trackW(), BossBarLayout.TRACK_H);
            Rect fill = new Rect(g.fillX(), g.fillY(), g.fillMaxW(), BossBarLayout.FILL_H);
            String at = "card " + cardW;
            for (Rect r : new Rect[] {plate, art, name, track, fill}) {
                assertTrue(r.inside(card), at + " " + r);
            }
            assertTrue(art.inside(plate), at + ": the portrait is inside its plate");
            assertFalse(plate.overlaps(name) || plate.overlaps(track), at + ": the plate is clear of the bar");
            assertFalse(name.overlaps(track), at + ": the name plate is over the bar, not on it");
            assertTrue(fill.inside(track), at + ": the fill is inside the track");
            assertEquals(1, g.fillX() - g.trackX(), at);
            assertEquals(1, g.fillY() - g.trackY(), at);
        }
    }

    @Test
    void theApprovedCardMeasuresWhatTheStyleGuideSays() {
        Geometry g = BossBarLayout.geometry(250, SHIFTER, 0);
        assertEquals(200, g.nameW(), "name plate 200 wide");
        assertEquals(200, g.trackW(), "bar 200 wide");
        assertEquals(34, BossBarLayout.PLATE);
        assertEquals(24, BossBarLayout.ART);
        assertEquals(13, BossBarLayout.NAME_H);
        assertEquals(9, BossBarLayout.TRACK_H);
        assertEquals(10, BossBarLayout.CHIP_H);
        assertEquals(46, BossBarLayout.CARD_H);
        assertEquals(50, g.tickX()[0] - g.trackX(), "a tick at 25 %");
        assertEquals(100, g.tickX()[1] - g.trackX(), "at 50 %");
        assertEquals(150, g.tickX()[2] - g.trackX(), "at 75 %");
    }

    @Test
    void theChipsAreRightAlignedInOrderWithTwoPixelsBetweenAndNeverRunOntoTheHealthText() {
        for (int cardW : new int[] {250, 230, 210}) {
            Geometry g = BossBarLayout.geometry(cardW, SHIFTER, 1);
            Rect name = new Rect(g.nameX(), 0, g.nameW(), 1);
            int lastRight = Integer.MAX_VALUE;
            for (int i = 2; i >= 0; i--) {
                if (g.chipW()[i] == 0) {
                    continue;
                }
                assertEquals(SHIFTER[i] + BossBarLayout.CHIP_PAD, g.chipW()[i], "a chip is its text and 6");
                int right = g.chipX()[i] + g.chipW()[i];
                assertTrue(g.chipX()[i] >= name.x() && right <= name.right(), cardW + ": chip " + i + " is on the name plate's width");
                if (lastRight != Integer.MAX_VALUE) {
                    assertEquals(BossBarLayout.CHIP_GAP, lastRight - right, cardW + ": chip " + i);
                }
                lastRight = g.chipX()[i];
            }
            int firstLeft = lastRight;
            assertTrue(g.hpX() + g.hpMaxW() <= firstLeft - BossBarLayout.HP_GAP, cardW + ": the health text ends before the chips");
            assertTrue(g.hpMaxW() >= BossBarLayout.MIN_HEALTH_W - 4 || g.chipW()[0] == 0, cardW + ": the health keeps its room unless the chips were cut down");
        }
    }

    @Test
    void aCardTooNarrowForAllThreeChipsDrawsOnlyTheLitOne() {
        Geometry narrow = BossBarLayout.geometry(200, SHIFTER, 2);
        assertEquals(0, narrow.chipW()[0]);
        assertEquals(0, narrow.chipW()[1]);
        assertTrue(narrow.chipW()[2] > 0, "the lit chip stays");
        Geometry roomy = BossBarLayout.geometry(250, SHIFTER, 2);
        assertTrue(roomy.chipW()[0] > 0 && roomy.chipW()[1] > 0 && roomy.chipW()[2] > 0, "the approved card shows all three");
    }

    @Test
    void aBossWithoutFormsHasNoChipsAndTheHealthTextTakesTheWholeRow() {
        Geometry g = BossBarLayout.geometry(250, new int[0], 0);
        assertEquals(0, g.chipX().length);
        assertEquals(g.nameX() + g.nameW() - g.hpX(), g.hpMaxW());
    }

    @Test
    void theFillIsProportionalAndAnyHealthShowsASliver() {
        assertEquals(0, BossBarLayout.fillWidth(198, 0f));
        assertEquals(0, BossBarLayout.fillWidth(198, -0.2f));
        assertEquals(0, BossBarLayout.fillWidth(198, Float.NaN));
        assertEquals(1, BossBarLayout.fillWidth(198, 0.0004f), "a boss on its last point still has a sliver");
        assertEquals(99, BossBarLayout.fillWidth(198, 0.5f));
        assertEquals(198, BossBarLayout.fillWidth(198, 1f));
        assertEquals(198, BossBarLayout.fillWidth(198, 7f));
    }

    @Test
    void theHealthIsWrittenWithCommasAndShortensToTheCurrentNumberBeforeItIsDropped() {
        BossBarLayout.Health h = BossBarLayout.health(1640, 2000, java.util.Locale.ROOT);
        assertEquals("1,640 / 2,000", h.full());
        assertEquals("1,640", h.compact());
        assertEquals("1,640 / 2,000", BossBarLayout.healthText(h, 100, BossBarLayoutTest::width));
        assertEquals("1,640", BossBarLayout.healthText(h, 40, BossBarLayoutTest::width));
        assertEquals("", BossBarLayout.healthText(h, 10, BossBarLayoutTest::width));
        BossBarLayout.Health big = BossBarLayout.health(12000, 12000, java.util.Locale.ROOT);
        Geometry g = BossBarLayout.geometry(250, SHIFTER, 0);
        String shown = BossBarLayout.healthText(big, g.hpMaxW(), BossBarLayoutTest::width);
        assertTrue(width(shown) <= g.hpMaxW(), "whatever is shown fits: " + shown);
    }

    @Test
    void theHealthIsGroupedTheWayThePlayersLanguageDoes() {
        assertEquals("1.640 / 2.000", BossBarLayout.health(1640, 2000, BossBarLayout.localeOf("de_de")).full());
        assertEquals("1.640 / 2.000", BossBarLayout.health(1640, 2000, BossBarLayout.localeOf("pt_br")).full());
        assertEquals("1,640 / 2,000", BossBarLayout.health(1640, 2000, BossBarLayout.localeOf("en_us")).full());
        assertEquals("1,640 / 2,000", BossBarLayout.health(1640, 2000, BossBarLayout.localeOf(null)).full(), "no language: the plain grouping");
        assertEquals("1,640 / 2,000", BossBarLayout.health(1640, 2000, BossBarLayout.localeOf("")).full());
    }

    @Test
    void theApprovedExampleFitsBesideTheChipsOnTheApprovedCard() {
        Geometry g = BossBarLayout.geometry(250, SHIFTER, 0);
        BossBarLayout.Health h = BossBarLayout.health(1640, 2000, java.util.Locale.ROOT);
        assertEquals(h.full(), BossBarLayout.healthText(h, g.hpMaxW(), BossBarLayoutTest::width), "1,640 / 2,000 is the first line of the style guide");
    }
}
