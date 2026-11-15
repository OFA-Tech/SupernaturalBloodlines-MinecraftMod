package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.client.PackKeybinds;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import org.lwjgl.glfw.GLFW;

/** The hub key ({@code J} by default), in our key category. Client only; registered only while the supernatural part is on. */
public final class HubKeybinds {
    public static final KeyMapping HUB = new KeyMapping("key.supernaturalbloodlines.hub", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,
            PackKeybinds.CATEGORY);

    private HubKeybinds() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(HUB));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (HUB.consumeClick()) {
                if (Minecraft.getInstance().screen == null) {
                    HubClient.open();
                }
            }
        });
    }
}
