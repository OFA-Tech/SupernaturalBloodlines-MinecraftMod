package net.ofatech.supernaturalbloodlines.shared.hud;

/**
 * The colours of the text the HUD puts on the kit's pictures, and the colours of those pictures (the middle of each, held equal to the PNGs by a test): the
 * letter on a key badge, the text of the Alt labels and of the mode pill. Each text colour reads at 4.5:1 or more on every pixel inside its picture ({@code HudContrastTest}). Pure.
 */
public final class HudFaces {
    private HudFaces() {
    }

    /** The middle of the key badge's picture. */
    public static int keyFace(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFF9A1C22;
            case BLOOD_MOON -> 0xFF10060A;
            case TENSURA -> 0xFF3FD0D6;
        };
    }

    /** The key's letter on that face: cream on the wax, gold on the dark plate, dark on the teal tab. */
    public static int keyLetter(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFFF4E2C0;
            case BLOOD_MOON -> 0xFFFFD36B;
            case TENSURA -> 0xFF0A1A28;
        };
    }

    /** The middle of {@code hud_label} (and of {@code plate}): the parchment, or the dark card. */
    public static int labelFace(HudKit k) {
        return switch (k) {
            case GRIMOIRE -> 0xFFE9DDB8;
            case BLOOD_MOON -> 0xFF1D0B13;
            case TENSURA -> 0xFF12293F;
        };
    }

    /** The middle of {@code hud_pill}: a dark red leather in the Grimoire, the same card as the label elsewhere. */
    public static int pillFace(HudKit k) {
        return k == HudKit.GRIMOIRE ? 0xFF4A1717 : labelFace(k);
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