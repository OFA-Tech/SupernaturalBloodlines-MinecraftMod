package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.CooldownClock;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.StackRules.Intent;
import org.junit.jupiter.api.Test;

class StackRulesTest {
    private static final String G = "flight";
    private static final String A = "tensura/tensura:gravity_flight";
    private static final String B = "tensura/ascension:angel_wings";

    @Test
    void anythingPendingOnAnotherSideKeepsThisOneOut() {
        GroupClock idle = GroupClock.idle();
        assertEquals(LockReason.GROUP_ACTIVE, StackRules.mayStart(idle, G, B, 100, List.of(new Intent(A, SideEvent.ON))).orElseThrow());
        assertEquals(LockReason.GROUP_COOLDOWN, StackRules.mayStart(idle, G, B, 100, List.of(new Intent(A, SideEvent.FIRED))).orElseThrow(),
                "an instant side fired in the same tick: the cooldown it starts is not in the clock yet");
        assertEquals(LockReason.GROUP_ACTIVE, StackRules.mayStart(idle, G, B, 100, List.of(new Intent(A, SideEvent.OFF))).orElseThrow());
    }

    @Test
    void whatIsPendingOnTheSameSideIsNotAConflict() {
        for (SideEvent e : SideEvent.values()) {
            assertTrue(StackRules.mayStart(GroupClock.idle(), G, A, 100, List.of(new Intent(A, e))).isEmpty(), e.name());
        }
        assertTrue(StackRules.mayStart(GroupClock.idle(), G, A, 100, List.of()).isEmpty());
    }

    @Test
    void theClockSpeaksBeforeWhatIsPending() {
        GroupClock on = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 100, 50);
        assertEquals(LockReason.GROUP_ACTIVE, StackRules.mayStart(on, G, A, 100, List.of(new Intent(B, SideEvent.FIRED))).orElseThrow());
        GroupClock cooling = GroupClock.idle().onEvent(G, B, SideEvent.FIRED, false, 100, 50);
        assertEquals(LockReason.GROUP_COOLDOWN, StackRules.mayStart(cooling, G, A, 100, List.of(new Intent(B, SideEvent.ON))).orElseThrow());
    }

    @Test
    void theOneOfTheFourArgumentFormIgnoresNothingItShouldNot() {
        GroupClock on = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 100, 50);
        assertEquals(LockReason.GROUP_ACTIVE, StackRules.mayStart(on, G, A, 100).orElseThrow());
        assertTrue(StackRules.mayStart(GroupClock.idle(), G, A, 100).isEmpty());
    }

    @Test
    void overlayCarriesTheSharedCooldownWhenItEndsLater() {
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.FIRED, false, 600, 1000);
        PowerState own = PowerState.READY;
        PowerState shown = StackRules.overlay(own, c, G, A, 1100);
        assertEquals(1600L, shown.cooldownEnd());
        assertEquals(600, shown.cooldownTotal(), "the sweep runs against the length the cooldown started with, not what is left");
        // the power's own cooldown that ends later stays
        PowerState longer = own.withCooldown(new CooldownClock.Reading(5000L, 4000));
        assertEquals(5000L, StackRules.overlay(longer, c, G, A, 1100).cooldownEnd());
        // once it is over nothing is carried
        assertEquals(own.cooldownEnd(), StackRules.overlay(own, c, G, A, 1600).cooldownEnd());
        assertEquals(own.cooldownEnd(), StackRules.overlay(own, GroupClock.idle(), G, A, 1100).cooldownEnd());
    }

    @Test
    void overlayLocksTheOtherSidesOnlyWhileASideIsOn() {
        GroupClock on = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 600, 1000);
        assertEquals(LockReason.GROUP_ACTIVE, StackRules.overlay(PowerState.READY, on, G, A, 1100).lock());
        assertEquals(LockReason.NONE, StackRules.overlay(PowerState.READY, on, G, B, 1100).lock(), "the side that is on can be turned off");
        // a cooldown shows as the sweep, not as a lock
        GroupClock cooling = GroupClock.idle().onEvent(G, B, SideEvent.FIRED, false, 600, 1000);
        assertEquals(LockReason.NONE, StackRules.overlay(PowerState.READY, cooling, G, A, 1100).lock());
        // a lock that is already there stays the one that was there
        PowerState locked = PowerState.READY.withLock(LockReason.REFUSED_NOW);
        assertEquals(LockReason.REFUSED_NOW, StackRules.overlay(locked, on, G, A, 1100).lock());
        assertEquals(LockReason.NONE, StackRules.overlay(PowerState.READY, GroupClock.idle(), G, A, 1100).lock());
    }

    @Test
    void theCheckIsDueEveryTenTicksOfThePlayer() {
        assertTrue(StackRules.reconcileDue(0));
        assertTrue(StackRules.reconcileDue(10));
        assertTrue(StackRules.reconcileDue(200));
        for (int t : new int[] {1, 5, 9, 11, 19, 21}) {
            assertFalse(StackRules.reconcileDue(t), "tick " + t);
        }
    }
}
