package net.ofatech.supernaturalbloodlines.core.ward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WardSpeechTest {
    @Test
    void onlyTheWholeMessageIsThePhrase() {
        assertTrue(WardSpeech.matches("come in", "come in"));
        assertTrue(WardSpeech.matches("  Come in! ", "come in"));
        assertTrue(WardSpeech.matches("COME IN...", "come in"));
        assertTrue(WardSpeech.matches("get out", WardSpeech.GET_OUT));
        assertFalse(WardSpeech.matches("please come in", "come in"));
        assertFalse(WardSpeech.matches("come in and sit", "come in"));
        assertFalse(WardSpeech.matches("", "come in"));
        assertFalse(WardSpeech.matches("   ", "come in"));
        assertFalse(WardSpeech.matches("come in", "get out"));
    }

    @Test
    void theLookedAtPlayerWinsOverACloserOne() {
        UUID far = UUID.randomUUID();
        UUID near = UUID.randomUUID();
        assertEquals(Optional.of(far), WardSpeech.pick(List.of(new WardSpeech.Candidate(near, 2.0, false), new WardSpeech.Candidate(far, 7.5, true))));
    }

    @Test
    void aLookedAtPlayerBeyondEightBlocksDoesNotCount() {
        UUID far = UUID.randomUUID();
        assertEquals(Optional.empty(), WardSpeech.pick(List.of(new WardSpeech.Candidate(far, 8.5, true))));
    }

    @Test
    void withNoOneLookedAtTheNearestWithinFiveIsPicked() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertEquals(Optional.of(b), WardSpeech.pick(List.of(new WardSpeech.Candidate(a, 4.0, false), new WardSpeech.Candidate(b, 3.0, false))));
        assertEquals(Optional.empty(), WardSpeech.pick(List.of(new WardSpeech.Candidate(a, 5.5, false))), "beyond five blocks and not looked at");
        assertEquals(Optional.empty(), WardSpeech.pick(List.of()));
    }

    @Test
    void theNearestOfSeveralLookedAtIsPicked() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertEquals(Optional.of(a), WardSpeech.pick(List.of(new WardSpeech.Candidate(b, 6.0, true), new WardSpeech.Candidate(a, 3.0, true))));
    }
}
