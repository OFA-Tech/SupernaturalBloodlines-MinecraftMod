package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.ProfileClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;

/**
 * One person, one page (the IA doc 3.6): what this player may know of them, "?" for what they may not (with how to find out), and the
 * actions their relationship allows, in the theme's Detail area. A route, not a nav page: the origin stays lit and Esc goes back.
 */
public final class ProfilePage implements HubPageView {
    static final String KEY = PageKit.HUB + "profile.";

    /** The facts in the order and groups of the page. */
    static final Map<String, List<Fact>> GROUPS = new java.util.LinkedHashMap<>(Map.of());

    static {
        GROUPS.put("identity", List.of(Fact.SPECIES, Fact.RANK, Fact.LEVEL));
        GROUPS.put("bonds", List.of(Fact.PACK, Fact.PACK_DAYS, Fact.SIRE, Fact.LINE, Fact.ANCHOR));
        GROUPS.put("secrets", List.of(Fact.GENES));
        GROUPS.put("presence", List.of(Fact.PRESENCE));
        GROUPS.put("contribution", List.of(Fact.BONUS));
    }

    /** How a known value is worded: a translation when one exists for the fact, else the value as sent. */
    static Component value(Fact fact, List<String> values) {
        if (values.isEmpty()) {
            return Component.translatable(KEY + "none");
        }
        List<Component> parts = new ArrayList<>();
        for (String v : values) {
            parts.add(fact == Fact.SPECIES || fact == Fact.RANK || fact == Fact.GENES || fact == Fact.PRESENCE
                    ? Component.translatableWithFallback(KEY + "value." + fact.name().toLowerCase(Locale.ROOT) + "." + v, HubModel.pretty(v)) : Component.literal(v));
        }
        Component out = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            out = Component.empty().append(out).append(", ").append(parts.get(i));
        }
        return out;
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubScreen.ProfileRoute route = f.screen().profile();
        HubTheme t = f.theme();
        ProfilePayload payload = route == null ? null : ProfileClient.of(route.id());
        Rect r = f.region(HubLayout.MAIN);
        Rect bar = f.layout().chrome().detail();
        if (bar != null && bar.h() < 60 && bar.y() > r.y()) {
            f.reserve(HubLayout.MAIN, Math.max(0, r.bottom() - bar.y() + 4)); // the Blood Moon's floating bar sits over the foot of the page
        }
        int y = f.begin(HubLayout.MAIN);
        if (route == null) {
            f.end(y);
            return;
        }
        Component back = Component.translatable(KEY + "back", Component.translatable(route.origin().langKey()));
        Rect b = new Rect(r.x(), y, Math.min(r.w(), PageWidgets.widthOf(f.font(), back)), PageWidgets.H);
        PageWidgets.button(f, "stage:profile:back", b, back, true, false, null, f.screen()::back);
        y = b.bottom() + 4;
        if (route.op()) {
            Component banner = Component.translatable(KEY + "op_banner");
            int h = f.font().split(banner, r.w() - 8).size() * PageKit.LINE + 4;
            f.g.fill(r.x(), y, r.right(), y + h, t.accent());
            int yy = y + 2;
            for (var line : f.font().split(banner, r.w() - 8)) {
                f.g.drawString(f.font(), line, r.x() + 4, yy, Contrast.bestText(t.accent()), false);
                yy += PageKit.LINE;
            }
            y += h + 4;
        }
        y = PageKit.title(f, Component.literal(route.name()), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        if (payload == null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "loading"), r.x(), y, r.w(), t.muted());
        } else {
            for (Map.Entry<String, List<Fact>> group : GROUPS.entrySet()) {
                y = PageKit.wrapped(f, Component.translatable(KEY + "group." + group.getKey()).withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), t.accent());
                for (Fact fact : group.getValue()) {
                    y = fact(f, r, y, fact, payload);
                }
                y += 3;
            }
        }
        if (bar != null && bar.h() < 60) {
            // the Blood Moon has no side column of its own: the legend closes the page
            y = PageKit.wrapped(f, Component.translatable(KEY + "legend"), r.x(), y + 2, r.w(), t.muted());
        }
        y = PageWidgets.reply(f, r, y + 2);
        f.end(y);

        f.detail(detail(f, route));
        if (f.layout().chrome().detail() != null && f.layout().chrome().detail().h() >= 60) {
            // the actions went to the theme's own pane or bar: this page says what a ? means
            Rect s = f.region(HubLayout.SIDE);
            int sy = f.begin(HubLayout.SIDE);
            sy = PageKit.wrapped(f, Component.translatable(KEY + "legend_title").withStyle(ChatFormatting.BOLD), s.x(), sy, s.w(), t.accent());
            sy = PageKit.wrapped(f, Component.translatable(KEY + "legend"), s.x(), sy, s.w(), t.ink());
            f.end(sy);
        }
    }

    private int fact(HubScreen.Frame f, Rect r, int y, Fact fact, ProfilePayload payload) {
        HubTheme t = f.theme();
        List<String> known = payload.facts().get(fact);
        Component label = Component.translatable(KEY + "fact." + fact.name().toLowerCase(Locale.ROOT));
        Component shown = known == null ? Component.translatable(KEY + "unknown") : value(fact, known);
        // a label that takes more than 5/8 of a narrow column (Tensura's) goes on its own line, the value under it
        boolean stacked = f.font().width(label) + 6 > r.w() * 5 / 8;
        int lw = stacked ? 8 : f.font().width(label) + 6;
        f.g.drawString(f.font(), label, r.x() + 4, y, t.muted(), false);
        int vy = stacked ? y + PageKit.LINE : y;
        int top = vy;
        for (var line : f.font().split(shown, r.w() - lw - 4)) {
            f.g.drawString(f.font(), line, r.x() + lw + 4, vy, known == null ? t.muted() : t.ink(), false);
            vy += PageKit.LINE;
        }
        Rect row = new Rect(r.x(), y - 1, r.w(), Math.max(vy - y, PageKit.LINE) + 1);
        String id = "panel:fact:" + fact.name();
        List<Component> tip = known == null
                ? List.of(label, Component.translatable(KEY + "unknown_tip"), Component.translatable(KEY + "howto." + fact.name().toLowerCase(Locale.ROOT)))
                : List.of(label, shown);
        f.target(id, row, tip, null);
        if (f.focused(id)) {
            t.drawFocus(f.g, row);
        }
        return y + Math.max(vy - y, PageKit.LINE);
    }

    /** The actions of the relationship, as the theme's Detail: pack and sire facts come from the hub snapshot, which is all the client has. */
    private Detail detail(HubScreen.Frame f, HubScreen.ProfileRoute route) {
        var snap = f.view.snapshot();
        UUID self = f.view.player().getUUID();
        boolean isSelf = route.id().equals(self);
        ProfileActions.Role role = ProfileActions.Role.NONE;
        for (HubSocial.Member m : snap.pack()) {
            if (m.id().filter(route.id()::equals).isPresent()) {
                role = ProfileActions.roleOf(m.base());
            }
        }
        boolean leader = false;
        for (HubSocial.Member m : snap.pack()) {
            leader |= m.id().filter(self::equals).isPresent() && ProfileActions.roleOf(m.base()) == ProfileActions.Role.LEADER;
        }
        WolfState wolf = f.view.bloodline().wolf();
        SupernaturalRace race = f.view.races().get(0);
        boolean alpha = wolf.rank() == WolfState.Rank.ALPHA || wolf.rank() == WolfState.Rank.TRUE_ALPHA;
        ProfileActions.Viewer viewer = new ProfileActions.Viewer(!snap.pack().isEmpty(), leader, alpha && !leader, alpha, wolf.rank() == WolfState.Rank.TRUE_ALPHA,
                HubPage.isWolfType(race), race == SupernaturalRace.ORIGINAL || race == SupernaturalRace.HYBRID || race == SupernaturalRace.ORIGINAL_HYBRID || race == SupernaturalRace.TRIBRID);
        boolean sire = snap.sire().filter(n -> !n.isEmpty()).flatMap(snap::idOf).filter(route.id()::equals).isPresent();
        boolean sired = snap.people().stream().anyMatch(p -> p.id().equals(route.id()) && snap.sired().contains(p.name()));
        ProfileActions.Subject subject = new ProfileActions.Subject(route.id(), route.name(), isSelf, role, sired, sire);
        Detail.Builder d = Detail.of(Component.literal(route.name()));
        d.subtitle(isSelf ? Component.translatable(KEY + "you") : role == ProfileActions.Role.NONE ? Component.translatable(KEY + "no_bond")
                : Component.translatable(PageKit.HUB + "pack.member." + role.name().toLowerCase(Locale.ROOT).replace("challenger", "coalpha_challenger")));
        if (route.op()) {
            d.kicker(Component.translatable(KEY + "op_kicker"));
        }
        for (ProfileActions.Action a : ProfileActions.of(viewer, subject, f.view.context().op() && !route.op())) {
            Component label = Component.translatable(KEY + "action." + a.id());
            Component why = a.disabled() == null ? null : Component.translatable(KEY + "disabled." + a.disabled());
            Detail.Confirm confirm = a.confirm() ? new Detail.Confirm(label, Component.translatable(KEY + "confirm." + a.id(), route.name())) : null;
            Runnable run = switch (a.kind()) {
                case COMMAND -> () -> HubClient.runCommand(a.rowId(), a.args(route.id()));
                case OPEN_RITES -> () -> f.screen().setPage(HubPage.RITES);
                case OPEN_ADMIN -> () -> f.screen().setPage(HubPage.ADMIN);
            };
            d.action(new Detail.Action(a.id(), label, a.primary(), a.enabled(), why, confirm, run));
        }
        return d.build();
    }
}

