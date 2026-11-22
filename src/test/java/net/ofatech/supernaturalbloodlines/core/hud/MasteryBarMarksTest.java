package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge.Look;
import org.junit.jupiter.api.Test;

/** The state of a mastery bar is in its shape too, not only in its colour. */
class MasteryBarMarksTest {
    private static final Box SLOT = new Box(100, 100, HudLayout.SLOT, HudLayout.SLOT);

    @Test
    void aLearningBarIsHollowAndTheOthersAreSolid() {
        int lit = 0;
        for (int c = 0; c < 16; c++) {
            assertTrue(MasteryBar.lit(Mastery.State.PROGRESS, c));
            assertTrue(MasteryBar.lit(Mastery.State.MASTERED, c));
            if (MasteryBar.lit(Mastery.State.LEARNING, c)) {
                lit++;
            }
        }
        assertEquals(8, lit, "every other column of a learning bar");
        assertFalse(MasteryBar.lit(Mastery.State.LEARNING, 1));
    }

    @Test
    void theMasteredDiamondSitsOnTheBarsEndAndClearsEveryBadge() {
        Box d = MasteryBar.diamond(SLOT);
        assertEquals(5, d.w());
        assertEquals(5, d.h());
        Box bar = MasteryBar.box(SLOT);
        assertTrue(d.overlaps(bar), "it is on the bar");
        assertTrue(d.x() >= SLOT.x() && d.right() <= SLOT.right() && d.bottom() <= SLOT.bottom(), "and inside the slot");
        for (Look look : Look.values()) {
            for (int label : new int[] {SlotBadge.CHAR_W, 2 * SlotBadge.CHAR_W}) {
                assertFalse(d.overlaps(SlotBadge.key(look, SLOT, label, MasteryBar.LIFT)), look + " badge " + label + " on the diamond");
            }
        }
    }
}
