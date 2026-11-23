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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderGuiEvent.Pre e) -> net.ofatech.supernaturalbloodlines.shared.hud.HudPlacement.beginFrame());
        LoadoutAccess.install(new ClientLoadout());
        HudSlots.register();
        BossBars.register();
        net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess.installHud();
        HudInput.register();
        net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.WheelInput.register();
        modBus.addListener(net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.WheelInput::registerLayers);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> HudModeState.reset());
        if (ModList.get().isLoaded("tensura")) {
            boolean blood = ModList.get().isLoaded("vampirism");
            if (blood) {
                BloodInFoodSpot.register();
            }
            TensuraHudFlags.register(AbilityHudLayer::shown, blood ? BloodInFoodSpot::hidesTensuraFood : () -> false);
        }
        if (ModList.get().isLoaded("vampirism") && ModList.get().isLoaded("werewolves")) {
            net.ofatech.supernaturalbloodlines.parts.hud.client.werewolves.SprintLeapClient.register();
        }
        KeyCover.register();
        KeyHiding.register();
    }
}
