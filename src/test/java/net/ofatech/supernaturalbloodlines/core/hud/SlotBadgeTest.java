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
    void theBadgeStaysOnTheSlotsOwnSideWhateverTheLift() {
        for (int lift : new int[] {0, MasteryBar.LIFT}) {
            Box bar = MasteryBar.box(SLOT);
            for (Look look : new Look[] {Look.GRIMOIRE, Look.BLOOD_MOON}) {
                Box badge = SlotBadge.key(look, SLOT, SlotBadge.CHAR_W, lift);
                if (lift > 0) {
                    assertFalse(badge.overlaps(bar), look + " badge on the mastery bar");
                }
                assertTrue(badge.bottom() <= SLOT.bottom(), look + " badge inside the slot's lower edge");
            }
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
