package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.wheel.ClickPlan.Action;
import net.ofatech.supernaturalbloodlines.core.wheel.ClickPlan.Scroll;
import net.ofatech.supernaturalbloodlines.core.wheel.ClickPlan.ScrollTarget;
import org.junit.jupiter.api.Test;

/** The click and scroll table, the mode cycle, and what the narrator says (plan W4 tasks 3 and 6). */
class ClickAndNarrationTest {
    private static ScrollTarget target(boolean outer, boolean power, boolean levels, boolean modes, boolean pageable) {
        return new ScrollTarget(outer, power, levels, modes, pageable);
    }

    @Test
    void leftUses() {
        assertEquals(Action.USE, ClickPlan.of(0, false, false));
    }

    @Test
    void rightOpensTheSlotPicker() {
        assertEquals(Action.SLOT_PICKER, ClickPlan.of(1, false, false));
    }

    @Test
    void shiftLeftIsTheSecondOption() {
        assertEquals(Action.USE_SECOND_OPTION, ClickPlan.of(0, false, true));
    }

    @Test
    void altLeftIsNextMode() {
        assertEquals(Action.MODE_NEXT, ClickPlan.of(0, true, false));
        assertEquals(Action.MODE_NEXT, ClickPlan.of(0, true, true), "Alt wins over Shift");
    }

    @Test
    void altRightIsPreviousMode() {
        assertEquals(Action.MODE_PREVIOUS, ClickPlan.of(1, true, false));
    }

    @Test
    void otherButtonsDoNothing() {
        assertEquals(Action.NONE, ClickPlan.of(2, false, false));
        assertEquals(Action.NONE, ClickPlan.of(7, true, true));
    }

    @Test
    void altScrollMovesEitherWay() {
        ScrollTarget modes = target(true, true, false, true, true);
        assertEquals(Scroll.MODE_NEXT, ClickPlan.scroll(-1, true, modes));
        assertEquals(Scroll.MODE_PREVIOUS, ClickPlan.scroll(1, true, modes));
    }

    @Test
    void plainScrollChangesALevelOnlyWhenThePowerHasLevels() {
        ScrollTarget levels = target(true, true, true, false, false);
        assertEquals(Scroll.LEVEL_UP, ClickPlan.scroll(1, false, levels));
        assertEquals(Scroll.LEVEL_DOWN, ClickPlan.scroll(-2, false, levels));
        assertEquals(Scroll.NONE, ClickPlan.scroll(1, false, target(true, true, false, false, false)));
    }

    @Test
    void altWithoutModesDoesNothing() {
        assertEquals(Scroll.NONE, ClickPlan.scroll(1, true, target(true, true, false, false, true)), "Alt over a power without modes is not a page");
        assertEquals(Scroll.NONE, ClickPlan.scroll(1, true, target(true, false, false, true, true)), "Alt over an empty place has no mode to change");
    }

    @Test
    void levelBeatsPagingOverAHoveredVariablePower() {
        assertEquals(Scroll.LEVEL_DOWN, ClickPlan.scroll(-1, false, target(true, true, true, true, true)));
    }

    @Test
    void plainScrollOverEmptyOuterRingPagesBothWays() {
        ScrollTarget empty = target(true, false, false, false, true);
        assertEquals(Scroll.PAGE_NEXT, ClickPlan.scroll(-1, false, empty));
        assertEquals(Scroll.PAGE_PREVIOUS, ClickPlan.scroll(1, false, empty));
    }

    @Test
    void plainScrollOverAPowerWithoutLevelsPages() {
        assertEquals(Scroll.PAGE_NEXT, ClickPlan.scroll(-1, false, target(true, true, false, true, true)));
    }

    @Test
    void scrollOverInnerOrMiddleRingDoesNothing() {
        assertEquals(Scroll.NONE, ClickPlan.scroll(1, false, target(false, false, false, false, true)));
        assertEquals(Scroll.NONE, ClickPlan.scroll(-1, false, target(false, true, false, false, true)), "an inner or middle cell is no power of the outer ring");
    }

