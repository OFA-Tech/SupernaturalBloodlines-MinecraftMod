package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import org.junit.jupiter.api.Test;

class GroupClockTest {
    private static final String G = "rage";
    private static final String A = "vampirism/vampirism:vampire_rage";
    private static final String B = "werewolves/werewolves:rage";

    @Test
    void instantStartsCooldownAtOnce() {
        GroupClock c = GroupClock.idle().onEvent(G, A, SideEvent.FIRED, false, 200, 1000);
        assertEquals(200, c.cooldownLeft(G, 1000));
        assertEquals(150, c.cooldownLeft(G, 1050));
        assertEquals(0, c.cooldownLeft(G, 1200));
        assertTrue(c.activeSide(G).isEmpty());
    }

    @Test
    void lastingStartsNothingOnOn() {
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 600, 1000);
        assertEquals(0, c.cooldownLeft(G, 1000), "no cooldown while the side is on");
        assertEquals(B, c.activeSide(G).orElseThrow());
    }

    @Test
    void lastingStartsCooldownAtOff() {
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 600, 1000).onEvent(G, B, SideEvent.OFF, true, 600, 1900);
        assertEquals(600, c.cooldownLeft(G, 1900));
        assertTrue(c.activeSide(G).isEmpty());
    }

    @Test
    void earlyOffStartsCooldownAtThatTick() {
        GroupClock c = GroupClock.idle().onEvent(G, A, SideEvent.ON, true, 600, 1000).onEvent(G, A, SideEvent.OFF, true, 600, 1030);
        assertEquals(1630, c.cooldownEnd(G));
    }

    @Test
    void offOfAnInactiveSideIsIgnored() {
        GroupClock on = GroupClock.idle().onEvent(G, A, SideEvent.ON, true, 600, 1000);
        GroupClock c = on.onEvent(G, B, SideEvent.OFF, true, 600, 1100);
        assertEquals(on, c);
        assertEquals(0, c.cooldownLeft(G, 1100));
        assertEquals(A, c.activeSide(G).orElseThrow(), "the side that is on stays on");
        // a double off starts the cooldown once
        GroupClock off = on.onEvent(G, A, SideEvent.OFF, true, 600, 1100);
        assertEquals(off, off.onEvent(G, A, SideEvent.OFF, true, 600, 1200));
    }

    @Test
    void onWhileAnotherSideIsOnIsRefusedGroupActive() {
        GroupClock c = GroupClock.idle().onEvent(G, A, SideEvent.ON, true, 600, 1000);
        assertEquals(LockReason.GROUP_ACTIVE, StackRules.mayStart(c, G, B, 1010).orElseThrow());
        assertTrue(StackRules.mayStart(c, G, A, 1010).isEmpty(), "the side that is on may be turned off");
    }

    @Test
    void fireDuringCooldownIsRefusedGroupCooldown() {
        GroupClock c = GroupClock.idle().onEvent(G, A, SideEvent.FIRED, false, 200, 1000);
        assertEquals(LockReason.GROUP_COOLDOWN, StackRules.mayStart(c, G, B, 1100).orElseThrow());
        assertEquals(LockReason.GROUP_COOLDOWN, StackRules.mayStart(c, G, A, 1100).orElseThrow(), "every side, the main too");
        assertTrue(StackRules.mayStart(c, G, B, 1200).isEmpty());
    }

    @Test
    void theCooldownLengthIsTheOneGivenAtTheEndNotAtTheStart() {
        // the caller passes the main side's cooldown at each event: the end of a lasting side counts the one of that moment, whichever side ended
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 100, 1000).onEvent(G, B, SideEvent.OFF, true, 600, 1100);
        assertEquals(600, c.cooldownLeft(G, 1100));
        assertEquals(1700, c.cooldownEnd(G));
        assertEquals(600, c.cooldownTotal(G));
    }

    @Test
    void mainWithNoCooldownMeansNoGroupCooldown() {
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 0, 1000).onEvent(G, B, SideEvent.OFF, true, 0, 1100);
        assertEquals(0, c.cooldownLeft(G, 1100));
        assertTrue(c.isIdle(), "nothing to remember");
        assertTrue(GroupClock.idle().onEvent(G, A, SideEvent.FIRED, false, 0, 1000).isIdle());
    }

    @Test
    void theTotalSurvivesSoASweepProgresses() {
        GroupClock c = GroupClock.idle().onEvent(G, A, SideEvent.FIRED, false, 200, 1000);
        assertEquals(200, c.cooldownTotal(G));
        assertEquals(200, c.cooldownTotal(G), "it is the length, not what is left");
        // an end that starts no cooldown keeps the one that runs
        GroupClock on = c.onEvent(G, B, SideEvent.ON, true, 0, 1050);
        GroupClock off = on.onEvent(G, B, SideEvent.OFF, true, 0, 1060);
        assertEquals(200, off.cooldownTotal(G));
        assertEquals(1200, off.cooldownEnd(G));
        assertEquals(0, GroupClock.idle().cooldownTotal(G));
    }

    @Test
    void cooldownSurvivesACodecRoundTrip() {
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 600, 1000).onEvent("bite", A, SideEvent.FIRED, false, 300, 1000);
        var json = GroupClock.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow();
        GroupClock back = GroupClock.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(c, back);
        assertEquals(300, back.cooldownLeft("bite", 1000));
        assertEquals(300, back.cooldownTotal("bite"));
        assertEquals(B, back.activeSide(G).orElseThrow());
    }

    @Test
    void theSideIsSavedByItsKeyNotItsPlace() {
        GroupClock c = GroupClock.idle().onEvent(G, B, SideEvent.ON, true, 600, 1000);
        JsonObject json = GroupClock.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow().getAsJsonObject();
        assertEquals(B, json.getAsJsonObject(G).get("active").getAsString());
    }

    @Test
    void aSavedClockWithoutATotalStillLoads() {
        JsonObject state = new JsonObject();
        state.addProperty("end", 1500L);
        JsonObject root = new JsonObject();
        root.add(G, state);
        GroupClock back = GroupClock.CODEC.parse(JsonOps.INSTANCE, root).getOrThrow();
        assertEquals(500, back.cooldownLeft(G, 1000));
        assertTrue(back.activeSide(G).isEmpty());
    }

    @Test
    void relogMidLastingThenOffStartsExactlyOneCooldown() {
        GroupClock on = GroupClock.idle().onEvent(G, A, SideEvent.ON, true, 600, 1000);
        var json = GroupClock.CODEC.encodeStart(JsonOps.INSTANCE, on).getOrThrow();
        GroupClock back = GroupClock.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        GroupClock off = back.onEvent(G, A, SideEvent.OFF, true, 600, 5000);
        assertEquals(600, off.cooldownLeft(G, 5000));
        assertEquals(off, off.onEvent(G, A, SideEvent.OFF, true, 600, 5100), "a second off changes nothing");
    }

    @Test
    void twoGroupsDoNotShareAClock() {
        GroupClock c = GroupClock.idle().onEvent("a", A, SideEvent.ON, true, 100, 1000).onEvent("b", B, SideEvent.FIRED, false, 50, 1000);
        assertEquals(A, c.activeSide("a").orElseThrow());
        assertTrue(c.activeSide("b").isEmpty());
        assertEquals(0, c.cooldownLeft("a", 1000));
        assertEquals(50, c.cooldownLeft("b", 1000));
        assertFalse(StackRules.mayStart(c, "b", A, 1000).isEmpty());
        assertTrue(StackRules.mayStart(c, "a", A, 1000).isEmpty());
    }

    @Test
    void anEventPrunesTheGroupsWhoseCooldownIsOver() {
        GroupClock c = GroupClock.idle().onEvent("old", A, SideEvent.FIRED, false, 100, 1000).onEvent("kept", A, SideEvent.FIRED, false, 5000, 1000);
        GroupClock later = c.onEvent("new", B, SideEvent.FIRED, false, 50, 2000);
        assertEquals(0, later.cooldownEnd("old"), "an expired group is not kept (the saved map would only grow)");
        assertEquals(6000, later.cooldownEnd("kept"));
        assertEquals(2050, later.cooldownEnd("new"));
        // a group that has a side on is never pruned, whatever its cooldown
        GroupClock on = GroupClock.idle().onEvent("on", A, SideEvent.ON, true, 0, 1000);
        assertEquals(A, on.onEvent("other", B, SideEvent.FIRED, false, 10, 9000).activeSide("on").orElseThrow());
    }
}
