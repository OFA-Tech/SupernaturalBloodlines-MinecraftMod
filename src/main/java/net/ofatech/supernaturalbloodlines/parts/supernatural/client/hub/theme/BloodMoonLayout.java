package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * The Blood Moon's geometry for a screen of {@code width x height} GUI pixels: a round icon rail down the left (a label flag shows beside the
 * chosen one when there is room), a round launcher rail down the right, the page title top left, the points and the look button and the close
 * mark top right, a stage in the middle for the moon, a card column on the right ({@link #RIGHT}) that Path and Pack use, two full height cards
 * ({@link HubLayout#MAIN} and {@link HubLayout#SIDE}) for the list pages, and a floating detail bar along the bottom ({@link Chrome#detail}).
 * Pure.
 */
public final class BloodMoonLayout {
    public static final String MOON = "moon";
    public static final String RIGHT = "right";
    public static final String HEAD = "head";
    static final int RAIL = 22;
    static final int EDGE = 6;
    static final int TOP = 40;
    static final int GAP = 6;
    static final int BAR_H = 38;
    /** Below this many GUI pixels a rail button's label flag would sit on the content, so it is left out. */
    static final int FLAG_MIN_WIDTH = 560;

    private BloodMoonLayout() {
    }

    public static HubLayout of(int width, int height, int tabs, int seals) {
        Rect screen = new Rect(0, 0, width, height);
        int step = Math.min(28, tabs <= 1 ? 28 : (height - TOP - EDGE - RAIL) / (tabs - 1));
        List<Rect> rail = new ArrayList<>();
        for (int i = 0; i < tabs; i++) {
            rail.add(new Rect(EDGE, TOP + i * step, RAIL, RAIL));
        }
        Rect close = new Rect(width - EDGE - 14, EDGE + 4, 14, 14);
        Rect themeButton = new Rect(close.x() - 6 - RAIL, EDGE, RAIL, RAIL);
        Rect counters = new Rect(themeButton.x() - 8 - 118, EDGE + 7, 118, 10);
        List<Rect> portals = new ArrayList<>();
        int launcherTop = TOP + 36;
        int launcherStep = Math.min(26, seals <= 1 ? 26 : (height - launcherTop - EDGE - 20) / (seals - 1));
        for (int i = 0; i < seals; i++) {
            portals.add(new Rect(width - EDGE - 20, launcherTop + i * launcherStep, 20, 20));
        }
        int left = EDGE + RAIL + 12 + (width >= FLAG_MIN_WIDTH ? 40 : 0); // wide screens leave room for the label flag of the chosen button
        int right = width - EDGE - RAIL - 12;
        Rect head = new Rect(left, EDGE, Math.max(60, counters.x() - left - 8), 30);
        Rect bar = new Rect(left + 8, height - EDGE - BAR_H, right - left - 16, BAR_H);
        int stageTop = EDGE + 30;
        Rect stage = new Rect(left, stageTop, right - left, bar.y() - GAP - stageTop);
        int colW = Math.min(176, (right - left) * 36 / 100);
        Rect col = new Rect(right - colW, stageTop + 2, colW, stage.h() - 2);
        int half = (right - left - GAP) / 2;
        int fullTop = stageTop + 2;
        int fullH = height - EDGE - fullTop;
        Rect main = new Rect(left, fullTop, half, fullH).inset(5);
        Rect side = new Rect(left + half + GAP, fullTop, right - left - half - GAP, fullH).inset(5);
        return new HubLayout(screen, Map.of(MOON, stage, RIGHT, col.inset(5), HEAD, head, HubLayout.MAIN, main, HubLayout.SIDE, side), rail, portals,
                new HubLayout.Chrome(themeButton, close, stage, counters, bar));
    }

    /** The card behind a content region. */
    static Rect card(Rect region) {
        return region.inset(-5);
    }
}
