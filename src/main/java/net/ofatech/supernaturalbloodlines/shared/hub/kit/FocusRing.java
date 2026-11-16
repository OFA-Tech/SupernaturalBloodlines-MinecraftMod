package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;

/** The keyboard focus ring: gold on a dark paper, deep red on a light one, so it is 3:1 or more against the paper either way. Pure. */
public final class FocusRing {
    public static final int GOLD = 0xFFE8B84A;
    public static final int DEEP_RED = 0xFF8A1C1C;
    /** WCAG's minimum for a user-interface outline. */
    public static final double MIN_RATIO = 3.0;

    private FocusRing() {
    }

    /** The ring colour that reads on {@code paper}. */
    public static int forPaper(int paper) {
        return Contrast.ratio(GOLD, paper) >= Contrast.ratio(DEEP_RED, paper) ? GOLD : DEEP_RED;
    }
}
