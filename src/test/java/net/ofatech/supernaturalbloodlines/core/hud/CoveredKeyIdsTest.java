package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelBuild;
import org.junit.jupiter.api.Test;

class CoveredKeyIdsTest {
    private static final Set<String> SPEC_WHEEL_IDS = Set.of(
            "keys.vampirism.action", "keys.vampirism.vision",
            "key.greatsage.menu", "key.greatsage.assess", "key.greatsage.scan", "key.greatsage.magic_sense", "key.greatsage.thought_acceleration",
            "key.infinity.wheel", "key.infinity.soul_control",
            "tensura.keybinding.race_ability");

    /** Keys that are not skill keys: never covered, whatever the flags. */
    private static final List<String> LEFT_ALONE = List.of(
            "tensura.keybinding.main_gui", "tensura.keybinding.next_mode", "tensura.keybinding.previous_mode", "tensura.keybinding.dodge", "tensura.keybinding.naming",
            "keys.vampirism.skill_screen", "keys.vampirism.select_skills",
            "key.greatsage.voice", "key.greatsage.panic",
            "key.infinity.soul_release");

    @Test
    void coveredIdsMatchTheSpecList() {
        List<String> all = CoveredKeyIds.all(true, true);
        assertEquals(SPEC_WHEEL_IDS, Set.copyOf(CoveredKeyIds.WHEEL_IDS));
        assertTrue(all.containsAll(SPEC_WHEEL_IDS));
        assertTrue(all.containsAll(CoveredKeyIds.all(true, false)));
        assertEquals(CoveredKeyIds.all(true, false).size() + SPEC_WHEEL_IDS.size(), all.size());
        assertEquals(all.size(), Set.copyOf(all).size(), "no id twice");
    }

    @Test
    void wheelIdsCoveredNowThatTheScreenExists() {
        assertTrue(WheelBuild.screenPresent(), "W4 has the wheel screen: the keys it replaces are covered");
        // what KeyCover asks for
        List<String> covered = CoveredKeyIds.all(true, WheelBuild.screenPresent());
        for (String id : SPEC_WHEEL_IDS) {
            assertTrue(covered.contains(id), id);
        }
        assertTrue(covered.contains("keys.vampirism.vision"));
        assertTrue(covered.contains("tensura.keybinding.race_ability"));
        // the lists without a screen still leave them alone (a build, a server or a setting without the wheel)
        assertEquals(CoveredKeyIds.all(true, false), CoveredKeyIds.all(true));
        for (String id : SPEC_WHEEL_IDS) {
            assertFalse(CoveredKeyIds.all(true).contains(id), id);
        }
    }

    @Test
    void hudPartOffAndAServerWithoutTheChannelRestoreEveryWheelKey() {
        // CoverRun restores a recorded bind of an id that is no longer listed: with the wheel keys left out of the list they all come back
        assertFalse(CoveredKeyIds.all(true, false).stream().anyMatch(SPEC_WHEEL_IDS::contains));
        assertTrue(CoveredKeyIds.all(true, true).containsAll(SPEC_WHEEL_IDS));
    }

    @Test
    void infinityKeysAreCoveredOnlyWhileTheWheelCanDoTheirJobAndReleaseIsLeftAlone() {
        List<String> on = CoveredKeyIds.all(true, true, true, true);
        assertTrue(on.contains("key.infinity.wheel") && on.contains("key.infinity.soul_control"));
        assertFalse(on.contains("key.infinity.soul_release"), "his release key stays his own");
        List<String> compatOff = CoveredKeyIds.all(true, true, false, false);
        assertFalse(compatOff.contains("key.infinity.wheel"), "with the compat off his wheel is the only way to use a stone");
        assertFalse(compatOff.contains("key.infinity.soul_control"));
        List<String> noSoulClasses = CoveredKeyIds.all(true, true, true, false);
        assertTrue(noSoulClasses.contains("key.infinity.wheel"));
        assertFalse(noSoulClasses.contains("key.infinity.soul_control"), "a covered key with no entry would strand a possessed player");
        // no wheel screen at all: none of the wheel keys, whatever the compat says
        assertFalse(CoveredKeyIds.all(true, false, true, true).contains("key.infinity.wheel"));
        // everything else is the same in every case
        assertEquals(on.size() - 2, compatOff.size());
    }

    @Test
    void hudPartOffRestoresTheInfinityKeys() {
        assertFalse(CoveredKeyIds.all(true).contains("key.infinity.wheel"));
        assertFalse(CoveredKeyIds.all(true).contains("key.infinity.soul_control"));
    }

