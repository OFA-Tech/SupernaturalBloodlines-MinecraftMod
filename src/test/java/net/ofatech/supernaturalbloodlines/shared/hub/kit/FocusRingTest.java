package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import org.junit.jupiter.api.Test;

class FocusRingTest {
    @Test
    void theRingIsGoldOnADarkPaperAndRedOnALightOne() {
        assertEquals(FocusRing.GOLD, FocusRing.forPaper(0xFF14233A));
        assertEquals(FocusRing.DEEP_RED, FocusRing.forPaper(0xFFEEE0C0));
    }

    @Test
    void theRingReadsOnEveryPaperAtThreeToOne() {
        for (int paper : new int[] {0xFF0E0508, 0xFF14233A, 0xFF1C1C28, 0xFFEEE0C0, 0xFFF4EAD0, 0xFF2A1A12}) {
            assertTrue(Contrast.ratio(FocusRing.forPaper(paper), paper) >= FocusRing.MIN_RATIO, Integer.toHexString(paper));
        }
    }

    @Test
    void aChipLabelIsWhiteOrBlackWhicheverReadsBetter() {
        assertEquals(0xFFFFFFFF, Contrast.bestText(0xFF8A1C1C));
        assertEquals(0xFF000000, Contrast.bestText(0xFFE0A83A));
        for (int bg : new int[] {0xFFE0484A, 0xFFE0A83A, 0xFF4A8AD8, 0xFF8A6A4A, 0xFF7A3FB8, 0xFF2E8B57}) {
            assertTrue(Contrast.ratio(Contrast.bestText(bg), bg) >= 4.5, "chip text on " + Integer.toHexString(bg));
        }
    }
}
