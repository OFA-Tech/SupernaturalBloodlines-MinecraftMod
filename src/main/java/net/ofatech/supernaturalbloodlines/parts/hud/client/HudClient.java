package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.bus.api.IEventBus;

/** Client registrations of the HUD part. Called only from {@code HudPart} behind a {@code Dist.CLIENT} check. */
public final class HudClient {
    private HudClient() {}

    public static void register(IEventBus modBus) {
    }
}
