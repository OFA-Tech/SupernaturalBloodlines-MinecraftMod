package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class BloodMoonLayoutTest {
    // the smallest GUI, GUI scale 4 on 1080p, 3 and 2 at 1920x1080
    private static final int[][] SCREENS = {{320, 240}, {480, 270}, {640, 360}, {960, 540}};
    private static final int TABS = 8;
    private static final int SEALS = 4;

    private static List<Rect> fixed(HubLayout l) {
        List<Rect> all = new ArrayList<>(l.tabs());
        all.addAll(l.seals());
        all.add(l.chrome().themeButton());
        all.add(l.chrome().close());
        all.add(l.chrome().counters());
        all.add(l.region(BloodMoonLayout.HEAD));
        return all;
    }

    @Test
    void theMoonAndItsGlowAreCappedOnAHugeWindowButNotOnASmallOne() {
        assertEquals(BloodMoonLayout.MAX_MOON_RADIUS, BloodMoonLayout.glowRadius(new Rect(0, 0, 8000, 6000)));
        assertEquals(BloodMoonLayout.MAX_MOON_RADIUS, BloodMoonLayout.moonRadius(new Rect(0, 0, 8000, 6000)));
        assertEquals(67, BloodMoonLayout.glowRadius(new Rect(0, 0, 200, 90)), "a small stage keeps its own size");
        assertEquals(12, BloodMoonLayout.moonRadius(new Rect(0, 0, 10, 10)), "the floor of 12 stays");
        assertEquals(92, BloodMoonLayout.moonRadius(new Rect(0, 0, 200, 200)));
    }

    @Test
    void everythingFitsTheScreen() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            Rect screen = new Rect(0, 0, s[0], s[1]);
            HubLayout l = BloodMoonLayout.of(s[0], s[1], TABS, SEALS);
            for (Rect r : fixed(l)) {
                assertTrue(r.inside(screen), at + " off screen: " + r);
            }
            for (String region : List.of(BloodMoonLayout.MOON, BloodMoonLayout.RIGHT, HubLayout.MAIN, HubLayout.SIDE)) {
                assertTrue(l.region(region).inside(screen), at + " " + region);
            }
            assertTrue(l.chrome().detail().inside(screen), at + " bar");
            assertEquals(TABS, l.tabs().size());
        }
    }

    @Test
    void theRailsTheCornerButtonsAndTheTitleDoNotTouchEachOther() {
        for (int[] s : SCREENS) {
            List<Rect> all = fixed(BloodMoonLayout.of(s[0], s[1], TABS, SEALS));
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    assertFalse(all.get(i).overlaps(all.get(j)), s[0] + "x" + s[1] + ": " + all.get(i) + " on " + all.get(j));
                }
            }
        }
    }

    @Test
    void theStageAndTheBarAreClearOfTheRailsAndOfEachOther() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = BloodMoonLayout.of(s[0], s[1], TABS, SEALS);
            Rect stage = l.region(BloodMoonLayout.MOON);
            Rect bar = l.chrome().detail();
            assertFalse(stage.overlaps(bar), at + " stage on the bar");
            for (Rect r : l.tabs()) {
                assertFalse(stage.overlaps(r) || bar.overlaps(r), at + " rail on the stage or the bar: " + r);
            }
            for (Rect r : l.seals()) {
                assertFalse(stage.overlaps(r) || bar.overlaps(r), at + " launcher on the stage or the bar: " + r);
            }
            assertTrue(stage.w() >= 150 && stage.h() >= 90, at + " the stage is too small for a moon: " + stage);
        }
    }

    @Test
    void theRightCardSitsOnTheStageAndTheListCardsAreTwinHalves() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = BloodMoonLayout.of(s[0], s[1], TABS, SEALS);
            Rect stage = l.region(BloodMoonLayout.MOON);
            assertTrue(BloodMoonLayout.card(l.region(BloodMoonLayout.RIGHT)).inside(stage), at);
            Rect main = BloodMoonLayout.card(l.region(HubLayout.MAIN));
            Rect side = BloodMoonLayout.card(l.region(HubLayout.SIDE));
            assertFalse(main.overlaps(side), at);
            assertTrue(Math.abs(main.w() - side.w()) <= 1, at + " halves differ");
            for (Rect r : l.tabs()) {
                assertFalse(main.overlaps(r), at + " list card on the rail");
            }
            for (Rect r : l.seals()) {
                assertFalse(side.overlaps(r), at + " list card on a launcher");
            }
            assertTrue(main.w() >= 100, at + " the list cards are too narrow to read");
        }
    }

    @Test
    void theLookButtonIsRoundAndTheCountersSitLeftOfIt() {
        for (int[] s : SCREENS) {
            HubLayout l = BloodMoonLayout.of(s[0], s[1], TABS, SEALS);
            assertNotNull(l.chrome().themeButton());
            assertEquals(BloodMoonLayout.RAIL, l.chrome().themeButton().w());
            assertTrue(l.chrome().counters().right() <= l.chrome().themeButton().x());
        }
    }

    @Test
    void aGuideCardEndsAboveTheFloatingBarNotUnderIt() {
        for (int[] s : SCREENS) {
            HubLayout l = BloodMoonLayout.of(s[0], s[1], TABS, SEALS);
            Rect bar = l.chrome().detail();
            assertNotNull(bar);
            for (String name : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                Rect region = l.region(name);
                int cut = BloodMoonLayout.cardCut(region, bar);
                assertTrue(cut > 0, s[0] + "x" + s[1] + " " + name + ": the region runs down behind the bar, so something must be kept out");
                Rect card = BloodMoonLayout.card(region);
                assertTrue(card.bottom() - cut <= bar.y() - BloodMoonLayout.GAP, s[0] + "x" + s[1] + " the card still reaches the bar");
                assertTrue(region.bottom() - cut + 5 <= bar.y() - BloodMoonLayout.GAP, "and so does the list");
            }
        }
    }

    @Test
    void aPaneBesideTheRegionOrAboveItTakesNothingFromIt() {
        Rect region = new Rect(100, 50, 200, 150);
        assertEquals(0, BloodMoonLayout.cardCut(region, null));
        assertEquals(0, BloodMoonLayout.cardCut(region, new Rect(320, 50, 100, 150)), "a pane to the right");
        assertEquals(0, BloodMoonLayout.cardCut(region, new Rect(100, 10, 200, 30)), "a bar above");
        assertEquals(0, BloodMoonLayout.cardCut(region, new Rect(100, 250, 200, 30)), "a bar well below");
    }
}