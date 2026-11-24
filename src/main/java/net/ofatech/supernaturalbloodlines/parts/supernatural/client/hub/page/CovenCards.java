package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.CovenClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * The coven's cards on the Group page (R250 to R268): the tier badge, the circle (open, join, leave, the three consent levels, the meter and the share), the
 * open vote with its Yes and No, and what a leader or an Elder may do with the member picked in the ledger. Drawn only for a coven and only from the two
 * views the server pushed; every button runs a row of {@code AdminCommands} (C1 to C10, P8), so the server checks it again. A pack's page never gets here.
 */
public final class CovenCards {
    private static final String KEY = PageKit.HUB + "coven.";
    private static final long CONFIRM_TICKS = 100;

    private CovenCards() {
    }

    /** What the consent button sends: the level's id, which the command parses back exactly. */
    static String consentArg(Consent level) {
        return level.id();
    }

    /** True when these cards belong on the page: the viewer is in a coven. */
    public static boolean applies(GroupView gv) {
        return gv.hasGroup() && gv.type() == GroupType.COVEN;
    }

    /** The coven's lines and buttons from {@code y}; returns the y under them. */
    public static int render(HubScreen.Frame f, Rect r, int y, GroupView gv, @Nullable HubSocial.Member chosen) {
        if (!applies(gv)) {
            return y;
        }
        CovenView cv = CovenClient.view();
        y = tier(f, r, y + 4, gv, cv);
        y = circle(f, r, y + 4, cv);
        y = votes(f, r, y + 4, gv, cv);
        return elders(f, r, y + 4, gv, chosen);
    }

