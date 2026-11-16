package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * The Grimoire's Bloodline: the family tree with a player face for everyone the client knows (your sire's line above you, the first of those you
 * sired below), the CALL seal for who may call, and on the facing page the genes in stoppered vials with Tensura's EP by leaders.
 */
public final class BookBloodlinePage implements HubPageView {
    private static final String KEY = PageKit.HUB + "bloodline.";
    private static final String BOOK = PageKit.HUB + "book.bloodline.";
    private int face = 14;

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        HubSnapshotPayload snap = f.view.snapshot();
        Rect r = f.region(BookKit.MAIN);
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle"));
        face = r.h() > 190 ? 14 : 11; // a short page (a small GUI) gets smaller faces so the tree and the CALL seal fit without scrolling
        int cx = r.x() + r.w() / 2;
        List<String> above = snap.family().ancestors();
        int from = Math.max(0, above.size() - 2);
        for (int i = above.size() - 1; i >= from; i--) {
            y = node(f, cx, y, BloodlinePage.name(above.get(i)).getString(), Component.empty(), false, t.muted());
            y = stem(f, cx, y, t.muted());
        }
        boolean hasSire = f.view.bloodline().sire() != null;
        if (hasSire) {
            String sire = snap.sire().orElse("");
            y = node(f, cx, y, BloodlinePage.name(sire).getString(), Component.translatable(BOOK + "your_sire"), false, t.ink());
            y = stem(f, cx, y, snap.severed() ? t.muted() : t.accent());
        }
        y = node(f, cx, y, f.view.player().getName().getString(), Component.translatable(BOOK + "you", PageKit.race(f.view.races().get(0))), true, t.accent());
        List<HubSocial.Kin> line = snap.family().line().stream().filter(k -> k.depth() == 0).toList();
        if (line.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none_sired"), r.x(), y + 4, r.w(), t.muted());
        } else {
            y = children(f, r, y + 2, line, snap.family().line().size() - Math.min(3, line.size()));
        }
        if (BloodlinePage.MAY_CALL.contains(f.view.races().get(0)) && !snap.sired().isEmpty()) {
            y += 2;
            BookKit.seal(f, "bloodline:call", Component.translatable(BOOK + "call"), true, r.right() - BookKit.SEAL, y, () -> BloodlinePage.command("bloodline call"),
                    List.of(Component.translatable(KEY + "call")));
            y = Math.max(y + BookKit.SEAL, BookKit.small(f, Component.translatable(BOOK + "call_note"), r.x(), y + 6, r.w() - BookKit.SEAL - 8, t.muted()));
        }
        f.end(y);
        genes(f, snap);
    }

    private int node(HubScreen.Frame f, int cx, int y, String name, Component caption, boolean you, int color) {
        HubTheme t = f.theme();
        BookKit.portrait(f.g, name, cx, y + face / 2 + 2, face, you ? t.accent() : t.muted(), t.paper());
        y += face + 5;
        String n = f.font().plainSubstrByWidth(name, 120);
        f.g.drawString(f.font(), n, cx - f.font().width(n) / 2, y, color, false);
        y += 9;
        if (!caption.getString().isEmpty()) {
            String c = f.font().plainSubstrByWidth(caption.getString(), 140);
            f.g.drawString(f.font(), c, cx - f.font().width(c) / 2, y, t.muted(), false);
            y += 9;
        }
        f.target("kin:" + name + ":" + y, new Rect(cx - 36, y - 30, 72, 30), List.of(Component.literal(name)), null);
        return y + 1;
    }

    private static int stem(HubScreen.Frame f, int cx, int y, int color) {
        f.g.fill(cx, y, cx + 1, y + 6, color);
        return y + 7;
    }

    /** The first three sired in a row under a bracket, and how many more there are. */
    private static int children(HubScreen.Frame f, Rect r, int y, List<HubSocial.Kin> line, int more) {
        HubTheme t = f.theme();
        int show = Math.min(3, line.size());
        int cx = r.x() + r.w() / 2;
        int step = r.w() / 3;
        f.g.fill(cx, y, cx + 1, y + 5, t.muted());
        int left = r.x() + step / 2;
        int right = r.x() + step / 2 + (show - 1) * step;
        f.g.fill(Math.min(left, cx), y + 5, Math.max(right, cx) + 1, y + 6, t.muted());
        y += 6;
        for (int i = 0; i < show; i++) {
            int x = r.x() + step / 2 + i * step;
            f.g.fill(x, y, x + 1, y + 4, t.muted());
            BookKit.portrait(f.g, line.get(i).name(), x, y + 4 + 5, 10, t.muted(), t.paper());
            String n = f.font().plainSubstrByWidth(BloodlinePage.name(line.get(i).name()).getString(), step - 4);
            f.g.drawString(f.font(), n, x - f.font().width(n) / 2, y + 18, t.ink(), false);
            f.target("kin:child:" + i, new Rect(x - step / 2, y, step, 28), List.of(Component.literal(line.get(i).name())), null);
        }
        y += 30;
        if (more > 0) {
            String m = Component.translatable(BOOK + "more", more).getString();
            f.g.drawString(f.font(), m, cx - f.font().width(m) / 2, y, t.muted(), false);
            y += 10;
        }
        return y;
    }

    private static void genes(HubScreen.Frame f, HubSnapshotPayload snap) {
        HubTheme t = f.theme();
        Rect s = f.region(BookKit.SIDE);
        int y = BookKit.head(f, BookKit.SIDE, Component.translatable(KEY + "genes"), Component.translatable(BOOK + "genes_sub"));
        int n = Genes.ALL.size();
        int step = s.w() / Math.max(1, n);
        for (int i = 0; i < n; i++) {
            String gene = Genes.ALL.get(i);
            Genes.Knowledge k = snap.genes().getOrDefault(gene, Genes.Knowledge.UNKNOWN);
            int vx = s.x() + step * i + (step - 26) / 2;
            vial(f, vx, y, k);
            Component name = Component.translatable(KEY + "gene." + gene);
            Component value = Component.translatable(KEY + "gene_value." + k.name().toLowerCase(Locale.ROOT));
            int mid = vx + 13;
            String shown = f.font().plainSubstrByWidth(name.getString().replace(" gene", ""), 2 * step / 1);
            int nw = BookKit.smallWidth(f, shown);
            BookKit.small(f, Component.literal(shown), mid - nw / 2, y + 52, nw + 4, t.ink());
            int vw = BookKit.smallWidth(f, value.getString());
            BookKit.small(f, value, mid - vw / 2, y + 61, vw + 4, k == Genes.Knowledge.TRUE ? t.good() : t.muted());
            f.target("gene:" + gene, new Rect(vx - 4, y, 34, 72), List.of(name.copy().append(": ").append(value), Component.translatable(KEY + "gene_hint").withStyle(ChatFormatting.GRAY)), null);
        }
        y += 76;
        y = BookKit.dropCap(f, Component.translatable(KEY + "gene_hint"), s.x(), y, s.w(), t.ink());
        HubHooks.TensuraInfo info = HubHooks.tensura().orElse(null);
        if (info != null) {
            y += 4;
            y = BookKit.leader(f, Component.translatable(BOOK + "race"), info.race(), s.x(), y, s.w(), t.ink());
            y = BookKit.leader(f, Component.translatable(BOOK + "ep"), Component.literal(String.format("%,d", (long) info.ep())), s.x(), y, s.w(), t.ink());
            y = BookKit.leader(f, Component.translatable(BOOK + "magicule"), Component.literal(String.format("%,d", (long) info.magicule())), s.x(), y, s.w(), t.ink());
        }
        f.end(y);
    }

    /** A stoppered vial: the cork, a glass body, and a liquid that is full and gold when known, low and pale when not. */
    private static void vial(HubScreen.Frame f, int x, int y, Genes.Knowledge k) {
        HubTheme t = f.theme();
        f.g.fill(x + 8, y, x + 18, y + 6, 0xFF7A5238);
        f.g.fill(x, y + 6, x + 26, y + 48, t.ink());
        f.g.fill(x + 1, y + 7, x + 25, y + 47, t.paper());
        int level = k == Genes.Knowledge.TRUE ? 34 : k == Genes.Knowledge.FALSE ? 10 : 8;
        int liquid = k == Genes.Knowledge.TRUE ? 0xFFB8903A : 0xFFC8BBA0;
        f.g.fill(x + 1, y + 47 - level, x + 25, y + 47, liquid);
        if (k == Genes.Knowledge.UNKNOWN) {
            f.g.drawString(f.font(), "?", x + 13 - f.font().width("?") / 2, y + 22, t.muted(), false);
        }
    }
}
