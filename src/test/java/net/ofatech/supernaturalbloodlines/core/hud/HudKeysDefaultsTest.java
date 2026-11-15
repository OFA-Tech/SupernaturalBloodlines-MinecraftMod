package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HudKeysDefaultsTest {
    @Test
    void noTwoOfOurDefaultsAreEqual() {
        Set<String> seen = new HashSet<>();
        for (HudKeyDefaults.Key k : HudKeyDefaults.ALL) {
            assertEquals(true, seen.add(k.glfwKey() + ":" + k.modifier()), k.id() + " clashes with another of our keys");
        }
    }

    @Test
    void noneCollidesWithTheOfaLiveBinds() {
        // the owner's live options.txt (plan 13, "OFA pack facts"): next_mode is Left Alt, race_ability [, great sage menu G, and Caps alone is the radio
        List<HudKeyDefaults.Key> live = List.of(new HudKeyDefaults.Key("next_mode", HudKeyDefaults.GLFW_LEFT_ALT, "NONE"),
                new HudKeyDefaults.Key("race_ability", HudKeyDefaults.GLFW_LEFT_BRACKET, "NONE"),
                new HudKeyDefaults.Key("greatsage.menu", HudKeyDefaults.GLFW_G, "NONE"),
                new HudKeyDefaults.Key("radio", HudKeyDefaults.GLFW_CAPS_LOCK, "NONE"));
        for (HudKeyDefaults.Key ours : HudKeyDefaults.ALL) {
            for (HudKeyDefaults.Key other : live) {
                assertFalse(ours.glfwKey() == other.glfwKey() && ours.modifier().equals(other.modifier()), ours.id() + " collides with " + other.id());
            }
        }
    }

    @Test
    void theSlotsAreRFVZXCAndTheModeKeyIsAltCaps() {
        assertEquals(List.of(HudKeyDefaults.GLFW_R, HudKeyDefaults.GLFW_F, HudKeyDefaults.GLFW_V, HudKeyDefaults.GLFW_Z, HudKeyDefaults.GLFW_X,
                HudKeyDefaults.GLFW_C), HudKeyDefaults.SLOTS.stream().map(HudKeyDefaults.Key::glfwKey).toList());
        assertEquals("ALT", HudKeyDefaults.HUD_MODE.modifier());
        assertEquals(HudKeyDefaults.GLFW_CAPS_LOCK, HudKeyDefaults.HUD_MODE.glfwKey());
    }

    @Test
    void theHubKeyIsHomeAndTheSixSlotIdsAreInOrder() {
        assertEquals(HudKeyDefaults.GLFW_HOME, HudKeyDefaults.HUB.glfwKey());
        assertEquals(List.of("key.supernaturalbloodlines.slot_1", "key.supernaturalbloodlines.slot_2", "key.supernaturalbloodlines.slot_3",
                "key.supernaturalbloodlines.slot_4", "key.supernaturalbloodlines.slot_5", "key.supernaturalbloodlines.slot_6"),
                HudKeyDefaults.SLOTS.stream().map(HudKeyDefaults.Key::id).toList());
    }
}
