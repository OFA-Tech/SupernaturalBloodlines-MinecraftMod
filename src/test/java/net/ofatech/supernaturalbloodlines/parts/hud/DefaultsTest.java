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
        assertEquals(HudClientConfig.Mode.AUTO, clientDefault("mode"));
        assertEquals(true, clientDefault("hide_covered_keys"));
        assertEquals(true, clientDefault("hide_tensura_skill_hud"));
        assertEquals(true, clientDefault("blood_in_food_spot"));
        assertEquals(5, HudClientConfig.SPEC.getSpec().valueMap().size(), "a client setting was added or removed: decide its player-ready default and list it here");
    }

    @Test
    void theServerSwitchesAreOnAndTheLimitsAreSane() {
        for (String key : List.of("next_preset_active", "tensura_feed_fills_blood")) {
            assertEquals(true, ((ModConfigSpec.ValueSpec) HudConfig.SPEC.getSpec().get(List.of("mixins", key))).getDefault(), key);
        }
        assertEquals(HudConfig.DEFAULT_RATE_LIMIT, ((ModConfigSpec.ValueSpec) HudConfig.SPEC.getSpec().get(List.of("rate_limit_per_second"))).getDefault());
    }

    @Test
    void aNewPlayersLoadoutHasBiteInTheFirstSlotOnly() {
        assertEquals(java.util.Optional.of(Loadout.BITE), Loadout.DEFAULT.get(0));
        for (int i = 1; i < Loadout.SLOTS; i++) {
            assertEquals(java.util.Optional.empty(), Loadout.DEFAULT.get(i));
        }
    }
}
