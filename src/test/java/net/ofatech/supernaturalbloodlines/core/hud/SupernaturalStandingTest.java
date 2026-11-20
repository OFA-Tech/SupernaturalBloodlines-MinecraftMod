package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SupernaturalStandingTest {
    @Test
    void aPlainHumanIsNotSupernaturalAndAnythingElseIs() {
        assertFalse(SupernaturalStanding.is(false, false, false, false));
        assertTrue(SupernaturalStanding.is(true, false, false, false), "a faction");
        assertTrue(SupernaturalStanding.is(false, true, false, false), "a species");
        assertTrue(SupernaturalStanding.is(false, false, true, false), "the turning");
        assertTrue(SupernaturalStanding.is(false, false, false, true), "a ghoul");
    }

    @Test
    void aHumanIsOfferedOnlyTensuraModeOrNoHudAtAll() {
        assertEquals(List.of(SlotMode.TENSURA), SlotMode.available(true, true, false));
        assertEquals(List.of(), SlotMode.available(false, true, false), "no Tensura: nothing at all");
        assertEquals(List.of(SlotMode.TENSURA, SlotMode.SUPERNATURAL), SlotMode.available(true, true, true));
        assertEquals(List.of(SlotMode.SUPERNATURAL), SlotMode.available(false, true, true), "becoming supernatural brings the mode");
    }

    @Test
    void theModeKeyHasNothingToSwitchToForAHuman() {
        List<SlotMode> human = SlotMode.available(true, true, false);
        assertEquals(SlotMode.TENSURA, SlotMode.next(SlotMode.TENSURA, human).orElseThrow(), "Caps Lock stays on Tensura");
        assertEquals(SlotMode.TENSURA, SlotMode.next(SlotMode.SUPERNATURAL, human).orElseThrow(), "and a mode it had before is dropped");
    }
}
