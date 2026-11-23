package net.ofatech.supernaturalbloodlines.parts.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import org.junit.jupiter.api.Test;

/** The "player-ready" defaults of plan 13 (13c Task 3): a new player gets the HUD, the keymap, the Tensura HUD swap and the blood spot with no setup. */
class DefaultsTest {
    private static Object clientDefault(String key) {
        return ((ModConfigSpec.ValueSpec) HudClientConfig.SPEC.getSpec().get(List.of(key))).getDefault();
    }

    @Test
    void theClientSettingsAreExactlyThePlayerReadyList() {
        assertEquals(true, clientDefault("hud_enabled"));
        assertEquals(net.ofatech.supernaturalbloodlines.core.hud.HudVisibility.AUTO, clientDefault("hud_visibility"));
        assertEquals(HudClientConfig.Mode.AUTO, clientDefault("mode"));
        assertEquals(true, clientDefault("sprint_leap"));
        assertEquals(true, clientDefault("hide_covered_keys"));
        assertEquals(true, clientDefault("hide_tensura_skill_hud"));
        assertEquals(true, clientDefault("blood_in_food_spot"));
        assertEquals(net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.TOP_CENTER, clientDefault("tracker_anchor"));
        assertEquals(net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor.TOP_LEFT, clientDefault("toast_anchor"));
        assertEquals(200, clientDefault("wheel_tap_ms"));
        assertEquals(true, clientDefault("wheel_remember_last"));
        assertEquals(11, HudClientConfig.SPEC.getSpec().valueMap().size(), "a client setting was added or removed: decide its player-ready default and list it here");
    }

    @Test
    void theServerSwitchesAreOnAndTheLimitsAreSane() {
        for (String key : List.of("next_preset_active", "tensura_feed_fills_blood")) {
            assertEquals(true, ((ModConfigSpec.ValueSpec) HudConfig.SPEC.getSpec().get(List.of("mixins", key))).getDefault(), key);
        }
        assertEquals(HudConfig.DEFAULT_RATE_LIMIT, ((ModConfigSpec.ValueSpec) HudConfig.SPEC.getSpec().get(List.of("rate_limit_per_second"))).getDefault());
    }

    @Test
    void aNewPlayersLoadoutIsEmptyUntilTheyCanBite() {
        for (int i = 0; i < Loadout.SLOTS; i++) {
            assertEquals(java.util.Optional.empty(), Loadout.DEFAULT.get(i));
        }
    }
}
