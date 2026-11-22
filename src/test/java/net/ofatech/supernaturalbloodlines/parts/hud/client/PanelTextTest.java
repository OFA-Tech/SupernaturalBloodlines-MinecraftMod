package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.hud.client.PanelText.Entry;
import net.ofatech.supernaturalbloodlines.parts.hud.client.PanelText.Line;
import net.ofatech.supernaturalbloodlines.parts.hud.client.PanelText.Role;
import org.junit.jupiter.api.Test;

class PanelTextTest {
    private static int width(String s) {
        return s.length() * 6;
    }

    private static final Entry PREDATOR = new Entry("R  Predator", null, "42%", "Blade mode \u00b7 Eats what it kills");
    private static final Entry POISON = new Entry("Z  Poison Resistance", "Cannot use it right now.", "Learning 60%", "Resists poison");
    private static final Entry SAGE = new Entry("X  Great Sage", null, "Ready in 4 s \u00b7 Learned \u00b7 88%", "Thinks for you");

    private static String text(List<Line> lines) {
        StringBuilder out = new StringBuilder();
        for (Line l : lines) {
            out.append(l.text()).append('|');
        }
        return out.toString();
    }

    @Test
    void thePanelCarriesEverythingTheLabelLeavesOut() {
        String all = text(PanelText.lines(List.of(PREDATOR, POISON, SAGE), 190, 40, PanelTextTest::width));
        assertTrue(all.contains("42%") && all.contains("Eats what it kills"), "the mastery and the description of a slot: " + all);
        assertTrue(all.contains("Cannot use it right now.") && all.contains("Learning 60%") && all.contains("Resists poison"), "the whole reason: " + all);
        assertTrue(all.contains("Ready in 4 s") && all.contains("Thinks for you"), all);
    }

    @Test
    void theRolesColourTheLines() {
        List<Line> lines = PanelText.lines(List.of(POISON), 190, 40, PanelTextTest::width);
        assertEquals(Role.NAME, lines.get(0).role());
        assertEquals(Role.WARN, lines.get(1).role());
        assertEquals(Role.BODY, lines.get(2).role());
    }

    @Test
    void whenTheRoomIsShortTheDescriptionsGoFirstAndEverySlotKeepsItsNameAndItsState() {
        List<Line> full = PanelText.lines(List.of(PREDATOR, POISON, SAGE), 190, 40, PanelTextTest::width);
        int fewer = full.size() - 3;
        List<Line> lines = PanelText.lines(List.of(PREDATOR, POISON, SAGE), 190, fewer, PanelTextTest::width);
        assertTrue(lines.size() <= fewer, lines.size() + " lines for " + fewer);
        String t = text(lines);
        assertTrue(t.contains("R  Predator") && t.contains("Z  Poison Resistance") && t.contains("X  Great Sage"), "every name: " + t);
        assertTrue(t.contains("42%") && t.contains("Cannot use it right now.") && t.contains("Ready in 4 s"), "every state and reason: " + t);
        assertFalse(t.contains("Thinks for you"), "the last description went first: " + t);
    }

    @Test
    void aVeryShortRoomStillKeepsTheNames() {
        List<Line> lines = PanelText.lines(List.of(PREDATOR, POISON, SAGE), 190, 3, PanelTextTest::width);
        String t = text(lines);
        assertTrue(t.contains("R  Predator") && t.contains("Z  Poison Resistance") && t.contains("X  Great Sage"), t);
        assertEquals(3, lines.size());
    }

    @Test
    void aLongNameWrapsToTwoLinesAndTheSecondIsTheFirstToGo() {
        Entry longName = new Entry("Z  A very long skill name that wraps", null, "42%", null);
        assertEquals(3, PanelText.lines(List.of(longName), 120, 40, PanelTextTest::width).size());
        assertEquals(2, PanelText.lines(List.of(longName), 120, 2, PanelTextTest::width).size());
    }
}
