package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * "Pack wolves" on the Pack page (R148, R149): the pack's recruited wolves and Tensura subordinates as rows under the players (name, what it is, what it was told),
 * and, for the one picked, its NPC profile: identity, bonds, presence, what it adds to the pack bonus and the things the hub never shows about an NPC (sire, line,
 * genes: a "?" with how to find out). Actions for a Brother that the viewer may command: Follow, Guard, Rest, Hunt and Sort on or off, Release (asks first).
 * A Tensura subordinate keeps Tensura's own orders, so it only shows the reason. The wolf is named by the ledger's token, never by an entity id.
 */
public final class PackWolves {
    private static final String KEY = PageKit.HUB + "pack.wolves.";

    private int picked;

    public PackWolves() {
    }

    /**
     * The whole section for a Pack page: the rows, the picked wolf's profile with its actions, and (for a werewolf) the "Recruit a wolf" link to the Rites page.
     * Returns the new y.
     */
    public int render(HubScreen.Frame f, Rect r, int y, boolean werewolf) {
        List<HubSocial.Npc> npcs = f.view.snapshot().npcs();
        HubSocial.Npc chosen = npcs.stream().filter(n -> n.token() == picked).findFirst().orElse(null);
        y = rows(f, r, y, npcs, chosen, token -> picked = token);
        if (chosen != null) {
            y = profile(f, r, y, chosen);
            y = links(f, r, y, chosen);
        }
        if (werewolf) {
            BookKit.link(f, "pack:recruit", Component.translatable(KEY + "recruit"), r.x(), y + 2, List.of(Component.translatable(KEY + "recruit_for")),
                    () -> f.screen().setPage(net.ofatech.supernaturalbloodlines.shared.hub.HubPage.RITES));
            y += 14;
        }
        return y;
    }

