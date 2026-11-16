package net.ofatech.supernaturalbloodlines.parts.hud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.ofatech.supernaturalbloodlines.client.PackKeybinds;
import net.ofatech.supernaturalbloodlines.core.hud.HudKeyDefaults;

/**
 * The HUD's own keys, in our key category, in game only. The six slot keys fire the slots, the mode key switches the mode.
 * There is no Bite key: Bite is an action in a slot. The hub key is registered by the supernatural part.
 * <p>
 * The mode key is plain Caps Lock (plan 16f). NeoForge fires a plain mapping only while no modifier is held, so {@code HudInput} also reads the key's
 * physical state and switches when it goes down with Alt, Ctrl or Shift held.
 */
public final class HudKeys {
    public static final KeyMapping[] SLOTS = HudKeyDefaults.SLOTS.stream().map(HudKeys::mapping).toArray(KeyMapping[]::new);
    public static final KeyMapping HUD_MODE = mapping(HudKeyDefaults.HUD_MODE);

    private HudKeys() {
    }

    private static KeyMapping mapping(HudKeyDefaults.Key k) {
        return new KeyMapping(k.id(), KeyConflictContext.IN_GAME, KeyModifier.valueFromString(k.modifier()), InputConstants.Type.KEYSYM, k.glfwKey(),
                PackKeybinds.CATEGORY);
    }

    public static void register(RegisterKeyMappingsEvent e) {
        for (KeyMapping slot : SLOTS) {
            e.register(slot);
        }
        e.register(HUD_MODE);
    }
}