    @Test
    void modOffRestoresItsKeyAlone() {
        // a mod that is not installed has no key to set, and the other mods' keys stay covered
        List<String> covered = CoveredKeyIds.all(true, true);
        assertTrue(covered.contains("key.greatsage.scan") && covered.contains("keys.vampirism.vision"));
        assertTrue(covered.stream().filter(id -> id.startsWith("key.greatsage.")).count() == 5);
    }

    @Test
    void leftAloneIdsAreNeverCovered() {
        for (String id : LEFT_ALONE) {
            assertFalse(CoveredKeyIds.all(true, true).contains(id), id);
            assertFalse(CoveredKeyIds.all(false, true).contains(id), id);
        }
        // the owner moved these two onto the wheel (2026-11-22)
        assertTrue(CoveredKeyIds.all(true, true).contains("keys.vampirism.vision"));
        assertTrue(CoveredKeyIds.all(true, true).contains("tensura.keybinding.race_ability"));
    }

    @Test
    void sprintLeapStillDecidesTheLeapKey() {
        assertTrue(CoveredKeyIds.all(true, true).contains(CoveredKeyIds.WEREWOLVES_LEAP));
        assertFalse(CoveredKeyIds.all(false, true).contains(CoveredKeyIds.WEREWOLVES_LEAP));
    }

    @Test
    void anUnknownIdIsIgnoredAndItsRecordKept() {
        // Great Sage is not installed: its keys are not in the game, so nothing is set and nothing recorded for them
        Map<String, String> current = new HashMap<>(Map.of("key.infinity.wheel", "key.keyboard.h"));
        CoveredKeys.Plan p = CoveredKeys.plan(current, Map.of("key.infinity.wheel", "key.keyboard.h"), CoveredKeyIds.all(true, true), Map.of(), true);
        assertEquals(Map.of("key.infinity.wheel", CoveredKeys.UNBOUND), p.set());
        assertEquals(Map.of("key.infinity.wheel", "key.keyboard.h"), p.record());
        // a recorded id of a mod that is gone stays recorded when restoring
        CoveredKeys.Plan back = CoveredKeys.plan(Map.of(), Map.of(), CoveredKeyIds.all(true, true), Map.of("key.greatsage.scan", "key.keyboard.b"), false);
        assertTrue(back.set().isEmpty());
        assertEquals(Map.of("key.greatsage.scan", "key.keyboard.b"), back.record());
    }

    @Test
    void restoreGivesEveryRecordedWheelKeyBack() {
        Map<String, String> originals = new LinkedHashMap<>();
        for (String id : SPEC_WHEEL_IDS) {
            originals.put(id, "key.keyboard." + id.hashCode());
        }
        Map<String, String> current = new HashMap<>(originals);
        Map<String, String> record = new LinkedHashMap<>();
        CoveredKeys.Plan cover = CoveredKeys.plan(current, originals, CoveredKeyIds.all(true, true), record, true);
        assertEquals(SPEC_WHEEL_IDS, cover.set().keySet());
        current.putAll(cover.set());
        CoveredKeys.Plan restore = CoveredKeys.plan(current, originals, CoveredKeyIds.all(true, true), cover.record(), false);
        current.putAll(restore.set());
        assertEquals(originals, current);
        assertTrue(restore.record().isEmpty());
    }

    @Test
    void coverWithAnUnreadableRecordTouchesNothing() {
        List<String> events = new ArrayList<>();
        Map<String, String> binds = new HashMap<>(Map.of("key.greatsage.menu", "key.keyboard.g"));
        CoverRun.Keys keys = new CoverRun.Keys() {
            public Map<String, String> current() {
                return new HashMap<>(binds);
            }

            public Map<String, String> defaults() {
                return Map.of("key.greatsage.menu", "key.keyboard.g");
            }

            public boolean set(String id, String bind) {
                events.add("set " + id);
                return true;
            }

            public void save() {
                events.add("save");
            }
        };
        CoverRun.Store store = new CoverRun.Store() {
            public Map<String, String> read() {
                return Map.of();
            }

            public boolean write(Map<String, String> record) {
                events.add("write");
                return true;
            }

            public boolean blocked() {
                return true;
            }
        };
        CoverRun.Result r = CoverRun.run(keys, store, CoveredKeyIds.all(true, true), true);
        assertEquals(CoverRun.Outcome.BLOCKED, r.outcome());
        assertTrue(events.isEmpty());
    }
}
