package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import org.junit.jupiter.api.Test;

class SlotLabelTest {
    /** A stand-in for the font: 6 px a character. */
    private static int width(String s) {
        return s.length() * 6;
    }

    @Test
    void theNameIsTheFirstLineAndTheSecondLineHoldsMasteryStateModeAndDescriptionInThatOrder() {
        assertArrayEquals(new String[] {"Fireball", ""}, SlotLabel.lines("Fireball", null, null, null, null));
        assertArrayEquals(new String[] {"Fireball", "42% \u00b7 Ready in 4 s \u00b7 Blade mode \u00b7 Throws fire"},
                SlotLabel.lines("Fireball", "42%", "Ready in 4 s", "Blade mode", "Throws fire"));
        assertArrayEquals(new String[] {"Fireball", "Mastered \u00b7 Throws fire"}, SlotLabel.lines("Fireball", "Mastered", " ", null, "Throws fire"));
    }

    @Test
    void theNameIsKeptWholeWhenItFitsAndTheSecondLineLosesItsEndFirst() {
        String[] lines = SlotLabel.lines("Poison Resistance", "42%", null, null, "Resists every kind of poison there is");
        assertEquals("Poison Resistance", SlotLabel.fit(lines[0], 17 * 6, SlotLabelTest::width));
        String second = SlotLabel.fit(lines[1], 100, SlotLabelTest::width);
        assertTrue(width(second) <= 100 && second.startsWith("42% \u00b7 Resists") && second.endsWith("\u2026"), second);
        // a name wider than the room is cut with an ellipsis, never run over
        assertTrue(SlotLabel.fit("Poison Resistance", 60, SlotLabelTest::width).endsWith("\u2026"));
    }

    @Test
    void everyLabelBoxThatIsDrawnHasRoomForTextAndAnythingFitsIt() {
        for (int[] s : new int[][] {{960, 540}, {640, 360}, {480, 270}, {427, 240}, {320, 180}, {171, 96}}) {
            for (HudLayout.Box b : HudLayout.labels(s[0], s[1])) {
                int room = b.w() - SlotLabel.PAD * 2;
                if (room < SlotLabel.MIN_ROOM) {
                    assertEquals(0, b.w(), "a box that is too small is not drawn at all: " + b);
                    continue;
                }
                for (String line : SlotLabel.lines("A very long skill name indeed", "100%", "Ready in 120 s", "Another long mode name", "A rather long description")) {
                    assertTrue(width(SlotLabel.fit(line, room, SlotLabelTest::width)) <= room, line + " in " + b);
                }
            }
        }
    }

    @Test
    void thePercentIsRounded() {
        assertEquals(42, SlotLabel.percent(Mastery.of(42.4, 100, 0, false)));
        assertEquals(100, SlotLabel.percent(Mastery.of(100, 100, 0, true)));
        assertEquals(60, SlotLabel.percent(Mastery.of(-40, 100, 100, false)));
    }
}
