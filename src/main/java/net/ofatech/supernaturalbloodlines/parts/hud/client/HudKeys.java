package net.ofatech.supernaturalbloodlines.parts.hud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.ofatech.supernaturalbloodlines.client.PackKeybinds;
import net.ofatech.supernaturalbloodlines.core.hud.HudKeyDefaults;

/**
 * The HUD's own keys, in our key category, in game only. The six slot keys and the mode key do nothing yet (13b adds the HUD that reads them).
 * There is no Bite key: Bite is an action in a slot. The hub key is registered by the supernatural part.
 * <p>
 * The mode key is Alt + Caps Lock. NeoForge's key lookup returns the mappings of the active modifier for that key and falls back to the unmodified ones
 * only when the pressed key is itself a modifier, so the plain Caps Lock radio key of the OFA pack does not fire with it (read from
 * {@code KeyMappingLookup.getAll}; not yet seen in a running client).
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