    @Test
    void scrollOnAOnePageGroupDoesNothing() {
        assertEquals(Scroll.NONE, ClickPlan.scroll(-1, false, target(true, false, false, false, false)));
        assertEquals(Scroll.NONE, ClickPlan.scroll(0, false, target(true, false, false, false, true)));
        assertEquals(Scroll.NONE, ClickPlan.scroll(Double.NaN, false, target(true, false, false, false, true)));
    }

    @Test
    void altNeverPages() {
        for (boolean power : new boolean[] {false, true}) {
            for (boolean modes : new boolean[] {false, true}) {
                Scroll s = ClickPlan.scroll(-1, true, target(true, power, true, modes, true));
                assertTrue(s != Scroll.PAGE_NEXT && s != Scroll.PAGE_PREVIOUS && s != Scroll.LEVEL_UP && s != Scroll.LEVEL_DOWN, power + " " + modes + " " + s);
            }
        }
    }

    @Test
    void lockedModesAreSkippedBothWays() {
        boolean[] open = {true, false, true, false};
        assertEquals(2, ModeCycle.next(0, open, false));
        assertEquals(0, ModeCycle.next(2, open, false));
        assertEquals(0, ModeCycle.next(2, open, true));
        assertEquals(2, ModeCycle.next(0, open, true));
    }

    @Test
    void allLockedReturnsMinusOne() {
        assertEquals(-1, ModeCycle.next(0, new boolean[] {false, false}, false));
        assertEquals(-1, ModeCycle.next(0, new boolean[0], false));
    }

    @Test
    void wrapsAround() {
        boolean[] all = {true, true, true};
        assertEquals(0, ModeCycle.next(2, all, false));
        assertEquals(2, ModeCycle.next(0, all, true));
        assertEquals(0, ModeCycle.next(-1, all, false), "from nowhere the first mode is next");
        assertEquals(0, ModeCycle.next(9, all, false));
        assertEquals(1, ModeCycle.next(1, new boolean[] {false, true, false}, false), "the only unlocked mode stays");
    }

    // ---- narration (task 6) ----

    private static String tr(String key, Object[] args) {
        return key.endsWith("page") ? "page " + args[0] + " of " + args[1] : args[0] + " of " + args[1];
    }

    @Test
    void narrationNamesCategoryPowerAndStateInOrder() {
        assertEquals(List.of("Tensura", "Skills", "page 2 of 4", "5 of 10", "Ultraspeed", "Predator", "on"),
                WheelNarration.lines("Tensura", "Skills", "Ultraspeed", "Predator", "on", "", 5, 10, 2, 4, ClickAndNarrationTest::tr));
    }

    @Test
    void whatAPowerAlsoCoversIsReadRightAfterItsName() {
        assertEquals(List.of("Tensura", "Skills", "1 of 3", "Sharp Sight", "Also covers: Vampirism Night Vision", "ready"),
                WheelNarration.lines("Tensura", "Skills", "Sharp Sight", "Also covers: Vampirism Night Vision", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr));
        assertEquals(List.of("Tensura", "Skills", "1 of 3", "Sharp Sight", "ready"), WheelNarration.lines("Tensura", "Skills", "Sharp Sight", "", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr));
    }

    @Test
    void theDescriptionIsReadAfterTheNameAndWhatItCovers() {
        assertEquals(List.of("Tensura", "Skills", "1 of 3", "Heat Wave", "Also covers: Vampirism Night Vision", "Scorching air.", "Devour", "ready"),
                WheelNarration.lines("Tensura", "Skills", "Heat Wave", "Also covers: Vampirism Night Vision", "Scorching air.", "Devour", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr));
        assertEquals(List.of("Tensura", "Skills", "1 of 3", "Heat Wave", "Scorching air.", "ready"),
                WheelNarration.lines("Tensura", "Skills", "Heat Wave", "", "Scorching air.", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr));
        assertEquals(List.of("Tensura", "Skills", "1 of 3", "Heat Wave", "ready"), WheelNarration.lines("Tensura", "Skills", "Heat Wave", "", "", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr));
    }

