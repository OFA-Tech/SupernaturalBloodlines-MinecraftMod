package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.parts.hud.client.tensura.BloodInFoodSpot;
import net.ofatech.supernaturalbloodlines.parts.hud.client.tensura.TensuraHudFlags;
import net.ofatech.supernaturalbloodlines.shared.hud.LoadoutAccess;

/** Client registrations of the HUD part. Called only from {@code HudPart} behind a {@code Dist.CLIENT} check. */
public final class HudClient {
    private HudClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> HudKeys.register(e));
        modBus.addListener(AbilityHudLayer::register);
        LoadoutAccess.install(new ClientLoadout());
        HudSlots.register();
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
                    net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons.reload();
                    net.ofatech.supernaturalbloodlines.shared.hub.kit.client.BakedTexture.clear();
                }));
        BossBars.register();
        net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess.installHud();
        HudInput.register();
        if (ModList.get().isLoaded("tensura")) {
            boolean blood = ModList.get().isLoaded("vampirism");
            if (blood) {
                BloodInFoodSpot.register();
            }
            TensuraHudFlags.register(AbilityHudLayer::shown, blood ? BloodInFoodSpot::hidesTensuraFood : () -> false);
        }
        KeyCover.register();
        KeyHiding.register();
    }
}
