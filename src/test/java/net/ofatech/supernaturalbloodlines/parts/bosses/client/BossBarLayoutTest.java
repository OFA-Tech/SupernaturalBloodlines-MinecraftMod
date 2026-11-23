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
    void theCardIsTheApprovedSizeOnAnyScreenThatHasRoomAndNarrowerOnOneThatHasNot() {
        assertEquals(250, BossBarLayout.cardWidth(960));
        assertEquals(250, BossBarLayout.cardWidth(480));
        assertEquals(250, BossBarLayout.cardWidth(258));
        assertEquals(240, BossBarLayout.cardWidth(248), "4 px of the screen are left on each side");
        assertEquals(355, BossBarLayout.cardX(960, 250));
    }

    @Test
    void aCardIsCentredOnTheScreen() {
        for (int guiW : new int[] {480, 640, 960, 427, 320}) {
            int w = BossBarLayout.cardWidth(guiW);
            int x = BossBarLayout.cardX(guiW, w);
            assertTrue(Math.abs((guiW - (x + w)) - x) <= 1, guiW + ": the margins are equal");
            assertTrue(x >= BossBarLayout.SCREEN_MARGIN, guiW + " " + x);
        }
    }

    @Test
    void withAStatusCornerTheStackStandsToItsRightWhereTheScreenHasRoom() {
        for (int guiW : new int[] {480, 427, 342, 320}) {
            int w = BossBarLayout.cardWidth(guiW, true);
            int x = BossBarLayout.cardX(guiW, w, true);
            assertTrue(x >= BossBarLayout.STATUS_CLEAR, guiW + ": the card starts at " + x + ", over the corner");
            assertTrue(x + w <= guiW - BossBarLayout.SCREEN_MARGIN, guiW + ": the card ends at " + (x + w) + ", off the screen");
            assertTrue(w >= BossBarLayout.MIN_CARD_W, guiW + ": " + w);
        }
        assertEquals(250, BossBarLayout.cardWidth(480, true), "the approved size is kept where it fits");
        assertEquals(144, BossBarLayout.cardX(480, 250, true));
        assertEquals(115, BossBarLayout.cardX(480, 250, false), "with no corner it is centred");
        assertEquals(195, BossBarLayout.cardX(640, 250, true), "at scale 3 the centred card already clears the corner");
        assertEquals(BossBarLayout.cardX(960, 250, false), BossBarLayout.cardX(960, 250, true));
    }

    @Test
    void aScreenTooNarrowForACardBesideTheCornerKeepsTheCardCentred() {
        assertEquals(BossBarLayout.SCREEN_MARGIN, BossBarLayout.leftClear(300, true));
        assertEquals(250, BossBarLayout.cardWidth(300, true));
        assertEquals(25, BossBarLayout.cardX(300, 250, true));
    }

    @Test
    void anotherModsBarUnderTheStackHasRoomForItsNameAboveIt() {
        for (int n = 1; n <= 4; n++) {
            int nextBar = BossBarLayout.cardY(12, 0) + BossBarLayout.increment(n);
            int lastCardBottom = BossBarLayout.cardY(12, n - 1) + BossBarLayout.CARD_H;
            assertTrue(nextBar - 9 >= lastCardBottom + 2, n + " cards: its name (9 px above it) starts at " + (nextBar - 9) + ", the card ends at " + lastCardBottom);
        }
        assertEquals(0, BossBarLayout.increment(0));
    }

    @Test
    void cardsStackWithASixPixelGapAndTheIncrementPutsAnotherModsBarUnderThemAll() {
        assertEquals(12, BossBarLayout.cardY(12, 0));
        assertEquals(12 + 52, BossBarLayout.cardY(12, 1));
        assertEquals(12 + 104, BossBarLayout.cardY(12, 2));
        for (int n = 1; n <= 5; n++) {
            int lastBottom = BossBarLayout.cardY(12, n - 1) + BossBarLayout.CARD_H;
            assertEquals(lastBottom - 12 + BossBarLayout.VANILLA_HEAD, BossBarLayout.increment(n), n + " cards: another mod's bar starts under the last, with room for its name");
        }
        assertEquals(0, BossBarLayout.increment(0));
    }

    @Test
    void threeCardsFitAtGuiScaleFourOnAFullHdScreenAndAtLeastOneAlwaysShows() {
        assertTrue(BossBarLayout.barsThatFit(270, 12) >= 3, "scale 4 (480 x 270): vanilla's own limit would show two");
        assertTrue(BossBarLayout.barsThatFit(360, 12) >= 3);
        assertTrue(BossBarLayout.barsThatFit(540, 12) >= 3);
        assertEquals(1, BossBarLayout.barsThatFit(100, 12), "a tiny screen still shows the first");
        int n = BossBarLayout.barsThatFit(270, 12);
        assertTrue(BossBarLayout.cardY(12, n - 1) + BossBarLayout.CARD_H <= 270 * 3 / 5, "the shown cards end inside the share of the screen");
    }

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
        assertEquals(6, BossBarLayout.GAP);
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
