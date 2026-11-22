package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PackRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.RingLabels;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * Pack: the portrait ring round the sigil (Alphas red, Betas gold, Packmates blue, Brothers brown, the rest small and pale) and the actions the
 * {@code /pack} command allows, in three styles. BOOK is the Grimoire's: a ledger with leaders, an ACCEPT seal pinned at the foot and a leave link
 * that asks first. MOON is the Blood Moon's: the ledger in its card, the ring on the stage, accept and leave in the floating bar. TENSURA has the ring
 * on the stage and the ledger and the buttons in the info pane.
 */
public final class BookPackPage implements HubPageView {
    public enum Style { BOOK, MOON, TENSURA }

    private static final String KEY = PageKit.HUB + "pack.";
    private static final String BOOK = PageKit.HUB + "book.pack.";
    /** Past this many portraits on the ring the names are left out (the tooltip has them). */
    private static final int NAMED_MAX = 12;
    /** A name round the ring is kept while at least this many pixels of it fit clear of the faces and the other names; below that it is left to the tooltip. */
    private static final int RING_NAME_MIN = 22;

    @Nullable
    private String selected;
    private final PackWolves wolves = new PackWolves();
    private final Style style;
    private final String ledgerRegion;
    private final String ringRegion;

    public BookPackPage() {
        this(Style.BOOK, BookKit.MAIN, BookKit.SIDE);
    }

    /** The Blood Moon: the ledger in {@code ledgerRegion}, the ring in {@code ringRegion}. */
    public BookPackPage(String ledgerRegion, String ringRegion) {
        this(Style.MOON, ledgerRegion, ringRegion);
    }

    /** Tensura: the ledger (a scrolling, focusable list of everyone, allies too) in the left column, the ring in the right one, the selection in the info pane. */
    public BookPackPage(Style style) {
        this(style, BookKit.MAIN, style == Style.TENSURA ? BookKit.SIDE : BookKit.MOON);
    }

    private BookPackPage(Style style, String ledgerRegion, String ringRegion) {
        this.style = style;
        this.ledgerRegion = ledgerRegion;
        this.ringRegion = ringRegion;
    }

    private static PackRing.Role role(String rank) {
        String r = rank.toLowerCase(Locale.ROOT);
        if (r.contains("alpha")) {
            return PackRing.Role.ALPHA;
        }
        if (r.contains("beta")) {
            return PackRing.Role.BETA;
        }
        if (r.contains("mate") || r.contains("emissary") || r.contains("ally")) {
            return PackRing.Role.PACKMATE;
        }
        if (r.contains("brother")) {
            return PackRing.Role.BROTHER;
        }
        return PackRing.Role.SUBORDINATE;
    }

    /** The crown on the Grimoire's pale paper (a darker gold, 3 to 1 against it) and on the dark themes. */
    public static final int CROWN_ON_PAPER = 0xFF8C6A1F;
    public static final int CROWN_ON_DARK = 0xFFD9A93C;

    /** The gold that reads on {@code background}: the dark one on a pale background, the light one on a dark one. */
    public static int crownColor(int background) {
        double lum = (0.2126 * ((background >> 16) & 0xFF) + 0.7152 * ((background >> 8) & 0xFF) + 0.0722 * (background & 0xFF)) / 255.0;
        return lum > 0.5 ? CROWN_ON_PAPER : CROWN_ON_DARK;
    }

    /** A 7 by 6 crown in gold: three points on a band. */
    static void crown(HubScreen.Frame f, int x, int y) {
        int gold = crownColor(f.theme().paper());
        f.g.fill(x, y + 4, x + 7, y + 6, gold);
        f.g.fill(x, y + 1, x + 1, y + 4, gold);
        f.g.fill(x + 3, y, x + 4, y + 4, gold);
        f.g.fill(x + 6, y + 1, x + 7, y + 4, gold);
    }

    private static boolean leads(HubScreen.Frame f, List<HubSocial.Member> pack) {
        return !pack.isEmpty() && pack.get(0).name().equals(f.view.player().getName().getString());
    }

