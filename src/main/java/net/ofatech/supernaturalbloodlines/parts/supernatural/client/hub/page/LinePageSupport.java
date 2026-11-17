package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/** What the three Bloodline pages share: the model, the words, the selection's detail and the few shapes every look draws the same way. */
public final class LinePageSupport {
    public static final String KEY = PageKit.HUB + "bloodline.";

    private LinePageSupport() {
    }

    public static LineTree.Model model(HubScreen.Frame f) {
        HubSnapshotPayload snap = f.view.snapshot();
        return LineTree.build(snap.family().origin(), f.view.player().getName().getString(), f.view.bloodline().sire() == null && snap.sire().isEmpty()
                        ? Optional.empty() : Optional.of(snap.sire().orElse("")),
                snap.severed(), snap.family().ancestors(), snap.family().line());
    }

    public static HubSocial.Origin origin(HubScreen.Frame f) {
        return f.view.snapshot().family().origin();
    }

    /** A name as it is drawn: an NPC Original through its lang key, a lost name as "a name that is lost", "?" for a hidden one. */
    public static Component display(String raw, boolean hidden) {
        if (hidden || raw.isEmpty()) {
            return Component.literal("?");
        }
        if (raw.startsWith("npc:")) {
            return Component.translatable("supernaturalbloodlines.original." + raw.substring(4));
        }
        return Component.literal(raw);
    }

    public static Component name(LineTree.Node n) {
        return display(n.name(), n.hidden());
    }

    public static Component race(HubScreen.Frame f) {
        return PageKit.race(f.view.races().get(0));
    }

    public static boolean mayCall(HubScreen.Frame f) {
        return BloodlinePage.MAY_CALL.contains(f.view.races().get(0)) && !f.view.snapshot().sired().isEmpty();
    }

    public static void command(String command) {
        BloodlinePage.command(command);
    }

    /** The line under a node's name: "Original · crowned", "Hybrid · you", "sired". */
    public static Component caption(HubScreen.Frame f, LineTree.Model m, LineTree.Node n) {
        return switch (n.kind()) {
            case ROOT -> Component.translatable(KEY + (n.hidden() ? "cap.root_hidden" : "cap.root"));
            case ANCESTOR -> Component.translatable(KEY + (n.hidden() ? "cap.ancestor_hidden" : "cap.ancestor"));
            case SIRE -> Component.translatable(KEY + (n.hidden() ? "cap.sire_unknown" : "cap.sire"));
            case YOU -> m.state() == LineRules.State.ORIGINAL ? Component.translatable(KEY + "cap.you_original") : Component.translatable(KEY + "cap.you", race(f));
            case DESCENDANT -> Component.translatable(KEY + "cap.down");
        };
    }

    /** What the narrator says about a node, as a sentence. */
    public static Component narration(LineTree.Model m, LineTree.Node n) {
        LineTree.Narr narr = LineTree.narrate(m, n);
        Object[] args = narr.args().stream().map(a -> (Object) display(a, false)).toArray();
        return Component.translatable(narr.key(), args);
    }

    /** The tooltip of the break in the line: where it was broken from, and by which rite; names the Original only when discovered. */
    public static Component breakTooltip(HubSocial.Origin o) {
        boolean imprisoned = o.freedKind() == HubSocial.FreedKind.IMPRISONED;
        if (o.freedFrom().isPresent()) {
            return Component.translatable(KEY + (imprisoned ? "link.broken_imprisoned" : "link.broken"), display(o.freedFrom().get(), false));
        }
        return Component.translatable(KEY + (imprisoned ? "link.broken_unknown_imprisoned" : "link.broken_unknown"));
    }

    private static Component lineText(LineTree.Model m, HubSocial.Origin o) {
        return switch (m.state()) {
            case ORIGINAL -> Component.translatable(KEY + "line.original");
            case IN_LINE -> o.root().isPresent() ? Component.translatable(KEY + "line.in", display(o.root().get(), false)) : Component.translatable(KEY + "line.in_unknown");
            case FREE -> o.freedFrom().isPresent() ? Component.translatable(KEY + "line.free", display(o.freedFrom().get(), false)) : Component.translatable(KEY + "line.free_unknown");
            case SIRE_UNKNOWN -> Component.translatable(KEY + "line.sire_unknown");
            case NOT_VAMPIRE -> Component.translatable(KEY + "line.none");
        };
    }

    private static Component note(LineTree.Model m, HubSocial.Origin o) {
        return switch (m.state()) {
            case ORIGINAL -> Component.translatable(KEY + "note.original");
            case IN_LINE -> o.root().isPresent() ? Component.translatable(KEY + "note.in_line", display(o.root().get(), false)) : Component.translatable(KEY + "note.unknown");
            case FREE -> o.freedFrom().isEmpty() ? Component.translatable(KEY + "note.free_unknown")
                    : Component.translatable(KEY + (o.freedKind() == HubSocial.FreedKind.IMPRISONED ? "note.free_imprisoned" : "note.free"), display(o.freedFrom().get(), false));
            case SIRE_UNKNOWN, NOT_VAMPIRE -> Component.translatable(KEY + "note.sire_unknown");
        };
    }

