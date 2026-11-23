package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.WardClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.WardHub;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;

/**
 * Your ward (R310): on the Path page, a card for each ward the player owns (its kind and place, who is invited, and buttons to withdraw an invitation,
 * invite somebody online or drop it) and a line of the homes that welcome them. Every button sends the same server-checked command as the typed one
 * (W1 to W3); the client decides nothing. A player with no ward and no welcome sees nothing.
 */
public final class WardCard {
    private static final String KEY = PageKit.HUB + "ward.";
    private static final int PEOPLE = 8;
    private static String confirming = "";

    private WardCard() {}

    /** Whether there is anything to draw. */
    public static boolean any() {
        return !WardClient.view().isEmpty();
    }

    /** The card in {@code w} at {@code x}, from {@code y}; returns the y under it ({@code y} itself when there is nothing to show). */
    public static int render(HubScreen.Frame f, int x, int y, int w) {
        WardHub.View v = WardClient.view();
        if (v.isEmpty()) {
            return y;
        }
        HubTheme t = f.theme();
        y += 4;
        if (!v.own().isEmpty()) {
            y = PageKit.title(f, Component.translatable(KEY + "title"), x, y, w, t.accent());
            for (WardHub.Own ward : v.own()) {
                y = ward(f, ward, x, y, w);
            }
        }
        if (!v.welcomedBy().isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "welcomed", String.join(", ", v.welcomedBy())), x, y + 2, w, t.ink());
        }
        return y + 4;
    }

    private static int ward(HubScreen.Frame f, WardHub.Own ward, int x, int y, int w) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, summary(ward), x, y + 1, w, ward.fallen() ? t.muted() : t.ink());
        if (ward.fallen()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "fallen_why"), x, y, w, t.muted());
        } else {
            y = names(f, "ward:withdraw:" + ward.id() + ":", ward.invited(), "withdraw", "W2", x, y, w);
            y = names(f, "ward:invite:" + ward.id() + ":", candidates(ward), "invite", "W1", x, y, w);
        }
        boolean sure = confirming.equals(ward.id());
        Component label = Component.translatable(KEY + (sure ? "drop_sure" : "drop"));
        Rect box = new Rect(x, y + 1, PageWidgets.widthOf(f.font(), label), PageWidgets.H);
        PageWidgets.button(f, "ward:drop:" + ward.id(), box, label, true, sure, Component.translatable(KEY + "drop_why"), () -> {
            if (sure) {
                confirming = "";
                HubClient.runCommand("W3", List.of(ward.id()));
            } else {
                confirming = ward.id();
            }
        });
        return box.bottom() + 4;
    }

    private static Component summary(WardHub.Own ward) {
        return Component.translatable(KEY + (ward.door() ? "door" : "area"), ward.x(), ward.y(), ward.z(), ward.invited().size());
    }

    /** One button per name, wrapped; the first ones only (the server holds the rest). Returns the y under them. */
    private static int names(HubScreen.Frame f, String idPrefix, List<WardHub.Member> people, String key, String row, int x, int y, int w) {
        if (people.isEmpty()) {
            return y;
        }
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + key + "_label"), x, y, w, t.muted());
        List<WardHub.Member> shown = people.subList(0, Math.min(people.size(), PEOPLE));
        List<Integer> widths = new ArrayList<>();
        for (WardHub.Member m : shown) {
            widths.add(PageWidgets.widthOf(f.font(), Component.literal(m.name())));
        }
        Flow.Result flow = Flow.wrap(new Rect(x, y, w, 0), widths, PageWidgets.H, 3);
        for (int i = 0; i < shown.size(); i++) {
            WardHub.Member m = shown.get(i);
            PageWidgets.button(f, idPrefix + m.id(), flow.rects().get(i), Component.literal(m.name()), true, false,
                    Component.translatable(KEY + key + "_tip", m.name()), () -> HubClient.runCommand(row, List.of(m.id().toString())));
        }
        return flow.bottom() + 3;
    }

    /** The players online who are not on the list yet, by name. */
    private static List<WardHub.Member> candidates(WardHub.Own ward) {
        if (WardClient.demo()) { // the dev screenshots: made-up people online
            return List.of(new WardHub.Member(new UUID(31, 1), "Caroline"), new WardHub.Member(new UUID(31, 2), "Damon"), new WardHub.Member(new UUID(31, 3), "Stefan"));
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || Minecraft.getInstance().player == null) {
            return List.of();
        }
        Set<UUID> skip = new HashSet<>();
        ward.invited().forEach(m -> skip.add(m.id()));
        skip.add(Minecraft.getInstance().player.getUUID());
        List<WardHub.Member> out = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (!skip.contains(info.getProfile().getId())) {
                out.add(new WardHub.Member(info.getProfile().getId(), info.getProfile().getName()));
            }
        }
        out.sort(Comparator.comparing(m -> m.name().toLowerCase(java.util.Locale.ROOT)));
        return out;
    }

    /** The same facts as the card for a theme whose Path page has a side panel: a field per ward and the withdraw and invite buttons. */
    public static void detail(Detail.Builder d) {
        WardHub.View v = WardClient.view();
        boolean listed = false;
        for (WardHub.Own ward : v.own()) {
            d.field(Component.translatable(KEY + "title"), summary(ward));
            // an invitation goes into every ward of the owner, so the invite and withdraw buttons are shown once
            if (!ward.fallen() && !listed) {
                listed = true;
                ward.invited().stream().limit(PEOPLE).forEach(m -> d.action("ward:withdraw:" + ward.id() + ":" + m.id(),
                        Component.translatable(KEY + "withdraw_tip", m.name()), false, true, () -> HubClient.runCommand("W2", List.of(m.id().toString()))));
                candidates(ward).stream().limit(PEOPLE).forEach(m -> d.action("ward:invite:" + ward.id() + ":" + m.id(),
                        Component.translatable(KEY + "invite_tip", m.name()), false, true, () -> HubClient.runCommand("W1", List.of(m.id().toString()))));
            }
            d.action("ward:drop:" + ward.id(), Component.translatable(KEY + "drop"), false, true, () -> HubClient.runCommand("W3", List.of(ward.id())));
        }
        if (!v.welcomedBy().isEmpty()) {
            d.field(Component.translatable(KEY + "welcomed_label"), Component.literal(String.join(", ", v.welcomedBy())));
        }
    }

    /** The hub closed: nothing waits for a second press. */
    public static void reset() {
        confirming = "";
    }
}
