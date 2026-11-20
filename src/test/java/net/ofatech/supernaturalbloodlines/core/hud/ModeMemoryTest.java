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
    void anUnavailableTickFallsBackForThatTickOnlyAndTheSettingBringsTheChoiceBack() {
        // the setting is the memory: the mode key wrote SUPERNATURAL
        assertEquals(SlotMode.SUPERNATURAL, ModeMemory.current("SUPERNATURAL", BOTH).orElseThrow());
        // login, respawn, dimension change: standing is not synced yet, so Supernatural mode is not on offer
        assertEquals(SlotMode.TENSURA, ModeMemory.current("SUPERNATURAL", HUMAN).orElseThrow());
        assertEquals(SlotMode.SUPERNATURAL, ModeMemory.current("SUPERNATURAL", BOTH).orElseThrow(), "back with the offer: nothing was overwritten");
    }

    @Test
    void severalUnavailableTicksStillKeepTheChoice() {
        for (int i = 0; i < 5; i++) {
            ModeMemory.current("SUPERNATURAL", HUMAN);
        }
        assertEquals(SlotMode.SUPERNATURAL, ModeMemory.current("SUPERNATURAL", BOTH).orElseThrow());
    }

    @Test
    void aChangeOfTheSettingAppliesAtOnce() {
        assertEquals(SlotMode.TENSURA, ModeMemory.current("TENSURA", BOTH).orElseThrow());
        assertEquals(SlotMode.SUPERNATURAL, ModeMemory.current("SUPERNATURAL", BOTH).orElseThrow(), "the hub Settings page picked it");
        assertEquals(SlotMode.TENSURA, ModeMemory.current("AUTO", BOTH).orElseThrow(), "auto: Tensura when it is installed");
    }

    @Test
    void aModeThatIsNotOnOfferFallsBackToTheFirstOneAndNothingMeansNoHud() {
        assertEquals(SlotMode.SUPERNATURAL, ModeMemory.current("TENSURA", List.of(SlotMode.SUPERNATURAL)).orElseThrow());
        assertTrue(ModeMemory.current("AUTO", List.of()).isEmpty());
    }

    @Test
    void theModeKeyNeedsTwoModes() {
        assertTrue(ModeMemory.canToggle(BOTH));
        assertFalse(ModeMemory.canToggle(HUMAN));
    }

    @Test
    void aHumanWithTensuraIsToldWhySupernaturalModeIsMissingButOnlyOnceStandingHasHadTimeToSync() {
        int old = ModeMemory.STANDING_SETTLE_TICKS;
        assertEquals(ModeMemory.Hint.LOCKED, ModeMemory.hint(true, true, false, old));
        assertEquals(ModeMemory.Hint.NOT_NOW, ModeMemory.hint(true, true, false, old - 1), "just logged in, respawned or changed dimension: it may simply not have arrived");
        assertEquals(ModeMemory.Hint.NOT_NOW, ModeMemory.hint(true, true, false, 0));
        assertEquals(ModeMemory.Hint.NONE, ModeMemory.hint(true, true, true, old), "a supernatural player has two modes");
        assertEquals(ModeMemory.Hint.NONE, ModeMemory.hint(false, true, false, old), "without Tensura there is no HUD to switch");
        assertEquals(ModeMemory.Hint.NONE, ModeMemory.hint(true, false, false, old), "without Vampirism there is no Supernatural mode to wait for");
    }
}
