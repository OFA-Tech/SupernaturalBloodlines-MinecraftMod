package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Where vanilla's boss bars end, read from the event that vanilla fires for each bar. Our layer is drawn before the boss overlay in the same frame, so the
 * value is last frame's (one frame late is fine for a pill that only has to stay clear of them).
 */
final class BossBars {
    private static int seen;
    private static int bottom;

    private BossBars() {
    }

    static void register() {
        // cancelled ones too: our own cards are cancelled and carry the height of the whole stack as their increment
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.NORMAL, true, CustomizeGuiOverlayEvent.BossEventProgress.class, e -> seen = Math.max(seen, e.getY() + e.getIncrement()));
    }

    /** The lowest pixel the boss bars used last frame, 0 with none. Call once per frame. */
    static int bottom() {
        bottom = seen;
        seen = 0;
        return bottom;
    }
}
