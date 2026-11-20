package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModeMemoryTest {
    private static final List<SlotMode> BOTH = List.of(SlotMode.TENSURA, SlotMode.SUPERNATURAL);
    private static final List<SlotMode> HUMAN = List.of(SlotMode.TENSURA);

    @Test
    void anUnavailableTickFallsBackForThatTickOnlyAndTheChoiceComesBack() {
        ModeMemory m = new ModeMemory();
        m.choose(SlotMode.SUPERNATURAL);
        assertEquals(SlotMode.SUPERNATURAL, m.current("AUTO", BOTH).orElseThrow());
        // login, respawn, dimension change: standing is not synced yet, so Supernatural mode is not on offer
        assertEquals(SlotMode.TENSURA, m.current("AUTO", HUMAN).orElseThrow());
        assertEquals(SlotMode.SUPERNATURAL, m.chosen(), "reading the current mode must not overwrite the choice");
        assertEquals(SlotMode.SUPERNATURAL, m.current("AUTO", BOTH).orElseThrow(), "back with the offer");
    }

    @Test
    void severalUnavailableTicksStillKeepTheChoice() {
        ModeMemory m = new ModeMemory();
        m.choose(SlotMode.SUPERNATURAL);
        for (int i = 0; i < 5; i++) {
            m.current("SUPERNATURAL", HUMAN);
        }
        assertEquals(SlotMode.SUPERNATURAL, m.current("TENSURA", BOTH).orElseThrow());
    }

    @Test
    void withNoChoiceTheSavedSettingStartsTheModeAndItIsNeverStored() {
        ModeMemory m = new ModeMemory();
        assertEquals(SlotMode.SUPERNATURAL, m.current("SUPERNATURAL", BOTH).orElseThrow());
        assertEquals(SlotMode.TENSURA, m.current("SUPERNATURAL", HUMAN).orElseThrow(), "not on offer: what is");
        assertEquals(null, m.chosen());
        assertEquals(SlotMode.SUPERNATURAL, m.current("SUPERNATURAL", BOTH).orElseThrow());
    }

    @Test
    void aChoiceThatIsNotOnOfferFallsBackToTheFirstOne() {
        ModeMemory m = new ModeMemory();
        m.choose(SlotMode.TENSURA);
        assertEquals(SlotMode.SUPERNATURAL, m.current("AUTO", List.of(SlotMode.SUPERNATURAL)).orElseThrow());
        assertTrue(m.current("AUTO", List.of()).isEmpty());
    }

    @Test
    void theModeKeyNeedsTwoModesAndAHumanWithTensuraIsToldWhy() {
        assertTrue(ModeMemory.canToggle(BOTH));
        assertFalse(ModeMemory.canToggle(HUMAN));
        assertTrue(ModeMemory.lockedHint(true, true, false));
        assertFalse(ModeMemory.lockedHint(true, true, true), "a supernatural player has two modes");
        assertFalse(ModeMemory.lockedHint(false, true, false), "without Tensura there is no HUD to switch");
        assertFalse(ModeMemory.lockedHint(true, false, false), "without Vampirism there is no Supernatural mode to wait for");
    }
}
