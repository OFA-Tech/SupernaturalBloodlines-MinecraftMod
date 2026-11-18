package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * Where the nodes of a {@link LineTree.Model} go, one function per theme: the Grimoire's ink column, the Blood Moon's chain under the moon, and
 * Tensura's hexes. Every rectangle is the node's portrait (with its ring) and lies inside the region; labels are drawn beside a chain node
 * (to the right) or under a sired one, in the room {@link #labelRoom} and {@link #KID_LABEL} leave. Pure.
 */
public final class LineTreeLayout {
    /** Height under a sired node's portrait for its name and caption. */
    public static final int KID_LABEL = 20;
    /** The crown over the crowned node. */
    public static final int CROWN = 10;
    private static final int MIN_GAP = 2;
    private static final int MAX_GAP = 18;
    private static final int KID_GAP = 6;
    /** The room a broken link keeps between the sire and you: the torn ink, the break mark and its label sit in it. */
    public static final int BREAK_GAP = 12;

    static boolean broken(LineTree.Model m) {
        return m.links().stream().anyMatch(l -> l.style() == LineTree.LinkStyle.BROKEN);
    }

    private LineTreeLayout() {
    }

    private static boolean isKid(LineTree.Node n) {
        return n.kind() == LineTree.Kind.DESCENDANT;
    }

    /** The portrait to use in a region of this height: the big one when the whole tree fits with it, else a smaller one. */
    public static int portraitFor(Rect region, LineTree.Model m) {
        for (int p : new int[] {14, 11, 9}) {
            if (fits(region.h(), m, p + 4)) {
                return p;
            }
        }
        return 9;
    }

    private static boolean fits(int height, LineTree.Model m, int ring) {
        long chain = m.nodes().stream().filter(n -> !isKid(n)).count();
        boolean kids = m.nodes().stream().anyMatch(LineTreeLayout::isKid);
        return CROWN + chain * ring + (chain - 1) * (MIN_GAP + 2) + (broken(m) ? BREAK_GAP : 0) + (kids ? KID_GAP + ring + KID_LABEL : 0) <= height;
    }

    /** The room to the right of a chain node's portrait for its name and caption. */
    public static int labelRoom(Rect region, Rect node) {
        return Math.max(0, region.right() - node.right() - 4);
    }

    /** The Grimoire: one column of portraits, top to bottom in chain order, the sired in a row under the player. */
    public static Map<String, Rect> grimoire(Rect region, LineTree.Model m, int portrait) {
        return column(region, m, portrait + 4, 0.34, region.y() + CROWN, 0);
    }

    /** The moon's disc at the top of the region, centred over the tree. */
    public static Rect moonDisc(Rect region) {
        return moonDisc(region, 38);
    }

    /** {@code pct}: how much of the height the disc may take; a tall chain under it asks for less. */
    static Rect moonDisc(Rect region, int pct) {
        int d = Math.max(30, Math.min(Math.min(region.h() * pct / 100, region.w() * 50 / 100), 120));
        int cx = region.x() + region.w() * 38 / 100;
        cx = Math.max(region.x() + d / 2, Math.min(region.right() - d / 2, cx));
        return new Rect(cx - d / 2, region.y() + 2, d, d);
    }

    /**
     * The Blood Moon: the crowned node inside the moon (the root, or the player when they are the Original), the rest of the chain hanging
     * under the moon, the sired at the foot. {@code bar} is the detail bar along the bottom: nothing is placed on it.
     */
    public static Map<String, Rect> moon(Rect region, Rect bar, LineTree.Model m) {
        Rect usable = region.bottom() > bar.y() ? new Rect(region.x(), region.y(), region.w(), Math.max(20, bar.y() - 4 - region.y())) : region;
        Map<String, Rect> out = new LinkedHashMap<>();
        boolean crowned = m.nodes().stream().anyMatch(n -> n.kind() == LineTree.Kind.ROOT)
                || m.state() == LineRules.State.ORIGINAL;
        LineTree.Node first = m.nodes().get(0);
        // a chain of three or more under the moon, or a broken link in it, asks for a smaller disc so the nodes keep their gaps
        long under = m.nodes().stream().filter(n -> !isKid(n) && (!crowned || !n.id().equals(first.id()))).count();
        Rect disc = moonDisc(usable, under >= 3 || broken(m) ? 28 : 38);
        int inner = Math.max(16, disc.w() * 48 / 100);
        if (crowned) {
            out.put(first.id(), new Rect(disc.x() + (disc.w() - inner) / 2, disc.y() + (disc.h() - inner) / 2, inner, inner));
        }
        int ring = Math.min(24, Math.max(14, usable.h() / 9));
        int top = disc.bottom() + 3;
        Rect below = new Rect(usable.x(), top, usable.w(), usable.bottom() - top);
        LineTree.Model rest = new LineTree.Model(m.state(), m.nodes().stream().filter(n -> !crowned || !n.id().equals(first.id())).toList(),
                m.links(), m.padlock(), m.hiddenBelow());
        if (!rest.nodes().isEmpty()) {
            // the tree under the moon keeps the moon's own axis
            double frac = (disc.x() + disc.w() / 2 - usable.x()) / (double) usable.w();
            out.putAll(column(below, rest, ring, frac, below.y(), 0));
        }
        return out;
    }

    /** Tensura: hexes on a vertical axis in the left of the stage; {@code pane} is the info pane, which nothing is placed on. */
    public static Map<String, Rect> tensura(Rect stage, Rect pane, LineTree.Model m) {
        int w = stage.right() > pane.x() ? Math.max(40, pane.x() - 4 - stage.x()) : stage.w();
        Rect area = new Rect(stage.x(), stage.y(), w, stage.h());
        // the genes take the right of a wide stage; the tree keeps the left
        Rect tree = area.w() >= 300 ? new Rect(area.x(), area.y(), area.w() * 55 / 100, area.h()) : area;
        int hex = tree.h() >= 190 ? 30 : 22;
        return column(tree, m, hex, 0.30, tree.y() + CROWN, 0);
    }

    private static Map<String, Rect> column(Rect area, LineTree.Model m, int wantedRing, double cxFraction, int top, int unused) {
        List<LineTree.Node> chain = m.nodes().stream().filter(n -> !isKid(n)).toList();
        List<LineTree.Node> kids = m.nodes().stream().filter(LineTreeLayout::isKid).toList();
        int bottom = area.bottom();
        int ring = wantedRing;
        int kidBlock = kids.isEmpty() ? 0 : KID_GAP + ring + KID_LABEL;
        int avail = bottom - top - kidBlock;
        int n = chain.size();
        if (n > 0 && n * ring + (n - 1) * MIN_GAP > avail) {
            ring = Math.max(8, (avail - (n - 1) * MIN_GAP) / n);
            kidBlock = kids.isEmpty() ? 0 : KID_GAP + ring + KID_LABEL;
            avail = bottom - top - kidBlock;
        }
        boolean broken = broken(m);
        int extra = broken ? BREAK_GAP - MIN_GAP : 0;
        while (broken && n > 1 && ring > 8 && n * ring + (n - 1) * MIN_GAP + extra > avail) {
            ring--;
            kidBlock = kids.isEmpty() ? 0 : KID_GAP + ring + KID_LABEL;
            avail = bottom - top - kidBlock;
        }
        int gap = n <= 1 ? 0 : Math.max(MIN_GAP, Math.min(MAX_GAP, (avail - n * ring - extra) / (n - 1)));
        double half = (kids.size() - 1) / 2.0;
        int step = kids.isEmpty() ? 0 : Math.max(ring + 6, Math.min(80, area.w() / Math.max(2, kids.size())));
        int cx = area.x() + (int) (area.w() * cxFraction);
        int lo = area.x() + ring / 2 + (int) Math.ceil(half * step) + 1;
        int hi = area.right() - ring / 2 - (int) Math.ceil(half * step) - 1;
        cx = lo > hi ? area.x() + area.w() / 2 : Math.max(lo, Math.min(hi, cx));
        Map<String, Rect> out = new LinkedHashMap<>();
        int y = top;
        for (int i = 0; i < chain.size(); i++) {
            LineTree.Node node = chain.get(i);
            out.put(node.id(), new Rect(cx - ring / 2, y, ring, ring));
            boolean aboveYou = broken && i + 1 < chain.size() && chain.get(i + 1).id().equals(LineTree.YOU);
            y += ring + (aboveYou ? Math.max(gap, BREAK_GAP) : gap);
        }
        int ky = bottom - KID_LABEL - ring;
        for (int i = 0; i < kids.size(); i++) {
            int kx = cx + (int) Math.round((i - half) * step);
            out.put(kids.get(i).id(), new Rect(kx - ring / 2, ky, ring, ring));
        }
        return out;
    }
}
