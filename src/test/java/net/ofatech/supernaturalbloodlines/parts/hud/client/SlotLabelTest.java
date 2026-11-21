package net.ofatech.supernaturalbloodlines.parts.hud.client;

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
    void theKeyAndNameComeFirstThenTheExtrasInOrder() {
        assertEquals("R  Fireball", SlotLabel.text("R", "Fireball", null, null, null));
        assertEquals("R  Fireball \u00b7 Blade mode \u00b7 Ready in 4 s \u00b7 Mastery 42%", SlotLabel.text("R", "Fireball", "Blade mode", "Ready in 4 s", "Mastery 42%"));
        assertEquals("Fireball \u00b7 Mastered", SlotLabel.text("", "Fireball", " ", null, "Mastered"));
    }

    @Test
    void aLongLineIsCutWithAnEllipsisAndKeepsTheName() {
        String text = SlotLabel.text("R", "Fireball", "Blade mode", "You cannot use this skill now.", "Mastery 42%");
        String fitted = SlotLabel.fit(text, 100, SlotLabelTest::width);
        assertTrue(width(fitted) <= 100, fitted);
        assertTrue(fitted.endsWith("\u2026") && fitted.startsWith("R  Fireball"), fitted);
        assertEquals("R  Fireball", SlotLabel.fit("R  Fireball", 100, SlotLabelTest::width));
    }

    @Test
    void everyLabelBoxHasRoomForTextAtEveryGuiScale() {
        for (int[] s : new int[][] {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {213, 120}, {171, 96}}) {
            for (HudLayout.Box b : HudLayout.labels(s[0], s[1])) {
                assertTrue(b.w() - SlotLabel.PAD * 2 >= SlotLabel.MIN_ROOM, b + " on " + s[0] + "x" + s[1]);
                String fitted = SlotLabel.fit(SlotLabel.text("R", "A very long skill name indeed", "Another long mode name", "Ready in 120 s", "Mastery 100%"), b.w() - SlotLabel.PAD * 2, SlotLabelTest::width);
                assertTrue(width(fitted) + SlotLabel.PAD * 2 <= b.w(), fitted + " is wider than " + b);
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
