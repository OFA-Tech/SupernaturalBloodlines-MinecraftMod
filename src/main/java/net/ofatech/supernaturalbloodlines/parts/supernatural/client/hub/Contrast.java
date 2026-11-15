package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

/** WCAG contrast of two ARGB colours (alpha ignored): 1 to 21, text needs 4.5. Pure. */
public final class Contrast {
    private Contrast() {
    }

    public static double ratio(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    static double luminance(int argb) {
        return 0.2126 * channel(argb >> 16) + 0.7152 * channel(argb >> 8) + 0.0722 * channel(argb);
    }

    private static double channel(int v) {
        double c = (v & 0xFF) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
