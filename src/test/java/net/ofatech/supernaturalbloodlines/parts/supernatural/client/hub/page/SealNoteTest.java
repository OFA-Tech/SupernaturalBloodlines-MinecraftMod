package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SealNoteTest {
    /** The Leave link stands at 24 below the top of the pack seal and is 11 tall. */
    private static final int LEAVE_BOTTOM = 24 + 11;

    @Test
    void theLineThatSaysWhatTheSealAcceptsClearsTheLeaveLinkAndStaysInsideTheSealsHeight() {
        assertTrue(BookKit.SEAL_NOTE_DY >= LEAVE_BOTTOM, "under the leave link");
        assertTrue(BookKit.SEAL_NOTE_DY + BookKit.SEAL_NOTE_H <= BookKit.SEAL, "inside the 44 px the page already keeps for the seal");
    }
}