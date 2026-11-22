package net.ofatech.supernaturalbloodlines.shared.hud;

/**
 * The colours of the text the HUD puts on the kit's pictures: the
 * letter on a key badge, the text of the Alt labels and of the mode pill. Each text colour reads at 4.5:1 or more on every pixel inside its picture ({@code HudContrastTest}). Pure.
 */
public final class HudFaces {
    private HudFaces() {
    }

    /** The key's letter on that face: cream on the wax, gold on the dark plate, dark on the teal tab. */
    public static int keyLetter(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFFF4E2C0;
            case BLOOD_MOON -> 0xFFFFD36B;
            case TENSURA -> 0xFF0A1A28;
        };
    }

    public static int pillText(HudKit k) {
        return k == HudKit.GRIMOIRE ? 0xFFF4E2C0 : k.ink();
    }

    /** The first line of a label: the slot's name, in the theme's accent. */
    public static int labelName(HudKit k) {
        return k.accent();
    }

    public static int labelBody(HudKit k) {
        return k.muted();
    }

    /** Why a slot cannot be used. */
    public static int labelWarn(HudKit k) {
        return k == HudKit.GRIMOIRE ? 0xFF9A1C22 : 0xFFFF7A70;
    }
}