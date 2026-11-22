package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge.Look;
import org.junit.jupiter.api.Test;

class SlotBadgeTest {
    private static final Box SLOT = new Box(100, 100, HudLayout.SLOT, HudLayout.SLOT);

    @Test
    void theCooldownSecondsNeverCoverTheKeyBadgeInAnyTheme() {
        for (Look look : Look.values()) {
            for (int label : new int[] {SlotBadge.CHAR_W, 2 * SlotBadge.CHAR_W}) {
                Box badge = SlotBadge.key(look, SLOT, label);
                for (int seconds : new int[] {1, 9, 30, 99, 100, 300, 5999}) {
                    Box text = SlotBadge.seconds(look, SLOT, SlotBadge.CHAR_W * SlotBadge.timeLabel(seconds).length());
                    assertFalse(text.overlaps(badge), look + " label " + label + "px, " + seconds + "s: " + text + " on " + badge);
                }
            }
        }
    }

    @Test
    void theTensuraModeMarkNeverCoversTheBadgeOrTheSeconds() {
        Box mark = SlotBadge.tensuraMark(SLOT);
        for (Look look : Look.values()) {
            for (int label : new int[] {SlotBadge.CHAR_W, 2 * SlotBadge.CHAR_W}) {
                assertFalse(mark.overlaps(SlotBadge.key(look, SLOT, label)), look + " badge");
            }
            for (int seconds : new int[] {1, 30, 99, 100, 5999}) {
                assertFalse(mark.overlaps(SlotBadge.seconds(look, SLOT, SlotBadge.CHAR_W * SlotBadge.timeLabel(seconds).length())), look + " " + seconds + "s");
            }
        }
    }

    @Test
    void aLongCooldownIsShownInMinutesSoItStaysNarrow() {
        assertEquals("4s", SlotBadge.timeLabel(4));
        assertEquals("99s", SlotBadge.timeLabel(99));
        assertEquals("2m", SlotBadge.timeLabel(100));
        assertEquals("5m", SlotBadge.timeLabel(300));
        for (int s = 0; s < 6000; s++) {
            assertTrue(SlotBadge.timeLabel(s).length() <= 3, s + "s");
        }
    }

    @Test
    void theBloodMoonPlateHoldsItsLetterWithRoomAround() {
        for (int label : new int[] {SlotBadge.CHAR_W, 2 * SlotBadge.CHAR_W}) {
            Box plate = SlotBadge.key(Look.BLOOD_MOON, SLOT, label);
            int letterX = SLOT.x() + (HudLayout.SLOT - label) / 2;
            assertTrue(plate.x() < letterX && plate.right() > letterX + label, "the plate is wider than the letter");
            assertTrue(plate.y() <= SLOT.bottom() - 9 && plate.bottom() >= SLOT.bottom() - 1, "and as tall");
        }
    }

    @Test
    void theKeyBadgeIsTheSizeOfTheKitsPictureInEveryTheme() {
        for (Look look : Look.values()) {
            Box one = SlotBadge.key(look, SLOT, SlotBadge.CHAR_W);
            Box two = SlotBadge.key(look, SLOT, 2 * SlotBadge.CHAR_W);
            assertEquals(11, one.w(), look + " one character");
            assertEquals(11, one.h());
            assertEquals(17, two.w(), look + " two characters");
            assertEquals(11, two.h());
        }
    }

    @Test
    void withAMasteryBarTheLiftedBadgeTheSecondsAndTheBarShareNoPixelInAnyTheme() {
        int lift = MasteryBar.LIFT;
        Box bar = MasteryBar.box(SLOT);
        for (Look look : Look.values()) {
            for (int label : new int[] {SlotBadge.CHAR_W, 2 * SlotBadge.CHAR_W}) {
                Box badge = SlotBadge.key(look, SLOT, label, lift);
                assertFalse(badge.overlaps(bar), look + " badge on the mastery bar");
                float scale = SlotBadge.secondsScale(look, lift);
                for (int seconds : new int[] {1, 9, 30, 99, 100, 5999}) {
                    int w = Math.round(SlotBadge.CHAR_W * SlotBadge.timeLabel(seconds).length() * scale);
                    Box text = SlotBadge.seconds(look, SLOT, w, lift);
                    assertFalse(text.overlaps(badge), look + " label " + label + "px, " + seconds + "s lifted: " + text + " on " + badge);
                    assertFalse(text.overlaps(bar), look + " " + seconds + "s on the bar");
                }
            }
        }
    }

    @Test
    void theSecondsAreSmallerOnlyWhereTheLiftedBadgeLeavesNoRoomForFullSize() {
        assertEquals(1f, SlotBadge.secondsScale(Look.GRIMOIRE, 0));
        assertEquals(1f, SlotBadge.secondsScale(Look.TENSURA, MasteryBar.LIFT));
        assertEquals(SlotBadge.SECONDS_SCALE, SlotBadge.secondsScale(Look.GRIMOIRE, MasteryBar.LIFT));
        assertEquals(SlotBadge.SECONDS_SCALE, SlotBadge.secondsScale(Look.BLOOD_MOON, MasteryBar.LIFT));
    }

