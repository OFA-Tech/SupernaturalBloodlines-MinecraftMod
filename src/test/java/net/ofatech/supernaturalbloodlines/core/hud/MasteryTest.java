package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge.Look;
import org.junit.jupiter.api.Test;

class MasteryTest {
    @Test
    void aSkillBetweenNothingAndMaxShowsItsShare() {
        Mastery m = Mastery.of(25, 100, 0, false);
        assertEquals(Mastery.State.PROGRESS, m.state());
        assertEquals(0.25f, m.fraction(), 1e-6);
        assertFalse(m.mastered());
        assertEquals(0.5f, Mastery.of(150, 300, 0, false).fraction(), 1e-6);
    }

    @Test
    void aMasteredSkillIsFullAndMastered() {
        for (Mastery m : new Mastery[] {Mastery.of(100, 100, 0, true), Mastery.of(100, 100, 0, false), Mastery.of(250, 100, 0, false), Mastery.of(99, 100, 0, true)}) {
            assertTrue(m.mastered());
            assertEquals(1f, m.fraction());
        }
        assertFalse(Mastery.of(99.9, 100, 0, false).mastered());
    }

    @Test
    void aResistSkillBeingLearnedCountsUpFromItsRequirementToZero() {
        assertEquals(Mastery.State.LEARNING, Mastery.of(-100, 100, 100, false).state());
        assertEquals(0f, Mastery.of(-100, 100, 100, false).fraction(), 1e-6);
        assertEquals(0.75f, Mastery.of(-25, 100, 100, false).fraction(), 1e-6);
        assertEquals(0.5f, Mastery.of(-500, 1000, 1000, false).fraction(), 1e-6);
        assertEquals(0f, Mastery.of(-5000, 100, 100, false).fraction());
        // a skill that is not a resist skill uses Tensura's own 100
        assertEquals(0.9f, Mastery.of(-10, 100, 0, false).fraction(), 1e-6);
        // being learned beats the mastered flag: a negative number is never mastery
        assertEquals(Mastery.State.LEARNING, Mastery.of(-1, 100, 100, true).state());
    }

    @Test
    void aResistSkillWhoseLearningIsOverIsLearnedAndAPlainSkillIsNot() {
        assertTrue(Mastery.of(10, 100, 100, false).learned());
        assertTrue(Mastery.of(100, 100, 100, true).learned());
        assertFalse(Mastery.of(10, 100, 0, false).learned());
        assertFalse(Mastery.of(-10, 100, 100, false).learned(), "still being learned");
    }

    @Test
    void aSkillWithNoMaximumHasNoBar() {
        assertNull(Mastery.of(10, 0, 0, false));
        assertNull(Mastery.of(10, -1, 0, false));
        assertNull(Mastery.of(Double.NaN, 100, 0, false));
    }

    @Test
    void aSlotWithoutMasteryHasNone() {
        SlotView plain = new SlotView(null, "R", 0f, 0, false, null, "x", null);
        assertNull(plain.mastery());
        assertNull(SlotView.EMPTY.mastery());
        Mastery m = Mastery.of(10, 100, 0, false);
        assertEquals(m, plain.withMastery(m).withKey("F").mastery());
    }

    @Test
    void theBarIsOnTheBottomEdgeInsideTheSlotAtLeastTwoPixelsTallAndNothingCoversIt() {
        Box slot = new Box(100, 100, HudLayout.SLOT, HudLayout.SLOT);
        Box bar = MasteryBar.box(slot);
        Box in = MasteryBar.inner(slot);
        assertTrue(bar.x() > slot.x() && bar.right() < slot.right() && bar.y() > slot.y() && bar.bottom() == slot.bottom() - 1, "inside the frame, on the bottom edge: " + bar);
        assertTrue(in.h() >= 2, "the fill is at least 2 px tall");
        assertTrue(in.x() > bar.x() && in.right() < bar.right() && in.y() > bar.y() && in.bottom() < bar.bottom(), "a dark track all round the fill");
        assertTrue(in.w() >= 14);
        for (Look look : Look.values()) {
            for (int labelW : new int[] {5, 6, 11}) {
                assertFalse(bar.overlaps(SlotBadge.key(look, slot, labelW, MasteryBar.LIFT)), look + " covers the key badge");
                assertFalse(bar.overlaps(SlotBadge.tensuraMark(slot)), look + " covers the mode diamond");
                // the cooldown's text box starts with a row the font leaves empty, so the digits themselves begin one row down
                Box sec = SlotBadge.seconds(look, slot, 17, MasteryBar.LIFT);
                assertFalse(bar.overlaps(new Box(sec.x(), sec.y() + 1, sec.w(), sec.h() - 1)), look + " covers the seconds");
            }
        }
    }

    @Test
    void withoutABarTheBadgeAndTheSecondsStandWhereTheyAlwaysDid() {
        Box slot = new Box(0, 0, HudLayout.SLOT, HudLayout.SLOT);
        for (Look look : Look.values()) {
            assertEquals(SlotBadge.key(look, slot, 6), SlotBadge.key(look, slot, 6, 0));
            assertEquals(SlotBadge.seconds(look, slot, 11), SlotBadge.seconds(look, slot, 11, 0));
        }
        // the bar's lift moves only what stands at the bottom
        assertEquals(SlotBadge.key(Look.TENSURA, slot, 6), SlotBadge.key(Look.TENSURA, slot, 6, MasteryBar.LIFT));
        assertEquals(SlotBadge.seconds(Look.GRIMOIRE, slot, 11), SlotBadge.seconds(Look.GRIMOIRE, slot, 11, MasteryBar.LIFT));
        assertEquals(SlotBadge.key(Look.BLOOD_MOON, slot, 6).y() - MasteryBar.LIFT, SlotBadge.key(Look.BLOOD_MOON, slot, 6, MasteryBar.LIFT).y());
    }
    @Test
    void theFillIsAWholeNumberOfPixelsAndOnlyFullWhenMastered() {
        int w = 12;
        assertEquals(0, MasteryBar.fill(Mastery.of(0, 100, 0, false), w));
        assertEquals(1, MasteryBar.fill(Mastery.of(1, 100, 0, false), w), "any progress shows a pixel");
        assertEquals(6, MasteryBar.fill(Mastery.of(50, 100, 0, false), w));
        assertEquals(w - 1, MasteryBar.fill(Mastery.of(99.9, 100, 0, false), w), "nearly there is not full");
        assertEquals(w, MasteryBar.fill(Mastery.of(100, 100, 0, true), w));
        assertEquals(3, MasteryBar.fill(Mastery.of(-75, 100, 100, false), w));
    }

    @Test
    void eachThemeHasItsOwnColourAndMasteredIsGoldInAll() {
        Set<Integer> progress = new HashSet<>();
        for (Look look : Look.values()) {
            progress.add(MasteryBar.color(look, Mastery.State.PROGRESS));
            assertEquals(MasteryBar.GOLD, MasteryBar.color(look, Mastery.State.MASTERED));
            assertEquals(MasteryBar.LEARNING, MasteryBar.color(look, Mastery.State.LEARNING));
            assertNotEquals(MasteryBar.GOLD, MasteryBar.color(look, Mastery.State.PROGRESS));
            assertNotEquals(MasteryBar.LEARNING, MasteryBar.color(look, Mastery.State.PROGRESS));
            assertNotNull(look);
        }
        assertEquals(3, progress.size());
    }
}
