package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;

/**
 * The colours of the text a boss bar puts on the kit's pictures: the boss's name on its plate and the health on the card (the chips' words are {@code KitFaces.label} of each chip, checked in the same test). Each reads
 * at 4.5:1 or more on every pixel inside its picture ({@code BossFacesTest} reads the PNGs). Pure.
 */
public final class BossFaces {
    private BossFaces() {
    }

    /** The name: cream on the Grimoire's dark red plate, the theme's ink on the other two (their plates are dark too). */
    public static int name(HudKit k) {
        return k == HudKit.GRIMOIRE ? 0xFFF4E2C0 : k.ink();
    }

    /**
     * The colours of a token's health ring: the filled part, the filled part while the boss shifts or lies fallen, and the empty track. They are the dominant colours of the kit's
     * own bar pictures (held equal to them by {@code BossFacesTest}) and stand out from the track at 3:1 or more, so the ring reads without colour names.
     */
    public static int ringFill(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFF8E1C22;
            case BLOOD_MOON -> 0xFFBF332B;
            case TENSURA -> 0xFF3FD0D6;
        };
    }

    public static int ringShifting(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFF6A4A0E;
            case BLOOD_MOON -> 0xFFFFD36B;
            case TENSURA -> 0xFFE8B33A;
        };
    }

    public static int ringTrack(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFFCBB985;
            case BLOOD_MOON -> 0xFF10060A;
            case TENSURA -> 0xFF06121F;
        };
    }

    /** The health in numbers, on the card. */
    public static int health(HudKit k) {
        return k.muted();
    }
}
