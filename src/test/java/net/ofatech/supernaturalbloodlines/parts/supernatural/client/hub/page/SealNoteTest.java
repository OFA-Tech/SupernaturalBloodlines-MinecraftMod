package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SealNoteTest {
    private static final int PAGE_BOTTOM = 400;

    @Test
    void theLineThatSaysWhatTheSealAcceptsStandsRightOverTheSealAndClearOfEverythingBesideIt() {
        int sealY = PAGE_BOTTOM - BookKit.SEAL;
        int noteY = BookKit.sealNoteY(sealY);
        assertEquals(sealY - BookKit.SEAL_NOTE_ABOVE, noteY);
        assertTrue(noteY + BookKit.SEAL_NOTE_H <= sealY, "the note ends before the seal begins");
        // the buttons beside the seal start at its top, so a note over the seal is clear of them
        assertTrue(noteY + BookKit.SEAL_NOTE_H <= sealY + 2);
    }

    @Test
    void theColumnAboveGivesUpEnoughRoomForTheNoteAndTheMoreBelowMarkerOverIt() {
        int sealY = PAGE_BOTTOM - BookKit.SEAL;
        int clipBottom = PAGE_BOTTOM - BookKit.sealReserve();
        int markerBottom = clipBottom + 1 + 8; // drawn one pixel under the column, 8 tall
        assertTrue(markerBottom <= BookKit.sealNoteY(sealY), "the marker runs into the note: " + markerBottom + " against " + BookKit.sealNoteY(sealY));
    }
}