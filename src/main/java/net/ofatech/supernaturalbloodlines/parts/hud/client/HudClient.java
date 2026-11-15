package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Client registrations of the HUD part. Called only from {@code HudPart} behind a {@code Dist.CLIENT} check. */
public final class HudClient {
    private HudClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> HudKeys.register(e));
        KeyCover.register();
        KeyHiding.register();
    }
}
