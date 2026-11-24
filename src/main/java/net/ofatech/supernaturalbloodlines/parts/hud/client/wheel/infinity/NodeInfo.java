package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;
import org.jetbrains.annotations.Nullable;

/**
 * One power of a stone as the wheel needs it, read from Infinity Stones' own state and tree: {@code stone} is his stone ordinal and {@code node} the node id (what his
 * {@code WheelPayloads.Use} takes), {@code levels} is non-null for a power with a setting changed by scroll, {@code menu} is the menu his wheel opens for it and
 * {@code menuOnLeft} says that his plain click is that menu (the Teleport GUI).
 */
public record NodeInfo(int stone, String stoneKey, String node, String name, String description, boolean unlocked, boolean toggled, int setting, boolean isSwitch, boolean isChannel,
                       boolean needsAim, boolean isDual, @Nullable Levels levels, StoneMenus.Kind menu, boolean menuOnLeft) {
    /** The setting of a power with levels: {@code min..max}, moved by {@code step} a notch. */
    public record Levels(int min, int max, int step) {
    }

    public String modeId() {
        return stoneKey + "." + node;
    }
}