    private static int tier(HubScreen.Frame f, Rect r, int y, GroupView gv, CovenView cv) {
        HubTheme t = f.theme();
        Component line = Component.translatable(KEY + "tier", Component.translatable(KEY + "tier." + cv.tier()), cv.size(), gv.sinceDays());
        y = PageKit.wrapped(f, line, r.x(), y, r.w(), t.accent());
        f.target("coven:tier", new Rect(r.x(), y - PageKit.LINE, r.w(), PageKit.LINE), List.of(line, Component.translatable(KEY + "tier.tip")), null);
        if (cv.shunnedDaysLeft() > 0) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "shunned", cv.shunnedDaysLeft()), r.x(), y, r.w(), PageWidgets.warn(t));
        }
        return y;
    }

    private static int circle(HubScreen.Frame f, Rect r, int y, CovenView cv) {
        HubTheme t = f.theme();
        y = PageKit.title(f, Component.translatable(KEY + "circle_title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        Component state = Component.translatable(KEY + "circle." + cv.circle().name().toLowerCase(java.util.Locale.ROOT), cv.anchorName());
        y = PageKit.wrapped(f, state, r.x(), y, r.w(), t.ink());
        if (!cv.circleNames().isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "circle_names", String.join(", ", cv.circleNames())), r.x(), y, r.w(), t.muted());
        }
        y += 2;
        y = circleButtons(f, r, y, cv);
        y = meter(f, r, y + 4, cv);
        y = consent(f, r, y + 4, cv);
        return share(f, r, y + 2, cv);
    }

    private static int circleButtons(HubScreen.Frame f, Rect r, int y, CovenView cv) {
        Component label;
        String id;
        Runnable run;
        switch (cv.circle()) {
            case ANCHOR -> {
                id = "coven:link_close";
                label = Component.translatable(KEY + "link_close");
                run = () -> HubClient.runCommand("C2", List.of());
            }
            case JOINED -> {
                id = "coven:link_leave";
                label = Component.translatable(KEY + "link_leave");
                run = () -> HubClient.runCommand("C4", List.of());
            }
            case CALLED -> {
                id = "coven:link_join";
                label = Component.translatable(KEY + "link_join");
                run = () -> HubClient.runCommand("C3", List.of());
            }
            default -> {
                id = "coven:link_open";
                label = Component.translatable(KEY + "link_open");
                run = () -> HubClient.runCommand("C1", List.of());
            }
        }
        Component why = Component.translatable(KEY + "circle_hint." + cv.circle().name().toLowerCase(java.util.Locale.ROOT));
        Rect b = new Rect(r.x(), y, Math.min(r.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
        PageWidgets.described(f, id, b, label, true, cv.circle() == CovenView.Circle.CALLED || cv.circle() == CovenView.Circle.NONE, why, run);
        return b.bottom() + 2;
    }
    private static int meter(HubScreen.Frame f, Rect r, int y, CovenView cv) {
        HubTheme t = f.theme();
        if (cv.meterCap() <= 0) {
            return y;
        }
        Component title = Component.translatable(KEY + "meter", cv.meter(), cv.meterCap());
        y = PageKit.wrapped(f, title, r.x(), y, r.w(), t.ink());
        int fill = (int) Math.round(r.w() * (cv.meter() / (double) cv.meterCap()));
        f.g.fill(r.x(), y, r.x() + r.w(), y + 5, (t.muted() & 0xFFFFFF) | 0x44000000);
        f.g.fill(r.x(), y, r.x() + Math.max(0, Math.min(r.w(), fill)), y + 5, t.accent());
        Component refill = cv.regenSeconds() <= 0 ? Component.translatable(KEY + "meter_full")
                : Component.translatable(KEY + "meter_refill", cv.regenSeconds() / 60, String.format("%02d", cv.regenSeconds() % 60));
        f.target("coven:meter", new Rect(r.x(), y - PageKit.LINE, r.w(), PageKit.LINE + 5), List.of(title, refill, Component.translatable(KEY + "meter.tip")), null);
        return PageKit.wrapped(f, refill, r.x(), y + 7, r.w(), t.muted());
    }

    private static int consent(HubScreen.Frame f, Rect r, int y, CovenView cv) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "consent"), r.x(), y, r.w(), t.accent());
        String base = "coven:consent";
        List<Component> labels = new ArrayList<>();
        for (Consent c : Consent.values()) {
            labels.add(Component.translatable(KEY + "consent." + c.id()));
        }
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y, r.w(), 0), labels.stream().map(l -> PageWidgets.widthOf(f.font(), l)).toList(), PageWidgets.H, PageWidgets.GAP);
        Consent[] all = Consent.values();
        for (int i = 0; i < all.length; i++) {
            Consent level = all[i];
            PageWidgets.described(f, base + ":" + level.id(), flow.rects().get(i), labels.get(i), true, cv.consent() == level,
                    Component.translatable(KEY + "consent." + level.id() + ".tip"), () -> HubClient.runCommand("C5", List.of(consentArg(level))));
        }
        return flow.bottom() + 2;
    }

    private static int share(HubScreen.Frame f, Rect r, int y, CovenView cv) {
        HubTheme t = f.theme();
        if (cv.circle() == CovenView.Circle.NONE || cv.circle() == CovenView.Circle.CALLED) {
            return y;
        }
        if (cv.shareKnown()) {
            return PageKit.wrapped(f, Component.translatable(KEY + "share", cv.sharePower(), cv.shareEp()), r.x(), y, r.w(), t.ink());
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "share_worst", cv.sharePower()), r.x(), y, r.w(), t.ink());
        return PageKit.wrapped(f, Component.translatable(KEY + "share_unknown"), r.x(), y, r.w(), t.muted());
    }

    private static int votes(HubScreen.Frame f, Rect r, int y, GroupView gv, CovenView cv) {
        HubTheme t = f.theme();
        y = PageKit.title(f, Component.translatable(KEY + "votes_title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        boolean elector = gv.viewerRank() == GroupView.LEADER || gv.viewerRank() == GroupView.ELDER;
        if (cv.vote().isEmpty()) {
            return PageKit.wrapped(f, Component.translatable(KEY + "no_vote"), r.x(), y, r.w(), t.muted());
        }
        CovenView.Vote v = cv.vote().get();
        y = PageKit.wrapped(f, Component.translatable(KEY + "vote." + v.kind().id(), v.subjectName()), r.x(), y, r.w(), t.ink());
        y = PageKit.wrapped(f, Component.translatable(KEY + "vote_counts", v.yes(), v.no(), v.electors(), v.hoursLeft()), r.x(), y, r.w(), t.muted());
        if (v.mine() != CovenView.Ballot.NONE) {
            return PageKit.wrapped(f, Component.translatable(KEY + "vote_mine." + v.mine().name().toLowerCase(java.util.Locale.ROOT)), r.x(), y, r.w(), t.muted());
        }
        if (!elector) {
            return PageKit.wrapped(f, Component.translatable(KEY + "vote_electors"), r.x(), y, r.w(), t.muted());
        }
        Component yes = Component.translatable(KEY + "vote_yes");
        Component no = Component.translatable(KEY + "vote_no");
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y + 2, r.w(), 0), List.of(PageWidgets.widthOf(f.font(), yes), PageWidgets.widthOf(f.font(), no)), PageWidgets.H, PageWidgets.GAP);
        PageWidgets.button(f, "coven:vote_yes", flow.rects().get(0), yes, true, true, null, () -> HubClient.runCommand("C9", List.of()));
        PageWidgets.button(f, "coven:vote_no", flow.rects().get(1), no, true, false, null, () -> HubClient.runCommand("C10", List.of()));
        return flow.bottom() + 2;
    }

    /** What the viewer may do with the member picked in the ledger: the same rules as the member's Profile. */
    private static int elders(HubScreen.Frame f, Rect r, int y, GroupView gv, @Nullable HubSocial.Member chosen) {
        HubTheme t = f.theme();
        String me = f.view.player().getName().getString();
        List<Act> plan = actionsFor(gv.viewerRank(), chosen == null ? null : chosen.base(), chosen != null && chosen.name().equals(me), chosen != null && chosen.id().isPresent());
        if (chosen == null || plan.isEmpty()) {
            return y;
        }
        y = PageKit.title(f, Component.translatable(KEY + "elders_title", chosen.name()), r.x(), y, r.w(), t.accent());
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

    /** One thing the leader or an Elder may do with a member: its button id, the row it runs, its lang key and whether it asks first. */
    public record Act(String id, String row, boolean confirm, String button) {
    }

    /** Pure: the Regent appoints and removes Elders; the Regent and the Elders may call a shunning vote on anyone but the Regent and themselves; an Elder may call for a new Regent naming another Elder. */
    public static List<Act> actionsFor(byte viewerRank, @Nullable String subjectBase, boolean self, boolean hasId) {
        List<Act> out = new ArrayList<>();
        if (subjectBase == null || self || !hasId) {
            return out;
        }
        boolean regent = viewerRank == GroupView.LEADER;
        boolean elder = viewerRank == GroupView.ELDER;
        if (regent && subjectBase.equals("member")) {
            out.add(new Act("elder_add", "C6", false, "coven:elder_add"));
        }
        if (regent && subjectBase.equals("elder")) {
            out.add(new Act("elder_remove", "C7", false, "coven:elder_remove"));
        }
        if ((regent || elder) && (subjectBase.equals("member") || subjectBase.equals("elder"))) {
            out.add(new Act("shun", "C8", true, "coven:shun"));
        }
        if (elder && subjectBase.equals("elder")) {
            out.add(new Act("call_regent", "P8", true, "pack:challenge"));
        }
        return out;
    }
}