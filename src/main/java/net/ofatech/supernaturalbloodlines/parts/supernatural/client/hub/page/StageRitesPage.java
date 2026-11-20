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
    /** Development only (the screenshots): open on the circle view. */
    private static volatile boolean devCircle;

    public static void devCircle(boolean on) {
        devCircle = on;
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Set<String> asked = HubRites.fromLadders(LadderTable.clientCopy(), f.view.races());
        List<HubRites.Listed> rites = HubRites.list(f.view.snapshot().rites(), f.view.races(), asked);
        HubRites.Listed chosen = rites.stream().filter(l -> l.rite().id().equals(selected != null ? selected : RitesPage.devSelected())).findFirst().orElse(rites.isEmpty() ? null : rites.get(0));

        if (devCircle) {
            circle = true;
        }
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
            for (Component fact : RitesPage.facts(chosen.rite())) {
                y = PageKit.wrapped(f, fact, s.x() + 20, y + 2, s.w() - 40, t.accent());
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
        // the ring is the rite's own circle: its size by the shape, its colour by the chalk (white ritual, red infernal, purple otherwhere)
        HubSocial.Circle circle = l.rite().circles().isEmpty() ? new HubSocial.Circle("medium_circle", "ritual_chalk") : l.rite().circles().get(0);
        int radius = switch (circle.shape()) {
            case "small_circle" -> 8;
            case "large_circle" -> 17;
            default -> 12;
        };
        int ring = RiteDiagram.chalkColor(circle);
        KitDraw.tickRing(f.g, cx, cy, radius, radius * 2, 1f, ring, ring);
        f.g.fill(cx - 2, cy - 2, cx + 2, cy + 2, GOLD);
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
        Detail.Builder d = Detail.of(RitesPage.name(rite)).subtitle(BookRitesPage.sub(rite));
        // the Alpha Trial's warning (or why it is not for you) comes first: the Blood Moon bar shows only the first two lines
        RitesPage.trialNotes(rite, f.view.bloodline().wolf()).forEach(d::body);
        RitesPage.descriptions(rite).forEach(d::body);
        RitesPage.facts(rite).forEach(d::body);
        d.body(items.isEmpty() ? Component.translatable(BOOK + "no_items") : Component.translatable(BOOK + "needs", items.stream().map(Component::getString).reduce((a, b) -> a + ", " + b).orElse("")));
        HubHooks.Launcher book = RitesPage.bookLauncher(rite);
        d.action(new Detail.Action("book", Component.translatable(BOOK + "enchanted_short"), false, book != null, null, null, () -> f.screen().launch(book)));
        d.action("circle", Component.translatable(BOOK + (circle ? "back" : "show_circle")), true, true, () -> circle = !circle);
        f.detail(d.build());
    }
}
