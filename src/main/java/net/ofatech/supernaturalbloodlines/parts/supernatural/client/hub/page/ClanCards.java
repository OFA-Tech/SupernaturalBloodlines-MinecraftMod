package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.ClanClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * The clan's cards on the Group page (R270 to R279): what the head may do with the member picked in the ledger, and (later in plan 20c) the Charter and the
 * contest. Drawn only for a clan and only from the views the server pushed; every button runs a row of {@code AdminCommands} (C11 to C17), so the server checks it again.
 */
public final class ClanCards {
    static final String KEY = PageKit.HUB + "clan.";

    private ClanCards() {
    }

    /** True when these cards belong on the page: the viewer is in a clan. */
    public static boolean applies(GroupView gv) {
        return gv.hasGroup() && gv.type() == GroupType.CLAN;
    }

    /** The clan's lines and buttons from {@code y}; returns the y under them. */
    public static int render(HubScreen.Frame f, Rect r, int y, GroupView gv, @Nullable HubSocial.Member chosen) {
        if (!applies(gv)) {
            return y;
        }
        y = plan(f, r, y + 4, ClanPlan.of(ClanClient.view(), gv.viewerRank()));
        return members(f, r, y + 4, gv, chosen);
    }

    /** The four rows the cards run, spelled out so the hub's reference check can see each one. */
    private static void run(String row) {
        switch (row) {
            case "C11" -> HubClient.runCommand("C11", List.of());
            case "C12" -> HubClient.runCommand("C12", List.of());
            case "C16" -> HubClient.runCommand("C16", List.of());
            case "C17" -> HubClient.runCommand("C17", List.of());
            default -> { }
        }
    }

    static Component words(String key, List<Object> args) {
        return Component.translatable(key, args.stream().map(a -> a instanceof ClanPlan.Key k ? Component.translatable(k.key()) : a).toArray());
    }

    private static int plan(HubScreen.Frame f, Rect r, int y, ClanPlan.Plan plan) {
        HubTheme t = f.theme();
        for (ClanPlan.Line l : plan.lines()) {
            Component text = words(l.key(), l.args());
            if (l.tone() == ClanPlan.Tone.TITLE) {
                boolean whole = f.whole(y + 2, 14) && y + 2 >= f.clipTop();
                y = PageKit.title(f, text, r.x(), y + 2, r.w(), t.accent());
                if (whole) {
                    t.drawRule(f.g, r.x(), y + 2, r.w());
                }
                y += 6;
            } else {
                int color = l.tone() == ClanPlan.Tone.INK ? t.ink() : l.tone() == ClanPlan.Tone.WARN ? PageWidgets.warn(t) : t.muted();
                y = PageKit.wrapped(f, text, r.x(), y, r.w(), color);
            }
        }
        for (ClanPlan.Button b : plan.buttons()) {
            Component label = Component.translatable(KEY + b.label());
            Rect rect = new Rect(r.x(), y + 2, Math.min(r.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
            Component tip = Component.translatable(KEY + (b.why().isEmpty() ? b.tip() : b.why()));
            String row = b.row();
            PageWidgets.described(f, b.id(), rect, label, b.enabled(), b.primary(), tip, () -> run(row));
            y = rect.bottom() + 1;
        }
        return y;
    }
    /** What the head may do with the member picked in the ledger: the same rules as the member's Profile. */
    private static int members(HubScreen.Frame f, Rect r, int y, GroupView gv, @Nullable HubSocial.Member chosen) {
        HubTheme t = f.theme();
        String me = f.view.player().getName().getString();
        List<Act> plan = actionsFor(gv.viewerRank(), chosen == null ? null : chosen.base(), chosen != null && chosen.name().equals(me), chosen != null && chosen.id().isPresent());
        if (chosen == null || plan.isEmpty()) {
            return y;
        }
        y = PageKit.title(f, Component.translatable(KEY + "member_title", chosen.name()), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        UUID id = chosen.id().orElseThrow();
        for (Act a : plan) {
            Component label = Component.translatable(KEY + a.id());
            Rect b = new Rect(r.x(), y, Math.min(r.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
            Runnable go = () -> HubClient.runCommand(a.row(), List.of(id.toString()));
            Runnable press = a.confirm() ? () -> f.confirm(label, Component.translatable(KEY + a.id() + ".body", chosen.name()), label, go) : go;
            PageWidgets.described(f, a.button(), b, label, true, false, Component.translatable(KEY + a.id() + ".what", chosen.name()), press);
            y = b.bottom() + 3;
        }
        return y;
    }

    /** One thing the head may do with a member: its lang key, the row it runs, whether it asks first and its button id. */
    public record Act(String id, String row, boolean confirm, String button) {
    }

    /** Pure: the head promotes a Night Walker, demotes a Day Walker and exiles either (asked twice); nobody else has any of these, and nobody can act on the head or an ally. */
    public static List<Act> actionsFor(byte viewerRank, @Nullable String subjectBase, boolean self, boolean hasId) {
        List<Act> out = new ArrayList<>();
        if (subjectBase == null || self || !hasId || viewerRank != GroupView.LEADER) {
            return out;
        }
        if (subjectBase.equals("member")) {
            out.add(new Act("promote", "C13", false, "clan:promote"));
        }
        if (subjectBase.equals("day_walker")) {
            out.add(new Act("demote", "C14", false, "clan:demote"));
        }
        if (subjectBase.equals("member") || subjectBase.equals("day_walker")) {
            out.add(new Act("exile", "C15", true, "clan:exile"));
        }
        return out;
    }
}
