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
    private static final java.util.List<Runnable> ON_RELOAD = new java.util.concurrent.CopyOnWriteArrayList<>();

    private KitClient() {
    }

    /** Another part of the client adds what it keeps until the resource packs change (the boss bars' portraits and cut texts). */
    public static void onReload(Runnable forget) {
        ON_RELOAD.add(forget);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) rm -> {
            ClientIcons.reload();
            BakedTexture.clear();
            UiKit.reload();
            net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache.clear();
            ON_RELOAD.forEach(Runnable::run);
        }));
    }
}
