package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldEdgeTest {
    private static final int WARN = 0xFFB3261E;
    private static final int ACCENT = 0xFF3FD0D6;

    @Test
    void aRefusedValueIsOutlinedInTheWarningColourAndABoxBeingTypedInInTheAccent() {
        assertEquals(WARN, FieldEdge.colour(false, true, WARN, ACCENT));
        assertEquals(ACCENT, FieldEdge.colour(true, false, WARN, ACCENT));
        assertEquals(0, FieldEdge.colour(false, false, WARN, ACCENT), "a quiet box has no line");
    }

    @Test
    void aRefusedValueStaysWarnedWhileItIsBeingEdited() {
        assertEquals(WARN, FieldEdge.colour(true, true, WARN, ACCENT));
    }
}