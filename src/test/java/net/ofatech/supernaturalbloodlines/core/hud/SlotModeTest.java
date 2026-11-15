package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlotModeTest {
    @Test
    void availableModesFollowTheInstalledMods() {
        assertEquals(List.of(), SlotMode.available(false, false));
        assertEquals(List.of(SlotMode.TENSURA), SlotMode.available(true, false));
        assertEquals(List.of(SlotMode.SUPERNATURAL), SlotMode.available(false, true));
        assertEquals(List.of(SlotMode.TENSURA, SlotMode.SUPERNATURAL), SlotMode.available(true, true));
    }

    @Test
    void toggleCyclesOnlyWhenThereIsAChoice() {
        List<SlotMode> both = SlotMode.available(true, true);
        assertEquals(SlotMode.SUPERNATURAL, SlotMode.next(SlotMode.TENSURA, both).orElseThrow());
        assertEquals(SlotMode.TENSURA, SlotMode.next(SlotMode.SUPERNATURAL, both).orElseThrow());
        assertEquals(SlotMode.TENSURA, SlotMode.next(SlotMode.TENSURA, SlotMode.available(true, false)).orElseThrow(), "one mode: stays");
        assertEquals(Optional.empty(), SlotMode.next(SlotMode.TENSURA, List.of()), "none: no HUD");
    }

    @Test
    void aModeThatIsNotInstalledFallsBackToTheFirstAvailable() {
        assertEquals(SlotMode.SUPERNATURAL, SlotMode.next(SlotMode.TENSURA, SlotMode.available(false, true)).orElseThrow());
    }

    @Test
    void theConfigChoosesTheStartMode() {
        List<SlotMode> both = SlotMode.available(true, true);
        assertEquals(SlotMode.TENSURA, SlotMode.start("AUTO", both).orElseThrow(), "auto: Tensura when it is installed");
        assertEquals(SlotMode.SUPERNATURAL, SlotMode.start("AUTO", SlotMode.available(false, true)).orElseThrow());
        assertEquals(SlotMode.SUPERNATURAL, SlotMode.start("SUPERNATURAL", both).orElseThrow());
        assertEquals(SlotMode.TENSURA, SlotMode.start("SUPERNATURAL", SlotMode.available(true, false)).orElseThrow(), "not installed: what is");
        assertTrue(SlotMode.start("TENSURA", List.of()).isEmpty());
        assertEquals(SlotMode.TENSURA, SlotMode.start("nonsense", both).orElseThrow());
    }
}
