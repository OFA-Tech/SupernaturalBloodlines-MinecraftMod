package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubView;

/**
 * Path in the Blood Moon theme: the moon above already shows the ladder (its phases), so the cards hold the next rung's tasks with their
 * claim stamps (left) and your skill points and story (right). Same tasks, same stamps, same claim packet as the shared Path page.
 */
public final class BloodMoonPathPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "path.";

    @Override
    public void render(HubScreen.Frame f) {
        HubView v = f.view;
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        if (v.ghoul()) {
            y = PathPage.turning(f, r, y);
        }
        boolean any = false;
        for (HubModel.LadderView ladder : v.ladders()) {
            if (ladder.atTop() || v.ghoul()) {
                continue;
            }
            any = true;
            HubModel.RungRow active = ladder.active().orElseThrow();
            Component head = Component.translatable(PageKit.HUB + "moon.evolution", PageKit.rung(active), active.claimed(), active.tasks());
            y = PageKit.wrapped(f, head.copy().withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), t.muted()) + 2;
            for (HubModel.TaskRow task : ladder.tasks()) {
                y = PageKit.taskRow(f, task, r.x(), y, r.w(), true, null, false);
            }
            y = PageKit.wrapped(f, PathPage.reward(active), r.x(), y + 1, r.w(), t.muted()) + 5;
        }
        if (!any && !v.ghoul()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "complete"), r.x(), y, r.w(), t.ink());
        }
        f.end(y);

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = PageKit.wrapped(f, Component.translatable(PageKit.HUB + "moon.points").withStyle(ChatFormatting.BOLD), s.x(), sy, s.w(), t.muted());
        sy = PageKit.title(f, Component.literal(String.valueOf(v.state().sp())), s.x(), sy + 1, s.w(), t.accent());
        sy = PageKit.wrapped(f, Component.translatable(PageKit.HUB + "moon.earned", v.state().spEarned()), s.x(), sy, s.w(), t.muted()) + 4;
        t.drawRule(f.g, s.x(), sy, s.w());
        sy = PageKit.wrapped(f, v.player().getName(), s.x(), sy + 4, s.w(), t.ink());
        for (Component line : java.util.stream.Stream.of(PathPage.sireLine(v), PathPage.tensuraLine()).flatMap(java.util.Optional::stream).toList()) {
            sy = PageKit.wrapped(f, line, s.x(), sy, s.w(), t.muted());
        }
        f.end(sy);
    }
}
