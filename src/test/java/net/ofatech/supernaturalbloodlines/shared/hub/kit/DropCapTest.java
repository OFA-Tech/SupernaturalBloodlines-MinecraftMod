package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DropCapTest {
    private static int width(String s) {
        return s.length() * 6;
    }

    private static final String REST = "ampire and werewolf at once. You feed in human form, shift under the moon, and your venom poisons vampires.";

    @Test
    void theRestOfTheFirstWordContinuesOnTheFirstLineBesideTheCapital() {
        List<DropCap.Line> lines = DropCap.layout(REST, 200, 13, 2, DropCapTest::width);
        assertTrue(lines.get(0).text().startsWith("ampire"), lines.get(0).text());
        assertEquals(13, lines.get(0).indent(), "right after the capital, not a word-space away from it");
    }

    @Test
    void onlyTheLinesBesideTheCapitalAreIndented() {
        List<DropCap.Line> lines = DropCap.layout(REST, 200, 13, 2, DropCapTest::width);
        assertTrue(lines.size() > 3, "the premise: the paragraph is longer than the capital");
        assertEquals(13, lines.get(1).indent());
        for (int i = 2; i < lines.size(); i++) {
            assertEquals(0, lines.get(i).indent(), "line " + i + " is below the capital");
        }
    }

    @Test
    void noLineIsWiderThanItsRoomAndNoWordIsLostOrBroken() {
        for (int width : new int[] {60, 90, 140, 200}) {
            List<DropCap.Line> lines = DropCap.layout(REST, width, 13, 2, DropCapTest::width);
            StringBuilder all = new StringBuilder();
            for (DropCap.Line l : lines) {
                assertTrue(width(l.text()) <= width - l.indent() || !l.text().contains(" "), "\"" + l.text() + "\" is wider than " + (width - l.indent()));
                all.append(all.length() == 0 ? "" : " ").append(l.text());
            }
            assertEquals(REST, all.toString(), "at " + width);
        }
    }

    @Test
    void anEmptyParagraphHasNoLines() {
        assertTrue(DropCap.layout("   ", 100, 13, 2, DropCapTest::width).isEmpty());
    }
}
