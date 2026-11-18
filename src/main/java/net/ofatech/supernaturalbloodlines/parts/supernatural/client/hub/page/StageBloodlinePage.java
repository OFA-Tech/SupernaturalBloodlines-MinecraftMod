package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineColors;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTreeLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * The Blood Moon's and Tensura's Bloodline: the same tree as the Grimoire's, placed by the theme (a chain under the moon with the Original crowned
 * inside it, or hexes in a status window), the selection's detail in the theme's own bar or pane, and the genes where the look has room for them.
 */
public final class StageBloodlinePage extends LinePageBase {
    public enum Look { MOON, TENSURA }

    private static final int RED = LineColors.MOON_RED;
    private static final int RED_DIM = LineColors.atLeast(LineColors.MOON_THREAD, LineColors.MOON_SKY_TOP, LineColors.MIN_RATIO);
    private static final int MOON_RING = LineColors.atLeast(LineColors.MOON_RING, LineColors.MOON_SKY_TOP, LineColors.MIN_RATIO);
    private final Look look;
    /** The x of the leftmost faint thread, which the margin note must stay clear of. */
    private int noteEdge = Integer.MAX_VALUE;

    public StageBloodlinePage(Look look) {
        this.look = look;
    }

    @Override
    public void render(HubScreen.Frame f) {
        LineTree.Model m = prepare(f);
        HubTheme t = f.theme();
        boolean moon = look == Look.MOON;
        Rect region = f.region(moon ? BookKit.MOON : BookKit.FULL);
        Rect detail = f.layout().chrome().detail();
        if (moon) {
            BookKit.moonHead(f, Component.translatable(LinePageSupport.KEY + "title"), Component.translatable(PageKit.HUB + "book.bloodline.subtitle"));
        }
        if (m.state() == LineRules.State.NOT_VAMPIRE) {
            int y = f.begin(moon ? BookKit.MOON : BookKit.FULL);
            f.end(EmptyState.render(f, region, y, "bloodline", HubPage.GUIDE, HubPage.PACK));
            return;
        }
        Rect stage = region;
        Rect pane = detail != null ? detail : new Rect(stage.right(), stage.y(), 0, stage.h());
        Map<String, Rect> rects = moon ? LineTreeLayout.moon(stage, detail != null ? detail : new Rect(0, stage.bottom(), 1, 1), m) : LineTreeLayout.tensura(stage, pane, m);
        draw(f, m, rects, stage, t, moon);
        Rect vials = moon ? null : tensuraVials(stage, pane, rects);
        // Tensura puts the genes in the info pane when the stage has no room for the vials; the Blood Moon says them in one line of the stage
        f.detail(LinePageSupport.detail(f, m, selectedNode(m), !moon && vials == null));
        if (moon) {
            moonGenes(f, stage, rects);
        } else if (vials != null) {
            tensuraGenes(f, stage, vials);
        }
    }

    private void draw(HubScreen.Frame f, LineTree.Model m, Map<String, Rect> rects, Rect stage, HubTheme t, boolean moon) {
        int line = moon ? RED_DIM : t.muted();
        int hot = moon ? RED : t.accent();
        noteEdge = Integer.MAX_VALUE;
        Rect you = rects.get(LineTree.YOU);
        for (LineTree.Link l : m.links()) {
            Rect fu = rects.get(l.upper());
            Rect fl = rects.get(l.lower());
            if (l.style() == LineTree.LinkStyle.FAINT && fu != null && fl != null && you != null) {
                int tx = fu.x() + fu.w() / 2 + (fl.x() + fl.w() / 2 < fu.x() + fu.w() / 2 ? -fu.w() / 2 - 6 : fu.w() / 2 + 6);
                if (tx < you.x() + you.w() / 2) {
                    noteEdge = Math.min(noteEdge, tx);
                }
            }
        }
        for (LineTree.Link l : m.links()) {
            Rect up = rects.get(l.upper());
            Rect low = rects.get(l.lower());
            if (up == null || low == null) {
                continue;
            }
            boolean kid = m.node(l.lower()).filter(n -> n.kind() == LineTree.Kind.DESCENDANT).isPresent();
            if (l.style() == LineTree.LinkStyle.FAINT) {
                int x = up.x() + up.w() / 2 + (low.x() + low.w() / 2 < up.x() + up.w() / 2 ? -up.w() / 2 - 6 : up.w() / 2 + 6);
                LinePageSupport.dotted(f.g, x, up.bottom() - up.h() / 2, x, low.y() - 6, line);
                LinePageSupport.dotted(f.g, x, low.y() - 6, low.x() + low.w() / 2, low.y(), line);
            } else if (!kid) {
                link(f, l, up, low, m, moon ? RED : t.accent(), line, stage, t);
            }
        }
        LinePageSupport.bracket(f.g, rects.get(LineTree.YOU), LinePageSupport.kidRects(m, rects), moon ? hot : t.accent());
        for (LineTree.Node n : m.nodes()) {
            node(f, m, n, rects.get(n.id()), stage, t, moon);
        }
    }

