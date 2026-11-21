package net.ofatech.supernaturalbloodlines.shared.supernatural;

/**
 * The plate a rite's circle picture is drawn on, in the hub and in Enchanted's book. Enchanted's pictures are white (ritual), dark red (infernal) and dark
 * purple (otherwhere) chalk on nothing at all: white needs a dark plate, the two dark chalks need a light one, and the six-pixel gold heart glyph then
 * gets a small dark square of its own. Every pairing here is at least 3:1 (WCAG contrast; a test checks it). Pure.
 */
public final class RitePlate {
    public static final int DARK = 0xFF16141C;
    public static final int LIGHT = 0xFFE8E2D0;
    /** The darkest opaque pixel of the gold heart glyph picture. */
    public static final int GLYPH = 0xFFB17F00;
    public static final double MIN_CONTRAST = 3.0;

    private RitePlate() {
    }

    /** The pixel of the chalk's picture that reads worst on its plate: the dimmest of the white, the lightest of the dark ones. */
    public static int chalkColor(String chalk) {
        return switch (chalk) {
            case "nether_chalk" -> 0xFF992F2F;
            case "otherwhere_chalk" -> 0xFF512689;
            default -> 0xFFE0E0E0;
        };
    }

    public static int plate(String chalk) {
        return switch (chalk) {
            case "nether_chalk", "otherwhere_chalk" -> LIGHT;
            default -> DARK;
        };
    }

    /** Gold on a light plate is under 3:1: the glyph gets a dark square behind it. */
    public static boolean glyphNeedsBacking(String chalk) {
        return plate(chalk) == LIGHT;
    }

    /** What the glyph sits on: its backing on a light plate, else the plate. */
    public static int glyphGround(String chalk) {
        return glyphNeedsBacking(chalk) ? DARK : plate(chalk);
    }

    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    public static double luminance(int argb) {
        return 0.2126 * channel(argb >> 16 & 0xFF) + 0.7152 * channel(argb >> 8 & 0xFF) + 0.0722 * channel(argb & 0xFF);
    }

    public static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }
}
