package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class HudMixinRulesTest {
    private static boolean next(String... installed) {
        return HudMixinRules.applies("AbilityStorageNextPresetMixin", Set.of(installed)::contains);
    }

    @Test
    void nextPresetHookNeedsTensura() {
        assertTrue(next("tensura"));
        assertFalse(next());
    }

    @Test
    void nextPresetHookStaysOutWhenTensuraSlotsIsInstalled() {
        assertFalse(next("tensura", "tensuraslots"));
    }

    @Test
    void raceFeedHookIsNotAffectedBySlots() {
        assertTrue(HudMixinRules.applies("VampireRaceFeedMixin", Set.of("tensura", "vampirism", "tensuraslots")::contains));
        assertFalse(HudMixinRules.applies("VampireRaceFeedMixin", Set.of("tensura")::contains));
    }
}
