package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class GroupClockTest {
    private static final String G = "rage";

    @Test
    void instantStartsCooldownAtOnce() {
        GroupClock c = GroupClock.idle().onEvent(G, 0, SideEvent.FIRED, false, 200, 1000);
        assertEquals(200, c.cooldownLeft(G, 1000));
        assertEquals(150, c.cooldownLeft(G, 1050));
        assertEquals(0, c.cooldownLeft(G, 1200));
        assertTrue(c.activeSide(G).isEmpty());
    }

    @Test
    void lastingStartsNothingOnOn() {
        GroupClock c = GroupClock.idle().onEvent(G, 1, SideEvent.ON, true, 600, 1000);
        assertEquals(0, c.cooldownLeft(G, 1000), "no cooldown while the side is on");
        assertEquals(1, c.activeSide(G).getAsInt());
    }

    @Test
    void lastingStartsCooldownAtOff() {
        GroupClock c = GroupClock.idle().onEvent(G, 1, SideEvent.ON, true, 600, 1000).onEvent(G, 1, SideEvent.OFF, true, 600, 1900);
        assertEquals(600, c.cooldownLeft(G, 1900));
        assertTrue(c.activeSide(G).isEmpty());
    }

    @Test
    void earlyOffStartsCooldownAtThatTick() {
        GroupClock c = GroupClock.idle().onEvent(G, 0, SideEvent.ON, true, 600, 1000).onEvent(G, 0, SideEvent.OFF, true, 600, 1030);
        assertEquals(1630, c.cooldownEnd(G));
    }

    @Test
    void offOfAnInactiveSideIsIgnored() {
        GroupClock on = GroupClock.idle().onEvent(G, 0, SideEvent.ON, true, 600, 1000);
        GroupClock c = on.onEvent(G, 1, SideEvent.OFF, true, 600, 1100);
        assertEquals(on, c);
        assertEquals(0, c.cooldownLeft(G, 1100));
        assertEquals(0, c.activeSide(G).getAsInt(), "the side that is on stays on");
        // a double off starts the cooldown once
        GroupClock off = on.onEvent(G, 0, SideEvent.OFF, true, 600, 1100);
        assertEquals(off, off.onEvent(G, 0, SideEvent.OFF, true, 600, 1200));
    }

    @Test
    void onWhileAnotherSideIsOnIsRefusedGroupActive() {
        GroupClock c = GroupClock.idle().onEvent(G, 0, SideEvent.ON, true, 600, 1000);
        assertEquals(net.ofatech.supernaturalbloodlines.core.wheel.LockReason.GROUP_ACTIVE, StackRules.mayStart(c, G, 1, 1010).orElseThrow());
        assertTrue(StackRules.mayStart(c, G, 0, 1010).isEmpty(), "the side that is on may be turned off");
    }

    @Test
    void fireDuringCooldownIsRefusedGroupCooldown() {
        GroupClock c = GroupClock.idle().onEvent(G, 0, SideEvent.FIRED, false, 200, 1000);
        assertEquals(net.ofatech.supernaturalbloodlines.core.wheel.LockReason.GROUP_COOLDOWN, StackRules.mayStart(c, G, 1, 1100).orElseThrow());
        assertEquals(net.ofatech.supernaturalbloodlines.core.wheel.LockReason.GROUP_COOLDOWN, StackRules.mayStart(c, G, 0, 1100).orElseThrow(), "every side, the main too");
        assertTrue(StackRules.mayStart(c, G, 1, 1200).isEmpty());
    }

    @Test
    void endOfTheNonMainSideStartsTheMainSidesCooldown() {
        // the main side's cooldown is 600 and the other side's is 200: the caller passes the main's, whichever side ended
        GroupClock c = GroupClock.idle().onEvent(G, 1, SideEvent.ON, true, 600, 1000).onEvent(G, 1, SideEvent.OFF, true, 600, 1100);
        assertEquals(600, c.cooldownLeft(G, 1100));
    }

    @Test
    void mainWithNoCooldownMeansNoGroupCooldown() {
        GroupClock c = GroupClock.idle().onEvent(G, 1, SideEvent.ON, true, 0, 1000).onEvent(G, 1, SideEvent.OFF, true, 0, 1100);
        assertEquals(0, c.cooldownLeft(G, 1100));
        assertTrue(c.isIdle(), "nothing to remember");
        assertTrue(GroupClock.idle().onEvent(G, 0, SideEvent.FIRED, false, 0, 1000).isIdle());
    }

    @Test
    void cooldownSurvivesACodecRoundTrip() {
        GroupClock c = GroupClock.idle().onEvent(G, 1, SideEvent.ON, true, 600, 1000).onEvent("bite", 0, SideEvent.FIRED, false, 300, 1000);
        var json = GroupClock.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow();
        GroupClock back = GroupClock.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(c, back);
        assertEquals(300, back.cooldownLeft("bite", 1000));
        assertEquals(1, back.activeSide(G).getAsInt());
    }

    @Test
    void relogMidLastingThenOffStartsExactlyOneCooldown() {
        GroupClock on = GroupClock.idle().onEvent(G, 0, SideEvent.ON, true, 600, 1000);
        var json = GroupClock.CODEC.encodeStart(JsonOps.INSTANCE, on).getOrThrow();
        GroupClock back = GroupClock.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        GroupClock off = back.onEvent(G, 0, SideEvent.OFF, true, 600, 5000);
        assertEquals(600, off.cooldownLeft(G, 5000));
        assertEquals(off, off.onEvent(G, 0, SideEvent.OFF, true, 600, 5100), "a second off changes nothing");
    }

    @Test
    void twoGroupsDoNotShareAClock() {
        GroupClock c = GroupClock.idle().onEvent("a", 0, SideEvent.ON, true, 100, 1000).onEvent("b", 1, SideEvent.FIRED, false, 50, 1000);
        assertEquals(0, c.activeSide("a").getAsInt());
        assertTrue(c.activeSide("b").isEmpty());
        assertEquals(0, c.cooldownLeft("a", 1000));
        assertEquals(50, c.cooldownLeft("b", 1000));
        assertFalse(StackRules.mayStart(c, "b", 0, 1000).isEmpty());
        assertTrue(StackRules.mayStart(c, "a", 0, 1000).isEmpty());
    }
}