    /** The leader may remove a member of the pack (not themselves, an emissary or the leader of an allied pack). */
    private static boolean canKick(HubScreen.Frame f, List<HubSocial.Member> pack, @Nullable HubSocial.Member chosen) {
        return chosen != null && leads(f, pack) && !chosen.name().equals(f.view.player().getName().getString())
                && !chosen.base().equals("ally") && !chosen.base().equals("emissary");
    }

    /** A second Alpha of the pack (an Alpha who does not lead it) may challenge its leader to a duel for the lead. */
    private static boolean canChallenge(HubScreen.Frame f, List<HubSocial.Member> pack, WolfState wolf, @Nullable HubSocial.Member chosen) {
        return chosen != null && !pack.isEmpty() && chosen == pack.get(0) && !leads(f, pack) && !chosen.base().equals("ally")
                && (wolf.rank() == WolfState.Rank.ALPHA || wolf.rank() == WolfState.Rank.TRUE_ALPHA) && pack.stream().anyMatch(m -> m.name().equals(f.view.player().getName().getString()));
    }

    /** The leader is asked for the lead by a second Alpha: they can accept or decline. */
    private static boolean askedForTheLead(HubScreen.Frame f, List<HubSocial.Member> pack) {
        return leads(f, pack) && pack.stream().anyMatch(m -> m.base().equals("coalpha_challenger"));
    }

    /** Who is asking for the lead, for the hovers of accept and decline. */
    private static String challengerName(List<HubSocial.Member> pack) {
        return pack.stream().filter(m -> m.base().equals("coalpha_challenger")).map(HubSocial.Member::name).findFirst().orElse("");
    }

    private static boolean canEndAlliance(HubScreen.Frame f, List<HubSocial.Member> pack, @Nullable HubSocial.Member chosen) {
        return chosen != null && leads(f, pack) && chosen.base().equals("ally");
    }

    /** Where Decline goes beside Accept (8 pixels after it), or -1 when it does not fit in the width and goes on the next line. */
    public static int declineX(int x, int acceptWidth, int declineWidth, int width) {
        int at = x + acceptWidth + 8;
        return at + declineWidth <= x + width ? at : -1;
    }

    /** A row whose rank (with its spark count) would leave the name fewer than this many pixels puts the rank on its own line. */
    public static final int MIN_NAME = 24;

    /** On a row that has to stack, the crown goes only when the rank would not fit on its own line beside it. */
    public static boolean dropCrown(int rowWidth, int rankWidth) {
        return rankWidth > rowWidth - 8 - 9;
    }

    public static boolean rankOnOwnLine(int width, int rankWidth) {
        return width - rankWidth - 10 < MIN_NAME;
    }

    /** The name cut so that the whole rank (with its spark count) always fits beside it: the name goes first, never the rank. */
    static String nameFor(HubScreen.Frame f, String name, Component rank, int width) {
        return net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(name, nameRoom(width, f.font().width(rank)), f.font()::width);
    }

    /** What is left of a row for the name once the whole rank and a gap are there: never less than a few letters. */
    public static int nameRoom(int width, int rankWidth) {
        return Math.max(16, width - rankWidth - 10);
    }

    /** "Alpha", or "Alpha ×3" with stacked sparks. */
    static Component rankLabel(HubScreen.Frame f, WolfState wolf) {
        Component name = Component.translatable("supernaturalbloodlines.hub.pack.rank." + wolf.rank().name().toLowerCase(Locale.ROOT));
        int sparks = ownSparks(f); // the server's count: a True Alpha's full sparks are in their own row of the pack
        return sparks > 1 && (wolf.rank() == WolfState.Rank.ALPHA || wolf.rank() == WolfState.Rank.TRUE_ALPHA)
                ? Component.empty().append(name).append(" \u00d7" + sparks) : name;
    }

    /** For a weak spark that carries no growth time (a save from before the field): the default of {@code weak_spark_grow_days}. */
    static final int DEFAULT_GROW_TICKS = 3 * 24000;

