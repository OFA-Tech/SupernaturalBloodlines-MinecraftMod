package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneRules;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.junit.jupiter.api.Test;

class StoneEntriesTest {
    private static List<WheelEntry> build(FakeStoneApi api, Map<Integer, String> current) {
        return StoneEntries.build(api, current, 1_000);
    }

    private static WheelEntry stone(List<WheelEntry> all, String key) {
        for (WheelEntry e : all) {
            if (e.ref().id().getPath().equals(key + "_stone")) {
                return e;
            }
        }
        return null;
    }

    @Test
    void oneEntryPerCarriedStoneInTheInfinityRingWithNoGroups() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true).plain(0, "b", true).plain(2, "c", true);
        List<WheelEntry> all = build(api, Map.of());
        assertEquals(2, all.size(), "two stones carried: two entries, however many powers each has");
        for (WheelEntry e : all) {
            assertEquals(PowerSource.INFINITY, e.ref().source());
            assertEquals(PowerGroup.MAIN, e.group(), "one group: no middle ring");
            assertFalse(e.slottable());
        }
        assertEquals(2, stone(all, "mind").state().modeCount(), "its unlocked powers are its modes");
    }

    @Test
    void aStoneNotCarriedHasNoEntry() {
        assertTrue(build(new FakeStoneApi(), Map.of()).isEmpty());
    }

    @Test
    void lockedPowersAreNeverModes() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true).plain(0, "b", false).plain(0, "c", true);
        WheelEntry mind = stone(build(api, Map.of()), "mind");
        assertEquals(2, mind.modeNames().size());
        assertEquals(2, mind.state().modeCount());
        // the cycle lands on c, never on b, and wraps back to a
        Map<Integer, String> current = new HashMap<>();
        StoneActions.cycle(api, current, mind, 1);
        assertEquals("c", current.get(0));
        WheelEntry onC = stone(build(api, current), "mind");
        StoneActions.cycle(api, current, onC, 1);
        assertEquals("a", current.get(0));
        StoneActions.cycle(api, current, stone(build(api, current), "mind"), -1);
        assertEquals("c", current.get(0));
    }

    @Test
    void aStoneWithNothingUnlockedIsLockedAndHasNoPower() {
        WheelEntry mind = stone(build(new FakeStoneApi().plain(0, "a", false), Map.of()), "mind");
        assertEquals(LockReason.NOT_UNLOCKED, mind.state().lock());
        assertNull(StoneEntries.nodeOf(new FakeStoneApi().plain(0, "a", false), mind));
    }

    @Test
    void theRememberedPowerIsTheCurrentModeAndAGoneOneFallsBackToTheFirst() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true).plain(0, "b", true).plain(0, "c", true);
        assertEquals(1, stone(build(api, Map.of(0, "b")), "mind").state().mode());
        assertEquals(0, stone(build(api, Map.of(0, "gone")), "mind").state().mode());
        Map<Integer, String> other = new HashMap<>(Map.of(1, "b"));
        assertEquals(0, stone(build(api, other), "mind").state().mode(), "another stone's choice is not this one's");
    }

    @Test
    void switchStaysOpenAndShowsToggled() {
        FakeStoneApi api = new FakeStoneApi().node(0, "sw", true, true, false, false, false, null);
        api.nodes.set(0, new NodeInfo(0, "mind", "sw", "Name sw", "", true, true, 0, true, false, false, false, null, StoneMenus.Kind.NONE, false));
        WheelEntry mind = stone(build(api, Map.of()), "mind");
        assertEquals(PowerKind.TOGGLE, mind.kind());
        assertTrue(mind.state().on(), "his toggled flag is the glow");
    }

    @Test
    void needsAimIsAimedAndAChannelIsAToggleThatStopsOnTheSecondClick() {
        assertEquals(PowerKind.AIMED, stone(build(new FakeStoneApi().node(0, "ray", true, false, false, true, false, null), Map.of()), "mind").kind());
        FakeStoneApi channel = new FakeStoneApi().node(0, "beam", true, false, true, false, false, null);
        assertEquals(PowerKind.TOGGLE, stone(build(channel, Map.of()), "mind").kind());
        channel.holding = "mind.beam";
        WheelEntry running = stone(build(channel, Map.of()), "mind");
        assertTrue(running.state().on(), "a channel in progress shows on");
        assertUse(channel, running, false, "use 0 beam 1");
    }

    @Test
    void shiftLeftOnADualSendsPhaseTwoAndOnANormalPowerPhaseZero() {
        FakeStoneApi api = new FakeStoneApi().node(0, "dual", true, false, false, false, true, null).plain(1, "plain", true);
        List<WheelEntry> all = build(api, Map.of());
        assertUse(api, stone(all, "mind"), true, "use 0 dual 2");
        assertUse(api, stone(all, "mind"), false, "use 0 dual 0");
        assertUse(api, stone(all, "power"), true, "use 1 plain 0");
    }

    @Test
    void plainScrollSendsALevelDeltaOnlyForAPowerWithLevels() {
        FakeStoneApi api = new FakeStoneApi().node(0, "beam", true, false, false, false, false, new NodeInfo.Levels(1, 4, 1)).plain(1, "p", true);
        List<WheelEntry> all = build(api, Map.of());
        assertLevel(api, stone(all, "mind"), 1, "level 0 beam 1");
        assertLevel(api, stone(all, "mind"), -3, "level 0 beam -1");
        api.calls.clear();
        StoneActions.level(api, stone(all, "power"), 1);
        assertTrue(api.calls.isEmpty(), "a power without levels sends no setting");
        assertTrue(stone(all, "mind").modeName().getString().endsWith("0/4"), "the level is in the mode caption: " + stone(all, "mind").modeName().getString());
    }

    @Test
    void aStateFromBeforeThisWorldWasJoinedIsShownLockedAndNotUsed() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true);
        api.receivedAt = 500;
        WheelEntry old = stone(StoneEntries.build(api, Map.of(), 1_000), "mind");
        assertEquals(LockReason.REFUSED_NOW, old.state().lock());
        assertTrue(old.state().locked());
        api.receivedAt = 1_500;
        assertEquals(LockReason.NONE, stone(StoneEntries.build(api, Map.of(), 1_000), "mind").state().lock());
    }

    @Test
    void soulControlEntryOnlyWhilePossessingAndNextToTheSoulStone() {
        FakeStoneApi api = new FakeStoneApi().plain(3, "r", true).plain(4, "s", true).plain(5, "t", true);
        assertNull(find(build(api, Map.of()), "soul_control"), "nothing possessed: absent");
        for (int mode : new int[] {1, 2, 3}) {
            api.soulMode = mode;
            List<WheelEntry> all = build(api, Map.of());
            WheelEntry control = find(all, "soul_control");
            assertNotNull(control, "mode " + mode);
            assertEquals(PowerKind.INSTANT, control.kind());
            assertEquals(PowerGroup.MAIN, control.group());
            assertFalse(control.slottable());
            assertEquals(all.indexOf(stone(all, "soul")) + 1, all.indexOf(control), "right after the Soul stone");
        }
    }

    @Test
    void soulControlGoesLastWhenTheSoulStoneIsNotCarried() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true);
        api.soulMode = 2;
        List<WheelEntry> all = build(api, Map.of());
        assertEquals("soul_control", all.get(all.size() - 1).ref().id().getPath());
    }

    @Test
    void soulControlNeedsHisSoulClasses() {
        FakeStoneApi api = new FakeStoneApi().plain(4, "s", true);
        api.soulMode = 2;
        api.soulSupported = false;
        assertNull(find(build(api, Map.of()), "soul_control"));
    }

    @Test
    void soulControlSendsControlAndInMobModeReleaseAndNothingToOurServer() {
        FakeStoneApi api = new FakeStoneApi();
        api.soulMode = 2;
        WheelEntry control = find(build(api, Map.of()), "soul_control");
        assertUse(api, control, false, "soul " + StoneRules.ACT_CONTROL);
        api.soulMode = 3;
        assertUse(api, control, false, "soul " + StoneRules.ACT_RELEASE);
        api.soulMode = 0;
        api.calls.clear();
        assertFalse(StoneActions.use(api, new HashMap<>(), control, false));
        assertTrue(api.calls.isEmpty(), "nothing possessed: nothing sent");
    }

    private static void assertUse(FakeStoneApi api, WheelEntry e, boolean second, String expected) {
        api.calls.clear();
        assertTrue(StoneActions.use(api, new HashMap<>(), e, second));
        assertEquals(List.of(expected), api.calls);
    }

    private static void assertLevel(FakeStoneApi api, WheelEntry e, int delta, String expected) {
        api.calls.clear();
        StoneActions.level(api, e, delta);
        assertEquals(List.of(expected), api.calls);
    }

    private static WheelEntry find(List<WheelEntry> all, String path) {
        for (WheelEntry e : all) {
            if (e.ref().id().getPath().equals(path)) {
                return e;
            }
        }
        return null;
    }

    @Test
    void aPlayerWithNoStoneGetsAPrayEntryThatSendsHisPrayerAndNothingToOurServer() {
        FakeStoneApi api = new FakeStoneApi();
        assertNull(find(build(api, Map.of()), "pray"), "carrying a stone, or no player: no prayer");
        api.prayShown = true;
        List<WheelEntry> all = build(api, Map.of());
        WheelEntry pray = find(all, "pray");
        assertNotNull(pray);
        assertEquals(PowerSource.INFINITY, pray.ref().source(), "in the Infinity ring");
        assertEquals(PowerKind.INSTANT, pray.kind());
        assertEquals(PowerGroup.MAIN, pray.group());
        assertFalse(pray.slottable());
        assertFalse(pray.state().locked(), "it needs none of his powers: never stale-locked");
        assertEquals("supernaturalbloodlines.wheel.infinity.pray", ((net.minecraft.network.chat.contents.TranslatableContents) pray.name().getContents()).getKey());
        assertUse(api, pray, false, "pray");
        api.prayShown = false;
        api.calls.clear();
        assertFalse(StoneActions.use(api, new HashMap<>(), pray, false), "his key sends nothing once a stone is carried");
        assertTrue(api.calls.isEmpty());
    }

    @Test
    void thePrayEntryComesAndGoesWithTheCache() {
        FakeStoneApi api = new FakeStoneApi();
        StoneEntries.Cache cache = new StoneEntries.Cache();
        assertEquals(0, cache.get(api, Map.of(), 1_000).size());
        api.prayShown = true;
        assertEquals(1, cache.get(api, Map.of(), 1_000).size(), "the entry appears without any other change");
        api.prayShown = false;
        assertEquals(0, cache.get(api, Map.of(), 1_000).size());
    }

    @Test
    void thePrayEntryIsTheOnlyEntryWhenNoStoneIsCarriedAndItStaysBesideStonesNever() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true);
        api.prayShown = true; // his own condition makes this false while a stone is carried; the entries do not second-guess it
        assertEquals(2, build(api, Map.of()).size());
    }}
