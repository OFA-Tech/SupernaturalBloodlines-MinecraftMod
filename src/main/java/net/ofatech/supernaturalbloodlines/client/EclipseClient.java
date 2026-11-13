package net.ofatech.supernaturalbloodlines.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The red tint of a lunar eclipse, on the overworld's fog and horizon. Client only: nothing but the payload handler in
 * ModNetwork refers to this class. NeoForge 1.21.1 has no event for the sky dome's own colour, so the zenith is not tinted.
 */
public final class EclipseClient {
    private static final float STRENGTH = 0.65f;
    private static final float[] RED = {0.55f, 0.03f, 0.03f};
    private static boolean active;
    private static boolean listening;

    private EclipseClient() {}

    public static void set(boolean on) {
        active = on;
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFogColor e) -> {
                if (active && Minecraft.getInstance().level != null && Minecraft.getInstance().level.dimension() == Level.OVERWORLD) {
                    e.setRed(mix(e.getRed(), RED[0]));
                    e.setGreen(mix(e.getGreen(), RED[1]));
                    e.setBlue(mix(e.getBlue(), RED[2]));
                }
            });
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> active = false);
        }
    }

    private static float mix(float from, float to) {
        return from + (to - from) * STRENGTH;
    }
}