    /** "A weaker spark is growing in you: ready in about 2 days." while the viewer holds one (R145); empty otherwise. Only the viewer's own state: it is shown to nobody else. */
    static java.util.Optional<Component> weakLine(WolfState wolf) {
        if (wolf.weakSparks() <= 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Component.translatable("supernaturalbloodlines.pack.weak_spark_line",
                net.ofatech.supernaturalbloodlines.core.bloodline.WeakSparkRules.daysLeft(wolf.weak(), DEFAULT_GROW_TICKS)));
    }

    /** The sparks the server counts for the viewer: their own row of the pack (a True Alpha's full count included), else the plain count. */
    static int ownSparks(HubScreen.Frame f) {
        java.util.UUID self = f.view.player().getUUID();
        return f.view.snapshot().pack().stream().filter(m -> m.id().filter(self::equals).isPresent()).mapToInt(HubSocial.Member::sparks)
                .findFirst().orElse(f.view.bloodline().wolf().sparks());
    }

    private static Component rankName(HubSocial.Member m) {
        Component name = Component.translatableWithFallback(KEY + "member." + m.base(), HubModel.pretty(m.base()));
        // an Alpha who stacked sparks (the Deucalion factor) is shown as "Alpha ×3"
        return m.sparks() > 1 ? Component.empty().append(name).append(" \u00d7" + m.sparks()) : name;
    }

    @Override
    public void render(HubScreen.Frame f) {
        WolfState wolf = f.view.bloodline().wolf();
        List<HubSocial.Member> pack = f.view.snapshot().pack();
        HubSocial.Member chosen = pack.stream().filter(m -> m.name().equals(selected)).findFirst().orElse(null);
        ledger(f, wolf, pack, chosen);
        ring(f, pack, wolf);
        if (style != Style.BOOK) {
            detail(f, wolf, pack, chosen);
        }
    }

