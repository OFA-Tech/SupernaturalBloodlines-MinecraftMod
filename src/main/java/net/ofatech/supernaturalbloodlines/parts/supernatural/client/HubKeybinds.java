package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.client.PackKeybinds;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import org.lwjgl.glfw.GLFW;

/**
 * The hub key, in our key category. Default Home: J is JourneyMap's map and the OFA pack's Mekanism legs key, every letter is taken in the
 * pack's keymap, and = and - are map zoom keys. Plan 13's keymap may move it. In game it opens the hub; inside the hub the screen closes on it. Client only; registered only
 * while the supernatural part is on.
 */
public final class HubKeybinds {
    public static final KeyMapping HUB = new KeyMapping("key.supernaturalbloodlines.hub", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_HOME, PackKeybinds.CATEGORY);

    /** Unbound by default: Home with the first waiting request focused, for a player who answers from the corner toast. */
    public static final KeyMapping ANSWER = new KeyMapping("key.supernaturalbloodlines.answer_request", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, PackKeybinds.CATEGORY);

    private HubKeybinds() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(HUB);
            e.register(ANSWER);
        });
        HubClient.register();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (ANSWER.consumeClick()) {
                if (Minecraft.getInstance().screen == null) {
                    HubClient.openRequests();
                }
            }
            while (HUB.consumeClick()) {
                if (Minecraft.getInstance().screen == null) {
                    HubClient.open();
                }
            }
        });
    }
}
