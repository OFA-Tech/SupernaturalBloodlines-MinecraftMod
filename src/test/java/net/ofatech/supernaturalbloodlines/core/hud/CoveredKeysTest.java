package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.hud.CoveredKeys.Plan;
import org.junit.jupiter.api.Test;

class CoveredKeysTest {
    private static final String U = CoveredKeys.UNBOUND;
    private static final List<String> COVERED = List.of("tensura.slot_1", "keys.vampirism.suck", "keys.werewolves.bite");
    private static final Map<String, String> DEFAULTS = Map.of("tensura.slot_1", "key.keyboard.r", "keys.vampirism.suck", "key.keyboard.v",
            "keys.werewolves.bite", "key.keyboard.v");

    private static Plan hide(Map<String, String> current, Map<String, String> record) {
        return CoveredKeys.plan(current, DEFAULTS, COVERED, record, true);
    }

    private static Plan show(Map<String, String> current, Map<String, String> record) {
        return CoveredKeys.plan(current, DEFAULTS, COVERED, record, false);
    }

    @Test
    void recordsBeforeUnbinding() {
        Plan p = hide(Map.of("tensura.slot_1", "key.keyboard.g", "keys.vampirism.suck", "key.keyboard.v", "keys.werewolves.bite", U), Map.of());
        assertEquals(Map.of("tensura.slot_1", U, "keys.vampirism.suck", U), p.set());
        assertEquals(Map.of("tensura.slot_1", "key.keyboard.g", "keys.vampirism.suck", "key.keyboard.v", "keys.werewolves.bite", U), p.record());
    }

    @Test
    void restoresPlayersOwnBind() {
        Plan hidden = hide(Map.of("tensura.slot_1", "key.keyboard.g:ALT"), Map.of());
        assertEquals("key.keyboard.g:ALT", hidden.record().get("tensura.slot_1"));
        // later: the key is unbound, the player's own G+Alt is restored, not the mod default (R)
        Plan back = show(Map.of("tensura.slot_1", U), hidden.record());
        assertEquals(Map.of("tensura.slot_1", "key.keyboard.g:ALT"), back.set());
        assertTrue(back.record().isEmpty());
    }

    @Test
    void restoreSkipsKeysThePlayerRebound() {
        Plan back = show(Map.of("tensura.slot_1", "key.keyboard.h"), Map.of("tensura.slot_1", "key.keyboard.g"));
        assertTrue(back.set().isEmpty(), "the player's new bind was overwritten");
        assertTrue(back.record().isEmpty());
    }

    @Test
    void secondRunRecordsNothingNew() {
        Map<String, String> first = Map.of("tensura.slot_1", "key.keyboard.g");
        Plan one = hide(first, Map.of());
        Plan two = hide(Map.of("tensura.slot_1", U), one.record());
        assertTrue(two.set().isEmpty());
        assertEquals(one.record(), two.record());
    }

    @Test
    void aKeyReboundWhileCoveredIsLeftAlone() {
        Plan p = hide(Map.of("tensura.slot_1", "key.keyboard.h"), Map.of("tensura.slot_1", "key.keyboard.g"));
        assertTrue(p.set().isEmpty());
        assertEquals("key.keyboard.g", p.record().get("tensura.slot_1"));
    }

    @Test
    void resetToDefaultIsCoveredAgain() {
        // Controls' "Reset All" puts every key back on its default
        Plan p = hide(Map.of("tensura.slot_1", "key.keyboard.r"), Map.of("tensura.slot_1", "key.keyboard.g"));
        assertEquals(Map.of("tensura.slot_1", U), p.set());
        assertEquals("key.keyboard.g", p.record().get("tensura.slot_1"));
    }

    @Test
    void missingModIgnored() {
        Plan p = hide(Map.of("tensura.slot_1", "key.keyboard.g"), Map.of());
        assertEquals(Map.of("tensura.slot_1", U), p.set());
        assertEquals(Map.of("tensura.slot_1", "key.keyboard.g"), p.record());
        assertTrue(!p.record().containsKey("keys.vampirism.suck"));
    }

    @Test
    void restoreKeepsTheRecordOfAModThatIsGone() {
        Plan p = show(Map.of(), Map.of("keys.vampirism.suck", "key.keyboard.v"));
        assertTrue(p.set().isEmpty());
        assertEquals(Map.of("keys.vampirism.suck", "key.keyboard.v"), p.record());
    }

    @Test
    void anAlreadyUnboundKeyIsRecordedAsUnboundAndNeverRebound() {
        Plan p = hide(Map.of("keys.werewolves.bite", U), Map.of());
        assertTrue(p.set().isEmpty());
        Plan back = show(Map.of("keys.werewolves.bite", U), p.record());
        assertTrue(back.set().isEmpty());
        assertTrue(back.record().isEmpty());
    }

    @Test
    void anIdOutsideTheCoveredListIsNeverTouched() {
        Plan p = hide(Map.of("key.jump", "key.keyboard.space"), Map.of());
        assertTrue(p.set().isEmpty() && p.record().isEmpty());
    }

    @Test
    void hideRestoreRoundTrip() {
        Map<String, String> before = Map.of("tensura.slot_1", "key.keyboard.g", "keys.vampirism.suck", "key.keyboard.v:SHIFT");
        Plan off = hide(before, Map.of());
        Plan on = show(Map.of("tensura.slot_1", U, "keys.vampirism.suck", U), off.record());
        assertEquals(before, on.set());
        assertTrue(on.record().isEmpty());
    }
}
