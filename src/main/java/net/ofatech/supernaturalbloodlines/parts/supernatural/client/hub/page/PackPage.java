package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Pack: your rank and the pack's members (from the snapshot: names, ranks, who is online) on the left; on the right the {@code /pack}
 * actions the command already allows, sent as that command so the server's own checks and messages apply.
 */
public final class PackPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "pack.";
    private static final long CONFIRM_TICKS = 100;

    private long confirmUntil;

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        WolfState wolf = f.view.bloodline().wolf();
        List<HubSocial.Member> pack = f.view.snapshot().pack();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y = PageKit.wrapped(f, Component.translatable(KEY + "rank", BookPackPage.rankLabel(f, wolf)),
                r.x(), y + 6, r.w(), t.ink()) + 3;
        y = PageKit.wrapped(f, Component.translatable(KEY + "members").withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), t.accent());
        if (pack.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "alone"), r.x() + 4, y, r.w() - 4, t.muted());
        }
        for (HubSocial.Member m : pack) {
            Component rank = Component.translatable(KEY + "member." + m.base()).copy().append(m.sparks() > 1 ? " \u00d7" + m.sparks() : "");
            int rw = f.font().width(rank);
            f.g.fill(r.x() + 1, y + 2, r.x() + 5, y + 6, m.online() ? 0xFF2f6e4a : 0xFF9a8566);
            String name = LabelFit.cut(m.name(), r.w() - 10 - rw - 4, f.font()::width);
            f.g.drawString(f.font(), name, r.x() + 9, y, t.ink(), false);
            f.g.drawString(f.font(), rank, r.right() - rw, y, t.muted(), false);
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, PageKit.LINE + 1);
            String id = "member:" + m.name();
            f.target(id, hit, List.of(Component.literal(m.name()), rank, Component.translatable(KEY + (m.online() ? "online" : "offline"))), null);
            if (f.focused(id)) {
                t.drawFocus(f.g, hit);
            }
            y += PageKit.LINE + 1;
        }
        f.end(y);

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = PageKit.title(f, Component.translatable(KEY + "actions"), s.x(), sy, s.w(), t.accent());
        t.drawRule(f.g, s.x(), sy + 2, s.w());
        sy = SkillsPage.stamp(f, s, sy + 2, "pack:accept", Component.translatable(KEY + "accept"), () -> command("pack accept"));
        if (!pack.isEmpty()) {
            // leaving takes two presses within 5 seconds: the first only asks
            long now = f.view.player().level().getGameTime();
            boolean asking = now < confirmUntil;
            sy = SkillsPage.stamp(f, s, sy + 2, "pack:leave", Component.translatable(KEY + (asking ? "leave_confirm" : "leave")), () -> {
                if (asking) {
                    confirmUntil = 0;
                    command("pack leave");
                } else {
                    confirmUntil = now + CONFIRM_TICKS;
                }
            });
        }
        boolean alpha = wolf.rank() == WolfState.Rank.ALPHA || wolf.rank() == WolfState.Rank.TRUE_ALPHA;
        boolean leads = !pack.isEmpty() && pack.get(0).name().equals(f.view.player().getName().getString());
        sy = PackInvite.render(f, s, sy + 6, leads || (pack.isEmpty() && alpha));
        f.end(sy);
    }

    /** Sends the command as the player typed it: the server checks it exactly as it checks a typed one. */
    static void command(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand(command);
        }
    }
}