    private void link(HubScreen.Frame f, LineTree.Link l, Rect up, Rect low, LineTree.Model m, int solid, int dim, Rect stage, HubTheme t) {
        int cx = up.x() + up.w() / 2;
        int y0 = up.bottom();
        int y1 = low.y();
        if (l.style() == LineTree.LinkStyle.BROKEN) {
            int mid = (y0 + y1) / 2;
            for (int y = y0; y < y1; y += 4) {
                if (Math.abs(y - mid) > 3) {
                    f.g.fill(cx, y, cx + 1, Math.min(y + 2, y1), RED);
                }
            }
            // the break: two short slanted strokes across the gap
            f.g.fill(cx - 3, mid - 3, cx - 1, mid - 2, RED);
            f.g.fill(cx - 1, mid - 2, cx + 1, mid - 1, RED);
            f.g.fill(cx + 1, mid - 1, cx + 3, mid, RED);
            f.g.fill(cx - 3, mid + 1, cx - 1, mid + 2, RED);
            f.g.fill(cx - 1, mid + 2, cx + 1, mid + 3, RED);
            String tag = Component.translatable(LinePageSupport.KEY + (look == Look.MOON ? "margin.broken" : "severed_label")).getString();
            int w = f.font().width(tag) + 8;
            Rect hit = new Rect(cx + 8, mid - 6, w, 12);
            if (look == Look.TENSURA) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), 0xFF2a0f16);
                f.g.renderOutline(hit.x(), hit.y(), hit.w(), hit.h(), RED);
                f.g.drawString(f.font(), tag, hit.x() + 4, hit.y() + 2, RED, false);
            } else {
                // the label sits left of the link: when the stage has no room there it is left out (the tooltip has it)
                int edge = Math.min(cx - 10, noteEdge - 4);
                int room = edge - stage.x();
                if (room >= 24) {
                    String shown = fit(f, Component.literal(tag), room);
                    f.g.drawString(f.font(), shown, edge - f.font().width(shown), mid - 4, RED, false);
                    hit = new Rect(edge - 2 - f.font().width(shown), mid - 6, f.font().width(shown) + 16, 12);
                } else {
                    hit = new Rect(cx - 8, mid - 6, 16, 12);
                }
            }
            f.target("line:break", hit, List.of(LinePageSupport.breakTooltip(LinePageSupport.origin(f))), () -> selected = LineTree.YOU);
            if (f.focused("line:break")) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            return;
        }
        boolean ghost = m.node(l.upper()).filter(LineTree.Node::hidden).isPresent();
        if (ghost) {
            LinePageSupport.dotted(f.g, cx, y0, cx, y1, dim);
        } else {
            LinePageSupport.vertical(f.g, cx, y0, y1, solid);
        }
    }

    private void node(HubScreen.Frame f, LineTree.Model m, LineTree.Node n, Rect r, Rect stage, HubTheme t, boolean moon) {
        boolean you = n.kind() == LineTree.Kind.YOU;
        boolean crowned = n.kind() == LineTree.Kind.ROOT || (you && m.state() == LineRules.State.ORIGINAL);
        Shapes.Shape shape = moon ? Shapes.Shape.ROUND : Shapes.Shape.HEX;
        int border = you ? (moon ? RED : t.accent()) : crowned ? LineColors.gold(t) : n.hidden() ? t.muted() : (moon ? MOON_RING : t.muted());
        if (n.id().equals(selected)) {
            KitDraw.shape(f.g, shape, r.x() - 2, r.y() - 2, r.w() + 4, t.focusColor());
        }
        LinePageSupport.portrait(f, n, r, shape, border, t.paper());
        if (crowned) {
            LinePageSupport.crown(f, r, LineColors.gold(t));
        }
        if (you && m.padlock()) {
            LinePageSupport.padlock(f, r, moon ? RED : t.accent(), t.paper());
        }
        Rect hit;
        if (n.kind() == LineTree.Kind.DESCENDANT) {
            String name = fit(f, LinePageSupport.name(n), 90);
            f.g.drawString(f.font(), name, r.x() + r.w() / 2 - f.font().width(name) / 2, r.bottom() + 3, t.ink(), false);
            String cap = fit(f, Component.translatable(LinePageSupport.KEY + "cap.down"), 90);
            f.g.drawString(f.font(), cap, r.x() + r.w() / 2 - f.font().width(cap) / 2, r.bottom() + 12, t.muted(), false);
            hit = new Rect(r.x() - 14, r.y(), r.w() + 28, r.h() + 22);
        } else {
            int x = r.right() + 12; // the faint threads run 6 px off the ring: the text starts clear of them
            int room = LineTreeLayout.labelRoom(stage, r) - 12;
            f.g.drawString(f.font(), fit(f, LinePageSupport.name(n), room), x, r.y() + r.h() / 2 - 9, you ? (moon ? RED : t.accent()) : t.ink(), false);
            f.g.drawString(f.font(), fit(f, LinePageSupport.caption(f, m, n), room), x, r.y() + r.h() / 2, t.muted(), false);
            hit = new Rect(r.x() - 2, r.y() - 2, Math.max(r.w() + 4, Math.min(160, room) + r.w() + 12), r.h() + 4);
        }
        nodeTarget(f, m, n, hit);
    }

    /** The Blood Moon says the genes in one line at the top right of the stage, and the red thread's legend in at most two lines at the bottom right. */
    private void moonGenes(HubScreen.Frame f, Rect stage, Map<String, Rect> rects) {
        Rect you = rects.get(LineTree.YOU);
        int x = Math.max(you.right() + 120, stage.x() + stage.w() * 62 / 100);
        int w = stage.right() - x;
        if (w < 60) {
            return;
        }
        String genes = fit(f, BloodlineGenes.summary(f.view.snapshot()), w);
        f.g.drawString(f.font(), genes, stage.right() - f.font().width(genes), stage.y() + 4, f.theme().muted(), false);
        var origin = LinePageSupport.origin(f);
        if (origin.state() == LineRules.State.IN_LINE || origin.freedKind() == net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.FreedKind.BROKEN_LINE) {
            var lines = f.font().split(Component.translatable(LinePageSupport.KEY + "thread"), w - 34);
            int shown = Math.min(2, lines.size());
            int ty = stage.bottom() - shown * 9 - 2;
            f.g.fill(x, ty + 4, x + 26, ty + 5, RED);
            for (int i = 0; i < shown; i++) {
                f.g.drawString(f.font(), lines.get(i), x + 32, ty + i * 9, f.theme().muted(), false);
            }
        }
    }

    /** Tensura: the vials in the free column between the tree and the pane. */
    private static Rect tensuraVials(Rect stage, Rect pane, Map<String, Rect> rects) {
        int treeRight = rects.values().stream().mapToInt(Rect::right).max().orElse(stage.x()) + 140;
        int x = Math.max(treeRight, stage.x() + stage.w() * 56 / 100);
        int w = Math.min(pane.x() - 8, stage.right()) - x;
        return w < 60 ? null : new Rect(x, stage.y() + 6, w, BloodlineGenes.HEIGHT);
    }

    private void tensuraGenes(HubScreen.Frame f, Rect stage, Rect area) {
        int x = area.x();
        String head = Component.translatable(LinePageSupport.KEY + "genes").getString().toUpperCase(java.util.Locale.ROOT);
        f.g.drawString(f.font(), head, x, stage.y() - 4, f.theme().muted(), false);
        BloodlineGenes.draw(f, area, stage.y() + 8, f.view.snapshot());
    }
}
