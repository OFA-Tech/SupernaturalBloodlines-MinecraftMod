package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.supernatural.VampirismSkillAccess;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubSkills;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.BuySkillPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.UnlockModSkillPayload;
import org.jetbrains.annotations.Nullable;

/**
 * Skills: our trees (bought with our SP through {@code BuySkillPayload}), the player's Vampirism or Werewolves tree (unlocked through
 * {@code UnlockModSkillPayload}, spending that mod's own points, read from its synced client data). The left page lists, the right page shows the chosen skill and its stamp.
 */
public final class SkillsPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "skills.";

    enum Tab { OURS, FACTION, LOADOUT }

    /** One skill of the faction's own tree, as the mod's client-side handler sees it. */
    private record ModSkill(String id, Component name, Component description, int cost, ISkillHandler.Result result) {
    }

    private final LoadoutTab loadout = new LoadoutTab();
    private Tab tab = Tab.OURS;
    private List<ModSkill> cached = List.of();
    private long cachedAt = Long.MIN_VALUE;
    @Nullable
    private IPlayableFaction<?> cachedFaction;
    @Nullable
    private String selected;

    Tab tab() {
        return tab;
    }

    void tab(Tab next) {
        tab = next;
        selected = null;
        loadout.reset();
    }

    @Nullable
    String selected() {
        return selected;
    }

    void select(String id) {
        selected = id;
    }

    @Override
    public void render(HubScreen.Frame f) {
        Optional<IPlayableFaction<?>> faction = faction(f.view.player());
        if (tab == Tab.FACTION && faction.isEmpty() || tab == Tab.LOADOUT && !LoadoutTab.available()) {
            tab = Tab.OURS;
        }
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = tabs(f, r, y, faction);
        switch (tab) {
            case OURS -> ours(f, r, y);
            case FACTION -> modSkills(f, r, y, faction.orElseThrow());
            case LOADOUT -> loadout.render(f, r, y);
        }
    }

    private int tabs(HubScreen.Frame f, Rect r, int y, Optional<IPlayableFaction<?>> faction) {
        int x = r.x();
        List<Tab> shown = new ArrayList<>(List.of(Tab.OURS));
        faction.ifPresent(fa -> shown.add(Tab.FACTION));
        if (LoadoutTab.available()) {
            shown.add(Tab.LOADOUT);
        }
        for (Tab t : shown) {
            Component full = t == Tab.FACTION ? faction.orElseThrow().getName() : Component.translatable(KEY + "tab." + t.name().toLowerCase(Locale.ROOT));
            // a long faction name is cut to the room left on the row, never drawn past the page
            int room = r.right() - x;
            if (room < 12) {
                break;
            }
            Component label = Component.literal(f.font().plainSubstrByWidth(full.getString(), room));
            int w = f.font().width(label);
            Rect hit = new Rect(x - 1, y - 1, w + 2, 11);
            int color = t == tab ? f.theme().accent() : f.theme().muted();
            f.g.drawString(f.font(), label, x, y, color, false);
            if (t == tab) {
                f.g.fill(x, y + 9, x + w, y + 10, color);
            }
            String id = "skilltab:" + t.name();
            f.target(id, hit, List.of(full), () -> {
                tab = t;
                selected = null;
                loadout.reset();
            });
            if (f.focused(id)) {
                f.theme().drawFocus(f.g, hit);
            }
            x += w + 8;
        }
        f.theme().drawRule(f.g, r.x(), y + 13, r.w());
        return y + 17;
    }

    private void ours(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "sp", f.view.state().sp()), r.x(), y, r.w(), t.ink()) + 2;
        List<HubSkills.Row> rows = HubSkills.rows(f.view.skills(), f.view.state(), f.view.races(),
                spec -> VampirismSkillAccess.covers(net.minecraft.client.Minecraft.getInstance().player, spec));
        HubSkills.Row chosen = rows.stream().filter(row -> row.id().equals(selected)).findFirst()
                .orElse(rows.stream().filter(row -> row.state() == HubSkills.State.BUYABLE).findFirst().orElse(rows.isEmpty() ? null : rows.get(0)));
        SupernaturalRace header = null;
        boolean dormantHeader = false;
        boolean many = f.view.races().size() > 1;
        for (HubSkills.Row row : rows) {
            if (row.race() != null && many && row.race() != header) {
                header = row.race();
                y = PageKit.wrapped(f, PageKit.race(header), r.x(), y + 1, r.w(), t.accent());
            } else if (row.race() == null && !dormantHeader) {
                dormantHeader = true;
                y = PageKit.wrapped(f, Component.translatable(KEY + "dormant_header"), r.x(), y + 1, r.w(), t.accent());
            }
            Component cost = row.state() == HubSkills.State.DORMANT ? Component.translatable(KEY + "dormant")
                    : row.state() == HubSkills.State.OWNED ? Component.translatable(KEY + "owned")
                    : row.state() == HubSkills.State.COVERED ? Component.translatable(KEY + "covered") : Component.translatable(KEY + "cost", row.cost());
            y = line(f, r, y, "skill:" + row.id(), mark(row.state()), name(row), cost, row == chosen,
                    List.of(name(row), state(row)), () -> selected = row.id());
        }
        if (rows.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        f.end(y);
        ourDetail(f, chosen);
    }

    private static RungStatus mark(HubSkills.State s) {
        return switch (s) {
            case OWNED -> RungStatus.REACHED;
            case BUYABLE -> RungStatus.CURRENT;
            case COVERED -> RungStatus.REACHED;
            case LOCKED, DORMANT -> RungStatus.LOCKED;
        };
    }

    static Component name(HubSkills.Row row) {
        return Component.translatableWithFallback(PageKit.HUB + "skill." + row.id(), row.name());
    }

    static Component state(HubSkills.Row row) {
        return switch (row.state()) {
            case OWNED -> Component.translatable(KEY + "state.owned");
            case BUYABLE -> Component.translatable(KEY + "state.buyable", row.cost());
            case DORMANT -> Component.translatable(KEY + "state.dormant");
            case COVERED -> Component.translatable(KEY + "state.covered");
            case LOCKED -> Component.translatable(row.why().langKey());
        };
    }

    /** One list line: a mark, the name, a note on the right. Returns the y under it. */
    private static int line(HubScreen.Frame f, Rect r, int y, String id, RungStatus mark, Component name, Component note, boolean chosen,
                            List<Component> tooltip, Runnable select) {
        HubTheme t = f.theme();
        Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, PageKit.LINE + 1);
        if (chosen) {
            f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
        }
        t.drawRungMark(f.g, r.x(), y, mark);
        int nw = f.font().width(note);
        String text = f.font().plainSubstrByWidth(name.getString(), r.w() - HubTheme.MARK - 4 - nw - 4);
        f.g.drawString(f.font(), text, r.x() + HubTheme.MARK + 4, y + 1, mark == RungStatus.LOCKED ? t.muted() : t.ink(), false);
        f.g.drawString(f.font(), note, r.right() - nw, y + 1, t.muted(), false);
        f.target(id, hit, tooltip, select);
        if (f.focused(id)) {
            t.drawFocus(f.g, hit);
        }
        return y + PageKit.LINE + 1;
    }

    private static void ourDetail(HubScreen.Frame f, @Nullable HubSkills.Row row) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.SIDE);
        int y = f.begin(HubLayout.SIDE);
        if (row == null) {
            f.end(y);
            return;
        }
        y = PageKit.title(f, name(row), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        String desc = PageKit.HUB + "skill_desc." + row.id();
        if (I18n.exists(desc)) {
            y = PageKit.wrapped(f, Component.translatable(desc), r.x(), y, r.w(), t.ink()) + 3;
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "cost_line", row.cost()), r.x(), y, r.w(), t.ink());
        if (row.entry() != null && row.race() != null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "needs_rung", PageKit.race(row.race()), row.entry().rung()), r.x(), y, r.w(), t.ink());
            if (row.entry().after() != null) {
                String after = row.entry().after();
                Component afterName = f.view.skills().skill(after).map(s -> (Component) Component.translatableWithFallback(PageKit.HUB + "skill." + after, s.name()))
                        .orElse(Component.literal(after));
                y = PageKit.wrapped(f, Component.translatable(KEY + "after", afterName), r.x(), y, r.w(), t.ink());
            }
            if (row.entry().quest() != null) {
                String q = row.entry().quest();
                Component questName = f.view.skills().quest(q).map(s -> (Component) Component.translatableWithFallback(PageKit.HUB + "quest." + q, s.name()))
                        .orElse(Component.literal(q));
                y = PageKit.wrapped(f, Component.translatable(KEY + "quest", questName), r.x(), y, r.w(), t.ink());
            }
        }
        y = PageKit.wrapped(f, state(row), r.x(), y + 3, r.w(), row.state() == HubSkills.State.BUYABLE ? t.good() : t.muted());
        if (row.state() == HubSkills.State.BUYABLE) {
            String id = row.id();
            y = stamp(f, r, y, "buy:" + id, Component.translatable(KEY + "buy"), () -> PacketDistributor.sendToServer(new BuySkillPayload(id)));
        }
        f.end(y);
    }

    static void buy(String id) {
        PacketDistributor.sendToServer(new BuySkillPayload(id));
    }

    /** A wax stamp with its word under it, centred on the page; the server checks what it sends. Returns the y under it. */
    static int stamp(HubScreen.Frame f, Rect r, int y, String id, Component word, Runnable action) {
        Rect s = new Rect(r.x() + (r.w() - HubTheme.STAMP) / 2, y + 6, HubTheme.STAMP, HubTheme.STAMP);
        f.theme().drawStamp(f.g, s.x(), s.y(), f.hovered(s) || f.focused(id));
        f.target(id, s, List.of(word), action);
        if (f.focused(id)) {
            f.theme().drawFocus(f.g, s);
        }
        return PageKit.centered(f, word, r, s.bottom() + 3, f.theme().accent());
    }

    static Optional<IPlayableFaction<?>> faction(Player p) {
        IPlayableFaction<?> fa = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return Optional.ofNullable(fa);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ModSkill> modSkills(Player p, IPlayableFaction<?> faction) {
        Optional<ISkillHandler> handler = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().map(fp -> (ISkillHandler) fp.getSkillHandler());
        List<ModSkill> out = new ArrayList<>();
        if (handler.isEmpty()) {
            return out;
        }
        for (var entry : VampirismRegistries.SKILL.get().entrySet()) {
            ISkill skill = entry.getValue();
            if (skill.getFaction().filter(fa -> fa == faction).isEmpty()) {
                continue;
            }
            ISkillHandler.Result result = handler.get().canSkillBeEnabled(skill);
            if (result == ISkillHandler.Result.NOT_FOUND) {
                continue; // in a tree the player has not opened: the mod's own screen does not show it either
            }
            out.add(new ModSkill(entry.getKey().location().toString(), skill.getName(), skill.getDescription(), skill.getSkillPointCost(), result));
        }
        out.sort(Comparator.comparingInt(s -> s.result() == ISkillHandler.Result.OK ? 0 : s.result() == ISkillHandler.Result.ALREADY_ENABLED ? 2 : 1));
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int points(Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().map(fp -> ((ISkillHandler) fp.getSkillHandler()).getLeftSkillPoints()).orElse(0);
    }

    private void modSkills(HubScreen.Frame f, Rect r, int y, IPlayableFaction<?> faction) {
        HubTheme t = f.theme();
        Player p = f.view.player();
        y = PageKit.wrapped(f, Component.translatable(KEY + "mod_points", faction.getName(), points(p)), r.x(), y, r.w(), t.ink()) + 2;
        long now = p.level().getGameTime();
        if (cachedAt != now || cachedFaction != faction) {
            // the registry walk and the mod's checks run once per game tick, not once per frame
            cached = modSkills(p, faction);
            cachedAt = now;
            cachedFaction = faction;
        }
        List<ModSkill> skills = cached;
        ModSkill chosen = skills.stream().filter(s -> s.id().equals(selected)).findFirst().orElse(skills.isEmpty() ? null : skills.get(0));
        for (ModSkill s : skills) {
            RungStatus mark = s.result() == ISkillHandler.Result.ALREADY_ENABLED ? RungStatus.REACHED
                    : s.result() == ISkillHandler.Result.OK ? RungStatus.CURRENT : RungStatus.LOCKED;
            Component note = s.result() == ISkillHandler.Result.ALREADY_ENABLED ? Component.translatable(KEY + "owned") : Component.translatable(KEY + "cost_mod", s.cost());
            y = line(f, r, y, "modskill:" + s.id(), mark, s.name(), note, s == chosen, List.of(s.name(), reason(s)), () -> selected = s.id());
        }
        if (skills.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        f.end(y);

        Rect side = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        if (chosen != null) {
            sy = PageKit.title(f, chosen.name(), side.x(), sy, side.w(), t.accent());
            t.drawRule(f.g, side.x(), sy + 2, side.w());
            sy = PageKit.wrapped(f, chosen.description(), side.x(), sy + 6, side.w(), t.ink()) + 3;
            sy = PageKit.wrapped(f, Component.translatable(KEY + "cost_mod_line", chosen.cost(), faction.getName()), side.x(), sy, side.w(), t.ink());
            sy = PageKit.wrapped(f, reason(chosen), side.x(), sy + 3, side.w(), chosen.result() == ISkillHandler.Result.OK ? t.good() : t.muted());
            if (chosen.result() == ISkillHandler.Result.OK) {
                String id = chosen.id();
                sy = stamp(f, side, sy, "unlock:" + id, Component.translatable(KEY + "unlock"),
                        () -> PacketDistributor.sendToServer(new UnlockModSkillPayload(id)));
            }
        }
        f.end(sy);
    }

    private static Component reason(ModSkill s) {
        return s.result() == ISkillHandler.Result.OK ? Component.translatable(KEY + "state.unlockable")
                : Component.translatable("supernaturalbloodlines.supernatural.modskill.refuse." + s.result().name().toLowerCase(Locale.ROOT));
    }
}
