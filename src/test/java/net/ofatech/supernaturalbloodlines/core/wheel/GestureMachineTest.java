package net.ofatech.supernaturalbloodlines.core.wheel;

import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect.CLOSE;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect.NONE;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect.OPEN;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect.REPEAT_LAST;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect.USE_HOVERED;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.CLICK_LEFT;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.CLICK_RIGHT;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.ESC;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.FOCUS_LOST;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.G_DOWN;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.G_UP;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.SCREEN_OPENED;
import static net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input.TICK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Step;
import org.junit.jupiter.api.Test;

class GestureMachineTest {
    private static final int TAP = 200;

    private static GestureMachine down(long at) {
        return GestureMachine.IDLE.step(G_DOWN, at, TAP).machine();
    }

    /** G down at 1000, polled until the wheel opens at 1200. */
    private static GestureMachine heldOpen() {
        Step s = down(1000).step(TICK, 1200, TAP);
        assertEquals(OPEN, s.effect());
        return s.machine();
    }

    @Test
    void tapUnder200RepeatsLastAndOpensNothing() {
        assertEquals(NONE, GestureMachine.IDLE.step(G_DOWN, 1000, TAP).effect(), "the press itself opens nothing");
        GestureMachine m = down(1000);
        assertEquals(NONE, m.step(TICK, 1100, TAP).effect(), "no open before 200 ms");
        Step up = m.step(G_UP, 1199, TAP);
        assertEquals(REPEAT_LAST, up.effect());
        assertFalse(up.machine().open());
        assertFalse(up.machine().pending());
    }

    @Test
    void heldPast200OpensWhileHeld() {
        GestureMachine m = heldOpen();
        assertTrue(m.open());
        assertEquals(NONE, m.step(TICK, 1500, TAP).effect(), "opens once");
    }

    @Test
    void exactly200IsAHold() {
        assertEquals(OPEN, down(1000).step(TICK, 1200, TAP).effect());
        Step up = down(1000).step(G_UP, 1200, TAP);
        assertEquals(OPEN, up.effect(), "released at exactly 200 ms with no poll in between: still a hold, it opens and stays");
        assertTrue(up.machine().open());
        assertEquals(REPEAT_LAST, down(1000).step(G_UP, 1199, TAP).effect());
    }

    @Test
    void releaseOverAPowerUsesIt() {
        Step up = heldOpen().step(G_UP, 1600, TAP, true);
        assertEquals(USE_HOVERED, up.effect());
        assertFalse(up.machine().open());
        assertTrue(up.machine().sticky().open() == false, "sticky() on a closed machine stays closed");
    }

    @Test
    void releaseOverNothingStaysOpen() {
        Step up = heldOpen().step(G_UP, 1600, TAP, false);
        assertEquals(NONE, up.effect());
        assertTrue(up.machine().open());
    }

    @Test
    void aUseThatKeepsTheWheelOpenLeavesItOpen() {
        GestureMachine used = heldOpen().step(G_UP, 1600, TAP, true).machine();
        // the screen decided a toggle stays open: it asks for the wheel to be left open
        GestureMachine reopened = heldOpen().sticky();
        assertTrue(reopened.open());
        assertFalse(used.open());
        assertEquals(CLOSE, reopened.step(G_DOWN, 2000, TAP).effect());
    }

    @Test
    void gAgainClosesAStickyWheel() {
        GestureMachine sticky = heldOpen().step(G_UP, 1600, TAP, false).machine();
        Step again = sticky.step(G_DOWN, 2000, TAP);
        assertEquals(CLOSE, again.effect());
        assertFalse(again.machine().open());
        // and the release of that same press does nothing
        assertEquals(NONE, again.machine().step(G_UP, 2050, TAP).effect());
        assertFalse(again.machine().step(G_UP, 2050, TAP).machine().open());
    }

    @Test
    void aClickOnAStickyWheelUsesOrCloses() {
        GestureMachine sticky = heldOpen().step(G_UP, 1600, TAP, false).machine();
        Step on = sticky.step(CLICK_LEFT, 2000, TAP, true);
        assertEquals(USE_HOVERED, on.effect());
        assertTrue(on.machine().open());
        Step off = sticky.step(CLICK_LEFT, 2000, TAP, false);
        assertEquals(CLOSE, off.effect());
        assertFalse(off.machine().open());
        assertEquals(NONE, sticky.step(CLICK_RIGHT, 2000, TAP, true).effect(), "the right click is the screen's (the slot picker)");
    }

    @Test
    void aClickWhileHeldUsesAndKeepsHolding() {
        Step click = heldOpen().step(CLICK_LEFT, 1300, TAP, true);
        assertEquals(USE_HOVERED, click.effect());
        assertTrue(click.machine().open());
        assertEquals(NONE, heldOpen().step(CLICK_LEFT, 1300, TAP, false).effect());
    }

    @Test
    void escClosesInEveryState() {
        for (GestureMachine m : new GestureMachine[] {GestureMachine.IDLE, down(1000), heldOpen(), heldOpen().step(G_UP, 1600, TAP).machine()}) {
            Step esc = m.step(ESC, 2000, TAP);
            assertEquals(m.open() ? CLOSE : NONE, esc.effect());
            assertFalse(esc.machine().open());
            assertFalse(esc.machine().pending());
        }
    }

    @Test
    void screenOpenedWhileHeldReleasesWithoutAnEffect() {
        Step s = down(1000).step(SCREEN_OPENED, 1050, TAP);
        assertEquals(NONE, s.effect());
        assertFalse(s.machine().pending());
        assertEquals(NONE, s.machine().step(G_UP, 1100, TAP).effect(), "the key that was held over the other screen does not repeat the last power");
    }

    @Test
    void focusLostWhileHeldCloses() {
        Step open = heldOpen().step(FOCUS_LOST, 1500, TAP);
        assertEquals(CLOSE, open.effect());
        assertFalse(open.machine().open());
        Step pending = down(1000).step(FOCUS_LOST, 1050, TAP);
        assertEquals(NONE, pending.effect());
        assertFalse(pending.machine().pending());
    }

    @Test
    void gUpWithoutGDownIsIgnored() {
        Step s = GestureMachine.IDLE.step(G_UP, 1000, TAP);
        assertEquals(NONE, s.effect());
        assertFalse(s.machine().open());
        for (Input in : Input.values()) {
            if (in != G_DOWN) {
                assertEquals(NONE, GestureMachine.IDLE.step(in, 5, TAP).effect(), in.name());
            }
        }
    }

    @Test
    void tapMsFromSettingIsUsed() {
        for (int tap : new int[] {100, 500}) {
            GestureMachine m = GestureMachine.IDLE.step(G_DOWN, 1000, tap).machine();
            assertEquals(NONE, m.step(TICK, 1000 + tap - 1, tap).effect(), "tap " + tap);
            assertEquals(OPEN, m.step(TICK, 1000 + tap, tap).effect(), "tap " + tap);
            assertEquals(REPEAT_LAST, m.step(G_UP, 1000 + tap - 1, tap).effect(), "tap " + tap);
        }
    }

    @Test
    void aSecondGDownWhileHeldIsKeyRepeatAndIgnored() {
        assertEquals(NONE, heldOpen().step(G_DOWN, 1300, TAP).effect());
        assertTrue(heldOpen().step(G_DOWN, 1300, TAP).machine().open());
        assertEquals(NONE, down(1000).step(G_DOWN, 1100, TAP).effect());
        assertEquals(OPEN, down(1000).step(G_DOWN, 1100, TAP).machine().step(TICK, 1200, TAP).effect(), "the first press's time stands");
    }
}
