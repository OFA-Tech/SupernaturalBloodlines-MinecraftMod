package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Act;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Input;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus.Kind;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import org.junit.jupiter.api.Test;

/** The calls a stone entry makes for each input: his screens opened, his requests sent, and which power an aim fires. Against a fake of his mod. */
class StoneMenuActionsTest {
    private static WheelEntry stone(FakeStoneApi api, String key) {
        for (WheelEntry e : StoneEntries.build(api, Map.of(), 1_000)) {
            if (e.ref().id().getPath().equals(key + "_stone")) {
                return e;
            }
        }
        throw new AssertionError(key);
    }

    private static List<String> run(FakeStoneApi api, WheelEntry e, Input input) {
        api.calls.clear();
        Act act = StoneActions.route(api, e, input);
        if (act != Act.PLAIN && act != Act.NOTHING) {
            assertTrue(StoneActions.act(api, new HashMap<>(), e, act));
        }
        return List.copyOf(api.calls);
    }

    @Test
    void theTeleportGuiOpensHisDestinationScreenForTheCasterOnLeftAndMiddle() {
        FakeStoneApi api = new FakeStoneApi().menu(2, "teleport_gui", false, false, Kind.DESTINATION, true);
        WheelEntry e = stone(api, "space");
        assertEquals(List.of("destination 2 teleport_gui false"), run(api, e, Input.USE));
        assertEquals(List.of("destination 2 teleport_gui false"), run(api, e, Input.MENU));
    }

    @Test
    void spatialBanishmentKeepsItsAimedUseAndItsMiddleClickMovesTheTarget() {
        FakeStoneApi api = new FakeStoneApi().menu(2, "spatial_banishment", true, true, Kind.DESTINATION_TARGET, false);
        WheelEntry e = stone(api, "space");
        assertEquals(Act.PLAIN, StoneActions.route(api, e, Input.USE), "left: aim, then his Use phase 0 (the End)");
        assertEquals(Act.PLAIN, StoneActions.route(api, e, Input.SECOND), "Shift: aim, then phase 2 (above the Nether roof)");
        assertEquals(List.of("destination 2 spatial_banishment true"), run(api, e, Input.MENU), "his DestinationScreen with movesTarget");
    }

    @Test
    void willConstructOpensHisConstructScreenOnShiftAndHisQuickSlotsOnMiddle() {
        FakeStoneApi api = new FakeStoneApi().menu(3, "will_construct", false, true, Kind.WILL, false);
        WheelEntry e = stone(api, "reality");
        assertEquals(List.of("construct"), run(api, e, Input.SECOND));
        assertEquals(List.of("quick"), run(api, e, Input.MENU));
        assertEquals(List.of(), run(api, e, Input.USE), "left is the grip toggle, the usual use");
    }

    @Test
    void aChoiceMenuPowerAsksHisServerOnMiddleAndSendsThePlainSecondOptionAtOnce() {
        FakeStoneApi api = new FakeStoneApi().menu(0, "suggestion", true, true, Kind.CHOICE, false);
        WheelEntry e = stone(api, "mind");
        assertEquals(List.of("choice 0 suggestion"), run(api, e, Input.MENU));
        assertEquals(List.of("use 0 suggestion 2"), run(api, e, Input.SECOND), "no aiming for the right click of a menu power");
        assertEquals(Act.PLAIN, StoneActions.route(api, e, Input.USE), "left still aims, as his");
    }

