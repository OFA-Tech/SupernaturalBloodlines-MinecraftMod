package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.NpcActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * "Pack wolves" (R148, R149): the recruited wolves and Tensura subordinates of the pack, one row each (head, name, source, order, distance, health), and under the
 * chosen one its profile (identity, bonds, presence, contribution, what an NPC does not have) with the orders as buttons. Every button runs a command row
 * (P21 to P24, see {@link NpcActions}) so the server's own checks apply. A Tensura subordinate keeps Tensura's orders: its buttons are off and say why.
 * Every row and button is a focus target, so the keyboard reaches the profile too. Drawn by every Pack page.
 */
public final class PackWolves {
    private static final String KEY = PageKit.HUB + "pack.wolves.";
    private static final int ROW = 20;

    private int selected;
    private static volatile int devSelected;

    /** Development only (the dev screenshots): the next Pack page drawn opens this wolf's profile. */
    public static void devSelect(int token) {
        devSelected = token;
    }

    /** The token of the chosen member, 0 for none (a dev screenshot picks one). */
    public void select(int token) {
        selected = token;
    }

    /** Draws the section from {@code y} and returns where it ends. Nothing at all when the pack has no wolves and the viewer cannot recruit one. */
    public int render(HubScreen.Frame f, Rect r, int y, boolean werewolf, boolean inlineProfile) {
        HubTheme t = f.theme();
        List<HubSocial.Npc> npcs = f.view.snapshot().npcs();
        if (devSelected != 0) {
            selected = devSelected;
            devSelected = 0;
        }
        if (npcs.isEmpty() && !werewolf) {
            return y;
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "title").withStyle(ChatFormatting.BOLD), r.x(), y + 2, r.w(), t.accent());
        if (npcs.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x() + 4, y, r.w() - 4, t.muted());
        }
        HubSocial.Npc chosen = null;
        for (HubSocial.Npc n : npcs) {
            boolean on = n.token() == selected;
            if (on) {
                chosen = n;
            }
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, ROW);
            if (on) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            f.g.renderItem(icon(n), r.x() + 1, y + 1);
            int textX = r.x() + 21;
            int barW = 34;
            int room = r.w() - 22 - barW - 4;
            f.g.drawString(f.font(), LabelFit.cut(n.name(), room, f.font()::width), textX, y, t.ink(), false);
            f.g.drawString(f.font(), LabelFit.cut(sub(n).getString(), room, f.font()::width), textX, y + 10, t.muted(), false);
            bar(f, r.right() - barW, y + 4, barW, n);
            String id = "npc:" + n.token();
            f.target(id, hit, tooltip(n), () -> selected = on ? 0 : n.token());
            if (f.focused(id)) {
                t.drawFocus(f.g, hit);
            }
            y += ROW + 1;
        }
        if (chosen != null && inlineProfile) {
            y = profile(f, r, y + 2, chosen);
        }
        if (werewolf) {
            Component label = Component.translatable(KEY + "recruit");
            Rect b = new Rect(r.x(), y + 3, PageWidgets.widthOf(f.font(), label), PageWidgets.H);
            PageWidgets.button(f, "pack:recruit", b, label, true, false, Component.translatable(KEY + "recruit_tip"), () -> f.screen().setPage(HubPage.RITES));
            y = b.bottom() + 3;
        }
        return y;
    }

    private static ItemStack icon(HubSocial.Npc n) {
        ResourceLocation id = ResourceLocation.tryParse(n.type());
        var type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
        return egg == null ? new ItemStack(Items.BONE) : new ItemStack(egg);
    }

    private static Component sourceName(HubSocial.Npc n) {
        return Component.translatable(KEY + "source." + n.source());
    }

    /** The short line under a name: a Brother's order and how far it is, a subordinate's kind (the tooltip and the profile have the rest). */
    static Component sub(HubSocial.Npc n) {
        if (!n.ours()) {
            return sourceName(n);
        }
        Component order = orderName(n);
        return n.distance() >= 0 ? Component.empty().append(order).append(" \u00b7 " + n.distance() + " m") : order;
    }

    private static Component orderName(HubSocial.Npc n) {
        return n.ours() ? Component.translatable(KEY + "order." + n.order()) : Component.translatable(KEY + "tensura_orders_short");
    }

    private static List<Component> tooltip(HubSocial.Npc n) {
        return List.of(Component.literal(n.name()), Component.translatable(KEY + "of", sourceName(n), n.owner()), orderName(n),
                Component.translatable(KEY + (n.loaded() ? "loaded" : "not_loaded")), Component.translatable(KEY + "open_profile"));
    }

    private static void bar(HubScreen.Frame f, int x, int y, int w, HubSocial.Npc n) {
        f.g.fill(x, y, x + w, y + 4, 0xFF3A2A2A);
        if (n.health() >= 0) {
            int hp = Math.max(1, w * Math.min(100, n.health()) / 100);
            f.g.fill(x, y, x + hp, y + 4, n.health() > 50 ? 0xFF2F6E4A : n.health() > 25 ? 0xFFB08A2E : 0xFF9A2A2A);
        }
    }

    /** The NPC profile: what the viewer may know about it. Sire, line, genes and anchor are "?" (an NPC has none a player can learn), each with its how-to. */
    int profile(HubScreen.Frame f, Rect r, int y, HubSocial.Npc n) {
        HubTheme t = f.theme();
        y = PageKit.title(f, Component.translatable(KEY + "profile", n.name()), r.x(), y, r.w(), t.ink());
        t.drawRule(f.g, r.x(), y, r.w());
        y += 3;
        for (Component line : lines(n)) {
            y = PageKit.wrapped(f, line, r.x() + 4, y, r.w() - 4, t.ink()) + 1;
        }
        Component unknown = Component.translatable(KEY + "unknown.all");
        List<Component> how = new ArrayList<>(List.of(unknown));
        for (String what : List.of("sire", "line", "genes", "anchor")) {
            how.add(Component.translatable(KEY + "unknown." + what + ".how"));
        }
        y = PageKit.wrapped(f, unknown, r.x() + 4, y, r.w() - 4, t.muted());
        Rect unknownHit = new Rect(r.x() + 3, y - PageKit.LINE - 1, r.w() - 4, PageKit.LINE + 1);
        f.target("npc:unknown", unknownHit, how, null);
        if (f.focused("npc:unknown")) {
            t.drawFocus(f.g, unknownHit);
        }
        return orders(f, r, y + 3, n);
    }

    private static Component typeName(HubSocial.Npc n) {
        ResourceLocation id = ResourceLocation.tryParse(n.type());
        return id == null ? Component.literal("?") : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(e -> e.getDescription()).orElse(Component.literal("?"));
    }

    /** Identity, bonds, presence and contribution, in this order. */
    private static List<Component> lines(HubSocial.Npc n) {
        return List.of(
                line("identity", n.ours() ? Component.translatable(KEY + "species_beast") : Component.translatable(KEY + "species_subordinate", typeName(n)),
                        Component.translatable(KEY + "rank_of", sourceName(n), n.owner())),
                line(n.days() < 0 ? "bonds_unknown" : "bonds", Component.literal(n.owner()), Component.literal(String.valueOf(Math.max(0, n.days())))),
                line(!n.loaded() ? "presence_away" : n.distance() >= 0 ? "presence_near" : "presence_far", Component.literal(String.valueOf(n.distance())), Component.literal(n.health() + "%")),
                n.ours() && n.weightPercent() == 0 ? line("contribution_over")
                        : line("contribution", Component.literal(String.format(java.util.Locale.ROOT, "%.2f", n.weightPercent() / 100.0))));
    }

    private static Component line(String key, Component... args) {
        return Component.translatable(KEY + "line." + key, (Object[]) args);
    }

    /** Closes the profile (the Grimoire shows it on the right page, where it replaces the ring until closed). */
    public void close() {
        selected = 0;
    }

    /** The wolf the viewer picked, if it is still in the snapshot. */
    @Nullable
    public HubSocial.Npc chosen(HubScreen.Frame f) {
        return f.view.snapshot().npcs().stream().filter(n -> n.token() == selected).findFirst().orElse(null);
    }

    /** The same profile and orders for a theme's info pane (the Blood Moon's bar, Tensura's pane). */
    public Detail detail(HubSocial.Npc n, boolean floatingBar) {
        Detail.Builder d = Detail.of(Component.literal(n.name())).subtitle(Component.translatable(KEY + "rank_of", sourceName(n), n.owner()));
        for (Component line : lines(n)) {
            d.body(line);
        }
        d.field(Component.translatable(KEY + "unknown.all"), Component.translatable(KEY + "unknown.how"));
        for (NpcActions.Act a : floatingBar ? NpcActions.forBar(n) : NpcActions.of(n)) {
            Detail.Confirm confirm = a.confirm() ? new Detail.Confirm(Component.translatable(KEY + "release_title", n.name()), Component.translatable(KEY + "release_body")) : null;
            boolean order = a.id().equals("follow") || a.id().equals("guard") || a.id().equals("rest");
            d.action(new Detail.Action("npc_" + a.id(), a.current() && order ? Component.literal("\u2022 ").append(label(a, n)) : label(a, n), false, a.enabled(),
                    a.enabled() ? null : Component.translatable(KEY + "why_" + a.why()), confirm, () -> HubClient.runCommand(a.rowId(), a.args())));
        }
        return d.build();
    }

    /** Follow, Guard, Rest, Hunt, Sort, Release (from {@link NpcActions}): each off with a reason when it cannot work now. */
    private int orders(HubScreen.Frame f, Rect r, int y, HubSocial.Npc n) {
        List<NpcActions.Act> acts = NpcActions.of(n);
        List<Component> labels = acts.stream().map(a -> label(a, n)).toList();
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y, r.w(), 0), labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList(), PageWidgets.H, 3);
        for (int i = 0; i < acts.size(); i++) {
            NpcActions.Act a = acts.get(i);
            Runnable run = a.confirm()
                    ? () -> f.confirm(Component.translatable(KEY + "release_title", n.name()), Component.translatable(KEY + "release_body"), Component.translatable(KEY + "release"),
                            () -> HubClient.runCommand(a.rowId(), a.args()))
                    : () -> HubClient.runCommand(a.rowId(), a.args());
            PageWidgets.button(f, "npc:" + n.token() + ":" + a.id(), flow.rects().get(i), labels.get(i), a.enabled(), a.current(),
                    a.enabled() ? null : Component.translatable(KEY + "why_" + a.why()), run);
        }
        return flow.bottom() + 2;
    }

    private static Component label(NpcActions.Act a, HubSocial.Npc n) {
        return switch (a.id()) {
            case "hunt" -> Component.translatable(KEY + (n.hunt() ? "hunt_on" : "hunt_off"));
            case "sort" -> Component.translatable(KEY + (n.sort() ? "sort_on" : "sort_off"));
            case "release" -> Component.translatable(KEY + "release");
            default -> Component.translatable(KEY + "order." + a.id());
        };
    }
}
