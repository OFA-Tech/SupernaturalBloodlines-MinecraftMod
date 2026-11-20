package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubRites;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * The Grimoire's Rites: an index of the rites with their altar power and whether you can perform them, on the left; on the facing page the chosen
 * rite with its ink circle, its ingredients by leaders (in hand or missing, from your inventory) and the link into Enchanted's own book.
 */
public final class BookRitesPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "rites.";
    private static final String BOOK = PageKit.HUB + "book.rites.";
    private static final int GOLD = 0xFF7A4508;

    @Nullable
    private String selected;

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.TaskRow> tasks = new ArrayList<>();
        f.view.ladders().forEach(l -> l.tasks().stream().filter(RitesPage::isRite).forEach(tasks::add));
        Set<String> asked = HubRites.fromLadders(LadderTable.clientCopy(), f.view.races());
        List<HubRites.Listed> rites = HubRites.list(f.view.snapshot().rites(), f.view.races(), asked);
        HubRites.Listed chosen = rites.stream().filter(l -> l.rite().id().equals(selected != null ? selected : RitesPage.devSelected())).findFirst().orElse(rites.isEmpty() ? null : rites.get(0));

        Rect r = f.region(BookKit.MAIN);
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle"));
        for (HubModel.TaskRow row : tasks) {
            y = PageKit.taskRow(f, row, r.x(), y, r.w(), true, null, false);
        }
        for (HubRites.Listed l : rites) {
            boolean sel = l == chosen;
            Rect hit = new Rect(r.x() - 2, y - 2, r.w() + 4, 23);
            if (sel) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            int ring = l.forYou() ? t.good() : t.muted();
            KitDraw.shape(f.g, Shapes.Shape.ROUND, r.x() + 1, y + 1, 16, ring);
            KitDraw.shape(f.g, Shapes.Shape.ROUND, r.x() + 3, y + 3, 12, t.paper());
            KitDraw.shape(f.g, Shapes.Shape.ROUND, r.x() + 6, y + 6, 6, ring);
            Component power = RitesPage.powerCard(l.rite());
            int pw = BookKit.smallWidth(f, power.getString());
            String name = LabelFit.cut(RitesPage.name(l.rite()).getString(), r.w() - 24, f.font()::width);
            f.g.drawString(f.font(), name, r.x() + 22, y + 1, sel ? t.accent() : t.ink(), false);
            BookKit.small(f, Component.translatable(BOOK + (l.forYou() ? "yours" : "anyone")), r.x() + 22, y + 12, r.w() - 24 - pw - 4, l.forYou() ? t.good() : t.muted());
            BookKit.small(f, power, r.right() - pw, y + 12, pw + 4, GOLD);
            String id = "rite:" + l.rite().id();
            f.target(id, hit, List.of(RitesPage.name(l.rite()), power), () -> selected = l.rite().id());
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            y += 25;
        }
        if (tasks.isEmpty() && rites.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        f.end(y);

        Rect s = f.region(BookKit.SIDE);
        if (chosen == null) {
            int sy = f.begin(BookKit.SIDE);
            f.end(PageKit.wrapped(f, Component.translatable(KEY + "witch_hint"), s.x(), sy, s.w(), t.muted()));
            return;
        }
        HubSocial.Rite rite = chosen.rite();
        int sy = BookKit.head(f, BookKit.SIDE, RitesPage.name(rite), sub(rite));
        sy = circle(f, s, sy, rite);
        sy = BookKit.caps(f, Component.translatable(BOOK + "ingredients"), s.x(), sy, t.muted());
        Player p = f.view.player();
        for (String entry : rite.items()) {
            boolean have = have(p, entry);
            sy = BookKit.leader(f, RitesPage.item(entry), Component.translatable(BOOK + (have ? "in_hand" : "missing")).copy().append(have ? " \u2713" : ""), s.x(), sy, s.w(),
                    have ? t.good() : t.accent());
        }
        if (rite.items().isEmpty()) {
            sy = PageKit.wrapped(f, Component.translatable(BOOK + "no_items"), s.x(), sy, s.w(), t.muted());
        }
        for (Component fact : RitesPage.facts(rite)) {
            sy = PageKit.wrapped(f, fact, s.x(), sy + 2, s.w(), t.accent());
        }
        for (Component line : RitesPage.descriptions(rite)) {
            sy = PageKit.wrapped(f, line, s.x(), sy + 4, s.w(), t.ink());
        }
        for (Component note : RitesPage.trialNotes(rite, f.view.bloodline().wolf())) {
            sy = PageKit.wrapped(f, note, s.x(), sy + 2, s.w(), t.accent());
        }
        sy += 6;
        HubHooks.Launcher book = RitesPage.bookLauncher(rite);
        BookKit.link(f, "link:enchanted", Component.translatable(BOOK + "open_book"), s.x(), sy, List.of(Component.translatable(BOOK + "open_book")),
                book == null ? null : () -> f.screen().launch(book));
        sy = BookKit.small(f, Component.translatable(BOOK + "how"), s.x(), sy + 14, s.w(), t.muted());
        f.end(sy);
    }

    /** "medium circle, infernal chalk · 6,000 altar power" (or no altar power), under a rite's name. */
    static Component sub(HubSocial.Rite rite) {
        Component circle = rite.shapes().isEmpty() ? Component.literal("-") : RitesPage.circleName(rite.shapes().get(0));
        return switch (HubRites.cost(rite)) {
            case ALTAR -> Component.translatable(BOOK + "sub", circle, String.format("%,d", rite.power()));
            case MAGICULES -> Component.translatable(BOOK + "sub_magicules", circle);
            case NONE -> Component.translatable(BOOK + "sub_free", circle);
        };
    }

    /** The rite's circle as Enchanted draws it, with its items on rings, one under the other pages' heading; returns the y under it. */
    static int circle(HubScreen.Frame f, Rect s, int y, HubSocial.Rite rite) {
        return RiteDiagram.draw(f, s, y + 2, rite);
    }

    static boolean have(Player p, String entry) {
        int space = entry.indexOf(' ');
        int count = 1;
        try {
            count = space < 0 ? 1 : Integer.parseInt(entry.substring(0, space));
        } catch (NumberFormatException ignored) {
            // a bad count in a rite file counts as one
        }
        if (net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubDev.handsFull()) {
            return true; // the dev screenshots' demo data: every ingredient is in hand, without giving the player anything
        }
        ResourceLocation id = ResourceLocation.tryParse(space < 0 ? entry : entry.substring(space + 1));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || !(p instanceof LocalPlayer)) {
            return false;
        }
        return p.getInventory().countItem(BuiltInRegistries.ITEM.get(id)) >= count;
    }
}