    @Test
    void aPowerWithNoMenuAndANonStoneEntryHaveNothingOnTheMiddleClick() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true);
        assertEquals(List.of(), run(api, stone(api, "mind"), Input.MENU));
        assertEquals(Act.NOTHING, StoneActions.route(api, stone(api, "mind"), Input.MENU));
        api.soulMode = 2;
        WheelEntry control = StoneEntries.build(api, Map.of(), 1_000).stream().filter(x -> x.ref().id().equals(StoneEntries.SOUL_CONTROL)).findFirst().orElseThrow();
        assertEquals(Act.NOTHING, StoneActions.route(api, control, Input.MENU));
        assertEquals(Act.PLAIN, StoneActions.route(api, control, Input.USE));
        assertFalse(StoneActions.act(api, new HashMap<>(), control, Act.OPEN_CHOICE), "no power behind the entry: nothing is sent");
        assertEquals(List.of(), List.copyOf(api.calls));
    }

    @Test
    void theMenuActsRemainOnlyForTheCurrentPowerOfTheStone() {
        FakeStoneApi api = new FakeStoneApi().plain(2, "teleport_x", true).menu(2, "teleport_gui", false, false, Kind.DESTINATION, true);
        Map<Integer, String> current = new HashMap<>();
        WheelEntry onFirst = StoneEntries.build(api, current, 1_000).get(0);
        assertEquals(Act.PLAIN, StoneActions.route(api, onFirst, Input.USE), "a stone on a plain power has no menu on its click");
        current.put(2, "teleport_gui");
        WheelEntry onGui = StoneEntries.build(api, current, 1_000).get(0);
        assertEquals(Act.OPEN_DESTINATION, StoneActions.route(api, onGui, Input.USE));
        assertEquals(List.of("menu_key/menu_destination", "keys_key/keys_menu"), StoneActions.help(api, onGui).stream().map(h -> h.key() + "/" + h.what()).toList());
        assertEquals(List.of(), StoneActions.help(api, onFirst));
    }

    @Test
    void aConfirmedAimFiresThePowerThatWasAimedNotTheOneTheStoneIsOnNow() {
        FakeStoneApi api = new FakeStoneApi().node(0, "ray", true, false, false, true, true, null).plain(0, "other", true);
        Map<Integer, String> current = new HashMap<>(Map.of(0, "ray"));
        assertTrue(StoneActions.useNode(api, current, "mind", "ray", false));
        assertTrue(StoneActions.useNode(api, current, "mind", "ray", true));
        current.put(0, "other"); // the stone was cycled while aiming
        assertTrue(StoneActions.useNode(api, current, "mind", "ray", true));
        assertEquals(List.of("use 0 ray 0", "use 0 ray 2", "use 0 ray 2"), api.calls);
        assertEquals("ray", current.get(0), "the power that fired is the one the stone shows again");
    }

    @Test
    void anAimOfAPowerThatIsGoneOrLockedOrUnknownFiresNothing() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "locked", false).plain(0, "ok", true);
        Map<Integer, String> current = new HashMap<>();
        assertFalse(StoneActions.useNode(api, current, "mind", "locked", false));
        assertFalse(StoneActions.useNode(api, current, "mind", "gone", false));
        assertFalse(StoneActions.useNode(api, current, "mind", null, false));
        assertFalse(StoneActions.useNode(api, current, "power", "ok", false), "another stone's power of the same name is not this one");
        assertEquals(List.of(), api.calls);
    }

    @Test
    void theEntriesAreBuiltOnlyWhenWhatTheyAreMadeFromChanges() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true).plain(0, "b", true);
        StoneEntries.Cache cache = new StoneEntries.Cache();
        Map<Integer, String> current = new HashMap<>();
        List<WheelEntry> first = cache.get(api, current, 1_000);
        for (int tick = 0; tick < 50; tick++) {
            assertSame(first, cache.get(api, current, 1_000), "nothing changed: the same list, built once");
        }
        assertEquals(1, cache.builds());

        current.put(0, "b");
        List<WheelEntry> second = cache.get(api, current, 1_000);
        assertNotSame(first, second);
        assertEquals(2, cache.builds(), "our current-power map changed");
        assertEquals(1, second.get(0).state().mode());

        api.plain(2, "c", true);
        assertEquals(2, cache.get(api, current, 1_000).size());
        assertEquals(3, cache.builds(), "his powers changed");

        api.soulMode = 2;
        assertEquals(3, cache.get(api, current, 1_000).size());
        assertEquals(4, cache.builds(), "a possession began");

        api.holding = "mind.a";
        cache.get(api, current, 1_000);
        assertEquals(5, cache.builds(), "a channel began");

        api.receivedAt += 1;
        cache.get(api, current, 1_000);
        assertEquals(6, cache.builds(), "his server sent a state");

        cache.get(api, current, 2_000);
        assertEquals(7, cache.builds(), "another world was joined");

        cache.get(api, current, 2_000);
        assertEquals(7, cache.builds());
        cache.clear();
        cache.get(api, current, 2_000);
        assertEquals(8, cache.builds(), "a reload forgets it");
    }

    @Test
    void aMapEditedInPlaceAfterTheBuildIsStillSeenAsAChange() {
        FakeStoneApi api = new FakeStoneApi().plain(0, "a", true).plain(0, "b", true);
        StoneEntries.Cache cache = new StoneEntries.Cache();
        Map<Integer, String> current = new HashMap<>();
        cache.get(api, current, 1_000);
        current.put(0, "b");
        assertEquals(1, cache.get(api, current, 1_000).get(0).state().mode(), "the map the cache compares to is its own copy, not the live one");
    }
}
