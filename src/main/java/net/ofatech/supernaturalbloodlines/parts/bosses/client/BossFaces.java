package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitFaces;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;

/**
 * The colours of the text a boss bar puts on the kit's pictures: the boss's name on its plate, the health on the card, and the chips' words on the chips. Each reads
 * at 4.5:1 or more on every pixel inside its picture ({@code BossFacesTest} reads the PNGs). Pure.
 */
public final class BossFaces {
    private BossFaces() {
    }

    /** The name: cream on the Grimoire's dark red plate, the theme's ink on the other two (their plates are dark too). */
    public static int name(HudKit k) {
        return k == HudKit.GRIMOIRE ? 0xFFF4E2C0 : k.ink();
    }

    /** The health in numbers, on the card. */
    public static int health(HudKit k) {
        return k.muted();
    }

    /** The word on a chip: the theme's ink when it reads on the chip's face, else white or black. */
    public static int chip(HudKit k, Part chip) {
        return KitFaces.label(k.kit(), chip, k.ink());
    }
}
