package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;

/** How a filter or tab chip is drawn: the lit part is only for the chosen chip, the pointer over any other one draws an outline round it. Pure. */
public final class ChipLook {
    public record Look(Part part, boolean outline) {
    }

    private ChipLook() {
    }

    public static Look of(boolean chosen, boolean hovered) {
        return new Look(chosen ? Part.CHIP_ON : Part.CHIP, hovered && !chosen);
    }
}