package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.ProfileClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.RiteLinks;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.DuelChecks;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.DuelView;
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

    /** The facts of the grid, in reading order; the rank is a chip, and shows here only while it is unknown. */
    private static final List<Fact> GRID = List.of(Fact.SPECIES, Fact.LEVEL, Fact.PACK, Fact.PACK_DAYS, Fact.PRESENCE, Fact.SIRE, Fact.LINE, Fact.GENES, Fact.ANCHOR);

    @Override
    public void render(HubScreen.Frame f) {
        HubScreen.ProfileRoute route = f.screen().profile();
        HubTheme t = f.theme();
        ProfilePayload payload = route == null ? null : ProfileClient.of(route.id());
        Rect r = f.region(HubLayout.MAIN);
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
            int ink = BookKit.chip(f, new Rect(r.x(), y, r.w(), h), true);
            int yy = y + 2;
            for (var line : f.font().split(banner, r.w() - 8)) {
                f.g.drawString(f.font(), line, r.x() + 4, yy, ink, false);
                yy += PageKit.LINE;
            }
            y += h + 4;
        }
        y = identity(f, r, y, route, payload);
        if (payload == null) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "loading"), r.x(), y + 2, r.w(), t.muted());
        } else {
            y = grid(f, r, y + 4, payload);
            List<String> bonus = payload.facts().get(Fact.BONUS);
            if (bonus != null && !bonus.isEmpty()) {
                y = bonusCard(f, r, y + 2, bonus.get(0));
            }
        }
        f.end(y);

        ActionView av = actions(f, route, payload);
        Rect pane = f.layout().chrome().detail();
        boolean paneHasRoom = pane != null && pane.h() >= 60;
        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = side(f, s, sy, route, av, !paneHasRoom);
        sy = PageWidgets.reply(f, s, sy + 2);
        f.end(sy);
        if (paneHasRoom) {
            // the tall pane says who this is and what a ? means
            Detail.Builder d = Detail.of(Component.literal(route.name())).subtitle(av.subtitle());
            if (route.op()) {
                d.kicker(Component.translatable(KEY + "op_kicker"));
            }
            boolean anyUnknown = payload != null && !route.op() && Fact.values().length > payload.facts().size();
            f.detail((anyUnknown ? d.body(Component.translatable(KEY + "legend")) : d).build());
        }
    }

    /** The portrait, the name (with the leader's crown) and the rank chip. */
    private int identity(HubScreen.Frame f, Rect r, int y, HubScreen.ProfileRoute route, ProfilePayload payload) {
        HubTheme t = f.theme();
        int face = f.layout().chrome().detail() != null && f.layout().chrome().detail().h() >= 60 ? 48 : r.w() >= 150 ? 40 : 28;
        boolean leader = payload != null && payload.facts().get(Fact.RANK) != null && payload.facts().get(Fact.RANK).contains("alpha");
        BookKit.portrait(f.g, route.name(), r.x() + face / 2 + 1, y + face / 2 + 1, face, route.id().equals(f.view.player().getUUID()) ? t.accent() : t.muted(), t.paper());
        List<String> rank = payload == null ? null : payload.facts().get(Fact.RANK);
        Component chip = rank == null || rank.isEmpty() ? null : value(Fact.RANK, rank);
        // beside the portrait when the name and the chip fit there, else under it (a narrow column must not squeeze or cut them)
        int besideX = r.x() + face + 8;
        boolean beside = r.right() - besideX >= Math.max(f.font().width(route.name()) + (leader ? 12 : 0), chip == null ? 0 : f.font().width(chip) + 8);
        int tx = beside ? besideX : r.x();
        int top = beside ? y : y + face + 3;
        int ty = PageKit.title(f, Component.literal(route.name()), tx, top, r.right() - tx - (leader ? 12 : 0), t.accent());
        if (leader) {
            int nameW = ty - top > PageKit.LINE + 2 ? r.right() - tx - 12 : (int) (f.font().width(route.name()) * (f.font().width(route.name()) * 1.5f <= r.right() - tx - 12 ? 1.5f : 1f));
            BookPackPage.crown(f, Math.min(r.right() - 8, tx + nameW + 5), top + 3);
        }
        int bottom = ty;
        if (chip != null) {
            String text = net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(chip.getString(), r.right() - tx - 8, f.font()::width);
            int w = f.font().width(text) + 8;
            int cy = beside ? Math.max(ty + 1, y + face - 11) : ty + 1;
            int ink = BookKit.chip(f, new Rect(tx, cy, w, 11), true);
            f.g.drawString(f.font(), text, tx + 4, cy + 2, ink, false);
            bottom = beside ? ty : cy + 11;
        }
        return beside ? y + Math.max(face + 2, bottom - y) : bottom + 3;
    }

    /** The facts as cells: the label small over its value, two columns where the page is wide, one where it is not, so a short value stays beside its label. */
    private int grid(HubScreen.Frame f, Rect r, int y, ProfilePayload payload) {
        HubTheme t = f.theme();
        int cols = r.w() >= 180 ? 2 : 1;
        int cw = (r.w() - (cols - 1) * 6) / cols;
        List<Fact> shown = new ArrayList<>(GRID);
        if (!payload.facts().containsKey(Fact.RANK)) {
            shown.add(0, Fact.RANK);
        }
        if (!payload.facts().containsKey(Fact.BONUS)) {
            shown.add(Fact.BONUS);
        }
        int col = 0;
        int rowTop = y;
        int rowH = 0;
        for (Fact fact : shown) {
            int x = r.x() + col * (cw + 6);
            List<String> known = payload.facts().get(fact);
            Component label = Component.translatable(KEY + "fact." + fact.name().toLowerCase(Locale.ROOT));
            Component text = known == null ? Component.translatable(KEY + "unknown") : value(fact, known);
            String caps = label.getString().toUpperCase(Locale.ROOT);
            int vy;
            if (cols == 1 && f.font().width(caps) + 8 + f.font().width(text) <= cw) {
                // a short value stays on the label's line, at the right edge
                f.g.drawString(f.font(), caps, x, rowTop, t.muted(), false);
                f.g.drawString(f.font(), text, x + cw - f.font().width(text), rowTop, known == null ? t.muted() : t.ink(), false);
                vy = rowTop + PageKit.LINE;
            } else {
                f.g.drawString(f.font(), caps, x, rowTop, t.muted(), false);
                vy = rowTop + 10;
                for (var line : f.font().split(text, cw)) {
                    f.g.drawString(f.font(), line, x, vy, known == null ? t.muted() : t.ink(), false);
                    vy += PageKit.LINE;
                }
            }
            int h = vy - rowTop + 3;
            Rect cell = new Rect(x - 1, rowTop - 1, cw + 2, h - 1);
            String id = "panel:fact:" + fact.name();
            List<Component> tip = known != null ? List.of(label, text)
                    : net.ofatech.supernaturalbloodlines.shared.supernatural.profile.UnknownReason.whileOffline(fact, payload.facts())
                            ? List.of(label, Component.translatable(KEY + "unknown_offline"))
                            : List.of(label, Component.translatable(KEY + "unknown_tip"), Component.translatable(KEY + "howto." + fact.name().toLowerCase(Locale.ROOT)));
            f.target(id, cell, tip, null);
            if (f.focused(id)) {
                t.drawFocus(f.g, cell);
            }
            rowH = Math.max(rowH, h);
            if (++col == cols) {
                col = 0;
                rowTop += rowH;
                rowH = 0;
            }
        }
        return col == 0 ? rowTop : rowTop + rowH;
    }

    private int bonusCard(HubScreen.Frame f, Rect r, int y, String text) {
        HubTheme t = f.theme();
        Component title = Component.translatable(KEY + "bonus_title");
        var lines = f.font().split(Component.literal(text), r.w() - 10);
        int h = 14 + lines.size() * PageKit.LINE + 4;
        BookKit.card(f, new Rect(r.x(), y, r.w(), h), t.accent());
        f.g.drawString(f.font(), title, r.x() + 5, y + 4, t.accent(), false);
        int ly = y + 14;
        for (var line : lines) {
            f.g.drawString(f.font(), line, r.x() + 5, ly, t.ink(), false);
            ly += PageKit.LINE;
        }
        f.target("panel:fact:BONUS", new Rect(r.x(), y, r.w(), h), List.of(title, Component.literal(text)), null);
        return y + h + 2;
    }

    /** What the relationship allows, with the words the page needs around it. */
    private record ActionView(List<ProfileActions.Action> actions, Component subtitle, UUID subject, boolean npc) {
    }

    /** The right page (or card, or column): what can be done with this person, each with what it does or why it is off, and the duel's rules. */
    private int side(HubScreen.Frame f, Rect s, int y, HubScreen.ProfileRoute route, ActionView av, boolean legend) {
        HubTheme t = f.theme();
        var duel = duelWith(route.id());
        if (duel.isPresent()) {
            DuelView v = duel.get();
            Component line = Component.translatable(v.langKey(), v.opponent().isEmpty() ? route.name() : v.opponent(), v.clock());
            int h = f.font().split(line, s.w() - 8).size() * PageKit.LINE + 4;
            int ink = BookKit.chip(f, new Rect(s.x(), y, s.w(), h), true);
            int yy = y + 2;
            for (var l : f.font().split(line, s.w() - 8)) {
                f.g.drawString(f.font(), l, s.x() + 4, yy, ink, false);
                yy += PageKit.LINE;
            }
            f.target("panel:duel", new Rect(s.x(), y, s.w(), h), List.of(line), null);
            y += h + 4;
        }
        y = PageKit.title(f, Component.translatable(KEY + "actions_title"), s.x(), y, s.w(), t.accent());
        t.drawRule(f.g, s.x(), y + 2, s.w());
        y += 6;
        if (av.actions().isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + (av.npc() ? "no_actions_npc" : "no_actions"), route.name()), s.x(), y, s.w(), t.muted());
        }
        boolean challenge = false;
        for (ProfileActions.Action a : av.actions()) {
            Component label = Component.translatable(KEY + "action." + a.id());
            Component why = a.disabled() == null ? null : Component.translatable(KEY + "disabled." + a.disabled());
            Runnable run = switch (a.kind()) {
                case COMMAND -> () -> HubClient.runCommand(a.rowId(), a.args(route.id()));
                case OPEN_RITES -> () -> RiteLinks.open("rite_of_severance");
                case OPEN_ADMIN -> () -> {
                    // only where Admin can be reached: an offer to a page that is not there would wait for a later visit
                    if (HubPage.visiblePages(f.view.context()).contains(HubPage.ADMIN)) {
                        net.ofatech.supernaturalbloodlines.shared.hub.AdminHandoff.offer(route.id(), route.name());
                        f.screen().setPage(HubPage.ADMIN);
                    }
                };
            };
            Runnable press = a.confirm()
                    ? () -> f.confirm(label, Component.translatable(KEY + "confirm." + a.id(), route.name()), label, run)
                    : run;
            Rect btn = new Rect(s.x(), y, Math.min(s.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
            PageWidgets.button(f, "stage:profile:act:" + a.id(), btn, label, a.enabled(), a.primary(), why, press);
            y = btn.bottom() + 1;
            y = PageKit.wrapped(f, Component.translatable(KEY + "action." + a.id() + ".what", route.name()), s.x(), y, s.w(), t.muted());
            if (why != null) {
                y = PageKit.wrapped(f, why, s.x(), y, s.w(), PageWidgets.warn(t));
            }
            y += 4;
            challenge |= a.id().equals("challenge");
        }
        if (challenge) {
            y = rules(f, s, y + 2, route);
        }
        if (legend) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "legend_title").withStyle(ChatFormatting.BOLD), s.x(), y + 4, s.w(), t.accent());
            y = PageKit.wrapped(f, Component.translatable(KEY + "legend"), s.x(), y, s.w(), t.muted());
        }
        return y;
    }

    /** "The rules, and where you stand": each rule of the duel with a tick, a cross or a question mark (the server's to say). */
    private int rules(HubScreen.Frame f, Rect s, int y, HubScreen.ProfileRoute route) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "rules_title").withStyle(ChatFormatting.BOLD), s.x(), y, s.w(), t.accent());
        y = PageKit.wrapped(f, Component.translatable(KEY + "rule.loser"), s.x(), y, s.w(), t.ink());
        var mc = net.minecraft.client.Minecraft.getInstance();
        var other = mc.level == null ? null : mc.level.getPlayerByUUID(route.id());
        double distance = other == null || mc.player == null ? -1 : other.distanceTo(mc.player);
        for (DuelChecks.Check c : DuelChecks.of(distance, RequestsClient.duel() != null)) {
            Component rule = Component.translatable(KEY + "rule." + c.key());
            String mark = switch (c.state()) {
                case OK -> "\u2713";
                case FAIL -> "x";
                case UNKNOWN -> "?";
            };
            int color = switch (c.state()) {
                case OK -> t.good();
                case FAIL -> PageWidgets.warn(t);
                case UNKNOWN -> t.muted();
            };
            String right = mark + (c.detail().isEmpty() ? "" : " " + c.detail());
            int rw = f.font().width(right);
            var lines = f.font().split(rule, s.w() - rw - 6);
            int ly = y;
            for (var line : lines) {
                f.g.drawString(f.font(), line, s.x(), ly, t.ink(), false);
                ly += PageKit.LINE;
            }
            f.g.drawString(f.font(), right, s.right() - rw, y, color, false);
            f.target("panel:rule:" + c.key(), new Rect(s.x(), y - 1, s.w(), ly - y + 1), List.of(rule, Component.translatable(KEY + "rule.state." + c.state().name().toLowerCase(Locale.ROOT))), null);
            y = ly + 2;
        }
        return y;
    }

    /** The viewer's own duel when its opponent is this person; the clock counts down from what the server last said. */
    static java.util.Optional<DuelView> duelWith(UUID subject) {
        var d = RequestsClient.duel();
        return d == null || !d.from().equals(subject) ? java.util.Optional.empty()
                : DuelView.of(d.token(), d.fromName(), RequestsClient.ticksLeft(d));
    }

    /** The actions of the relationship: pack and sire facts come from the hub snapshot, which is all the client has. */
    private ActionView actions(HubScreen.Frame f, HubScreen.ProfileRoute route, ProfilePayload payload) {
        var snap = f.view.snapshot();
        UUID self = f.view.player().getUUID();
        boolean isSelf = route.id().equals(self);
        ProfileActions.Role role = ProfileActions.Role.NONE;
        String base = "";
        for (HubSocial.Member m : snap.pack()) {
            if (m.id().filter(route.id()::equals).isPresent()) {
                role = ProfileActions.roleOf(m.base());
                base = m.base();
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
                HubPage.isWolfType(race), PackRules.pathOpen(wolf), wolf.protectPath());
        boolean sire = snap.sire().filter(n -> !n.isEmpty()).flatMap(snap::idOf).filter(route.id()::equals).isPresent();
        boolean sired = snap.people().stream().anyMatch(p -> p.id().equals(route.id()) && snap.sired().contains(p.name()));
        boolean npc = payload != null && payload.npc();
        ProfileActions.Subject subject = new ProfileActions.Subject(route.id(), route.name(), isSelf, role, sired, sire, duelWith(route.id()).isPresent(), npc);
        Component sub = isSelf ? Component.translatable(KEY + "you") : npc ? Component.translatable(KEY + "npc") : role == ProfileActions.Role.NONE ? Component.translatable(KEY + "no_bond")
                : Component.translatableWithFallback(PageKit.HUB + "pack.member." + base, HubModel.pretty(base));
        return new ActionView(ProfileActions.of(viewer, subject, f.view.context().op() && !route.op()), sub, route.id(), npc);
    }
}