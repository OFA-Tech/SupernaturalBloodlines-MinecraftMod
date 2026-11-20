package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class ChoiceLayoutTest {
    /** The GUI sizes of a 1280x720 window at scales 4, 3 and 2, and of a 1920x1080 one at 4 and 3. */
    private static final int[][] SCREENS = {{320, 180}, {427, 240}, {640, 360}, {480, 270}, {640, 360}};

    @Test
    void everyPartIsOnTheScreenInsideThePanelAndNothingOverlaps() {
        for (int[] s : SCREENS) {
            for (int lines = 1; lines <= 12; lines++) {
                for (int choices = 1; choices <= 8; choices++) {
                    ChoiceLayout l = ChoiceLayout.of(s[0], s[1], lines, choices);
                    String at = s[0] + "x" + s[1] + " lines " + lines + " choices " + choices;
                    assertTrue(l.panel().inside(new Rect(0, 0, s[0], s[1])), "the panel leaves the screen at " + at);
                    assertTrue(l.text().inside(l.panel()) && l.list().inside(l.panel()) && l.decline().inside(l.panel()), "a part leaves the panel at " + at);
                    assertFalse(l.text().overlaps(l.list()) || l.list().overlaps(l.decline()) || l.text().overlaps(l.decline()), "parts overlap at " + at);
                    assertTrue(l.rows() >= 1 || choices == 0, "no choice is visible at " + at);
                }
            }
        }
    }

    @Test
    void theOfferOfElijahFitsAtGuiScaleFourOn720pWithItsWholeTextAndTwoChoices() {
        // his text wraps to about 8 lines at that width; five choices do not fit, so the list scrolls
        ChoiceLayout l = ChoiceLayout.of(320, 180, 8, 5);
        assertEquals(8, l.textLines(), "the text lost lines it needed");
        assertTrue(l.rows() >= 2, "fewer than two choices visible: " + l.rows());
        assertTrue(l.rows() < 5, "five choices cannot fit at this size, so the list has to scroll");
    }

    @Test
    void aRoomyScreenShowsEveryChoiceAndEveryLine() {
        ChoiceLayout l = ChoiceLayout.of(640, 360, 8, 5);
        assertEquals(8, l.textLines());
        assertEquals(5, l.rows());
    }

    @Test
    void whenThereIsNoRoomTheTextGivesUpLinesNeverTheChoices() {
        ChoiceLayout l = ChoiceLayout.of(320, 120, 12, 5);
        assertTrue(l.rows() >= 2, "the choices were squeezed out: " + l.rows());
        assertTrue(l.textLines() < 12 && l.textLines() >= 1);
    }

    @Test
    void theDeclineButtonIsAlwaysTheLowestAndRowsAreStackedFromTheListTop() {
        ChoiceLayout l = ChoiceLayout.of(427, 240, 6, 5);
        assertTrue(l.decline().y() >= l.list().bottom(), "the decline button is not below the list");
        assertEquals(l.list().y(), l.row(0, false).y());
        assertEquals(l.list().y() + ChoiceLayout.ROW, l.row(1, false).y());
        assertTrue(l.row(0, true).w() < l.row(0, false).w(), "a list that scrolls leaves room for the bar");
    }
}