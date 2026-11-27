package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubDev;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.RiteLinks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PortraitGeometry;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Home: who you are, what waits for an answer (each request with its own buttons and a countdown), what to do next, what the moon is doing,
 * who your people are and what you can do now. Every block is a door to the page that owns the thing; the controls here send the same
 * commands as there.
 */
public final class HomePage implements HubPageView {
    static final String KEY = PageKit.HUB + "home.";

    /** What a request card offers: the label key suffix and the {@code AdminCommands} row it runs. The token is the card's own. */
    record Answer(String label, String rowId, boolean usesToken, boolean primary) {
    }

    /** The answers of a kind of request, the first being the primary one. A compulsion has two buttons and a challenge answers by accept or decline. */
    static List<Answer> answers(HubRequests.Kind kind) {
        return switch (kind) {
            case SIRE_CALL -> List.of(new Answer("accept", "B4", true, true));
            case PACK_INVITE -> List.of(new Answer("accept", "P2", true, true));
            case ALLY_INVITE -> List.of(new Answer("accept", "P6", true, true));
            case CHALLENGE -> List.of(new Answer("accept", "P9", false, true), new Answer("decline", "P10", false, false));
            case COMPULSION -> List.of(new Answer("discount", "B5", true, true), new Answer("follow", "B6", true, false));
            case DUEL -> List.of();
            case CIRCLE_CALL -> List.of(new Answer("join", "C3", true, true));
            case GROUP_VOTE -> List.of(new Answer("yes", "C9", true, true), new Answer("no", "C10", true, false));
            case CHARTER_CONTEST -> List.of(new Answer("accept", "C12", false, true));
        };
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y = you(f, r, y + 6);
        y = next(f, r, y + 6);
        y = world(f, r, y + 6);
        y = doNow(f, r, y + 6);
        f.end(y);
        PageWidgets.info(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "info"));

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = waiting(f, s, sy);
        sy = people(f, s, sy + 6);
        sy = PageWidgets.reply(f, s, sy + 4);
        f.end(sy);
    }

    private java.util.Set<String> claimSent = java.util.Set.of();
    private long claimSentAt;

    private static int section(HubScreen.Frame f, Rect r, int y, String key) {
        return PageKit.wrapped(f, Component.translatable(KEY + key).withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), f.theme().accent());
    }

    /** The next step: what is ready to claim, with one button that claims all of it the way each single claim does. */
    private int next(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = section(f, r, y, "next_title");
        List<String> ready = ClaimAll.ids(f.view.ladders());
        if (ready.isEmpty()) {
            return PageKit.wrapped(f, Component.translatable(KEY + "caught_up"), r.x(), y, r.w(), t.muted());
        }
        boolean waiting = ClaimAll.waiting(claimSent, claimSentAt, ready, System.currentTimeMillis());
        // one summary line: what is ready, or that it was sent and the server has not answered yet
        y = PageKit.wrapped(f, waiting ? Component.translatable(KEY + "claiming", claimSent.size()) : Component.translatable(KEY + "ready", ready.size()), r.x(), y, r.w(), t.ink());
        Component label = Component.translatable(KEY + "claim_all");
        Rect b = new Rect(r.x(), y + 2, Math.min(r.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
        PageWidgets.button(f, "stage:home:claim_all", b, label, !waiting, true, Component.translatable(KEY + "claiming", claimSent.size()), () -> {
            claimSent = new java.util.HashSet<>(ready);
            claimSentAt = System.currentTimeMillis();
            ready.forEach(f::claim);
        });
        return b.bottom() + 2;
    }

    /** The viewer as a card: portrait, name, chips for pack rank and species, and the numbers that matter. */
    private int you(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = section(f, r, y, "you");
        String name = f.view.player().getGameProfile().getName();
        int face = 28;
        BookKit.portrait(f.g, name, PortraitGeometry.cxAt(r.x(), face), PortraitGeometry.cyAt(y, face), face, t.accent(), t.paper());
        int tx = PortraitGeometry.besideX(r.x(), face);
        int ty = PageKit.title(f, Component.literal(name), tx, y + 2, r.right() - tx, t.ink());
        WolfState wolf = f.view.bloodline().wolf();
        List<Component> chips = new ArrayList<>();
        if (wolf.rank() != WolfState.Rank.NONE) {
            chips.add(BookPackPage.rankLabel(f, wolf));
        }
        List<Component> races = new ArrayList<>();
        f.view.races().forEach(race -> races.add(PageKit.race(race)));
        chips.add(races.size() > 1 ? Component.empty().append(races.get(0)).append(" + ").append(races.get(1)) : races.get(0));
        int cx = tx;
        int cy = ty + 1;
        for (Component chip : chips) {
            int w = f.font().width(chip) + 8;
            if (cx + w > r.right() && cx > tx) {
                cx = tx;
                cy += 12;
            }
            int text = BookKit.chip(f, new Rect(cx, cy, w, 11), false);
            f.g.drawString(f.font(), chip, cx + 4, cy + 2, text, false);
            cx += w + 4;
        }
        y = Math.max(PortraitGeometry.below(y, face), cy + 14);
        y = PageKit.wrapped(f, Component.translatable(KEY + "sp", f.view.state().sp()), r.x(), y, r.w(), t.ink());
        if (f.view.ghoul()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "ghoul"), r.x(), y + 2, r.w(), t.muted());
        } else if (f.view.races().get(0) == SupernaturalRace.HUMAN) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "human"), r.x(), y + 2, r.w(), t.muted());
        }
        Rect self = new Rect(r.x(), y + 2, Math.min(r.w(), PageWidgets.widthOf(f.font(), Component.translatable(KEY + "profile"))), PageWidgets.H);
        PageWidgets.button(f, "stage:home:profile", self, Component.translatable(KEY + "profile"), true, false, null,
                () -> f.screen().openProfile(f.view.player().getUUID(), name));
        return self.bottom() + 2;
    }
    private int waiting(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = section(f, r, y, "waiting_title");
        List<HubRequests.Request> open = RequestsClient.requests();
        if (open.isEmpty()) {
            return hidden(f, r, PageKit.wrapped(f, Component.translatable(KEY + "nothing"), r.x(), y, r.w(), t.muted()));
        }
        for (HubRequests.Request q : open) {
            y = card(f, r, y + 2, q);
        }
        return hidden(f, r, y);
    }

    /** "Show hidden (2)": the cards the player hid come back. Shown with or without any card in view. */
    private int hidden(HubScreen.Frame f, Rect r, int y) {
        int n = RequestsClient.hiddenCount();
        if (n == 0) {
            return y;
        }
        Component label = Component.translatable(KEY + "show_hidden", n);
        Rect b = new Rect(r.x(), y + 2, Math.min(r.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
        PageWidgets.button(f, "stage:home:show_hidden", b, label, true, false, null, RequestsClient::showHidden);
        return b.bottom() + 2;
    }

    /** The longest a request has been seen with, so its bar drains from full: the server sends only what is left. */
    private static final java.util.Map<String, Integer> PEAK = new java.util.HashMap<>();

    /** Drops the peaks of requests that are no longer listed (and all of them when {@code open} is empty), so the map does not grow over a long session. */
    public static void prunePeaks(List<HubRequests.Request> open) {
        java.util.Set<String> keep = new java.util.HashSet<>();
        open.forEach(q -> keep.add(q.token() + q.from()));
        PEAK.keySet().retainAll(keep);
    }

    /** A request's time left: m:ss, but whole hours (rounded up) once it is over an hour, as a coven vote's day is. */
    static Component clock(int seconds) {
        return seconds >= 3600 ? Component.translatable(KEY + "hours", (seconds + 3599) / 3600) : Component.literal(seconds / 60 + ":" + String.format("%02d", seconds % 60));
    }

    private int card(HubScreen.Frame f, Rect r, int y, HubRequests.Request q) {
        HubTheme t = f.theme();
        String id = "stage:request:" + q.kind() + ":" + q.token() + q.from();
        var vote = q.kind() == HubRequests.Kind.GROUP_VOTE ? net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind.ofAnswer(q.token()).orElse(null) : null;
        Component what = vote != null ? Component.translatable(CovenCards.KEY + "vote." + vote.id(), q.fromName())
                : Component.translatable("supernaturalbloodlines.hub.request.kind." + q.kind().name().toLowerCase(Locale.ROOT));
        int ticks = RequestsClient.ticksLeft(q);
        int left = (ticks + 19) / 20;
        int peak = PEAK.merge(q.token() + q.from(), ticks, Math::max);
        int top = y;
        int x = r.x() + 5;
        int w = r.w() - 10;
        boolean named = !q.fromName().isEmpty() && !q.from().equals(HubRequests.NOBODY);
        int face = 16;
        int tx = named ? x + face + 4 : x;
        Component clock = clock(left);
        int cw = f.font().width(clock);
        int ty = y + 3;
        if (named) {
            BookKit.face(f.g, q.fromName(), x, ty, face);
            Component who = Component.literal(q.fromName());
            Rect nameRect = new Rect(tx, ty, Math.min(w - face - 4 - cw - 4, f.font().width(who) + 2), PageKit.LINE);
            f.g.drawString(f.font(), who, nameRect.x(), ty, f.hovered(nameRect) || f.focused(id) ? t.accent() : t.ink(), false);
            f.target(id, new Rect(x, ty, face + 4 + nameRect.w(), face), List.of(who, Component.translatable(KEY + "open_profile")), () -> f.screen().openProfile(q.from(), q.fromName()));
            if (f.focused(id)) {
                t.drawFocus(f.g, nameRect);
            }
        }
        f.g.drawString(f.font(), clock, x + w - cw, ty, left <= 10 ? PageWidgets.warn(t) : t.muted(), false);
        int textW = x + w - tx - cw - 4;
        int ky = named ? ty + PageKit.LINE : ty;
        for (var line : f.font().split(what, Math.max(20, textW))) {
            f.g.drawString(f.font(), line, tx, ky, t.muted(), false);
            ky += PageKit.LINE;
        }
        y = Math.max(ky, named ? ty + face : ky) + 2;
        int barW = w;
        f.g.fill(x, y, x + barW, y + 3, (t.muted() & 0xFFFFFF) | 0x44000000);
        f.g.fill(x, y, x + Math.max(1, (int) (barW * (ticks / (double) Math.max(1, peak)))), y + 3, t.accent());
        y += 7;
        List<Answer> answers = answers(q.kind());
        List<Component> labels = new ArrayList<>();
        answers.forEach(a -> labels.add(Component.translatable(KEY + "answer." + a.label())));
        boolean dismissable = q.kind() != HubRequests.Kind.CHALLENGE && q.kind() != HubRequests.Kind.COMPULSION && q.kind() != HubRequests.Kind.CHARTER_CONTEST;
        if (dismissable) {
            labels.add(Component.translatable(KEY + "answer.dismiss"));
        }
        List<Integer> widths = labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList();
        Flow.Result flow = Flow.wrap(new Rect(x, y, w, 0), widths, PageWidgets.H, PageWidgets.GAP);
        for (int i = 0; i < labels.size(); i++) {
            Rect b = flow.rects().get(i);
            if (i < answers.size()) {
                Answer a = answers.get(i);
                Component tip = vote != null ? CovenCards.voteTip(vote, q.fromName(), a.primary())
                        : q.kind() == HubRequests.Kind.COMPULSION ? Component.translatable(KEY + "answer." + a.label() + ".tip") : null;
                PageWidgets.described(f, id + ":" + a.label(), b, labels.get(i), true, a.primary(), tip,
                        () -> HubClient.runCommand(a.rowId(), a.usesToken() ? List.of(q.token()) : List.of()));
            } else {
                PageWidgets.button(f, id + ":dismiss", b, labels.get(i), true, false, null, () -> RequestsClient.dismiss(q));
            }
        }
        int bottom = flow.bottom() + 3;
        if (q.kind() == HubRequests.Kind.COMPULSION) {
            // what each choice does, in one line each, so the buttons are not a guess
            for (Answer a : answers) {
                bottom = PageKit.wrapped(f, Component.translatable(KEY + "answer." + a.label() + ".tip"), x, bottom, w, t.muted()) + 1;
            }
        }
        BookKit.cardOver(f, new Rect(r.x(), top, r.w(), bottom - top), t.accent());
        return bottom + 3;
    }
    /** The pack and the people of your line as faces: a click opens the Profile; the dot says who is online and the crown who leads. */
    private int people(HubScreen.Frame f, Rect s, int y) {
        HubTheme t = f.theme();
        y = section(f, s, y, "people");
        var snap = f.view.snapshot();
        net.ofatech.supernaturalbloodlines.core.group.GroupView gv = GroupLabels.view();
        boolean grp = GroupLabels.active(gv);
        List<HubSocial.Member> pack = grp ? GroupLabels.members(gv) : snap.pack();
        if (pack.isEmpty()) {
            y = PageKit.wrapped(f, GroupLabels.t(gv, KEY + "no_pack", "no_pack"), s.x(), y, s.w(), t.muted());
        }
        int face = 16;
        int step = face + 4;
        int perRow = Math.max(1, s.w() / step);
        int online = 0;
        for (int i = 0; i < pack.size(); i++) {
            HubSocial.Member m = pack.get(i);
            int fx = s.x() + (i % perRow) * step;
            int fy = y + 6 + (i / perRow) * (face + 8);
            Rect hit = new Rect(fx, fy, face, face);
            boolean drawn = f.whole(fy, face + 4);
            if (drawn) {
                BookKit.face(f.g, m.name(), fx, fy, face);
                f.g.fill(fx + face - 4, fy + face + 1, fx + face, fy + face + 4, m.online() ? 0xFF2F6E4A : 0xFF9A8566);
                if (i == 0 && !m.base().equals("ally")) {
                    BookPackPage.crown(f, fx + 4, fy - 6);
                }
            }
            online += m.online() ? 1 : 0;
            String tid = "stage:person:" + m.name();
            Component role = grp ? GroupLabels.memberRank(gv, m.base()) : Component.translatableWithFallback(PageKit.HUB + "pack.member." + m.base(), HubModel.pretty(m.base()));
            UUID id = m.id().orElse(null);
            f.target(tid, hit, List.of(Component.literal(m.name()), role, Component.translatable(KEY + "open_profile")), id == null ? null : () -> f.screen().openProfile(id, m.name()));
            if (drawn && (f.focused(tid) || (id != null && f.hovered(hit)))) {
                t.drawFocus(f.g, hit);
            }
        }
        if (!pack.isEmpty()) {
            y += 6 + ((pack.size() - 1) / perRow + 1) * (face + 8);
            y = PageKit.wrapped(f, GroupLabels.t(gv, KEY + "pack_count", "pack_count", pack.size(), online), s.x(), y, s.w(), t.muted());
        }
        if (snap.sire().filter(n -> !n.isEmpty()).isPresent()) {
            String n = snap.sire().get();
            y = person(f, s, y + 2, n, snap.idOf(n).orElse(null), Component.translatable(KEY + "sire"));
        }
        if (!snap.sired().isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "sired", snap.sired().size()), s.x(), y + 2, s.w(), t.ink());
        }
        return y;
    }

    private int person(HubScreen.Frame f, Rect s, int y, String name, UUID id, Component role) {
        HubTheme t = f.theme();
        Rect row = new Rect(s.x(), y, s.w(), 18);
        boolean drawn = f.whole(y, 18);
        String tid = "stage:person:" + name;
        if (drawn) {
            BookKit.face(f.g, name, s.x(), y + 1, 16);
            f.g.drawString(f.font(), LabelFit.cut(name, s.w() - 24 - f.font().width(role), f.font()::width), s.x() + 20, y + 5, id != null && (f.hovered(row) || f.focused(tid)) ? t.accent() : t.ink(), false);
            f.g.drawString(f.font(), role, row.right() - f.font().width(role), y + 5, t.muted(), false);
        }
        f.target(tid, row, List.of(Component.literal(name), role), id == null ? null : () -> f.screen().openProfile(id, name));
        if (drawn && f.focused(tid)) {
            t.drawFocus(f.g, row);
        }
        return y + 20;
    }

    /** A moon disc (lit by the phase) with the days to the full one. */
    private int world(HubScreen.Frame f, Rect s, int y) {
        HubTheme t = f.theme();
        y = section(f, s, y, "world");
        int phase = Math.floorMod(HubDev.moonPhase(f.view.player().level()), 8);
        int days = Math.floorMod(8 - phase, 8);
        int d = 16;
        int cx = s.x() + d / 2;
        int cy = y + 2 + d / 2;
        double lit = 1 - Math.abs(phase - 0) / 4.0; // 0 = full, 4 = new
        for (int dy = -d / 2; dy < d / 2; dy++) {
            int half = (int) Math.round(Math.sqrt(Math.max(0, (d / 2.0) * (d / 2.0) - (dy + 0.5) * (dy + 0.5))));
            f.g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFF5E5050);
            int litHalf = (int) Math.round(half * Math.max(0, lit));
            f.g.fill(cx - litHalf, cy + dy, cx + litHalf, cy + dy + 1, 0xFFF4D98A);
        }
        Component now = days == 0 ? Component.translatable(KEY + "full_moon") : Component.translatable(KEY + "moon_in", days);
        int tx = s.x() + d + 6;
        int ny = PageKit.wrapped(f, now, tx, y + 2, s.right() - tx, t.ink());
        y = Math.max(ny, y + 2 + d + 2);
        // R140: what tonight is, what the rules say to this player, and the way to the calendar
        var night = MoonPage.tonight(f.view.player().level());
        if (night != net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night.NONE) {
            y = PageKit.wrapped(f, Component.translatable(MoonPage.KEY + "night." + night.name().toLowerCase(Locale.ROOT)), s.x(), y, s.w(), t.ink());
        }
        var flags = f.view.snapshot().world();
        boolean fullNight = night == net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night.FULL_MOON
                || night == net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night.ECLIPSE;
        if (fullNight && flags.sleepRule()) {
            y = PageKit.wrapped(f, Component.translatable(MoonPage.KEY + "no_sleep"), s.x(), y, s.w(), t.ink());
        }
        if (flags.awakeningPending()) {
            y = PageKit.wrapped(f, Component.translatable(MoonPage.KEY + "stirs"), s.x(), y, s.w(), t.ink());
        }
        if (flags.trial() != net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.TRIAL_NONE) {
            boolean running = flags.trial() == net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.TRIAL_RUNNING;
            y = PageKit.wrapped(f, Component.translatable(KEY + (running ? "trial_running" : "trial_possible")), s.x(), y + 2, s.w(), running ? PageWidgets.warn(t) : t.accent());
            if (!running) {
                Component rites = Component.translatable(KEY + "open_trial");
                Rect tb = new Rect(s.x(), y + 2, Math.min(s.w(), PageWidgets.widthOf(f.font(), rites)), PageWidgets.H);
                PageWidgets.button(f, "stage:home:trial", tb, rites, RiteLinks.hasBook(), false, RiteLinks.tip("alpha_trial", null), () -> RiteLinks.open("alpha_trial"));
                y = tb.bottom() + 2;
            }
        }
        if (flags.bossNear() != net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.BossNear.NONE_KIND) {
            y = bossLine(f, s, y + 2, flags);
        }
        Component label = Component.translatable(KEY + "open_moon");
        Rect b = new Rect(s.x(), y + 2, Math.min(s.w(), PageWidgets.widthOf(f.font(), label)), PageWidgets.H);
        PageWidgets.button(f, "stage:home:moon", b, label, true, false, null, f.screen()::openMoon);
        return b.bottom() + 2;
    }

    /**
     * A boss within 128 blocks. Its own marker (a diamond) and colour, so it is not read as the Alpha Trial line: crimson for one who walks, steel
     * blue for an Original who lies fallen (and so is open to white oak). Named only when the player may know who it is.
     */
    private int bossLine(HubScreen.Frame f, Rect s, int y, net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags flags) {
        boolean fallen = flags.bossNear() == net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.BossNear.FALLEN;
        int color = fallen ? 0xFF3E6FA3 : 0xFFB02A2A;
        String key = KEY + (fallen ? "boss_fallen" : "boss_near") + (flags.bossName().isEmpty() ? "" : "_named");
        Component line = flags.bossName().isEmpty() ? Component.translatable(key)
                : Component.translatable(key, Component.translatable("supernaturalbloodlines.boss.name." + flags.bossName()));
        int cx = s.x() + 4;
        int cy = y + 5;
        for (int dy = -4; dy <= 4; dy++) {
            int half = 4 - Math.abs(dy);
            f.g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
        return PageKit.wrapped(f, line, s.x() + 12, y, s.w() - 12, color);
    }

    /** What the viewer can do now, each with a button; one that does not apply is off and says why. */
    private int doNow(HubScreen.Frame f, Rect s, int y) {
        HubTheme t = f.theme();
        y = section(f, s, y, "do");
        SupernaturalRace race = f.view.races().get(0);
        boolean line = race == SupernaturalRace.ORIGINAL || race == SupernaturalRace.HYBRID || race == SupernaturalRace.ORIGINAL_HYBRID || race == SupernaturalRace.TRIBRID;
        boolean alpha = f.view.bloodline().wolf().rank().ordinal() >= WolfState.Rank.ALPHA.ordinal();
        record Do(String id, Component label, boolean on, String why, Runnable run) {
        }
        List<Do> all = new ArrayList<>();
        all.add(new Do("call", Component.translatable(KEY + "call"), line && !f.view.snapshot().sired().isEmpty(), line ? "no_sired" : "not_line", () -> HubClient.runCommand("B3", List.of())));
        all.add(new Do("roar", Component.translatable(KEY + "roar"), alpha, "not_alpha", () -> HubClient.runCommand("P14", List.of())));
        boolean wolf = f.view.bloodline().wolf().lineage() != WolfState.Lineage.NONE;
        all.add(new Do("surge", Component.translatable(KEY + "surge"), wolf && net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.surgeMates(f.view.snapshot().pack(), f.view.player().getUUID()) > 0, wolf ? "no_pack" : "not_werewolf", () -> HubClient.runCommand("P19", List.of())));
        all.add(new Do("spirits", Component.translatable(KEY + "spirits"), alpha, "not_alpha_spirits", () -> HubClient.runCommand("P20", List.of())));
        all.add(new Do("quests", Component.translatable(KEY + "open_quests"), true, "", () -> f.screen().setPage(HubPage.QUESTS)));
        if (f.view.context().enchanted()) {
            all.add(new Do("rites", Component.translatable(KEY + "rites_line"), RiteLinks.hasBook(), "no_book", HubClient::openEnchantedBook));
        }
        for (Do d : all) {
            Rect b = new Rect(s.x(), y + 2, Math.min(s.w(), PageWidgets.widthOf(f.font(), d.label())), PageWidgets.H);
            Component why = d.on() ? null : Component.translatable(KEY + "why." + d.why());
            PageWidgets.button(f, "stage:do:" + d.id(), b, d.label(), d.on(), false, why, d.run());
            y = b.bottom();
            if (why != null) {
                y = PageKit.wrapped(f, why, s.x() + 2, y + 1, s.w() - 2, t.muted());
            }
        }
        return y;
    }
}
