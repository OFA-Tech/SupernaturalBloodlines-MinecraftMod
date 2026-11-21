package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import org.junit.jupiter.api.Test;

class SlotLabelTest {
    /** A stand-in for the font: 6 px a character (Minecraft's average is a little under, so this is the cautious side). */
    private static int width(String s) {
        return s.length() * 6;
    }

    /** The text room of the left column's labels at GUI scale 4 of a 1920 x 1080 window (480 x 270), the tightest size where labels are drawn. */
    private static int roomAtScale4() {
        return HudLayout.labels(480, 270)[0].w() - SlotLabel.PAD * 2;
    }

    @Test
    void theNameIsTheFirstLineAndTheSecondHoldsStateMasteryModeAndDescriptionInThatOrder() {
        assertEquals(java.util.List.of("Fireball", ""), java.util.List.of(SlotLabel.lines("Fireball", false, null, null, null, null, 500, SlotLabelTest::width)));
        assertEquals(java.util.List.of("Fireball", "Ready in 4 s \u00b7 42% \u00b7 Blade mode \u00b7 Throws fire"),
                java.util.List.of(SlotLabel.lines("Fireball", false, "Ready in 4 s", "42%", "Blade mode", "Throws fire", 500, SlotLabelTest::width)));
        assertEquals(java.util.List.of("Fireball", "Mastered \u00b7 Throws fire"),
                java.util.List.of(SlotLabel.lines("Fireball", false, " ", "Mastered", null, "Throws fire", 500, SlotLabelTest::width)));
    }

    @Test
    void theNameIsKeptWholeWhenItFitsAndTheDescriptionIsTheFirstThingCut() {
        int room = roomAtScale4();
        String[] lines = SlotLabel.lines("Poison Resistance", false, null, "42%", null, "Resists every kind of poison there is", room, SlotLabelTest::width);
        assertEquals("Poison Resistance".length() * 6 <= room ? "Poison Resistance" : lines[0], lines[0]);
        assertEquals(2, lines.length);
        assertTrue(width(lines[1]) <= room && lines[1].startsWith("42% \u00b7 Resist") && lines[1].endsWith("\u2026"), lines[1]);
        // a name wider than the room is cut with an ellipsis, never run over
        assertTrue(SlotLabel.lines("Poison Resistance", false, null, null, null, null, 60, SlotLabelTest::width)[0].endsWith("\u2026"));
        assertEquals("Predator", SlotLabel.lines("Predator", false, null, null, null, null, 60, SlotLabelTest::width)[0]);
    }

    @Test
    void aBlockedSlotWrapsItsReasonToAThirdLineAndNeverCutsIt() {
        int room = roomAtScale4();
        String reason = "Cannot use it right now."; // what the lang gives; the test below has the long sentence
        String[] lines = SlotLabel.lines("Poison Resistance", true, reason, "Learning 60%", null, "Resists poison", room, SlotLabelTest::width);
        assertEquals(3, lines.length, "name, reason and the rest, no more: " + String.join("|", lines));
        assertTrue(String.join(" ", java.util.Arrays.copyOfRange(lines, 1, lines.length)).startsWith(reason), "the reason is whole over the lines after the name: " + String.join("|", lines));
        for (String line : lines) {
            assertTrue(width(line) <= room, line);
        }
        // and the same slot, not blocked, stays on two lines with the reason absent
        assertEquals(2, SlotLabel.lines("Poison Resistance", false, null, "Learning 60%", null, "Resists poison", room, SlotLabelTest::width).length);
    }

    @Test
    void aLongReasonInANarrowLabelIsStillWholeAndTakesTheLinesItNeeds() {
        String reason = "This action is not unlocked for your faction.";
        String[] lines = SlotLabel.lines("Bat form", true, reason, "42%", null, "Fly", roomAtScale4(), SlotLabelTest::width);
        assertTrue(lines.length <= 5 && String.join(" ", java.util.Arrays.copyOfRange(lines, 1, lines.length)).startsWith(reason), String.join("|", lines));
    }

    @Test
    void everyLabelBoxThatIsDrawnHasRoomAndNoLineIsWiderThanIt() {
        for (int[] s : new int[][] {{960, 540}, {640, 360}, {480, 270}, {427, 240}, {320, 180}, {171, 96}}) {
            for (HudLayout.Box b : HudLayout.labels(s[0], s[1])) {
                int room = b.w() - SlotLabel.PAD * 2;
                if (room < SlotLabel.MIN_ROOM) {
                    assertEquals(0, b.w(), "a box that is too small is not drawn at all: " + b);
                    continue;
                }
                for (boolean blocked : new boolean[] {false, true}) {
                    for (String line : SlotLabel.lines("A very long skill name indeed", blocked, "You cannot use this skill now.", "Learning 100%", "Another long mode name",
                            "A rather long description", room, SlotLabelTest::width)) {
                        assertTrue(width(line) <= room, line + " in " + b);
                    }
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
