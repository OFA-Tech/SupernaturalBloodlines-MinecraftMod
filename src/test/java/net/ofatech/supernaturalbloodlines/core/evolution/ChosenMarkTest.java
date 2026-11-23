package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ChosenMarkTest {
    @Test
    void theServersAnswerSetsTheMark() {
        assertEquals("a:b", ChosenMark.after(null, ChosenMark.CHOSEN, "a:b"));
        assertEquals("a:b", ChosenMark.after("c:d", ChosenMark.CHOSEN_IS, "a:b"));
    }

    @Test
    void clearedAndNoneClearIt() {
        assertNull(ChosenMark.after("a:b", ChosenMark.CLEARED, null));
        assertNull(ChosenMark.after("a:b", ChosenMark.NONE, null));
    }

    @Test
    void anythingElseLeavesItAlone() {
        assertEquals("a:b", ChosenMark.after("a:b", "supernaturalbloodlines.refusal.mastery.system", null));
        assertEquals("a:b", ChosenMark.after("a:b", "chat.type.text", "someone"));
        assertEquals("a:b", ChosenMark.after("a:b", ChosenMark.CHOSEN, null));
    }
}