package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Blood Moon's geometry: the whole screen, a rail of page icons on the left and of launchers on the right, the moon band at the top
 * (region {@link #MOON}) and two cards under it ({@link HubLayout#MAIN} left, {@link HubLayout#SIDE} right). Pure.
 */
public final class BloodMoonLayout {
    /** The band the moon and its phases are drawn in. */
    public static final String MOON = "moon";
    static final int RAIL = 22;
    static final int RAIL_STEP = 26;
    static final int EDGE = 6;
    static final int TOP = 36;
    static final int GAP = 6;
    static final int MOON_MAX = 112;

    private BloodMoonLayout() {
    }

    public static HubLayout of(int width, int height, int tabs, int seals) {
        Rect screen = new Rect(0, 0, width, height);
        List<Rect> rail = new ArrayList<>();
        for (int i = 0; i < tabs; i++) {
            rail.add(new Rect(EDGE, TOP + i * RAIL_STEP, RAIL, RAIL));
        }
        List<Rect> portals = new ArrayList<>();
        for (int i = 0; i < seals; i++) {
            portals.add(new Rect(width - EDGE - RAIL, TOP + i * RAIL_STEP, RAIL, RAIL));
        }
        int left = EDGE + RAIL + GAP;
        int right = width - EDGE - RAIL - GAP;
        int moonH = Math.min(MOON_MAX, (height - 2 * EDGE) * 2 / 5);
        Rect moon = new Rect(left, EDGE, right - left, moonH);
        int cardsTop = moon.bottom() + GAP;
        int cardsH = height - EDGE - cardsTop;
        int mainW = (right - left - GAP) * 11 / 20;
        // the cards' text sits inside a 5 px border, so the regions are inset by it
        Rect main = new Rect(left, cardsTop, mainW, cardsH).inset(5);
        Rect side = new Rect(left + mainW + GAP, cardsTop, right - left - mainW - GAP, cardsH).inset(5);
        Rect themeButton = new Rect(width - EDGE - RAIL, EDGE, RAIL, RAIL); // top right, above the launcher rail
        return new HubLayout(screen, Map.of(MOON, moon, HubLayout.MAIN, main, HubLayout.SIDE, side), rail, portals,
                new HubLayout.Chrome(themeButton, null, moon, null, HubLayout.DetailKind.SIDE_REGION, HubLayout.NavStyle.ICON_RAIL));
    }

    /** Where the SP and EP counters go: the bottom right of the moon band, two lines (the top right is the toasts'). */
    public static Rect counters(Rect band) {
        return new Rect(band.right() - 90, band.bottom() - 20, 88, 20);
    }

    /** The card behind a content region. */
    static Rect card(Rect region) {
        return region.inset(-5);
    }
}
