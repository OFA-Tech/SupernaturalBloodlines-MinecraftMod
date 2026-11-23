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
    void wheelIdsAreAbsentWhileNoScreenExists() {
        assertFalse(WheelBuild.screenPresent(), "W1 to W3 have no wheel screen: nothing may be covered for it");
        assertEquals(CoveredKeyIds.all(true, false), CoveredKeyIds.all(true));
        assertEquals(CoveredKeyIds.all(false, false), CoveredKeyIds.all(false));
        for (String id : SPEC_WHEEL_IDS) {
            assertFalse(CoveredKeyIds.all(true).contains(id), id);
            assertFalse(CoveredKeyIds.all().contains(id), id);
        }
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
