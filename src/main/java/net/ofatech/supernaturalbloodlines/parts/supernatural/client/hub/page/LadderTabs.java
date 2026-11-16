package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * A player with two ladders (a hybrid with a witch's, a tribrid) sees one at a time: a bookmark per ladder, the chosen one filled, a small dot on a bookmark
 * whose ladder has a task ready to claim. SP from both go into one pool. With one ladder there is nothing to draw.
 */
final class LadderTabs {
    private LadderTabs() {
    }

    /** The ladders to show: the chosen one when there are several, else all (there is at most one). */
    static List<HubModel.LadderView> shown(HubScreen.Frame f) {
        List<HubModel.LadderView> all = f.view.ladders();
        if (all.size() < 2) {
            return all;
        }
        return List.of(all.get(f.screen().ladderIndex(all.size())));
    }

    /** True when a ladder has a task ready to claim: the dot on its bookmark. */
    static boolean ready(HubModel.LadderView ladder) {
        return ladder.tasks().stream().anyMatch(r -> r.state() == TaskState.CLAIMABLE);
    }

    /**
     * Where the bookmarks go in {@code maxWidth}: one small tab per ladder, or, when they would be narrower than 34 pixels, a single tab that turns to the
     * next ladder when pressed. Pure, so a test can keep them off the margin note.
     */
    static List<Rect> layout(int x, int y, int maxWidth, int count) {
        List<Rect> out = new ArrayList<>();
        int each = (maxWidth - 4 * (count - 1)) / Math.max(1, count);
        if (each < 34) {
            out.add(new Rect(x, y, Math.max(20, Math.min(maxWidth, 70)), 13));
            return out;
        }
        each = Math.min(60, each);
        for (int i = 0; i < count; i++) {
            out.add(new Rect(x + i * (each + 4), y, each, 13));
        }
        return out;
    }

    /** Draws the bookmarks from {@code (x, y)}, within {@code width}; returns the height used (0 with one ladder). */
    static int render(HubScreen.Frame f, int x, int y, int width) {
        List<HubModel.LadderView> all = f.view.ladders();
        if (all.size() < 2) {
            return 0;
        }
        HubTheme t = f.theme();
        int chosen = f.screen().ladderIndex(all.size());
        List<Rect> rects = layout(x, y, width, all.size());
        boolean cycle = rects.size() == 1;
        for (int k = 0; k < rects.size(); k++) {
            int i = cycle ? chosen : k;
            Rect r = rects.get(k);
            boolean on = cycle || i == chosen;
            int fill = on ? t.accent() : t.paper();
            f.g.fill(r.x(), r.y(), r.right(), r.bottom(), t.accent());
            f.g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, on || f.hovered(r) ? (on ? fill : (t.accent() & 0xFFFFFF) | 0x33000000) : fill);
            String name = PageKit.race(all.get(i).race()).getString();
            String label = LabelFit.cut(name + (cycle ? " \u203a" : ""), r.w() - 8, f.font()::width);
            f.g.drawString(f.font(), label, r.x() + (r.w() - f.font().width(label)) / 2, r.y() + 3, on ? Contrast.bestText(fill) : t.accent(), false);
            if (ready(all.get(i)) || (cycle && all.stream().anyMatch(LadderTabs::ready))) {
                f.g.fill(r.right() - 5, r.y() - 1, r.right() + 1, r.y() + 5, t.good());
            }
            int idx = i;
            String id = "ladder:" + k;
            List<Component> tip = ready(all.get(i)) ? List.of(PageKit.race(all.get(i).race()), Component.translatable(PageKit.HUB + "ladder.ready")) : List.of(PageKit.race(all.get(i).race()));
            f.target(id, r, tip, () -> f.screen().setLadder(cycle ? (idx + 1) % all.size() : idx));
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, r, t.focusColor());
            }
        }
        return 17;
    }
}