package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubRites;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * Rites for the Blood Moon and Tensura: a grid of rite cards (a mini circle, the name, the altar power in gold and a pill that says whether you can
 * do it), as many columns as the stage allows up to three. The chosen rite is in the theme's detail with Open Enchanted's book and Show the circle,
 * which swaps the grid for the ink circle and the ingredients.
 */
public final class StageRitesPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "rites.";
    private static final String BOOK = PageKit.HUB + "book.rites.";
    private static final int GOLD = 0xFFE0A83A;

    static final int CARD_H = 56;

    /** Columns of rite cards: up to three, but never so many that a name has less than about 120 pixels. */
    static int columns(int width) {
        return Math.max(1, Math.min(3, width / 190));
    }

    @Nullable
    private String selected;
    private boolean circle;

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Set<String> asked = HubRites.fromLadders(LadderTable.clientCopy(), f.view.races());
        List<HubRites.Listed> rites = HubRites.list(f.view.snapshot().rites(), f.view.races(), asked);
        HubRites.Listed chosen = rites.stream().filter(l -> l.rite().id().equals(selected)).findFirst().orElse(rites.isEmpty() ? null : rites.get(0));

        BookKit.moonHead(f, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle_short"));
        Rect s = f.region(BookKit.FULL);
        int top = f.begin(BookKit.FULL);
        if (chosen != null && circle) {
            int y = BookRitesPage.circle(f, s, top + 2, chosen.rite());
            y = BookKit.caps(f, Component.translatable(BOOK + "ingredients"), s.x(), y, t.muted());
            for (String entry : chosen.rite().items()) {
                boolean have = BookRitesPage.have(f.view.player(), entry);
                y = BookKit.leader(f, RitesPage.item(entry), Component.translatable(BOOK + (have ? "in_hand" : "missing")).copy().append(have ? " \u2713" : ""), s.x() + 20, y,
                        s.w() - 40, have ? t.good() : t.accent());
            }
            f.end(y);
        } else {
            int cols = columns(s.w());
            int gap = 6;
            int cw = (s.w() - gap * (cols - 1)) / cols;
            int y = top + 1;
            for (int i = 0; i < rites.size(); i++) {
                int col = i % cols;
                if (col == 0 && i > 0) {
                    y += CARD_H + 6;
                }
                card(f, rites.get(i), new Rect(s.x() + col * (cw + gap), y, cw, CARD_H), rites.get(i) == chosen);
            }
            f.end(y + CARD_H + 6);
        }
        if (chosen != null) {
            detail(f, chosen);
        }
    }

    private void card(HubScreen.Frame f, HubRites.Listed l, Rect box, boolean sel) {
        HubTheme t = f.theme();
        f.g.fill(box.x(), box.y(), box.right(), box.bottom(), sel ? t.accent() : (t.muted() & 0xFFFFFF) | 0x66000000);
        f.g.fill(box.x() + 1, box.y() + 1, box.right() - 1, box.bottom() - 1, t.paper());
        int cx = box.x() + 20;
        int cy = box.y() + 22;
        int ring = l.forYou() ? t.good() : t.muted();
        KitDraw.tickRing(f.g, cx, cy, 13, 28, 1f, ring, ring);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + Math.PI * i / 2;
            int px = cx + (int) Math.round(13 * Math.cos(a));
            int py = cy + (int) Math.round(13 * Math.sin(a));
            f.g.fill(px - 2, py - 2, px + 2, py + 2, GOLD);
        }
        int tx = box.x() + 40;
        int tw = box.w() - 46;
        // the name in up to two lines, cut with an ellipsis only when even that does not fit
        List<String> name = LabelFit.wrap(RitesPage.name(l.rite()).getString(), tw, 2, f.font()::width);
        for (int k = 0; k < name.size(); k++) {
            f.g.drawString(f.font(), name.get(k), tx, box.y() + 5 + 10 * k, sel ? t.accent() : t.ink(), false);
        }
        int py = box.y() + 5 + 10 * name.size() + 2;
        BookKit.small(f, Component.translatable(BOOK + "power", String.format("%,d", l.rite().power())), tx, py, tw, GOLD);
        Component status = Component.translatable(BOOK + (l.forYou() ? "yours" : "anyone"));
        int sw = Math.min(tw, BookKit.smallWidth(f, status.getString()) + 8);
        f.g.fill(tx, py + 11, tx + sw, py + 22, l.forYou() ? (t.good() & 0xFFFFFF) | 0x55000000 : (t.muted() & 0xFFFFFF) | 0x33000000);
        BookKit.small(f, Component.literal(LabelFit.cut(status.getString(), sw - 6, s -> BookKit.smallWidth(f, s))), tx + 4, py + 13, sw, l.forYou() ? t.good() : t.muted());
        String id = "rite:" + l.rite().id();
        f.target(id, box, List.of(RitesPage.name(l.rite())), () -> selected = l.rite().id());
        if (f.focused(id)) {
            KitDraw.focusRing(f.g, box, t.focusColor());
        }
    }

    private void detail(HubScreen.Frame f, HubRites.Listed l) {
        HubSocial.Rite rite = l.rite();
        List<Component> items = new ArrayList<>();
        rite.items().forEach(e -> items.add(RitesPage.item(e)));
        Component shape = rite.shapes().isEmpty() ? Component.literal("-") : RitesPage.shapeName(rite.shapes().get(0));
        Detail.Builder d = Detail.of(RitesPage.name(rite)).subtitle(Component.translatable(BOOK + "sub", shape, String.format("%,d", rite.power())))
                .body(items.isEmpty() ? Component.translatable(BOOK + "no_items") : Component.translatable(BOOK + "needs", items.stream().map(Component::getString).reduce((a, b) -> a + ", " + b).orElse("")));
        HubHooks.Launcher book = HubHooks.launchers().stream().filter(x -> x.id().equals("enchanted")).findFirst().orElse(null);
        d.action(new Detail.Action("book", Component.translatable(BOOK + "enchanted_short"), false, book != null, null, null, () -> f.screen().launch(book)));
        d.action("circle", Component.translatable(BOOK + (circle ? "back" : "show_circle")), true, true, () -> circle = !circle);
        f.detail(d.build());
    }
}
