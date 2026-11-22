package net.ofatech.supernaturalbloodlines.shared.hud;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor;
import org.jetbrains.annotations.Nullable;

/**
 * What the HUD tells the request toast about where things stand: the corner the player chose for the toast and the box the tracked task's pill was drawn in.
 * The HUD part writes it each frame, the supernatural part reads it, so neither imports the other. Without the HUD part the toast keeps its default corner.
 */
public final class HudPlacement {
    private static volatile TrackerAnchor toastAnchor = TrackerAnchor.TOP_LEFT;
    private static volatile Box trackerBox;

    private HudPlacement() {
    }

    public static TrackerAnchor toastAnchor() {
        return toastAnchor;
    }

    public static void toastAnchor(TrackerAnchor anchor) {
        toastAnchor = anchor;
    }

    @Nullable
    public static Box trackerBox() {
        return trackerBox;
    }

    public static void trackerBox(@Nullable Box box) {
        trackerBox = box;
    }
}