    /** The row lines of the page: one per wolf, the picked one tinted. Returns the new y. */
    static int rows(HubScreen.Frame f, Rect r, int y, List<HubSocial.Npc> npcs, @Nullable HubSocial.Npc chosen, java.util.function.IntConsumer pick) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "title"), r.x(), y + 2, r.w(), t.accent());
        if (npcs.isEmpty()) {
            return PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        for (HubSocial.Npc n : npcs) {
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, 11);
            if (chosen != null && chosen.token() == n.token()) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            Component what = what(n);
            BookKit.leader(f, Component.literal(net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(n.name(), Math.max(20, r.w() - f.font().width(what) - 14), f.font()::width)),
                    what, r.x() + 4, y, r.w() - 4, t.muted());
            String id = "npc:" + n.token();
            f.target(id, hit, List.of(Component.literal(n.name()), what, Component.translatable(KEY + (n.loaded() ? "here" : "away"))), () -> pick.accept(n.token()));
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            y += 11;
        }
        return y;
    }

    /** "Brother, guarding" or "Subordinate". */
    static Component what(HubSocial.Npc n) {
        Component source = Component.translatable(KEY + "source." + n.source());
        return n.ours() && !n.order().isEmpty() ? Component.empty().append(source).append(", ").append(Component.translatable(KEY + "order." + n.order())) : source;
    }

    /** The picked wolf's profile, as lines of text. Returns the new y. */
    static int profile(HubScreen.Frame f, Rect r, int y, HubSocial.Npc n) {
        HubTheme t = f.theme();
        y = PageKit.title(f, Component.literal(n.name()), r.x(), y + 2, r.w(), t.ink());
        for (Component line : profileLines(n)) {
            y = PageKit.wrapped(f, line, r.x(), y, r.w(), t.muted());
        }
        return y + 2;
    }

    /** Identity, bonds, presence, contribution and the unknowns, in this order. Pure apart from lang. */
    static List<Component> profileLines(HubSocial.Npc n) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable(KEY + "identity", kind(n.type()), Component.translatable(KEY + "source." + n.source())));
        out.add(Component.translatable(KEY + "bonds", n.owner(), n.days()));
        out.add(n.loaded() ? Component.translatable(KEY + "presence_here", n.distance() < 0 ? Component.translatable(KEY + "far") : Component.literal(n.distance() + " m"), Math.max(0, n.health()))
                : Component.translatable(KEY + "presence_away"));
        out.add(Component.translatable(KEY + "contribution", n.weightPercent()));
        out.add(Component.translatable(KEY + "unknown"));
        return out;
    }

    private static Component kind(String typeId) {
        ResourceLocation id = ResourceLocation.tryParse(typeId);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.get(id).getDescription() : Component.literal(typeId);
    }

    /** Whether each action is on offer for this wolf and, when not, why (a lang suffix). Pure. */
    public static String refusal(HubSocial.Npc n) {
        if (!n.canOrder()) {
            return "not_yours";
        }
        if (!n.ours()) {
            return "tensura";
        }
        return n.loaded() ? "" : "not_loaded";
    }

    /** The actions of the picked wolf, each with the command it runs (typed command text, server-checked). */
    public static List<Action> actions(HubSocial.Npc n) {
        String why = refusal(n);
        List<Action> out = new ArrayList<>();
        for (String order : List.of("follow", "guard", "rest")) {
            out.add(new Action("order_" + order, "order." + order, "pack npc order " + n.token() + " " + order, why, false, order.equals(n.order())));
        }
        out.add(new Action("hunt", n.hunt() ? "hunt_off" : "hunt_on", "pack npc hunt " + n.token(), why, false, false));
        out.add(new Action("sort", n.sort() ? "sort_off" : "sort_on", "pack npc sort " + n.token(), why.equals("not_loaded") ? why : why, false, false));
        out.add(new Action("release", "release", "pack npc release " + n.token(), why.equals("not_loaded") ? "" : why, true, false));
        return out;
    }

    /** One button: {@code lang} a suffix of the pack.wolves keys, {@code why} empty when it is on offer, {@code confirm} whether it asks first, {@code current} the order now. */
    public record Action(String id, String lang, String command, String why, boolean confirm, boolean current) {
        public boolean enabled() {
            return why.isEmpty();
        }
    }

    /** The links of the Grimoire's ledger. Returns the new y. */
    static int links(HubScreen.Frame f, Rect r, int y, HubSocial.Npc n) {
        for (Action a : actions(n)) {
            Component label = Component.translatable(KEY + a.lang());
            List<Component> tip = List.of(a.enabled() ? label : Component.translatable(KEY + "why." + a.why()));
            Runnable run = !a.enabled() ? null : a.confirm()
                    ? () -> f.confirm(Component.translatable(KEY + "release_title", n.name()), Component.translatable(KEY + "release_body"), label, () -> BloodlinePage.command(a.command()))
                    : () -> BloodlinePage.command(a.command());
            BookKit.link(f, "npc:" + n.token() + ":" + a.id(), a.current() ? Component.literal("\u2022 ").append(label) : label, r.x(), y, tip, run);
            y += 12;
        }
        return y;
    }

    /** The same actions in a theme's detail strip or info pane. */
    static void detailActions(HubScreen.Frame f, Detail.Builder d, HubSocial.Npc n) {
        for (Action a : actions(n)) {
            Component label = Component.translatable(KEY + a.lang());
            Runnable run = a.confirm()
                    ? () -> f.confirm(Component.translatable(KEY + "release_title", n.name()), Component.translatable(KEY + "release_body"), label, () -> BloodlinePage.command(a.command()))
                    : () -> BloodlinePage.command(a.command());
            d.action(new Detail.Action("npc_" + a.id(), a.current() ? Component.literal("\u2022 ").append(label) : label, false, a.enabled(),
                    a.enabled() ? null : Component.translatable(KEY + "why." + a.why()), null, run));
        }
    }

    /** Lowercase source word for a lang suffix. */
    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
