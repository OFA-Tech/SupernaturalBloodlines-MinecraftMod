package net.ofatech.supernaturalbloodlines.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.network.RoarPayload;

/**
 * The Alpha Roar key. Always registered, so a game or a server without the HUD still has a non-command way to roar. With the HUD the Roar is an action in a
 * slot, so {@code KeyCover} unbinds and hides this key while the HUD can take over ({@code CoveredKeyIds}) and gives it back when it cannot. Unbound by
 * default (the pack's keymap script binds it); client only.
 */
public final class PackKeybinds {
    public static final String CATEGORY = "key.categories.supernaturalbloodlines";
    public static final KeyMapping ALPHA_ROAR = new KeyMapping("key.supernaturalbloodlines.alpha_roar",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private PackKeybinds() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(ALPHA_ROAR));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (ALPHA_ROAR.consumeClick()) {
                if (Minecraft.getInstance().player != null) {
                    PacketDistributor.sendToServer(RoarPayload.INSTANCE);
                }
            }
        });
    }
}
