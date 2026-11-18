package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

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
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
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
        };
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y = waiting(f, r, y + 6);
        y = you(f, r, y + 6);
        f.end(y);
        PageWidgets.info(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "info"));

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = people(f, s, sy);
        sy = world(f, s, sy + 6);
        sy = doNow(f, s, sy + 6);
        sy = PageWidgets.reply(f, s, sy + 4);
        f.end(sy);
    }

    private static int section(HubScreen.Frame f, Rect r, int y, String key) {
        return PageKit.wrapped(f, Component.translatable(KEY + key).withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), f.theme().accent());
    }

    private int you(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = section(f, r, y, "you");
        List<Component> races = new ArrayList<>();
        f.view.races().forEach(race -> races.add(PageKit.race(race)));
        Component race = races.size() > 1 ? Component.empty().append(races.get(0)).append(" + ").append(races.get(1)) : races.get(0);
        y = PageKit.wrapped(f, Component.literal(f.view.player().getGameProfile().getName() + " \u00b7 ").append(race), r.x(), y, r.w(), t.ink());
        WolfState wolf = f.view.bloodline().wolf();
        if (wolf.rank() != WolfState.Rank.NONE) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "rank", BookPackPage.rankLabel(wolf)), r.x(), y, r.w(), t.ink());
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "sp", f.view.state().sp()), r.x(), y, r.w(), t.ink());
        if (f.view.ghoul()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "ghoul"), r.x(), y + 2, r.w(), t.muted());
        } else if (f.view.races().get(0) == SupernaturalRace.HUMAN) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "human"), r.x(), y + 2, r.w(), t.muted());
        }
        Rect self = new Rect(r.x(), y + 2, Math.min(r.w(), PageWidgets.widthOf(f.font(), Component.translatable(KEY + "profile"))), PageWidgets.H);
        PageWidgets.button(f, "stage:home:profile", self, Component.translatable(KEY + "profile"), true, false, null,
                () -> f.screen().openProfile(f.view.player().getUUID(), f.view.player().getGameProfile().getName()));
        return self.bottom() + 2;
    }

    private int waiting(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = section(f, r, y, "waiting_title");
        List<HubRequests.Request> open = RequestsClient.requests();
        if (open.isEmpty()) {
            return PageKit.wrapped(f, Component.translatable(KEY + "nothing"), r.x(), y, r.w(), t.muted());
        }
        for (HubRequests.Request q : open) {
            y = card(f, r, y + 2, q);
        }
        return y;
    }

    private int card(HubScreen.Frame f, Rect r, int y, HubRequests.Request q) {
        HubTheme t = f.theme();
        String id = "stage:request:" + q.kind() + ":" + q.token() + q.from();
        Component what = Component.translatable("supernaturalbloodlines.hub.request.kind." + q.kind().name().toLowerCase(Locale.ROOT));
        int left = (RequestsClient.ticksLeft(q) + 19) / 20;
        int top = y;
        if (!q.fromName().isEmpty() && !q.from().equals(HubRequests.NOBODY)) {
            Component who = Component.literal(q.fromName());
            Rect nameRect = new Rect(r.x() + 5, y, Math.min(r.w() - 5, f.font().width(who) + 2), PageKit.LINE);
            f.g.drawString(f.font(), who, nameRect.x(), y, f.hovered(nameRect) || f.focused(id) ? t.accent() : t.ink(), false);
            f.target(id, nameRect, List.of(who, Component.translatable(KEY + "open_profile")), () -> f.screen().openProfile(q.from(), q.fromName()));
            if (f.focused(id)) {
                t.drawFocus(f.g, nameRect);
            }
            y += PageKit.LINE;
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "request", what, left), r.x() + 5, y, r.w() - 5, t.muted());
        List<Answer> answers = answers(q.kind());
        List<Component> labels = new ArrayList<>();
        answers.forEach(a -> labels.add(Component.translatable(KEY + "answer." + a.label())));
        boolean dismissable = q.kind() != HubRequests.Kind.CHALLENGE && q.kind() != HubRequests.Kind.COMPULSION;
        if (dismissable) {
            labels.add(Component.translatable(KEY + "answer.dismiss"));
        }
        List<Integer> widths = labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList();
        Flow.Result flow = Flow.wrap(new Rect(r.x() + 5, y + 1, r.w() - 5, 0), widths, PageWidgets.H, PageWidgets.GAP);
        for (int i = 0; i < labels.size(); i++) {
            Rect b = flow.rects().get(i);
            if (i < answers.size()) {
                Answer a = answers.get(i);
                PageWidgets.button(f, id + ":" + a.label(), b, labels.get(i), true, a.primary(), null,
                        () -> HubClient.runCommand(a.rowId(), a.usesToken() ? List.of(q.token()) : List.of()));
            } else {
                PageWidgets.button(f, id + ":dismiss", b, labels.get(i), true, false, null, () -> RequestsClient.dismiss(q));
            }
        }
        f.g.fill(r.x(), top, r.x() + 2, flow.bottom(), t.accent());
        return flow.bottom() + 3;
    }

    private int people(HubScreen.Frame f, Rect s, int y) {
        HubTheme t = f.theme();
        y = section(f, s, y, "people");
        var snap = f.view.snapshot();
        List<HubSocial.Member> pack = snap.pack();
        if (pack.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "no_pack"), s.x(), y, s.w(), t.muted());
        }
        for (HubSocial.Member m : pack) {
            y = person(f, s, y, m.name(), m.id().orElse(null), Component.translatableWithFallback(PageKit.HUB + "pack.member." + m.base(), HubModel.pretty(m.base())), m.online());
        }
        if (snap.sire().filter(n -> !n.isEmpty()).isPresent()) {
            String n = snap.sire().get();
            y = person(f, s, y, n, snap.idOf(n).orElse(null), Component.translatable(KEY + "sire"), false);
        }
        if (!snap.sired().isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "sired", snap.sired().size()), s.x(), y, s.w(), t.ink());
        }
        return y;
    }

    private int person(HubScreen.Frame f, Rect s, int y, String name, UUID id, Component role, boolean online) {
        HubTheme t = f.theme();
        Rect row = new Rect(s.x(), y, s.w(), PageKit.LINE + 1);
        f.g.fill(row.x() + 1, y + 2, row.x() + 5, y + 6, online ? 0xFF2F6E4A : 0xFF9A8566);
        String shown = f.font().plainSubstrByWidth(name, s.w() - 12 - f.font().width(role) - 4);
        String tid = "stage:person:" + name;
        f.g.drawString(f.font(), shown, row.x() + 9, y, id != null && (f.hovered(row) || f.focused(tid)) ? t.accent() : t.ink(), false);
        f.g.drawString(f.font(), role, row.right() - f.font().width(role), y, t.muted(), false);
        f.target(tid, row, List.of(Component.literal(name), role), id == null ? null : () -> f.screen().openProfile(id, name));
        if (f.focused(tid)) {
            t.drawFocus(f.g, row);
        }
        return y + PageKit.LINE + 1;
    }

    private int world(HubScreen.Frame f, Rect s, int y) {
        HubTheme t = f.theme();
        y = section(f, s, y, "world");
        int phase = Math.floorMod(f.view.player().level().getMoonPhase(), 8);
        int days = Math.floorMod(8 - phase, 8);
        return PageKit.wrapped(f, days == 0 ? Component.translatable(KEY + "full_moon") : Component.translatable(KEY + "moon_in", days), s.x(), y, s.w(), t.ink());
    }

    private int doNow(HubScreen.Frame f, Rect s, int y) {
        y = section(f, s, y, "do");
        List<Component> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        SupernaturalRace race = f.view.races().get(0);
        if (!f.view.snapshot().sired().isEmpty() && (race == SupernaturalRace.ORIGINAL || race == SupernaturalRace.HYBRID || race == SupernaturalRace.ORIGINAL_HYBRID || race == SupernaturalRace.TRIBRID)) {
            labels.add(Component.translatable(KEY + "call"));
            ids.add("call");
            actions.add(() -> HubClient.runCommand("B3", List.of()));
        }
        if (f.view.bloodline().wolf().rank().ordinal() >= WolfState.Rank.ALPHA.ordinal()) {
            labels.add(Component.translatable(KEY + "roar"));
            ids.add("roar");
            actions.add(() -> HubClient.runCommand("P14", List.of()));
        }
        labels.add(Component.translatable(KEY + "open_quests"));
        ids.add("quests");
        actions.add(() -> f.screen().setPage(HubPage.QUESTS));
        List<Integer> widths = labels.stream().map(c -> PageWidgets.widthOf(f.font(), c)).toList();
        Flow.Result flow = Flow.wrap(new Rect(s.x(), y + 2, s.w(), 0), widths, PageWidgets.H, PageWidgets.GAP);
        for (int i = 0; i < labels.size(); i++) {
            PageWidgets.button(f, "stage:do:" + ids.get(i), flow.rects().get(i), labels.get(i), true, false, null, actions.get(i));
        }
        return flow.bottom();
    }
}

