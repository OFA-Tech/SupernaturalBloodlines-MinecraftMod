package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;

/**
 * The Tensura expansion's geometry: Tensura's framed window made wider, a navy list on the left with the pages from the top and the
 * mods from the bottom, and two panels ({@link HubLayout#MAIN}, {@link HubLayout#SIDE}). Pure.
 */
public final class TensuraHubLayout {
    static final int MAX_W = 470;
    static final int MAX_H = 270;
    static final int MARGIN = 6;
    static final int INSET = 6;
    static final int ROW_H = 16;
    static final int ROW_STEP = 18;
    static final int PANEL_PAD = 6;

    private TensuraHubLayout() {
    }

    public static HubLayout of(int width, int height, int tabs, int seals) {
        int w = Math.min(MAX_W, width - 2 * MARGIN);
        int h = Math.min(MAX_H, height - 2 * MARGIN);
        Rect frame = new Rect((width - w) / 2, (height - h) / 2, w, h);
        Rect nav = nav(frame);
        List<Rect> pages = new ArrayList<>();
        for (int i = 0; i < tabs; i++) {
            pages.add(new Rect(nav.x() + 4, nav.y() + 5 + i * ROW_STEP, nav.w() - 8, ROW_H));
        }
        List<Rect> mods = new ArrayList<>();
        for (int i = 0; i < seals; i++) {
            mods.add(new Rect(nav.x() + 4, nav.bottom() - 5 - (seals - i) * ROW_STEP + 2, nav.w() - 8, ROW_H));
        }
        int left = nav.right() + INSET;
        int inner = frame.right() - INSET - left;
        int mainW = (inner - INSET) * 20 / 43; // the mockup's 1 : 1.15
        Rect main = new Rect(left, nav.y(), mainW, nav.h());
        Rect side = new Rect(left + mainW + INSET, nav.y(), inner - mainW - INSET, nav.h());
        return new HubLayout(frame, Map.of(HubLayout.MAIN, main.inset(PANEL_PAD), HubLayout.SIDE, side.inset(PANEL_PAD)), pages, mods);
    }

    static Rect nav(Rect frame) {
        int navW = Math.max(72, Math.min(110, frame.w() / 4));
        return new Rect(frame.x() + INSET, frame.y() + INSET, navW, frame.h() - 2 * INSET);
    }

    /** The panel behind a content region. */
    static Rect panel(Rect region) {
        return region.inset(-PANEL_PAD);
    }
}