    private void ledger(HubScreen.Frame f, WolfState wolf, List<HubSocial.Member> pack, @Nullable HubSocial.Member chosen) {
        HubTheme t = f.theme();
        Rect r = f.region(ledgerRegion);
        if (style == Style.BOOK) {
            f.reserve(ledgerRegion, BookKit.sealReserve()); // the ACCEPT seal and the line that says what it accepts are pinned at the foot of the page
        }
        int y = BookKit.head(f, ledgerRegion, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle"));
        y = PageKit.title(f, Component.translatable(KEY + "rank", rankLabel(f, wolf)), r.x(), y, r.w(), t.accent());
        if (weakLine(wolf).isPresent()) {
            y = PageKit.wrapped(f, weakLine(wolf).get(), r.x(), y, r.w(), t.muted()) + 2;
        }
        if (pack.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "alone"), r.x(), y + 2, r.w(), t.muted());
        }
        for (HubSocial.Member m : pack) {
            Component rank = rankName(m);
            // the first of the list leads the pack: a small gold crown before the name
            boolean leads = m == pack.get(0) && !m.base().equals("ally");
            int crown = leads ? 9 : 0;
            // the rank (Alpha x3) is never what is cut: on a row too narrow for both it goes on its own line. The crown stays unless
            // the rank would not fit beside it even there
            boolean tight = rankOnOwnLine(r.w() - 8 - crown, f.font().width(rank));
            if (tight && dropCrown(r.w(), f.font().width(rank))) {
                crown = 0;
            }
            boolean stacked = tight || (style == Style.TENSURA && f.font().width(m.name()) + f.font().width(rank) + 14 > r.w() - 8 - crown);
            // the row, and its tint, are as tall as what is drawn in it (20 pixels when the rank has a line of its own)
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, stacked ? 20 : 11);
            // a row that is not whole above the "more below" marker is not drawn at all (its target is kept)
            boolean drawn = f.whole(y - 1, hit.h());
            if (drawn && m == chosen) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            if (drawn) {
                f.g.fill(r.x(), y + 2, r.x() + 4, y + 6, m.online() ? 0xFF2F6E4A : 0xFF9A8566);
                if (leads && crown > 0) {
                    crown(f, r.x() + 8, y + 1);
                }
                if (stacked) {
                    f.g.drawString(f.font(), net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache.cut(f.font(), m.name(), r.w() - 8 - crown, f.font()::width), r.x() + 8 + crown, y, t.ink(), false);
                    f.g.drawString(f.font(), net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache.cut(f.font(), rank.getString(), r.w() - 8 - crown, f.font()::width), r.x() + 8 + crown, y + 10, t.muted(), false);
                } else {
                    BookKit.leader(f, Component.literal(nameFor(f, m.name(), rank, r.w() - 8 - crown)), rank, r.x() + 8 + crown, y, r.w() - 8 - crown, t.muted());
                }
            }
            String id = "member:" + m.name();
            List<Component> tip = new java.util.ArrayList<>(List.of(Component.literal(m.name()), rank, Component.translatable(KEY + (m.online() ? "online" : "offline"))));
            if (leads) {
                tip.add(1, Component.translatable(KEY + "leader"));
            }
            tip.add(Component.translatable(KEY + "open_profile"));
            f.target(id, hit, tip, openProfile(f, m.name()));
            if (drawn && f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            y += stacked ? 21 : 11;
        }
        y = wolves.render(f, r, y + 2, wolf.lineage() != WolfState.Lineage.NONE, false);
        t.drawRule(f.g, r.x(), y + 3, r.w());
        y += 9;
        if (style == Style.BOOK && chosen != null) {
            y = BookKit.caps(f, Component.translatable(BOOK + "selected"), r.x(), y, t.muted());
            y = PageKit.title(f, Component.literal(chosen.name()), r.x(), y, r.w(), t.ink());
            y = PageKit.wrapped(f, Component.translatable(KEY + (chosen.online() ? "online" : "offline")), r.x(), y, r.w(), t.muted()) + 4;
            if (canKick(f, pack, chosen)) {
                BookKit.link(f, "pack:kick", Component.translatable(KEY + "kick"), r.x(), y, List.of(Component.translatable(KEY + "kick_for", chosen.name())),
                        () -> f.confirm(Component.translatable(KEY + "kick_title", chosen.name()), Component.translatable(KEY + "kick_body"), Component.translatable(KEY + "kick"),
                                () -> BloodlinePage.command("pack kick " + chosen.name())));
                y += 13;
            } else if (canChallenge(f, pack, wolf, chosen)) {
                BookKit.link(f, "pack:challenge", Component.translatable(KEY + "challenge"), r.x(), y, List.of(Component.translatable(KEY + "challenge_for", chosen.name())),
                        () -> f.confirm(Component.translatable(KEY + "challenge_title", chosen.name()), Component.translatable(KEY + "challenge_body"),
                                Component.translatable(KEY + "challenge"), () -> BloodlinePage.command("pack challenge " + chosen.name())));
                y += 13;
            } else if (canEndAlliance(f, pack, chosen)) {
                BookKit.link(f, "pack:ally_end", Component.translatable(KEY + "ally_end"), r.x(), y, List.of(Component.translatable(KEY + "ally_end_for", chosen.name())),
                        () -> f.confirm(Component.translatable(KEY + "ally_end_title", chosen.name()), Component.translatable(KEY + "ally_end_body"),
                                Component.translatable(KEY + "ally_end"), () -> BloodlinePage.command("pack ally remove " + chosen.name())));
                y += 13;
            }
        }
        if (style == Style.BOOK && askedForTheLead(f, pack)) {
            String asking = challengerName(pack);
            int acceptW = BookKit.link(f, "pack:duel_accept", Component.translatable(KEY + "duel_accept"), r.x(), y, List.of(Component.translatable(KEY + "duel_accept_for", asking)),
                    () -> BloodlinePage.command("pack challenge accept"));
            Component decline = Component.translatable(KEY + "duel_decline");
            // beside the accept link when it fits, under it when it does not
            int dx = declineX(r.x(), acceptW, f.font().width(decline), r.w());
            int dy = dx < 0 ? y + 13 : y;
            BookKit.link(f, "pack:duel_decline", decline, dx < 0 ? r.x() : dx, dy, List.of(Component.translatable(KEY + "duel_decline_for", asking)),
                    () -> BloodlinePage.command("pack challenge decline"));
            y = dy + 13;
        }
        boolean mayInvite = leads(f, pack) || (pack.isEmpty() && (wolf.rank() == WolfState.Rank.ALPHA || wolf.rank() == WolfState.Rank.TRUE_ALPHA));
        if (style != Style.BOOK) {
            y = PackInvite.render(f, r, y + 2, mayInvite);
        }
        f.end(y);
        if (style == Style.BOOK) {
            // the foot of the page: the seal, the two invite buttons beside it and, under them, the way out
            int sy = r.bottom() - BookKit.SEAL;
            BookKit.seal(f, "pack:accept", Component.translatable(BOOK + "accept"), true, r.x(), sy, () -> BloodlinePage.command("pack accept"),
                    List.of(Component.translatable(KEY + "accept")));
            PackInvite.buttons(f, new Rect(r.x() + BookKit.SEAL + 8, sy + 2, r.w() - BookKit.SEAL - 8, PageWidgets.H), sy + 2, mayInvite);
            // the seal says only ACCEPT: the line beside it says what it accepts
            String note = net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache.cut(f.font(), Component.translatable(KEY + "accept").getString(), r.w(), f.font()::width);
            f.g.drawString(f.font(), note, r.x(), BookKit.sealNoteY(sy), t.muted(), false);
            if (!pack.isEmpty()) {
                BookKit.link(f, "pack:leave", Component.translatable(KEY + "leave"), r.x() + BookKit.SEAL + 8, sy + 24, List.of(Component.translatable(KEY + "leave")),
                        () -> f.confirm(Component.translatable(BOOK + "leave_title"), Component.translatable(BOOK + "leave_body"), Component.translatable(BOOK + "leave_yes"),
                                () -> BloodlinePage.command("pack leave")));
            }
        }
    }

    /** A click on a member (portrait or ledger row) selects them and opens their Profile when the snapshot named an id for them. */
    private Runnable openProfile(HubScreen.Frame f, String name) {
        java.util.UUID id = f.view.snapshot().pack().stream().filter(m -> m.name().equals(name)).flatMap(m -> m.id().stream()).findFirst().orElse(null);
        return () -> {
            selected = name;
            if (id != null) {
                f.screen().openProfile(id, name);
            }
        };
    }

    /** The Blood Moon's floating bar and Tensura's info pane: the chosen member (or the pack) and what the command allows. */
    private void detail(HubScreen.Frame f, WolfState wolf, List<HubSocial.Member> pack, @Nullable HubSocial.Member chosen) {
        HubSocial.Npc pickedWolf = wolves.chosen(f);
        if (pickedWolf != null) {
            f.detail(wolves.detail(pickedWolf, style == Style.MOON));
            return;
        }
        Component title = chosen != null ? Component.literal(chosen.name()) : Component.translatable(KEY + "title");
        Component body = chosen != null ? Component.translatable(KEY + (chosen.online() ? "online" : "offline")) : Component.translatable(BOOK + "subtitle");
        Detail.Builder d = Detail.of(title).body(body);
        if (style == Style.TENSURA) {
            d.field(Component.translatable(KEY + "rank", rankLabel(f, wolf)), Component.empty());
            if (chosen != null && !pack.isEmpty() && chosen == pack.get(0) && !chosen.base().equals("ally")) {
                d.field(Component.translatable(KEY + "role"), Component.translatable(KEY + "leader"));
            }
        }
        d.action("accept", Component.translatable(KEY + "accept"), true, true, () -> BloodlinePage.command("pack accept"));
        if (canKick(f, pack, chosen)) {
            d.action(new Detail.Action("kick", Component.translatable(KEY + "kick"), false, true, null,
                    new Detail.Confirm(Component.translatable(KEY + "kick_title", chosen.name()), Component.translatable(KEY + "kick_body")),
                    () -> BloodlinePage.command("pack kick " + chosen.name())));
        } else if (canChallenge(f, pack, wolf, chosen)) {
            d.action(new Detail.Action("challenge", Component.translatable(KEY + "challenge_btn", chosen.name()), false, true, null,
                    new Detail.Confirm(Component.translatable(KEY + "challenge_title", chosen.name()), Component.translatable(KEY + "challenge_body")),
                    () -> BloodlinePage.command("pack challenge " + chosen.name())));
        } else if (canEndAlliance(f, pack, chosen)) {
            d.action(new Detail.Action("ally_end", Component.translatable(KEY + "ally_end"), false, true, null,
                    new Detail.Confirm(Component.translatable(KEY + "ally_end_title", chosen.name()), Component.translatable(KEY + "ally_end_body")),
                    () -> BloodlinePage.command("pack ally remove " + chosen.name())));
        }
        if (askedForTheLead(f, pack)) {
            d.action("duel_accept", Component.translatable(KEY + "duel_accept"), true, true, () -> BloodlinePage.command("pack challenge accept"));
            d.action("duel_decline", Component.translatable(KEY + "duel_decline"), false, true, () -> BloodlinePage.command("pack challenge decline"));
            d.field(Component.translatable(KEY + "duel_asked"), Component.literal(challengerName(pack)));
        }
        if (!pack.isEmpty()) {
            d.action(new Detail.Action("leave", Component.translatable(KEY + "leave"), false, true, null,
                    new Detail.Confirm(Component.translatable(BOOK + "leave_title"), Component.translatable(BOOK + "leave_body")), () -> BloodlinePage.command("pack leave")));
        }
        f.detail(d.build());
    }

    private void ring(HubScreen.Frame f, List<HubSocial.Member> pack, WolfState wolf) {
        HubTheme t = f.theme();
        Rect s = f.region(ringRegion);
        int top = f.begin(ringRegion);
        HubSocial.Npc pickedWolf = wolves.chosen(f);
        if (style == Style.BOOK && pickedWolf != null) {
            // the Grimoire's ledger scrolls: the picked wolf's profile and orders are drawn on the right page, in full view, in place of the ring until it is closed
            int py = wolves.profile(f, new Rect(s.x(), top, s.w(), s.bottom() - top), top, pickedWolf);
            BookKit.link(f, "pack:wolf_close", Component.translatable(KEY + "wolves.close"), s.x(), py + 4, List.of(Component.translatable(KEY + "wolves.close_tip")), wolves::close);
            f.end(py + 18);
            return;
        }
        if (pack.isEmpty() && f.view.snapshot().npcs().isEmpty()) {
            // nobody beside you: the empty state instead of an empty ring
            int emptyBottom = EmptyState.render(f, s, top + Math.max(0, (s.bottom() - top - 150) / 2), "pack", HubPage.GUIDE);
            if (style == Style.BOOK) {
                PackInvite.picker(f, new Rect(s.x(), top, s.w(), 0), top, true);
            }
            f.end(Math.max(s.bottom(), emptyBottom));
            return;
        }
        // on the Blood Moon stage the ledger card takes the right of it, so the ring is centred in what is left
        int areaW = style == Style.MOON ? Math.max(60, f.region("right").x() - 14 - s.x()) : s.w();
        // the legend is drawn at the width of that area, 9 pixels a line: the room kept for it is that many lines, whole
        int legend = 9 * Math.max(1, f.font().split(Component.translatable(BOOK + "legend"), areaW).size()) + 3;
        int cy = top + (s.bottom() - legend - top) / 2;
        int cx = s.x() + areaW / 2;
        List<PackRing.Member> members = new ArrayList<>();
        pack.stream().filter(m -> !m.name().equals(f.view.player().getName().getString()) && !m.base().equals("ally")).forEach(m -> members.add(new PackRing.Member(m.name(), role(m.rank()))));
        f.view.snapshot().npcs().forEach(n -> members.add(new PackRing.Member(n.name(), role(n.source()))));
        int face = members.size() > 8 ? 12 : 16;
        // the faces sit on a ring; their names go outward (beside the side ones, over the top ones, under the bottom ones), small, inside the page
        int radius = PackRing.radiusFor(areaW, s.bottom() - legend - top, face);
        int inner = Math.max(14, radius * 2 / 5);
        f.emblem(new Rect(cx - inner - 2, cy - inner - 2, 2 * inner + 4, 2 * inner + 4));
        Component you = Component.translatable(BOOK + "you", f.view.player().getName(), Component.translatable(KEY + "rank." + wolf.rank().name().toLowerCase(Locale.ROOT)));
        PackRing.Layout ring = PackRing.layout(members, cx, cy, radius, face);
        // the line under the model is for a ring with room: where a face stands in its way it is left out (the ledger has the rank)
        int yw = BookKit.smallWidth(f, you.getString());
        boolean showYou = radius >= 40 && PackRing.clearOfFaces(ring, cx - yw / 2, cy + inner + 4, yw + 4, 9, 2);
        if (showYou) {
            BookKit.small(f, you, cx - yw / 2, cy + inner + 4, yw + 4, t.accent());
        }
        boolean named = ring.slots().size() <= NAMED_MAX && radius >= PackRing.NAMED_RADIUS; // a crowded or small ring keeps the faces and the tooltips (the ledger has the names)
        // what a name may not cover: every face with its ring, the emblem, and the "you" line under the model
        List<Rect> blocked = new ArrayList<>();
        blocked.add(new Rect(cx - inner - 2, cy - inner - 2, 2 * inner + 4, 2 * inner + 4));
        if (showYou) {
            blocked.add(new Rect(cx - yw / 2, cy + inner + 4, yw + 4, 9));
        }
        List<RingLabels.Request> asked = new ArrayList<>();
        for (PackRing.Slot slot : ring.slots()) {
            int size = slot.size();
            blocked.add(new Rect(slot.x() - size / 2 - 2, slot.y() - size / 2 - 2, size + 4, size + 4));
            if (named) {
                String n = TextCache.cut(f.font(), slot.member().id(), 56, f.font()::width);
                int nw = BookKit.smallWidth(f, n);
                int side = Math.abs(slot.x() - cx) > radius / 2 ? (slot.x() > cx ? 1 : -1) : 0;
                int ny = side != 0 ? slot.y() - 3 : slot.y() < cy ? slot.y() - size / 2 - 11 : slot.y() + size / 2 + 3;
                asked.add(side == 0 ? new RingLabels.Request(slot.x(), ny, nw, 9, RingLabels.Anchor.CENTER)
                        : side > 0 ? new RingLabels.Request(slot.x() + size / 2 + 3, ny, nw, 9, RingLabels.Anchor.LEFT)
                        : new RingLabels.Request(slot.x() - size / 2 - 3, ny, nw, 9, RingLabels.Anchor.RIGHT));
            }
        }
        List<Rect> where = TextCache.ring(asked, blocked, new Rect(s.x(), top - 12, areaW, s.bottom() - top + 12), RING_NAME_MIN, 2);
        int named_i = 0;
        for (PackRing.Slot slot : ring.slots()) {
            int size = slot.size();
            int color = (slot.member().role().color() & 0xFFFFFF) | ((int) (slot.alpha() * 255) << 24);
            BookKit.portrait(f.g, slot.member().id(), slot.x(), slot.y(), size, color, t.paper());
            if (named) {
                Rect at = where.get(named_i++);
                if (at != null) {
                    String n = TextCache.cut(f.font(), slot.member().id(), Math.min(56, at.w()), f.font()::width);
                    BookKit.small(f, Component.literal(n), at.x(), at.y(), at.w() + 4, slot.member().role().small() ? t.muted() : t.ink());
                }
            }
            f.target("ring:" + slot.member().id(), new Rect(slot.x() - size / 2 - 2, slot.y() - size / 2 - 2, size + 4, size + 4),
                    List.of(Component.literal(slot.member().id()), Component.translatable(KEY + "open_profile")), openProfile(f, slot.member().id()));
        }
        if (ring.hidden() > 0) {
            Component more = Component.translatable(BOOK + "ring_more", ring.hidden());
            f.g.drawString(f.font(), more, cx - f.font().width(more) / 2, PackRing.moreLabelY(ring, cy), t.muted(), false);
        }
        BookKit.small(f, Component.translatable(BOOK + "legend"), s.x(), s.bottom() - legend + 1, areaW, t.muted());
        if (style == Style.BOOK) {
            // the players to pick from, on a card over the top of the ring
            PackInvite.picker(f, new Rect(s.x(), top, s.w(), 0), top, true);
        }
        f.end(s.bottom());
    }
}
