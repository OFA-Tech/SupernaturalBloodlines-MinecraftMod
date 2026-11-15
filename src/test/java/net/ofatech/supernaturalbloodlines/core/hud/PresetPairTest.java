package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PresetPairTest {
    @Test
    void theRightColumnIsTheNextPreset() {
        assertEquals(0, PresetPair.left(0));
        assertEquals(1, PresetPair.right(0, 9));
        assertEquals(5, PresetPair.right(4, 9));
    }

    @Test
    void theLastPresetPairsWithTheFirst() {
        assertEquals(0, PresetPair.right(8, 9), "preset 9 pairs with preset 1");
    }

    @Test
    void everyPresetPairsWithItsSuccessorAndTheRingCloses() {
        for (int p = 0; p < PresetPair.TENSURA_PRESETS; p++) {
            assertEquals((p + 1) % 9, PresetPair.right(p, PresetPair.TENSURA_PRESETS));
        }
    }

    @Test
    void anActivePresetOutOfRangeIsWrappedNotCrashed() {
        assertEquals(1, PresetPair.right(9, 9));
        assertEquals(0, PresetPair.right(-1, 9));
        assertEquals(0, PresetPair.right(0, 1), "a single preset pairs with itself");
        assertEquals(0, PresetPair.right(3, 0), "no presets");
    }
}
