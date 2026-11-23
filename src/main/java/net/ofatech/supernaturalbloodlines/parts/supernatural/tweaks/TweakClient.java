package net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;

/** Client only: back to this client's own file when it leaves a world, so a server's value never outlives the visit. */
public final class TweakClient {
    private TweakClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> SupernaturalConfig.clearPushed());
    }
}