    /** The selection's detail, drawn as the page opposite, the bar or the info pane. Only the player's own node carries the actions. */
    public static Detail detail(HubScreen.Frame f, LineTree.Model m, LineTree.Node n) {
        HubSocial.Origin o = origin(f);
        Detail.Builder b = Detail.of(name(n)).subtitle(Component.translatable(KEY + "selected", caption(f, m, n)));
        if (n.kind() != LineTree.Kind.YOU) {
            return b.body(narration(m, n)).build();
        }
        HubSnapshotPayload snap = f.view.snapshot();
        boolean sired = f.view.bloodline().sire() != null;
        b.field(Component.translatable(KEY + "field.species"), race(f));
        b.field(Component.translatable(KEY + "field.sire"), snap.sire().filter(s -> !s.isEmpty()).<Component>map(Component::literal)
                .orElse(Component.translatable(sired ? KEY + "sire_unknown" : KEY + "no_sire")));
        b.field(Component.translatable(KEY + "field.line"), lineText(m, o));
        b.body(note(m, o));
        boolean mayCall = mayCall(f);
        b.action(new Detail.Action("call", Component.translatable(KEY + "act.call"), true, mayCall,
                mayCall ? null : Component.translatable(KEY + "act.why_cannot_call"), null, () -> command("bloodline call")));
        boolean original = m.state() == LineRules.State.ORIGINAL || m.state() == LineRules.State.NOT_VAMPIRE;
        boolean canSever = sired && !snap.severed() && !original;
        b.action(new Detail.Action("severance", Component.translatable(KEY + "act.severance"), false, canSever,
                canSever ? Component.translatable(KEY + "act.severance_hint") : Component.translatable(KEY + (snap.severed() ? "act.why_severed" : "act.why_no_sire")),
                null, () -> f.screen().setPage(HubPage.RITES)));
        boolean rootUnknown = (m.state() == LineRules.State.IN_LINE && o.root().isEmpty()) || (m.state() == LineRules.State.FREE && o.freedFrom().isEmpty());
        if (rootUnknown) {
            b.action(new Detail.Action("revelation", Component.translatable(KEY + "act.revelation"), false, true, null, null, () -> f.screen().setPage(HubPage.RITES)));
        }
        boolean free = m.state() == LineRules.State.FREE;
        boolean canBreak = m.state() == LineRules.State.IN_LINE;
        b.action(new Detail.Action("broken_line", Component.translatable(KEY + (free ? "act.broken_done" : "act.broken_line")), false, canBreak,
                canBreak ? Component.translatable(KEY + "act.broken_line_hint") : free ? breakTooltip(o) : Component.translatable(KEY + "act.why_not_in_line"),
                null, () -> f.screen().setPage(HubPage.RITES)));
        return b.build();
    }

    // ---- shapes ----

    /** A node's portrait: its shape in {@code border}, the face (or a "?") inside. */
    public static void portrait(HubScreen.Frame f, LineTree.Node n, Rect r, Shapes.Shape shape, int border, int fill) {
        GuiGraphics g = f.g;
        KitDraw.shape(g, shape, r.x(), r.y(), r.w(), border);
        KitDraw.shape(g, shape, r.x() + 1, r.y() + 1, r.w() - 2, fill);
        if (n.hidden()) {
            String q = "?";
            g.drawString(f.font(), q, r.x() + (r.w() - f.font().width(q)) / 2 + 1, r.y() + (r.h() - 8) / 2 + 1, f.theme().muted(), false);
            return;
        }
        int inset = shape == Shapes.Shape.HEX ? r.w() / 4 : 3;
        int size = Math.max(6, r.w() - 2 * inset);
        BookKit.face(g, n.name(), r.x() + (r.w() - size) / 2, r.y() + (r.h() - size) / 2, size);
    }

    public static void crown(HubScreen.Frame f, Rect r, int color) {
        int size = net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTreeLayout.CROWN - 1;
        KitDraw.icon(f.g, Icons.CROWN, r.x() + (r.w() - size) / 2, r.y() - size - 1, size, color);
    }

    /** A small open padlock: the body, and a shackle that does not close. */
    public static void openLock(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y + 4, x + 7, y + 9, color);
        g.fill(x + 1, y, x + 3, y + 4, color);
        g.fill(x + 3, y, x + 6, y + 1, color);
        g.fill(x + 5, y + 1, x + 6, y + 2, color);
    }

    /** The padlock badge on the player's portrait: a disc with the open lock. */
    public static void padlock(HubScreen.Frame f, Rect portrait, int disc, int lock) {
        int size = Math.max(10, portrait.w() * 2 / 3);
        int x = portrait.right() - size * 2 / 3;
        int y = portrait.y() - size / 3;
        KitDraw.shape(f.g, Shapes.Shape.ROUND, x, y, size, disc);
        openLock(f.g, x + (size - 7) / 2, y + (size - 9) / 2, lock);
    }

    public static void dotted(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i += 3) {
            int x = steps == 0 ? x0 : x0 + (x1 - x0) * i / steps;
            int y = steps == 0 ? y0 : y0 + (y1 - y0) * i / steps;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    public static void vertical(GuiGraphics g, int x, int y0, int y1, int color) {
        g.fill(x, Math.min(y0, y1), x + 1, Math.max(y0, y1), color);
    }

    /** The stems from the player to the sired: down, across, down. */
    public static void bracket(GuiGraphics g, Rect you, List<Rect> kids, int color) {
        if (kids.isEmpty()) {
            return;
        }
        int cx = you.x() + you.w() / 2;
        int mid = (you.bottom() + kids.get(0).y()) / 2;
        vertical(g, cx, you.bottom(), mid + 1, color);
        int left = cx;
        int right = cx;
        for (Rect k : kids) {
            int kx = k.x() + k.w() / 2;
            left = Math.min(left, kx);
            right = Math.max(right, kx);
            vertical(g, kx, mid, k.y(), color);
        }
        g.fill(left, mid, right + 1, mid + 1, color);
    }

    public static List<Rect> kidRects(LineTree.Model m, java.util.Map<String, Rect> rects) {
        List<Rect> out = new ArrayList<>();
        m.nodes().stream().filter(n -> n.kind() == LineTree.Kind.DESCENDANT).forEach(n -> out.add(rects.get(n.id())));
        return out;
    }
}
