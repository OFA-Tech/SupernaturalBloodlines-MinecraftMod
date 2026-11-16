package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PackRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.jetbrains.annotations.Nullable;

/**
 * The Grimoire's Pack: a ledger of the pack (leaders between each name and its rank), the chosen member with the actions the command allows
 * (the ACCEPT seal, a red link to leave that asks first), and on the facing page the portrait ring round the sigil: Alphas red, Betas gold,
 * Packmates blue, Brothers brown, the rest small and pale.
 */
public final class BookPackPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "pack.";
    private static final String BOOK = PageKit.HUB + "book.pack.";

    @Nullable
    private String selected;
    private final String ledgerRegion;
    private final String ringRegion;

    public BookPackPage() {
        this(BookKit.MAIN, BookKit.SIDE);
    }

    /** @param ledgerRegion where the ledger and the actions go; @param ringRegion where the portrait ring goes (the Blood Moon puts them in its card and on the stage) */
    public BookPackPage(String ledgerRegion, String ringRegion) {
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
        if (r.contains("mate") || r.contains("emissary")) {
            return PackRing.Role.PACKMATE;
        }
        if (r.contains("brother")) {
            return PackRing.Role.BROTHER;
        }
        return PackRing.Role.SUBORDINATE;
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        WolfState wolf = f.view.bloodline().wolf();
        List<HubSocial.Member> pack = f.view.snapshot().pack();
        Rect r = f.region(ledgerRegion);
        int y = BookKit.head(f, ledgerRegion, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle"));
        y = PageKit.title(f, Component.translatable(KEY + "rank", Component.translatable(KEY + "rank." + wolf.rank().name().toLowerCase(Locale.ROOT))), r.x(), y, r.w(), t.accent());
        if (pack.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "alone"), r.x(), y + 2, r.w(), t.muted());
        }
        HubSocial.Member chosen = pack.stream().filter(m -> m.name().equals(selected)).findFirst().orElse(null);
        for (HubSocial.Member m : pack) {
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, 11);
            if (m == chosen) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            f.g.fill(r.x(), y + 2, r.x() + 4, y + 6, m.online() ? 0xFF2F6E4A : 0xFF9A8566);
            Component rank = Component.translatableWithFallback(KEY + "member." + m.rank(), net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.pretty(m.rank()));
            BookKit.leader(f, Component.literal(m.name()), rank, r.x() + 8, y, r.w() - 8, t.muted());
            String id = "member:" + m.name();
            f.target(id, hit, List.of(Component.literal(m.name()), rank, Component.translatable(KEY + (m.online() ? "online" : "offline"))), () -> selected = m.name());
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            y += 11;
        }
        t.drawRule(f.g, r.x(), y + 3, r.w());
        y += 9;
        if (chosen != null) {
            y = BookKit.caps(f, Component.translatable(BOOK + "selected"), r.x(), y, t.muted());
            y = PageKit.title(f, Component.literal(chosen.name()), r.x(), y, r.w(), t.ink());
            y = PageKit.wrapped(f, Component.translatable(KEY + (chosen.online() ? "online" : "offline")), r.x(), y, r.w(), t.muted()) + 4;
        }
        boolean moon = !ledgerRegion.equals(BookKit.MAIN);
        if (!moon) {
            BookKit.seal(f, "pack:accept", Component.translatable(BOOK + "accept"), true, r.x(), y, () -> BloodlinePage.command("pack accept"),
                    List.of(Component.translatable(KEY + "accept")));
        }
        int lx = r.x() + BookKit.SEAL + 8;
        if (!pack.isEmpty() && !moon) {
            BookKit.link(f, "pack:leave", Component.translatable(KEY + "leave"), lx, y + 6, List.of(Component.translatable(KEY + "leave")),
                    () -> f.confirm(Component.translatable(BOOK + "leave_title"), Component.translatable(BOOK + "leave_body"), Component.translatable(BOOK + "leave_yes"),
                            () -> BloodlinePage.command("pack leave")));
        }
        y += moon ? 2 : BookKit.SEAL + 4;
        y = BookKit.small(f, Component.translatable(BOOK + "invite"), r.x(), y, r.w(), t.muted());
        f.end(y);
        ring(f, pack, wolf);
        if (moon) {
            bar(f, pack, chosen);
        }
    }

    /** The Blood Moon's floating bar: the chosen member (or the pack), and what the command allows: accept an invitation, leave (asks first). */
    private void bar(HubScreen.Frame f, List<HubSocial.Member> pack, @Nullable HubSocial.Member chosen) {
        Component title = chosen != null ? Component.literal(chosen.name()) : Component.translatable(KEY + "title");
        Component body = chosen != null ? Component.translatable(KEY + (chosen.online() ? "online" : "offline")) : Component.translatable(BOOK + "subtitle");
        Detail.Builder d = Detail.of(title).body(body);
        d.action("accept", Component.translatable(KEY + "accept"), true, true, () -> BloodlinePage.command("pack accept"));
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
        int legend = 17;
        // on the Blood Moon stage the ledger card takes the right of it, so the ring is centred in what is left
        int areaW = ringRegion.equals(BookKit.MOON) ? Math.max(60, f.region("right").x() - 14 - s.x()) : s.w();
        int cy = top + (s.bottom() - legend - top) / 2;
        int cx = s.x() + areaW / 2;
        List<PackRing.Member> members = new ArrayList<>();
        pack.stream().filter(m -> !m.name().equals(f.view.player().getName().getString())).forEach(m -> members.add(new PackRing.Member(m.name(), role(m.rank()))));
        int face = members.size() > 8 ? 12 : 16;
        // the faces sit on a ring; their names go outward (beside the side ones, over the top ones, under the bottom ones), small, inside the page
        int radius = Math.max(26, Math.min(areaW / 2 - 34, (s.bottom() - legend - top) / 2 - face / 2 - 12));
        int inner = Math.max(14, radius * 2 / 5);
        f.emblem(new Rect(cx - inner - 2, cy - inner - 2, 2 * inner + 4, 2 * inner + 4));
        Component you = Component.translatable(BOOK + "you", f.view.player().getName(), Component.translatable(KEY + "rank." + wolf.rank().name().toLowerCase(Locale.ROOT)));
        int yw = BookKit.smallWidth(f, you.getString());
        BookKit.small(f, you, cx - yw / 2, cy + inner + 4, yw + 4, t.accent());
        for (PackRing.Slot slot : PackRing.ring(members, cx, cy, radius, face)) {
            int size = slot.size();
            int color = (slot.member().role().color() & 0xFFFFFF) | ((int) (slot.alpha() * 255) << 24);
            BookKit.portrait(f.g, slot.member().id(), slot.x(), slot.y(), size, color, t.paper());
            String n = f.font().plainSubstrByWidth(slot.member().id(), 56);
            int nw = BookKit.smallWidth(f, n);
            int side = Math.abs(slot.x() - cx) > radius / 2 ? (slot.x() > cx ? 1 : -1) : 0;
            int nx = side == 0 ? slot.x() - nw / 2 : side > 0 ? slot.x() + size / 2 + 3 : slot.x() - size / 2 - 3 - nw;
            int ny = side != 0 ? slot.y() - 3 : slot.y() < cy ? slot.y() - size / 2 - 9 : slot.y() + size / 2 + 3;
            BookKit.small(f, Component.literal(n), Math.max(s.x(), Math.min(s.x() + areaW - nw - 2, nx)), ny, nw + 4, slot.member().role().small() ? t.muted() : t.ink());
            f.target("ring:" + slot.member().id(), new Rect(slot.x() - size / 2 - 2, slot.y() - size / 2 - 2, size + 4, size + 4),
                    List.of(Component.literal(slot.member().id())), () -> selected = slot.member().id());
        }
        BookKit.small(f, Component.translatable(BOOK + "legend"), s.x(), s.bottom() - legend + 1, areaW, t.muted());
        f.end(s.bottom());
    }
}