package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.minecraft.util.FormattedCharSequence;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineColors;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTreeLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;

/**
 * The Grimoire's Bloodline: the line in ink on the left page (the crowned Original at the top, "?" until discovered; a break as torn red ink with a
 * margin note; the sired as faint dotted threads back to the Original; a wax padlock on a freed player), the selected node's details and the genes in
 * vials on the right page.
 */
public final class GrimoireBloodlinePage extends LinePageBase {
    private static final String BOOK = PageKit.HUB + "book.bloodline.";

    @Override
    public void render(HubScreen.Frame f) {
        LineTree.Model m = prepare(f);
        tree(f, m);
        side(f, m);
    }

    private void tree(HubScreen.Frame f, LineTree.Model m) {
        HubTheme t = f.theme();
        Rect r = f.region(BookKit.MAIN);
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(LinePageSupport.KEY + "title"), Component.translatable(BOOK + "subtitle"));
        if (m.state() == LineRules.State.NOT_VAMPIRE) {
            EmptyState.render(f, r, y, "bloodline", HubPage.GUIDE, HubPage.PACK);
            f.end(r.bottom());
            return;
        }
        Rect area = new Rect(r.x(), y + 2, r.w(), r.bottom() - y - 2);
        int portrait = LineTreeLayout.portraitFor(area, m);
        Map<String, Rect> rects = LineTreeLayout.grimoire(area, m, portrait);
        // the faint threads first, so the ink lies over them
        for (LineTree.Link l : m.links()) {
            if (l.style() == LineTree.LinkStyle.FAINT) {
                faint(f, rects.get(l.upper()), rects.get(l.lower()), area, t);
            }
        }
        for (LineTree.Link l : m.links()) {
            Rect up = rects.get(l.upper());
            Rect low = rects.get(l.lower());
            if (l.style() == LineTree.LinkStyle.FAINT || low == null || up == null || m.node(l.lower()).filter(n -> n.kind() == LineTree.Kind.DESCENDANT).isPresent()) {
                continue;
            }
            int cx = up.x() + up.w() / 2;
            if (l.style() == LineTree.LinkStyle.BROKEN) {
                torn(f, up, low, area, t);
            } else if (m.node(l.upper()).filter(LineTree.Node::hidden).isPresent()) {
                LinePageSupport.dotted(f.g, cx, up.bottom(), cx, low.y(), t.muted());
            } else {
                LinePageSupport.vertical(f.g, cx, up.bottom(), low.y(), t.ink());
            }
        }
        LinePageSupport.bracket(f.g, rects.get(LineTree.YOU), LinePageSupport.kidRects(m, rects), t.ink());
        for (LineTree.Node n : m.nodes()) {
            node(f, m, n, rects.get(n.id()), area, t);
        }
        f.end(r.bottom());
    }

    private static void faint(HubScreen.Frame f, Rect root, Rect kid, Rect area, HubTheme t) {
        int cx = root.x() + root.w() / 2;
        boolean left = kid.x() + kid.w() / 2 < cx;
        int x = left ? cx - root.w() / 2 - 4 : cx + root.w() / 2 + 4;
        int turn = kid.y() - 8;
        LinePageSupport.dotted(f.g, x, root.bottom(), x, turn, t.muted());
        LinePageSupport.dotted(f.g, x, turn, kid.x() + kid.w() / 2, kid.y() - 1, t.muted());
    }

    /** A link torn in two: ragged red ink either side of a gap, the margin note and its tooltip. */
    private void torn(HubScreen.Frame f, Rect up, Rect low, Rect area, HubTheme t) {
        int cx = up.x() + up.w() / 2;
        int mid = (up.bottom() + low.y()) / 2;
        int c = t.accent();
        LinePageSupport.vertical(f.g, cx, up.bottom(), mid - 2, c);
        LinePageSupport.vertical(f.g, cx, mid + 3, low.y(), c);
        f.g.fill(cx - 2, mid - 3, cx, mid - 2, c);
        f.g.fill(cx + 1, mid - 2, cx + 3, mid - 1, c);
        f.g.fill(cx - 3, mid + 2, cx - 1, mid + 3, c);
        f.g.fill(cx + 1, mid + 3, cx + 3, mid + 4, c);
        int end = cx - up.w() / 2 - 12;
        int room = end - area.x();
        if (room > 24) {
            List<FormattedCharSequence> lines = f.font().split(Component.translatable(LinePageSupport.KEY + "margin.broken"), room);
            int ty = mid - lines.size() * 9 / 2;
            for (FormattedCharSequence line : lines) {
                f.g.drawString(f.font(), line, end - f.font().width(line), ty, c, false);
                ty += 9;
            }
            f.g.fill(end + 3, mid, cx - 4, mid + 1, c);
            f.g.fill(cx - 6, mid - 1, cx - 5, mid + 2, c);
        }
        Rect hit = new Rect(cx - 8, mid - 6, 16, 12);
        f.target("line:break", hit, List.of(LinePageSupport.breakTooltip(LinePageSupport.origin(f))), () -> selected = LineTree.YOU);
        if (f.focused("line:break")) {
            KitDraw.focusRing(f.g, hit, t.focusColor());
        }
    }

    private void node(HubScreen.Frame f, LineTree.Model m, LineTree.Node n, Rect r, Rect area, HubTheme t) {
        boolean you = n.kind() == LineTree.Kind.YOU;
        boolean crowned = n.kind() == LineTree.Kind.ROOT || (you && m.state() == LineRules.State.ORIGINAL);
        boolean selected = n.id().equals(this.selected);
        if (selected) {
            KitDraw.shape(f.g, Shapes.Shape.ROUND, r.x() - 2, r.y() - 2, r.w() + 4, t.accent() & 0xFFFFFF | 0x66000000);
        }
        int gold = LineColors.gold(t);
        int border = you ? t.accent() : crowned ? gold : n.hidden() ? t.muted() : t.ink();
        LinePageSupport.portrait(f, n, r, Shapes.Shape.ROUND, border, t.paper());
        if (crowned) {
            LinePageSupport.crown(f, r, gold);
        }
        if (you && m.padlock()) {
            LinePageSupport.padlock(f, r, t.accent(), t.paper());
        }
        Rect hit;
        if (n.kind() == LineTree.Kind.DESCENDANT) {
            String name = fit(f, LinePageSupport.name(n), 70);
            // a label is centred under its portrait but never past the page edge
            int nx = Math.max(area.x(), Math.min(area.right() - f.font().width(name), r.x() + r.w() / 2 - f.font().width(name) / 2));
            f.g.drawString(f.font(), name, nx, r.bottom() + 3, t.ink(), false);
            if (n.id().equals("line:down:" + (kidCount(m) - 1)) && m.hiddenBelow() > 0) {
                String more = Component.translatable(LinePageSupport.KEY + "more_sired", m.hiddenBelow()).getString();
                int mx = Math.max(area.x(), Math.min(area.right() - f.font().width(more), r.x() + r.w() / 2 - f.font().width(more) / 2));
                f.g.drawString(f.font(), more, mx, r.bottom() + 12, t.muted(), false);
            }
            hit = new Rect(r.x() - 8, r.y(), r.w() + 16, r.h() + 12);
        } else {
            int x = r.right() + 10;
            int room = LineTreeLayout.labelRoom(area, r) - 8;
            String name = fit(f, LinePageSupport.name(n), room);
            f.g.drawString(f.font(), name, x, r.y() + r.h() / 2 - 9, you ? t.accent() : t.ink(), false);
            f.g.drawString(f.font(), fit(f, LinePageSupport.caption(f, m, n, room), room), x, r.y() + r.h() / 2, t.muted(), false);
            hit = new Rect(r.x() - 2, r.y() - 2, Math.max(r.w() + 4, area.right() - r.x() + 2), r.h() + 4);
        }
        nodeTarget(f, m, n, hit);
    }

    private static int kidCount(LineTree.Model m) {
        return (int) m.nodes().stream().filter(n -> n.kind() == LineTree.Kind.DESCENDANT).count();
    }

    private void side(HubScreen.Frame f, LineTree.Model m) {
        HubTheme t = f.theme();
        Rect s = f.region(BookKit.SIDE);
        boolean none = m.state() == LineRules.State.NOT_VAMPIRE;
        Detail d = LinePageSupport.detail(f, m, selectedNode(m));
        int y = none ? BookKit.head(f, BookKit.SIDE, Component.translatable(LinePageSupport.KEY + "genes"), Component.translatable(BOOK + "genes_sub"))
                : BookKit.head(f, BookKit.SIDE, d.title(), d.subtitle());
        // a page too short for the seal and the tall vials (the 480 x 270 GUI) keeps every part by making each one smaller
        boolean tight = s.h() < TIGHT_BELOW;
        if (!none) {
            for (Detail.Field field : d.fields()) {
                if (field.label().equals(Component.translatable(LinePageSupport.KEY + "field.line")) && m.state() == LineRules.State.FREE) {
                    y = linePill(f, field, s, y + 1);
                } else {
                    y = BookKit.leader(f, field.label(), field.value(), s.x(), y + 1, s.w(), t.ink());
                }
            }
            for (Component body : d.body()) {
                if (tight) {
                    // two lines, the second ending with an ellipsis: the whole note is in the margin and the tooltip of the line
                    for (String line : net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.wrap(body.getString(), s.w(), 2, f.font()::width)) {
                        f.g.drawString(f.font(), line, s.x(), y + 3, t.muted(), false);
                        y += 9;
                    }
                    y += 3;
                } else {
                    y = BookKit.small(f, body, s.x(), y + 3, s.w(), t.muted());
                }
            }
            if (!d.actions().isEmpty()) {
                y = tight ? links(f, d, s, y + 2, t) : actions(f, d, s, y + 4, t);
            }
            if (tight) {
                y = PageKit.wrapped(f, Component.translatable(LinePageSupport.KEY + "genes").withStyle(net.minecraft.ChatFormatting.BOLD), s.x(), y, s.w(), t.ink());
            } else {
                y = PageKit.title(f, Component.translatable(LinePageSupport.KEY + "genes"), s.x(), y + 4, s.w(), t.ink());
                t.drawRule(f.g, s.x(), y + 2, s.w());
                y += 6;
            }
        }
        y = tight && !none ? BloodlineGenes.drawCompact(f, s, y + 1, f.view.snapshot()) : BloodlineGenes.draw(f, s, y + 2, f.view.snapshot());
        if (!tight || none) {
            y = BookKit.small(f, Component.translatable(LinePageSupport.KEY + "gene_hint"), s.x(), y, s.w(), t.muted());
        }
        f.end(y);
    }

    /** The page is tight below this many GUI pixels (the 480 x 270 GUI gives about 190). */
    static final int TIGHT_BELOW = 240;

    /** "Line  [padlock Free of Klaus's line]": the green pill that says a freed line. */
    private static int linePill(HubScreen.Frame f, Detail.Field field, Rect s, int y) {
        HubTheme t = f.theme();
        f.g.drawString(f.font(), field.label(), s.x(), y + 2, t.ink(), false);
        String text = LabelFit.cut(field.value().getString(), s.w() - f.font().width(field.label()) - 34, f.font()::width);
        int w = f.font().width(text) + 22;
        Rect pill = new Rect(s.right() - w, y, w, 13);
        f.g.fill(pill.x(), pill.y(), pill.right(), pill.bottom(), LineColors.FREE_PILL);
        LinePageSupport.openLock(f.g, pill.x() + 4, pill.y() + 2, LineColors.FREE_PILL_INK);
        f.g.drawString(f.font(), text, pill.x() + 15, pill.y() + 3, LineColors.FREE_PILL_INK, false);
        return y + 15;
    }

    /** The actions as red links with a chevron, one under another: the primary first. For a page with no room for the wax seal. */
    private static int links(HubScreen.Frame f, Detail d, Rect s, int y, HubTheme t) {
        t.drawRule(f.g, s.x(), y, s.w());
        y += 4;
        for (Detail.Action a : d.actions()) {
            Component label = Component.literal(fit(f, a.label(), s.w() - 14) + (a.enabled() ? " \u203a" : ""));
            BookKit.link(f, "bloodline:" + a.id(), label, s.x(), y, List.of(a.disabledReason() != null && !a.enabled() ? a.disabledReason() : a.label()), a.enabled() ? a.run() : null);
            y += 11;
        }
        return y + 1;
    }

    /** The wax Call seal, and Severance and Broken Line as red links beside it. */
    private static int actions(HubScreen.Frame f, Detail d, Rect s, int y, HubTheme t) {
        t.drawRule(f.g, s.x(), y, s.w());
        y += 6;
        int linkX = s.x();
        for (Detail.Action a : d.actions()) {
            if (a.primary()) {
                BookKit.seal(f, "bloodline:" + a.id(), Component.translatable(BOOK + "call"), a.enabled(), s.x(), y, a.enabled() ? a.run() : null,
                        List.of(a.disabledReason() != null && !a.enabled() ? a.disabledReason() : a.label()));
                linkX = s.x() + BookKit.SEAL + 10;
            }
        }
        int ly = y + 2;
        int room = s.right() - linkX;
        for (Detail.Action a : d.actions()) {
            if (!a.primary()) {
                Component label = Component.literal(fit(f, a.label(), room - 8));
                BookKit.link(f, "bloodline:" + a.id(), label, linkX, ly, List.of(a.disabledReason() != null ? a.disabledReason() : a.label()),
                        a.enabled() ? a.run() : null);
                ly += 15;
            }
        }
        return Math.max(y + BookKit.SEAL, ly) + 6;
    }
}