    @Test
    void theBloodMoonPlateOnEverySlotHangsOffTheCornerSoItNeverCoversTheIconTheLockOrTheSeconds() {
        assertEquals(Look.TENSURA, SlotBadge.placement(Look.BLOOD_MOON));
        assertEquals(Look.GRIMOIRE, SlotBadge.placement(Look.GRIMOIRE));
        assertEquals(Look.TENSURA, SlotBadge.placement(Look.TENSURA));
        Box lock = SlotBadge.lock(SLOT);
        Box tab = SlotBadge.key(SlotBadge.placement(Look.BLOOD_MOON), SLOT, 2 * SlotBadge.CHAR_W);
        int lw = Math.max(0, Math.min(tab.right(), lock.right()) - Math.max(tab.x(), lock.x()));
        int lh = Math.max(0, Math.min(tab.bottom(), lock.bottom()) - Math.max(tab.y(), lock.y()));
        assertTrue(lw * lh * 8 <= lock.w() * lock.h(), "the tab covers " + lw * lh + " of the lock's 100 pixels, a corner at most");
        assertTrue(SlotBadge.key(Look.BLOOD_MOON, SLOT, SlotBadge.CHAR_W).overlaps(lock), "the premise: the plate at the bottom was on the lock");
        Box icon = new Box(SLOT.x() + 3, SLOT.y() + 3, 16, 16);
        for (int lift : new int[] {0, MasteryBar.LIFT}) {
            Box badge = SlotBadge.key(SlotBadge.placement(Look.BLOOD_MOON), SLOT, 2 * SlotBadge.CHAR_W, lift);
            int w = Math.min(badge.right(), icon.right()) - Math.max(badge.x(), icon.x());
            int h = Math.min(badge.bottom(), icon.bottom()) - Math.max(badge.y(), icon.y());
            int covered = Math.max(0, w) * Math.max(0, h);
            assertTrue(covered * 4 <= icon.w() * icon.h(), "the plate covers " + covered + " of " + icon.w() * icon.h() + " icon pixels (lift " + lift + ")");
        }
    }

    @Test
    void aLongKeyNameIsShownAsWhatThePlayerKnowsItBy() {
        assertEquals("R", SlotBadge.shortKey("R"));
        assertEquals("F5", SlotBadge.shortKey("F5"));
        assertEquals("LS", SlotBadge.shortKey("Left Shift"));
        assertEquals("RC", SlotBadge.shortKey("Right Control"));
        assertEquals("CL", SlotBadge.shortKey("Caps Lock"));
        assertEquals("B4", SlotBadge.shortKey("Button 4"));
        assertEquals("M4", SlotBadge.shortKey("Mouse Button 4"));
        assertEquals("K1", SlotBadge.shortKey("Keypad 1"));
        assertEquals("Sp", SlotBadge.shortKey("Space"));
        for (String name : new String[] {"Left Shift", "Mouse Button 4", "Keypad 1", "Space", "Right Alt", "Page Down"}) {
            assertTrue(SlotBadge.shortKey(name).length() <= 2, name);
        }
    }

    @Test
    void aNumberedKeyKeepsItsWholeNumberSoF12IsNotF1AndButton10IsNotButton1() {
        for (String name : new String[] {"F10", "F11", "F12", "F13"}) {
            assertEquals(name, SlotBadge.shortKey(name), name + " whole");
            assertNotEquals(SlotBadge.shortKey("F1"), SlotBadge.shortKey(name), name + " is not F1");
        }
        assertEquals("B10", SlotBadge.shortKey("Button 10"));
        assertEquals("B11", SlotBadge.shortKey("Button 11"));
        assertEquals("M12", SlotBadge.shortKey("Mouse Button 12"));
        assertNotEquals(SlotBadge.shortKey("Button 1"), SlotBadge.shortKey("Button 10"));
        assertNotEquals(SlotBadge.shortKey("Button 1"), SlotBadge.shortKey("Button 11"));
        assertEquals("Sp", SlotBadge.shortKey("Space"), "a word is still two characters");
    }

    @Test
    void aThreeCharacterKeyGetsABadgeWideEnoughForItsRimAndTheOthersKeepThePictures() {
        assertEquals(11, SlotBadge.badgeWidth(SlotBadge.CHAR_W));
        assertEquals(17, SlotBadge.badgeWidth(2 * SlotBadge.CHAR_W));
        assertEquals(3 * SlotBadge.CHAR_W + 5, SlotBadge.badgeWidth(3 * SlotBadge.CHAR_W));
        for (Look look : Look.values()) {
            Box badge = SlotBadge.key(look, SLOT, 3 * SlotBadge.CHAR_W);
            assertTrue(badge.w() >= 3 * SlotBadge.CHAR_W + 2 * 2, look + " holds three characters with a rim");
        }
    }

    @Test
    void theSecondsBackingIsAPixelBiggerOnEverySide() {
        Box text = new Box(10, 20, 13, 5);
        Box back = SlotBadge.secondsBacking(text);
        assertEquals(new Box(9, 19, 15, 7), back);
    }
    @Test
    void theTensuraMarkIsTheSizeOfTheKitsDiamondAndSitsOnTheCorner() {
        Box mark = SlotBadge.tensuraMark(SLOT);
        assertEquals(7, mark.w());
        assertEquals(7, mark.h());
        assertTrue(mark.x() < SLOT.right() && mark.right() > SLOT.right(), "over the right edge");
        assertTrue(mark.y() < SLOT.y(), "mostly above the slot");
    }
}
