package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void theBloodMoonPlateOnTheSquareSlotsOfTensuraModeHangsOffTheCornerSoItNeverCoversTheIcon() {
        assertEquals(Look.TENSURA, SlotBadge.placement(Look.BLOOD_MOON, true));
        assertEquals(Look.BLOOD_MOON, SlotBadge.placement(Look.BLOOD_MOON, false));
        assertEquals(Look.GRIMOIRE, SlotBadge.placement(Look.GRIMOIRE, true));
        Box icon = new Box(SLOT.x() + 3, SLOT.y() + 3, 16, 16);
        for (int lift : new int[] {0, MasteryBar.LIFT}) {
            Box badge = SlotBadge.key(SlotBadge.placement(Look.BLOOD_MOON, true), SLOT, 2 * SlotBadge.CHAR_W, lift);
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
        assertEquals("M4", SlotBadge.shortKey("Mouse Button 4"));
        assertEquals("K1", SlotBadge.shortKey("Keypad 1"));
        assertEquals("Sp", SlotBadge.shortKey("Space"));
        for (String name : new String[] {"Left Shift", "Mouse Button 4", "Keypad 1", "Space", "Right Alt", "Page Down"}) {
            assertTrue(SlotBadge.shortKey(name).length() <= 2, name);
        }
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
