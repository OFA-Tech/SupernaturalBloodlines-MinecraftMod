package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HudKeysDefaultsTest {
    // GLFW key codes of the other addons' keys in the Holy Packs options.txt
    private static final int GLFW_PERIOD = 46;
    private static final int GLFW_O = 79;
    private static final int GLFW_H = 72;
    private static final int GLFW_J = 74;
    private static final int GLFW_L = 76;
    private static final int GLFW_Y = 89;
    private static final int GLFW_LEFT_CONTROL = 341;

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
                // the other addon keys that are set in the Holy Packs options.txt (Great Sage: assess H, magic sense Y, thought acceleration J;
                // Tensura Trainer open L, Tensura's main GUI O, name period, dodge Left Ctrl). Great Sage's own default for assess is V, which is our slot 3:
                // the pack moved it, a reset to defaults brings the clash back (docs/compat/tensura-addons.md)
                new HudKeyDefaults.Key("greatsage.assess", GLFW_H, "NONE"), new HudKeyDefaults.Key("greatsage.magic_sense", GLFW_Y, "NONE"),
                new HudKeyDefaults.Key("greatsage.thought_acceleration", GLFW_J, "NONE"), new HudKeyDefaults.Key("trainer.open", GLFW_L, "NONE"),
                new HudKeyDefaults.Key("tensura.main_gui", GLFW_O, "NONE"), new HudKeyDefaults.Key("tensura.name", GLFW_PERIOD, "NONE"),
                new HudKeyDefaults.Key("tensura.dodge", GLFW_LEFT_CONTROL, "NONE"));
        // the radio left Caps for plan 16f (the mode key is plain Caps Lock now): the pack's staged keymap moves it, see OFA-notes/KEYMAP.md
        for (HudKeyDefaults.Key ours : HudKeyDefaults.ALL) {
            for (HudKeyDefaults.Key other : live) {
                assertFalse(ours.glfwKey() == other.glfwKey() && ours.modifier().equals(other.modifier()), ours.id() + " collides with " + other.id());
            }
        }
    }

    @Test
    void theSlotsAreRFVZXCAndTheModeKeyIsPlainCapsLock() {
        assertEquals(List.of(HudKeyDefaults.GLFW_R, HudKeyDefaults.GLFW_F, HudKeyDefaults.GLFW_V, HudKeyDefaults.GLFW_Z, HudKeyDefaults.GLFW_X,
                HudKeyDefaults.GLFW_C), HudKeyDefaults.SLOTS.stream().map(HudKeyDefaults.Key::glfwKey).toList());
        assertEquals("NONE", HudKeyDefaults.HUD_MODE.modifier());
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
