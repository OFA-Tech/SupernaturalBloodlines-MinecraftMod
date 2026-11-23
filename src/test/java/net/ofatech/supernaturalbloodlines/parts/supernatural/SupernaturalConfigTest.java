package net.ofatech.supernaturalbloodlines.parts.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import org.junit.jupiter.api.Test;

class SupernaturalConfigTest {
    @Test
    void everyVampirismTweakIsOffInTheSpec() {
        assertEquals(4, Tweak.values().length);
        for (Tweak t : Tweak.values()) {
            Object v = SupernaturalConfig.SPEC.getValues().get(t.key);
            assertNotNull(v, t.key + " is not in the spec");
            assertFalse((Boolean) ((ModConfigSpec.ConfigValue<?>) v).getDefault(), t.key + " is on by default (R213: all four default to false)");
        }
    }

    @Test
    void beforeTheConfigLoadsTheFallbackAnswersOff() {
        for (Tweak t : Tweak.values()) {
            assertFalse(SupernaturalConfig.tweak(t), t.key);
        }
    }
}
