package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubSkills;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Radial;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.VampirismSkillAccess;
import org.lwjgl.glfw.GLFW;

/**
 * The Grimoire's Skills: the chosen skill written on the left page (name, rank pips, a drop-cap description, leaders for what it costs and
 * needs, the LEARN seal and a red link to the Loadout), and a radial tree on the right page, the sigil in the middle and one medallion per skill
 * round it. The other tabs (a mod's own tree, the loadout slots) are the shared Skills page, reached from the tab row of this one.
 * Skills have no ranks in this mod, so a skill's pips are one: learned or not.
 */
public final class BookSkillsPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "skills.";
    private static final String BOOK = PageKit.HUB + "book.skills.";
    private final SkillsPage legacy = new SkillsPage();
    private List<Radial.Node> nodes = List.of();
    private List<String> ids = List.of();

    @Override
    public void render(HubScreen.Frame f) {
        if (legacy.tab() != SkillsPage.Tab.OURS) {
            legacy.render(f);
            return;
        }
        HubTheme t = f.theme();
        List<HubSkills.Row> rows = HubSkills.rows(f.view.skills(), f.view.state(), f.view.races(),
                spec -> VampirismSkillAccess.covers(f.view.player(), spec));
        HubSkills.Row chosen = rows.stream().filter(row -> row.id().equals(legacy.selected())).findFirst()
                .orElse(rows.stream().filter(row -> row.state() == HubSkills.State.BUYABLE).findFirst().orElse(rows.isEmpty() ? null : rows.get(0)));

        Rect r = f.region(BookKit.MAIN);
        Component chapter = Component.empty();
        for (int i = 0; i < f.view.races().size(); i++) {
            chapter = ((net.minecraft.network.chat.MutableComponent) chapter).append(i == 0 ? Component.empty() : Component.literal(" \u00b7 ")).append(PageKit.race(f.view.races().get(i)));
        }
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(PageKit.HUB + "page.skills"), Component.translatable(BOOK + "chapter", chapter));
        if (chosen == null) {
            f.end(PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted()));
        } else {
            f.end(chosenSkill(f, r, y, chosen));
        }
        tree(f, rows, chosen);
    }

    private int chosenSkill(HubScreen.Frame f, Rect r, int y, HubSkills.Row row) {
        HubTheme t = f.theme();
        boolean known = row.state() == HubSkills.State.OWNED || row.state() == HubSkills.State.COVERED;
        y = PageKit.title(f, SkillsPage.name(row), r.x(), y, r.w(), t.accent());
        KitDraw.pips(f.g, r.x() + 1, y + 1, 1, known ? 1 : 0, t.accent(), t.muted());
        f.g.drawString(f.font(), Component.translatable(BOOK + (known ? "learned" : "not_learned")), r.x() + 12, y - 2, t.muted(), false);
        y += 11;
        String desc = PageKit.HUB + "skill_desc." + row.id();
        y = I18n.exists(desc) ? BookKit.dropCap(f, Component.translatable(desc), r.x(), y, r.w(), t.ink())
                : PageKit.wrapped(f, SkillsPage.state(row), r.x(), y, r.w(), t.ink()) + 3;
        y += 4;
        y = BookKit.leader(f, Component.translatable(BOOK + "cost"), Component.translatable(PageKit.HUB + "skills.cost_short", row.cost()), r.x(), y, r.w(), t.ink());
        if (row.entry() != null && row.race() != null) {
            boolean met = row.state() != HubSkills.State.LOCKED;
            Component need = Component.translatable(PageKit.HUB + "skills.needs_short", PageKit.race(row.race()), row.entry().rung());
            y = BookKit.leader(f, Component.translatable(BOOK + "requires"), met ? need.copy().append(" \u2713") : need, r.x(), y, r.w(), met ? t.good() : t.ink());
        }
        y = BookKit.leader(f, Component.translatable(BOOK + "source"), Component.translatable(BOOK + "source_sb"), r.x(), y, r.w(), t.ink());
        if (row.state() == HubSkills.State.LOCKED || row.state() == HubSkills.State.DORMANT || row.state() == HubSkills.State.COVERED) {
            y = PageKit.wrapped(f, SkillsPage.state(row), r.x(), y + 3, r.w(), t.muted());
        }
        y += 6;
        String id = row.id();
        boolean buyable = row.state() == HubSkills.State.BUYABLE;
        BookKit.seal(f, "buy:" + id, Component.translatable(KEY + "buy"), buyable, r.x(), y, () -> SkillsPage.buy(id),
                List.of(Component.translatable(KEY + "buy"), SkillsPage.state(row)));
        if (f.view.context().loadout()) {
            BookKit.link(f, "link:hud", Component.translatable(BOOK + "hud_link"), r.x() + BookKit.SEAL + 8, y + BookKit.SEAL / 2 - 4,
                    List.of(Component.translatable(BOOK + "hud_tip")), () -> f.screen().setPage(HubPage.LOADOUT));
        }
        return y + BookKit.SEAL + 4;
    }

    private void tree(HubScreen.Frame f, List<HubSkills.Row> rows, HubSkills.Row chosen) {
        HubTheme t = f.theme();
        Rect s = f.region(BookKit.SIDE);
        int y = f.begin(BookKit.SIDE);
        y = tabs(f, s, y);
        int top = y + 4;
        int cx = s.x() + s.w() / 2;
        int cy = top + (s.bottom() - top) / 2;
        int radius = Math.max(28, Math.min(s.w() / 2 - 26, (s.bottom() - top) / 2 - 14));
        int inner = Math.max(14, radius * 9 / 20);
        Rect centre = new Rect(cx - inner - 2, cy - inner - 2, 2 * inner + 4, 2 * inner + 4);
        List<Radial.Node> ring = Radial.ring(cx, cy, radius, Math.max(1, rows.size()));
        nodes = rows.isEmpty() ? List.of() : ring;
        ids = rows.stream().map(HubSkills.Row::id).toList();
        for (int i = 0; i < rows.size(); i++) {
            Radial.Node n = ring.get(i);
            double a = Math.atan2(n.y() - cy, n.x() - cx);
            int sx = cx + (int) Math.round((inner + 4) * Math.cos(a));
            int sy = cy + (int) Math.round((inner + 4) * Math.sin(a));
            int line = rows.get(i).state() == HubSkills.State.LOCKED || rows.get(i).state() == HubSkills.State.DORMANT ? t.muted() & 0x70FFFFFF | 0x70000000 : t.muted();
            KitDraw.line(f.g, sx, sy, n.x() - (int) Math.round(8 * Math.cos(a)), n.y() - (int) Math.round(8 * Math.sin(a)), line);
        }
        f.emblem(centre);
        for (int i = 0; i < rows.size(); i++) {
            node(f, s, rows.get(i), ring.get(i), cy, rows.get(i) == chosen);
        }
        f.end(s.bottom());
    }

    private void node(HubScreen.Frame f, Rect page, HubSkills.Row row, Radial.Node n, int cy, boolean selected) {
        HubTheme t = f.theme();
        boolean known = row.state() == HubSkills.State.OWNED || row.state() == HubSkills.State.COVERED;
        boolean locked = row.state() == HubSkills.State.LOCKED || row.state() == HubSkills.State.DORMANT;
        int size = selected ? 18 : 14;
        int border = selected ? t.accent() : known ? t.good() : locked ? t.muted() : t.accent();
        int fill = known ? t.good() : locked ? t.paper() : t.accent();
        KitDraw.shape(f.g, Shapes.Shape.ROUND, n.x() - size / 2, n.y() - size / 2, size, border);
        KitDraw.shape(f.g, Shapes.Shape.ROUND, n.x() - size / 2 + 2, n.y() - size / 2 + 2, size - 4, fill);
        String name = f.font().plainSubstrByWidth(SkillsPage.name(row).getString(), 84);
        int nw = BookKit.smallWidth(f, name);
        int ly = n.y() >= cy ? n.y() + size / 2 + 2 : n.y() - size / 2 - 8;
        BookKit.small(f, Component.literal(name), Math.max(page.x(), Math.min(page.right() - nw - 4, n.x() - nw / 2)), ly, nw + 4, selected ? t.accent() : locked ? t.muted() : t.ink());
        Rect hit = new Rect(n.x() - size / 2 - 2, n.y() - size / 2 - 2, size + 4, size + 4);
        String id = "skill:" + row.id();
        f.target(id, hit, List.of(SkillsPage.name(row), SkillsPage.state(row)), () -> legacy.select(row.id()));
        if (f.focused(id)) {
            KitDraw.focusRing(f.g, hit, t.focusColor());
        }
    }

    private int tabs(HubScreen.Frame f, Rect s, int y) {
        List<SkillsPage.Tab> shown = new ArrayList<>(List.of(SkillsPage.Tab.OURS));
        var faction = SkillsPage.faction(f.view.player());
        faction.ifPresent(fa -> shown.add(SkillsPage.Tab.FACTION));
        if (LoadoutTab.available()) {
            shown.add(SkillsPage.Tab.LOADOUT);
        }
        List<String> labels = new ArrayList<>();
        for (SkillsPage.Tab tab : shown) {
            labels.add(tab == SkillsPage.Tab.FACTION ? faction.orElseThrow().getName().getString()
                    : Component.translatable(KEY + "tab." + tab.name().toLowerCase(java.util.Locale.ROOT)).getString());
        }
        return BookKit.filterRow(f, labels, 0, s.x(), y, s.w(), i -> legacy.tab(shown.get(i)));
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (legacy.tab() != SkillsPage.Tab.OURS || nodes.isEmpty() || ids.size() != nodes.size()) {
            return false;
        }
        Radial.Dir dir = switch (key) {
            case GLFW.GLFW_KEY_UP -> Radial.Dir.UP;
            case GLFW.GLFW_KEY_DOWN -> Radial.Dir.DOWN;
            case GLFW.GLFW_KEY_LEFT -> Radial.Dir.LEFT;
            case GLFW.GLFW_KEY_RIGHT -> Radial.Dir.RIGHT;
            default -> null;
        };
        if (dir == null) {
            return false;
        }
        int from = Math.max(0, ids.indexOf(legacy.selected()));
        legacy.select(ids.get(Radial.neighbour(nodes, from, dir)));
        return true;
    }
}
