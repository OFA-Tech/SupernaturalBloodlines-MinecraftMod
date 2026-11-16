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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Radial;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SkillIcons;
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
    private static final int GOLD = 0xFFB8903A;
    private final SkillsPage legacy = new SkillsPage();
    private final boolean moon;
    private final Shapes.Shape shape;

    public BookSkillsPage() {
        this(false);
    }

    /** @param moon the Blood Moon's version: the tree round the moon on the stage and the chosen skill in the floating bar, no left page */
    public BookSkillsPage(boolean moon) {
        this(moon, Shapes.Shape.ROUND);
    }

    /** @param shape the node's outline: round for the Blood Moon, hexagons for Tensura's status window */
    public BookSkillsPage(boolean moon, Shapes.Shape shape) {
        this.moon = moon;
        this.shape = shape;
    }

    /** True while another tab (a mod's own tree, the loadout slots) is showing, which draws into the two list cards. */
    public boolean legacyActive() {
        return legacy.tab() != SkillsPage.Tab.OURS;
    }
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

        if (moon) {
            moon(f, rows, chosen);
            return;
        }
        Rect r = f.region(BookKit.MAIN);
        Component chapter = Component.empty();
        for (int i = 0; i < f.view.races().size(); i++) {
            chapter = ((net.minecraft.network.chat.MutableComponent) chapter).append(i == 0 ? Component.empty() : Component.literal(" \u00b7 ")).append(PageKit.race(f.view.races().get(i)));
        }
        if (chosen != null) {
            f.reserve(BookKit.MAIN, BookKit.SEAL + 8); // the LEARN seal is pinned under the text, never cut
        }
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(PageKit.HUB + "page.skills"), Component.translatable(BOOK + "chapter", chapter));
        if (chosen == null) {
            f.end(PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted()));
        } else {
            f.end(chosenSkill(f, r, y, chosen));
            pinned(f, r, chosen);
        }
        tree(f, rows, chosen, BookKit.SIDE);
    }

    /** The Blood Moon page: title, the tree round the moon, and the chosen skill in the bar. */
    private void moon(HubScreen.Frame f, List<HubSkills.Row> rows, HubSkills.Row chosen) {
        Component chapter = Component.empty();
        for (int i = 0; i < f.view.races().size(); i++) {
            chapter = ((net.minecraft.network.chat.MutableComponent) chapter).append(i == 0 ? Component.empty() : Component.literal(" \u00b7 ")).append(PageKit.race(f.view.races().get(i)));
        }
        BookKit.moonHead(f, Component.translatable(PageKit.HUB + "page.skills"), chapter);
        tree(f, rows, chosen, BookKit.MOON);
        if (chosen != null) {
            boolean known = chosen.state() == HubSkills.State.OWNED || chosen.state() == HubSkills.State.COVERED;
            String id = chosen.id();
            Detail.Builder d = Detail.of(SkillsPage.name(chosen)).pips(known ? 1 : 0, 1).body(SkillsPage.state(chosen));
            d.action("buy", Component.translatable(BOOK + "learn_cost", chosen.cost()), true, chosen.state() == HubSkills.State.BUYABLE, () -> SkillsPage.buy(id));
            if (f.view.context().loadout()) {
                d.action("hud", Component.translatable(BOOK + "hud_link"), false, true, () -> f.screen().setPage(HubPage.LOADOUT));
            }
            f.detail(d.build());
        }
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
        return y;
    }

    /** The LEARN seal and the link to the Loadout, at the foot of the page above the page number. */
    private void pinned(HubScreen.Frame f, Rect r, HubSkills.Row row) {
        int y = r.bottom() - BookKit.SEAL;
        String id = row.id();
        BookKit.seal(f, "buy:" + id, Component.translatable(KEY + "buy"), row.state() == HubSkills.State.BUYABLE, r.x(), y, () -> SkillsPage.buy(id),
                List.of(Component.translatable(KEY + "buy"), SkillsPage.state(row)));
        if (f.view.context().loadout()) {
            BookKit.link(f, "link:hud", Component.translatable(BOOK + "hud_link"), r.x() + BookKit.SEAL + 8, y + BookKit.SEAL / 2 - 4,
                    List.of(Component.translatable(BOOK + "hud_tip")), () -> f.screen().setPage(HubPage.LOADOUT));
        }
    }

    private void tree(HubScreen.Frame f, List<HubSkills.Row> rows, HubSkills.Row chosen, String region) {
        HubTheme t = f.theme();
        Rect s = f.region(region);
        int y = f.begin(region);
        y = tabs(f, s, y);
        int top = y + 4;
        int count = Math.max(1, rows.size());
        int cx = s.x() + s.w() / 2;
        int cy = top + (s.bottom() - top) / 2;
        // a node is 16 to 20 px and its name takes two small lines under (or over) it; keep all of that inside the page, above the page number
        int radius = Math.max(26, Math.min(s.w() / 2 - 30, (s.bottom() - top) / 2 - 10 - 18));
        int inner = Math.max(14, radius * 9 / 20);
        Rect centre = new Rect(cx - inner - 2, cy - inner - 2, 2 * inner + 4, 2 * inner + 4);
        List<Radial.Node> ring = Radial.ring(cx, cy, radius, count);
        nodes = rows.isEmpty() ? List.of() : ring;
        ids = rows.stream().map(HubSkills.Row::id).toList();
        for (int i = 0; i < rows.size(); i++) {
            Radial.Node n = ring.get(i);
            double a = Math.atan2(n.y() - cy, n.x() - cx);
            int sx = cx + (int) Math.round((inner + 4) * Math.cos(a));
            int sy = cy + (int) Math.round((inner + 4) * Math.sin(a));
            boolean locked = rows.get(i).state() == HubSkills.State.LOCKED || rows.get(i).state() == HubSkills.State.DORMANT;
            KitDraw.line(f.g, sx, sy, n.x() - (int) Math.round(9 * Math.cos(a)), n.y() - (int) Math.round(9 * Math.sin(a)), locked ? (t.muted() & 0xFFFFFF) | 0x70000000 : t.muted());
        }
        f.emblem(centre);
        int labelWidth = net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.radialWidth(radius, count, 36, 110);
        for (int i = 0; i < rows.size(); i++) {
            node(f, s, rows.get(i), ring.get(i), cy, rows.get(i) == chosen, labelWidth);
        }
        f.end(s.bottom());
    }

    /** One skill: owned is filled, available is ringed in gold, locked is a faint dotted ring; its icon inside and its name in two small lines. */
    private void node(HubScreen.Frame f, Rect page, HubSkills.Row row, Radial.Node n, int cy, boolean selected, int labelWidth) {
        HubTheme t = f.theme();
        boolean known = row.state() == HubSkills.State.OWNED || row.state() == HubSkills.State.COVERED;
        boolean locked = row.state() == HubSkills.State.LOCKED || row.state() == HubSkills.State.DORMANT;
        int size = 18;
        int x = n.x() - size / 2;
        int y = n.y() - size / 2;
        if (selected) {
            KitDraw.shape(f.g, shape, x - 3, y - 3, size + 6, t.accent());
            KitDraw.shape(f.g, shape, x - 2, y - 2, size + 4, t.paper());
        }
        Icons icon = SkillIcons.of(row.id());
        if (known) {
            KitDraw.shape(f.g, shape, x, y, size, t.good());
            KitDraw.icon(f.g, icon, n.x() - 6, n.y() - 6, 12, t.paper());
        } else if (locked) {
            KitDraw.tickRing(f.g, n.x(), n.y(), size / 2, 16, 0f, (t.muted() & 0xFFFFFF) | 0xA0000000, (t.muted() & 0xFFFFFF) | 0xA0000000);
            KitDraw.icon(f.g, icon, n.x() - 6, n.y() - 6, 12, (t.muted() & 0xFFFFFF) | 0x80000000);
        } else {
            KitDraw.shape(f.g, shape, x, y, size, GOLD);
            KitDraw.shape(f.g, shape, x + 2, y + 2, size - 4, t.paper());
            KitDraw.icon(f.g, icon, n.x() - 6, n.y() - 6, 12, t.ink());
        }
        List<net.minecraft.util.FormattedCharSequence> lines = f.font().split(SkillsPage.name(row), (int) (labelWidth / 0.75f));
        if (lines.size() > 2) {
            lines = lines.subList(0, 2);
        }
        int height = (int) Math.ceil(lines.size() * 9 * 0.75f);
        int ly = n.y() >= cy ? n.y() + size / 2 + 2 : n.y() - size / 2 - 2 - height;
        int color = selected ? t.accent() : locked ? t.muted() : t.ink();
        for (var line : lines) {
            int lw = (int) Math.ceil(f.font().width(line) * 0.75f);
            int lx = Math.max(page.x(), Math.min(page.right() - lw, n.x() - lw / 2));
            var pose = f.g.pose();
            pose.pushPose();
            pose.translate(lx, ly, 0);
            pose.scale(0.75f, 0.75f, 1f);
            f.g.drawString(f.font(), line, 0, 0, color, false);
            pose.popPose();
            ly += 7;
        }
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
        return moon ? BookKit.pillRow(f, labels, 0, s.x(), y, s.w(), i -> legacy.tab(shown.get(i)))
                : BookKit.filterRow(f, labels, 0, s.x(), y, s.w(), i -> legacy.tab(shown.get(i)));
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
