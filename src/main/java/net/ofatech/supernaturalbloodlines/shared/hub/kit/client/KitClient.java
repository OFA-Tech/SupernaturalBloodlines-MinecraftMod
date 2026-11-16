package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;

/**
 * What the kit keeps on the client and must give up when the resource packs change: the icon cache and the baked textures. Registered from the mod's own
 * constructor on the client, so it runs whichever parts are on (the hub and the HUD are both optional). Client only: load it behind a {@code Dist.CLIENT} check.
 */
public final class KitClient {
    private KitClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) rm -> {
            ClientIcons.reload();
            BakedTexture.clear();
        }));
    }
}
