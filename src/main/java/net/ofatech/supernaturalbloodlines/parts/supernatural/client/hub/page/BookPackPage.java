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

    private static PackRing.Role role(String rank) {
        String r = rank.toLowerCase(Locale.ROOT);
        if (r.contains("alpha")) {
            return PackRing.Role.ALPHA;
        }
        if (r.contains("beta")) {
            return PackRing.Role.BETA;
        }
        if (r.contains("mate")) {
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
        Rect r = f.region(BookKit.MAIN);
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle"));
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
            Component rank = Component.translatable(KEY + "member." + m.rank());
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
        BookKit.seal(f, "pack:accept", Component.translatable(BOOK + "accept"), true, r.x(), y, () -> BloodlinePage.command("pack accept"),
                List.of(Component.translatable(KEY + "accept")));
        int lx = r.x() + BookKit.SEAL + 8;
        if (!pack.isEmpty()) {
            BookKit.link(f, "pack:leave", Component.translatable(KEY + "leave"), lx, y + 6, List.of(Component.translatable(KEY + "leave")),
                    () -> f.confirm(Component.translatable(BOOK + "leave_title"), Component.translatable(BOOK + "leave_body"), Component.translatable(BOOK + "leave_yes"),
                            () -> BloodlinePage.command("pack leave")));
        }
        y += BookKit.SEAL + 4;
        y = BookKit.small(f, Component.translatable(BOOK + "invite"), r.x(), y, r.w(), t.muted());
        f.end(y);
        ring(f, pack, wolf);
    }

    private void ring(HubScreen.Frame f, List<HubSocial.Member> pack, WolfState wolf) {
        HubTheme t = f.theme();
        Rect s = f.region(BookKit.SIDE);
        int top = f.begin(BookKit.SIDE);
        int legend = 20;
        int cy = top + (s.bottom() - legend - top) / 2;
        int cx = s.x() + s.w() / 2;
        int radius = Math.max(26, Math.min(s.w() / 2 - 16, (s.bottom() - legend - top) / 2 - 12));
        int inner = Math.max(14, radius / 2);
        f.emblem(new Rect(cx - inner - 2, cy - inner - 2, 2 * inner + 4, 2 * inner + 4));
        Component you = Component.translatable(BOOK + "you", f.view.player().getName(), Component.translatable(KEY + "rank." + wolf.rank().name().toLowerCase(Locale.ROOT)));
        f.g.drawString(f.font(), you, cx - f.font().width(you) / 2, cy + inner + 4, t.accent(), false);
        List<PackRing.Member> members = new ArrayList<>();
        pack.stream().filter(m -> !m.name().equals(f.view.player().getName().getString())).forEach(m -> members.add(new PackRing.Member(m.name(), role(m.rank()))));
        for (PackRing.Slot slot : PackRing.ring(members, cx, cy, radius, 16)) {
            int size = slot.size();
            int color = (slot.member().role().color() & 0xFFFFFF) | ((int) (slot.alpha() * 255) << 24);
            BookKit.portrait(f.g, slot.member().id(), slot.x(), slot.y(), size, color, t.paper());
            String n = f.font().plainSubstrByWidth(slot.member().id(), 44);
            f.g.drawString(f.font(), n, slot.x() - f.font().width(n) / 2, slot.y() + size / 2 + 3, slot.member().role().small() ? t.muted() : t.ink(), false);
            f.target("ring:" + slot.member().id(), new Rect(slot.x() - size / 2 - 2, slot.y() - size / 2 - 2, size + 4, size + 4),
                    List.of(Component.literal(slot.member().id())), () -> selected = slot.member().id());
        }
        BookKit.small(f, Component.translatable(BOOK + "legend"), s.x(), s.bottom() - legend, s.w(), t.muted());
        f.end(s.bottom());
    }
}
