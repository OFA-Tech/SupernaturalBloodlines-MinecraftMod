package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.ChoiceModel.Key;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ChoiceModel.Type;
import org.junit.jupiter.api.Test;

class ChoiceModelTest {
    @Test
    void theDeclineButtonHasTheFocusFirstSoEnterAloneSwearsNothing() {
        ChoiceModel m = new ChoiceModel(5, 2);
        assertEquals(5, m.focus());
        assertEquals(Type.DECLINE, m.press(Key.ENTER).type());
    }

    @Test
    void upAndDownMoveTheFocusAndTheListFollowsIt() {
        ChoiceModel m = new ChoiceModel(5, 2);
        assertEquals(Type.NONE, m.press(Key.UP).type());
        assertEquals(4, m.focus());
        assertEquals(3, m.scroll(), "the last choice is brought into view");
        m.press(Key.UP);
        m.press(Key.UP);
        m.press(Key.UP);
        assertEquals(1, m.focus());
        assertEquals(1, m.scroll());
        m.press(Key.UP);
        assertEquals(0, m.scroll(), "going above the first row scrolls up");
        m.press(Key.UP);
        assertEquals(0, m.focus(), "up stops at the first");
        for (int i = 0; i < 9; i++) {
            m.press(Key.DOWN);
        }
        assertEquals(5, m.focus(), "down stops at the decline button");
    }

    @Test
    void walkingDownOffTheLastShownRowScrollsItIntoView() {
        ChoiceModel m = new ChoiceModel(5, 2);
        m.press(Key.TAB);
        assertEquals(0, m.focus());
        m.press(Key.DOWN);
        assertEquals(0, m.scroll());
        m.press(Key.DOWN);
        assertEquals(2, m.focus());
        assertEquals(1, m.scroll(), "the third choice is not on screen yet");
        m.press(Key.DOWN);
        assertEquals(2, m.scroll());
    }

    @Test
    void enterChoosesTheFocusedChoiceByItsPositionInTheWholeList() {
        ChoiceModel m = new ChoiceModel(5, 2);
        m.press(Key.UP);
        m.press(Key.UP);
        ChoiceModel.Answer a = m.press(Key.ENTER);
        assertEquals(Type.PICK, a.type());
        assertEquals(3, a.index());
    }

    @Test
    void tabCyclesThroughEverythingAndBack() {
        ChoiceModel m = new ChoiceModel(3, 3);
        m.press(Key.TAB);
        assertEquals(0, m.focus(), "after the decline button, the first choice");
        m.press(Key.TAB);
        m.press(Key.TAB);
        m.press(Key.TAB);
        assertEquals(3, m.focus());
    }

    @Test
    void theMouseMovesTheFocusToTheRowOrTheDeclineButtonUnderIt() {
        ChoiceModel m = new ChoiceModel(5, 2);
        assertFalse(m.focusDecline(), "it had the focus already");
        assertTrue(m.focusRow(1));
        assertEquals(1, m.focus());
        assertFalse(m.focusRow(1), "nothing changed");
        m.wheel(1);
        m.wheel(1);
        assertTrue(m.focusRow(0));
        assertEquals(2, m.focus(), "row 0 of a list scrolled by two is the third choice");
        assertFalse(m.focusRow(2), "a row that is not shown");
        assertFalse(m.focusRow(-1));
        assertTrue(m.focusDecline());
        assertEquals(5, m.focus());
        assertFalse(new ChoiceModel(1, 3).focusRow(1), "a row past the last choice");
    }

    @Test
    void whenNoChoiceIsDrawnNoChoiceCanHaveTheFocus() {
        ChoiceModel m = new ChoiceModel(5, 0);
        for (ChoiceModel.Key k : new ChoiceModel.Key[] {Key.UP, Key.UP, Key.TAB, Key.DOWN, Key.TAB}) {
            m.press(k);
            assertEquals(5, m.focus(), "the focus left the decline button for a choice nobody can see");
        }
        m.wheel(1);
        assertEquals(0, m.scroll());
        assertEquals(Type.NONE, m.click(0).type());
        assertFalse(m.focusRow(0));
        assertEquals(Type.DECLINE, m.press(Key.ENTER).type());
    }

    @Test
    void escapeOnlyClosesAndChoosesNothing() {
        assertEquals(Type.CLOSE, new ChoiceModel(5, 2).press(Key.ESCAPE).type());
    }

    @Test
    void theWheelMovesTheListNotTheFocusAndStopsAtBothEnds() {
        ChoiceModel m = new ChoiceModel(5, 2);
        m.wheel(-1);
        assertEquals(0, m.scroll());
        for (int i = 0; i < 9; i++) {
            m.wheel(1);
        }
        assertEquals(3, m.scroll(), "the last two are the last page");
        assertEquals(5, m.focus(), "the wheel does not move the focus");
        assertTrue(m.scrolls());
        assertFalse(new ChoiceModel(2, 2).scrolls());
    }

    @Test
    void aClickOnARowChoosesThatChoiceWhateverTheScrollAndAClickOnNothingDoesNot() {
        ChoiceModel m = new ChoiceModel(5, 2);
        m.wheel(1);
        m.wheel(1);
        ChoiceModel.Answer a = m.click(1);
        assertEquals(Type.PICK, a.type());
        assertEquals(3, a.index());
        assertEquals(Type.DECLINE, m.click(-1).type());
        assertEquals(Type.NONE, m.click(2).type(), "a row that is not shown");
        ChoiceModel few = new ChoiceModel(1, 3);
        assertEquals(Type.NONE, few.click(1).type(), "a row past the last choice");
    }
}