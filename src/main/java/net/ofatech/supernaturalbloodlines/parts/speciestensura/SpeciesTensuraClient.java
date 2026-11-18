package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client only: back to this client's own file when it leaves a world, so a server's values never outlive the visit. */
final class SpeciesTensuraClient {
    private SpeciesTensuraClient() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> SpeciesTensuraConfig.clearPushed());
    }
}
