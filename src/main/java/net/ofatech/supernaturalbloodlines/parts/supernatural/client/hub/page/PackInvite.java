package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.Pickable;

/**
 * The Invite and Ally controls of the Pack page: two buttons and, under the one pressed, a list of the players online (nearest first). A pick sends the
 * same server-checked command as the typed one ({@code pack invite <player>}, {@code pack ally invite <player>}); the server says why when it refuses.
 */
public final class PackInvite {
    private enum Mode { NONE, INVITE, ALLY }

    private static Mode mode = Mode.NONE;
    /** Built when a picker opens, not every frame: the list of players is read off the connection and sorted once per press. */
    private static List<Pickable.Person> offered = List.of();
    private static int beyond;

    private static void toggle(Mode wanted, HubScreen.Frame f) {
        mode = mode == wanted ? Mode.NONE : wanted;
        List<Pickable.Person> all = online();
        offered = mode == Mode.NONE ? List.of() : Pickable.of(all, f.view.player().getUUID(), idsOf(f));
    }

    private PackInvite() {}

    private static boolean demo;

    /** Development only (the dev screenshots): the Invite picker open with a made-up list of players, 14 more than the cap. */
    public static void devOpen() {
        mode = Mode.INVITE;
        demo = true;
        offered = new ArrayList<>();
        String[] names = {"Aria", "Bram", "Cleo", "Dax", "Eira", "Finn", "Gwen", "Hale", "Iris", "Jory", "Kade", "Lena"};
        for (int k = 0; k < names.length; k++) {
            offered.add(new Pickable.Person(new UUID(31, k), names[k], k < 8 ? 6 + k * 9 : -1));
        }
        beyond = 14;
    }

    /** The hub closed: the next one starts with no picker open. */
    public static void reset() {
        mode = Mode.NONE;
        offered = List.of();
        beyond = 0;
        demo = false;
    }

    /** True while a picker is open. */
    static boolean picking() {
        return mode != Mode.NONE;
    }

    /** The Invite and Ally buttons in a row; {@code mayInvite} is the viewer leading a pack, or being an Alpha with none yet. Returns the y under them. */
    static int buttons(HubScreen.Frame f, Rect r, int y, boolean mayInvite) {
        String key = PageKit.HUB + "pack.";
        Component why = Component.translatable(key + "invite_why");
        Component invite = Component.translatable(key + "invite_btn");
        Component ally = Component.translatable(key + "ally_btn");
        List<Integer> widths = List.of(PageWidgets.widthOf(f.font(), invite), PageWidgets.widthOf(f.font(), ally));
        Flow.Result flow = Flow.wrap(new Rect(r.x(), y, r.w(), 0), widths, PageWidgets.H, PageWidgets.GAP);
        PageWidgets.button(f, "stage:pack:invite", flow.rects().get(0), invite, mayInvite, mode == Mode.INVITE, why, () -> toggle(Mode.INVITE, f));
        PageWidgets.button(f, "stage:pack:ally", flow.rects().get(1), ally, mayInvite, mode == Mode.ALLY, why, () -> toggle(Mode.ALLY, f));
        if (!mayInvite) {
            mode = Mode.NONE;
        }
        return flow.bottom() + 3;
    }

    /** The players to pick from, under the pressed button, in {@code r}; {@code card} puts them on a card of the page's paper (over what is beneath). Returns the y under them. */
    static int picker(HubScreen.Frame f, Rect r, int y, boolean card) {
        if (mode == Mode.NONE) {
            return y;
        }
        HubTheme t = f.theme();
        String key = PageKit.HUB + "pack.";
        Component title = Component.translatable(key + (mode == Mode.INVITE ? "invite_who" : "ally_who"));
        List<Pickable.Person> people = demo ? offered : Pickable.stillHere(offered, PackInvite::stillHere);
        if (!demo) {
            beyond = Pickable.beyondCap(online(), f.view.player().getUUID(), idsOf(f)); // every frame: players come and go while it is open
        }
        int pad = card ? 4 : 0;
        Rect in = new Rect(r.x() + pad, y + pad, r.w() - 2 * pad, 0);
        Component more = beyond > 0 ? Component.translatable(key + "pick_more", beyond).append(" ").append(Component.translatable(key + "pick_more_why")) : null;
        int moreH = more == null ? 0 : f.font().split(more, in.w()).size() * PageKit.LINE + 2;
        List<Integer> pw = people.stream().map(p -> PageWidgets.widthOf(f.font(), Component.literal(p.name()))).toList();
        Flow.Result pf = Flow.wrap(new Rect(in.x(), in.y() + PageKit.LINE + 2, in.w(), 0), pw, PageWidgets.H, 3);
        int none = people.isEmpty() ? f.font().split(Component.translatable(key + "pick_none"), in.w()).size() * PageKit.LINE : 0;
        int bottom = (people.isEmpty() ? in.y() + PageKit.LINE + 2 + none : pf.bottom() + moreH) + pad;
        if (card) {
            f.g.fill(r.x(), y, r.right(), bottom, 0xFF000000 | t.paper());
            f.g.renderOutline(r.x(), y, r.w(), bottom - y, t.accent());
        }
        f.g.drawString(f.font(), title.copy().withStyle(ChatFormatting.BOLD), in.x(), in.y(), t.accent(), false);
        if (people.isEmpty()) {
            PageKit.wrapped(f, Component.translatable(key + "pick_none"), in.x(), in.y() + PageKit.LINE + 2, in.w(), t.muted());
            return bottom + 3;
        }
        String row = mode == Mode.INVITE ? "P1" : "P5";
        for (int i = 0; i < people.size(); i++) {
            Pickable.Person p = people.get(i);
            Component tip = p.distance() >= 0 ? Component.translatable(key + "pick_near", Math.round(p.distance())) : Component.translatable(key + "pick_far");
            PageWidgets.button(f, "stage:pack:pick:" + p.name(), pf.rects().get(i), Component.literal(p.name()), true, false, tip, () -> {
                HubClient.runCommand(row, List.of(p.id().toString()));
                mode = Mode.NONE;
                offered = List.of();
            });
        }
        if (more != null) {
            // the cap is the nearest 30: the rest are not listed, and a plain line says how many and why (not a button that does nothing)
            PageKit.wrapped(f, more, in.x(), pf.bottom() + 2, in.w(), t.muted());
        }
        return bottom + 3;
    }

    /** Both together, one under the other: for a column with room. */
    static int render(HubScreen.Frame f, Rect r, int y, boolean mayInvite) {
        y = buttons(f, r, y, mayInvite);
        if (!mayInvite) {
            return PageKit.wrapped(f, Component.translatable(PageKit.HUB + "pack.invite_why"), r.x(), y, r.w(), f.theme().muted());
        }
        return picker(f, r, y, false);
    }
    private static boolean stillHere(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && mc.getConnection().getPlayerInfo(id) != null;
    }

    private static List<Pickable.Person> online() {
        Minecraft mc = Minecraft.getInstance();
        List<Pickable.Person> out = new ArrayList<>();
        if (mc.getConnection() == null || mc.player == null) {
            return out;
        }
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            var near = mc.level == null ? null : mc.level.getPlayerByUUID(id);
            out.add(new Pickable.Person(id, info.getProfile().getName(), near == null ? -1 : near.distanceTo(mc.player)));
        }
        return out;
    }

    static Set<UUID> idsOf(HubScreen.Frame f) {
        Set<UUID> out = new HashSet<>();
        f.view.snapshot().pack().forEach(m -> m.id().ifPresent(out::add));
        return out;
    }
}