    @Test
    void stateIsWordsNotColour() {
        List<String> ready = WheelNarration.lines("Tensura", "Skills", "Coercion", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr);
        assertEquals("ready", ready.get(ready.size() - 1));
        List<String> cooling = WheelNarration.lines("Tensura", "Skills", "Coercion", "", "cooling down 12 seconds", "", 1, 3, 1, 1, ClickAndNarrationTest::tr);
        assertEquals("cooling down 12 seconds", cooling.get(cooling.size() - 1));
    }

    @Test
    void stateWordsNeedNoLevelAndRoundTheCooldownUp() {
        java.util.function.BiFunction<String, Object[], String> tr = (k, a) -> a.length == 0 ? k.substring(k.lastIndexOf(".") + 1) : k.substring(k.lastIndexOf(".") + 1) + " " + a[0];
        assertEquals("on", WheelNarration.stateWord(true, 200, tr), "on wins over a cooldown");
        assertEquals("cooling 1", WheelNarration.stateWord(false, 1, tr));
        assertEquals("cooling 1", WheelNarration.stateWord(false, 20, tr));
        assertEquals("cooling 2", WheelNarration.stateWord(false, 21, tr));
        assertEquals("ready", WheelNarration.stateWord(false, 0, tr));
        assertEquals("ready", WheelNarration.stateWord(false, -5, tr));
    }

    @Test
    void lockedReasonIsRead() {
        List<String> l = WheelNarration.lines("Tensura", "Skills", "Heat Wave", "", "ready", "Locked: Needs Fire Resistance level 6", 1, 3, 1, 1, ClickAndNarrationTest::tr);
        assertEquals("Locked: Needs Fire Resistance level 6", l.get(l.size() - 1));
        assertTrue(!l.contains("ready"));
    }

    @Test
    void modeIsReadWhenThereIsOne() {
        assertTrue(WheelNarration.lines("Tensura", "Skills", "Predator", "Devour", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr).contains("Devour"));
        assertEquals(5, WheelNarration.lines("Tensura", "Skills", "Coercion", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr).size(), "no mode line without a mode");
    }

    @Test
    void groupIsOmittedForAOneGroupMod() {
        List<String> l = WheelNarration.lines("Vampirism", "", "Rage", "", "ready", "", 1, 3, 1, 1, ClickAndNarrationTest::tr);
        assertEquals(List.of("Vampirism", "1 of 3", "Rage", "ready"), l);
    }

    @Test
    void pageIsReadOnlyWhenPaged() {
        assertTrue(WheelNarration.lines("Tensura", "Magic", "Fireball", "", "ready", "", 1, 27, 1, 3, ClickAndNarrationTest::tr).contains("page 1 of 3"));
        assertTrue(WheelNarration.lines("Tensura", "Magic", "Fireball", "", "ready", "", 1, 7, 1, 1, ClickAndNarrationTest::tr).stream().noneMatch(s -> s.startsWith("page")));
    }

    @Test
    void tabWalksModesThenGroupsThenWraps() {
        // 3 mods then 3 groups: 0 1 2 | 3 4 5 | back to 0
        int f = -1;
        int[] seen = new int[7];
        for (int i = 0; i < 7; i++) {
            f = WheelNarration.tabStep(f, 3, 3, false);
            seen[i] = f;
        }
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 0), java.util.Arrays.stream(seen).boxed().toList());
        assertEquals(5, WheelNarration.tabStep(0, 3, 3, true));
        assertEquals(5, WheelNarration.tabStep(-1, 3, 3, true));
        // no middle ring (one group): only the mods
        assertEquals(0, WheelNarration.tabStep(2, 3, 1, false));
        assertEquals(-1, WheelNarration.tabStep(0, 0, 0, false));
    }

    @Test
    void enterOnALockedEntryDoesNothingAndSaysWhy() {
        assertEquals(Optional.empty(), WheelNarration.enter(PowerState.READY));
        assertEquals(Optional.of(LockReason.NOT_UNLOCKED), WheelNarration.enter(PowerState.READY.withLock(LockReason.NOT_UNLOCKED)));
        assertEquals(Optional.of(LockReason.NEEDS_SLOT), WheelNarration.enter(PowerState.READY.withLock(LockReason.NEEDS_SLOT)));
    }
}
