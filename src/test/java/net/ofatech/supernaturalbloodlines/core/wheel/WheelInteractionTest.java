package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.wheel.HoldPlan.Trigger;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Hit;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Ring;
import org.junit.jupiter.api.Test;

class WheelInteractionTest {
    @Test
    void aHeldPowerStartedByGOrTheTapIsReleasedAtOnce() {
        assertTrue(HoldPlan.releaseAtOnce(PowerKind.HELD, Trigger.G_RELEASE));
        assertTrue(HoldPlan.releaseAtOnce(PowerKind.HELD, Trigger.TAP_REPEAT));
    }

    @Test
    void aHeldPowerStartedByTheMouseOrEnterStaysDownUntilThatInputGoesUp() {
        assertFalse(HoldPlan.releaseAtOnce(PowerKind.HELD, Trigger.MOUSE_CLICK));
        assertFalse(HoldPlan.releaseAtOnce(PowerKind.HELD, Trigger.KEY_PRESS));
        assertTrue(HoldPlan.heldUntilInputUp(PowerKind.HELD, Trigger.MOUSE_CLICK));
        assertTrue(HoldPlan.heldUntilInputUp(PowerKind.HELD, Trigger.KEY_PRESS));
        assertFalse(HoldPlan.heldUntilInputUp(PowerKind.HELD, Trigger.G_RELEASE));
    }

    @Test
    void aPowerThatIsNotHeldIsNeverHeld() {
        for (PowerKind k : new PowerKind[] {PowerKind.INSTANT, PowerKind.TOGGLE, PowerKind.AIMED}) {
            for (Trigger t : Trigger.values()) {
                assertFalse(HoldPlan.releaseAtOnce(k, t), k + " " + t);
                assertFalse(HoldPlan.heldUntilInputUp(k, t), k + " " + t);
            }
        }
    }

    @Test
    void aKeyMovesTheFocusAndARestingMouseDoesNotTakeItBack() {
        WheelCursor c = new WheelCursor();
        Hit under = new Hit(Ring.OUTER, 2);
        Hit focused = new Hit(Ring.OUTER, 5);
        c.mouseMoved(under);
        assertEquals(under, c.active().orElseThrow());
        c.keyFocus(focused);
        assertEquals(focused, c.active().orElseThrow(), "the key won");
        assertTrue(c.hover().isEmpty());
        // nothing moved the mouse: asking again changes nothing
        assertEquals(focused, c.active().orElseThrow());
        c.mouseMoved(under);
        assertEquals(under, c.active().orElseThrow(), "the mouse moved: it decides again");
        c.mouseMoved(null);
        assertEquals(focused, c.active().orElseThrow(), "off every cell the focus is what is left");
        c.clearHover();
        assertEquals(focused, c.active().orElseThrow());
        assertTrue(new WheelCursor().active().isEmpty());
    }
}
