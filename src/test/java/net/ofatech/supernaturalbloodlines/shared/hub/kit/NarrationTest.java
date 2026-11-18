package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NarrationTest {
    @Test
    void aSymbolButtonIsNamedByItsHintAndAPlainButtonByItsLabel() {
        assertTrue(Narration.namedByHint(true, true), "a stepper with a hint must be named by it");
        assertFalse(Narration.namedByHint(true, false), "no hint: the label is all there is");
        assertFalse(Narration.namedByHint(false, true), "a plain button keeps its label as the name");
    }

    @Test
    void theHubTellsThePageThenTheFocusedTargetAndItsTooltip() {
        assertEquals(List.of("Path", "Track on HUD", "Needs the ability HUD"), Narration.lines("Path", List.of("Track on HUD", "Needs the ability HUD"), null, null, null));
        assertEquals(List.of("Path"), Narration.lines("Path", null, null, null, null));
        assertEquals(List.of("Path", "Close"), Narration.lines("Path", List.of("", "Close", " "), null, null, null));
    }

    @Test
    void overADialogOnlyTheDialogIsRead() {
        assertEquals(List.of("Leave the pack?", "You lose your rank.", "Stay"), Narration.lines("Pack", List.of("Close"), "Leave the pack?", "You lose your rank.", "Stay"));
        assertEquals(List.of("Remove?"), Narration.lines("Pack", List.of("Close"), "Remove?", "", ""));
    }
}
