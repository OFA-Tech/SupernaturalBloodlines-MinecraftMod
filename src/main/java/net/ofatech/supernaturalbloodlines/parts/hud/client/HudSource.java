package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hud.DevHud;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/**
 * Everything the HUD layer draws from, in one place: the slots, the mode, the Alt panel, Bite's blood, the moon warning and the tracked task. The layer
 * asks {@link #current()} once a frame and never learns where the data comes from: {@link #LIVE} reads the game, {@link #DEMO} (development screenshots
 * only, while {@link DevHud#on}) makes it up.
 */
interface HudSource {
    SlotView[] views();

    /** The mode the pill names; null when there is no HUD. */
    SlotMode mode();

    /** The mode the panel shows, or null for no panel (only one mode is installed). */
    SlotMode other();

    SlotView[] otherViews(SlotMode other);

    /** True while Alt is held (or the demo says so): the tooltips and the panel show. */
    boolean alt();

    /** The share (0 to 1) of the blood ring round Bite, or a negative number for none. */
    float blood();

    /** Ticks until the forced shift when it is near, else 0. */
    long moonTicks();

    Optional<TrackerAccess.Line> tracked();

    /** The lowest pixel of the boss bars (0 for none). */
    int bossBottom();

    /** Whether the texts of the tooltips may be kept from a frame to the next (the live game changes once a tick; the demo is fixed). */
    default boolean cacheable() {
        return true;
    }

    HudSource LIVE = new LiveHud();
    HudSource DEMO = new HudDemo();

    static HudSource current() {
        return DevHud.on ? DEMO : LIVE;
    }
}
