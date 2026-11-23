package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubDev;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * Path in the Blood Moon: the moon in the middle of the stage, lit as far as you have climbed, the rungs of your ladder as gems down its left
 * with their names, an almanac of the next moons under them; on the right the next rung's tasks as cards with their progress; in the bar the rung,
 * its count and what you can do about it. The same tasks, tooltips and claim packet as the shared Path page.
 */
public final class BloodMoonPathPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "path.";
    private static final String MOON = PageKit.HUB + "moon.";

    private final HubPageView ghoul = new StageGhoulPage();

    @Override
    public void render(HubScreen.Frame f) {
        if (f.view.ghoul()) {
            ghoul.render(f);
            return;
        }
        HubTheme t = f.theme();
        List<HubModel.LadderView> ladders = f.view.ladders();
        HubModel.LadderView ladder = ladders.isEmpty() ? null : ladders.get(f.screen().ladderIndex(ladders.size()));
        BookKit.moonHead(f, Component.translatable(MOON + "path_of", PageKit.race(f.view.races().get(0))), subtitle(f, ladder));

        Rect stage = f.region(BookKit.MOON);
        Rect col = f.region("right");
        int y0 = f.begin(BookKit.MOON);
        y0 += LadderTabs.render(f, stage.x(), y0, Math.max(60, col.x() - 14 - stage.x()));
        int labels = 78;
        Rect area = new Rect(stage.x(), y0, Math.max(60, col.x() - 14 - stage.x()), stage.bottom() - y0);
        int event = eventRoom(f, area); // lines under the moon for the lunar events
        int size = Math.max(60, Math.min(area.w() - labels, area.h() - 34 - event));
        Rect moon = new Rect(area.x() + labels + Math.max(0, (area.w() - labels - size) / 2), area.y() + (area.h() - 18 - event - size) / 2, size, size);
        f.emblem(moon);
        if (ladder != null) {
            gems(f, ladder, area, moon);
        }
        almanac(f, area);
        f.end(stage.bottom());

        card(f, col, ladder);
        bar(f, ladder);
    }

    private static Component subtitle(HubScreen.Frame f, HubModel.LadderView l) {
        Component name = f.view.player().getName();
        if (l == null) {
            return name;
        }
        int reached = (int) l.rungs().stream().filter(r -> r.status() != net.ofatech.supernaturalbloodlines.shared.hub.RungStatus.LOCKED).count();
        Component line = Component.translatable(MOON + "sub", name, PageKit.race(l.race()), reached, l.rungs().size());
        return net.ofatech.supernaturalbloodlines.shared.hub.HubHooks.tensura().map(info -> (Component) line.copy().append(" \u00b7 ").append(info.race())).orElse(line);
    }

    /** The rungs as gems down the left of the moon, the top of the ladder at the top, each with its name. */
    private void gems(HubScreen.Frame f, HubModel.LadderView ladder, Rect area, Rect moon) {
        HubTheme t = f.theme();
        List<HubModel.RungRow> rungs = ladder.rungs();
        int n = rungs.size();
        int cx = moon.x() + moon.w() / 2;
        int cy = moon.y() + moon.h() / 2;
        int radius = moon.w() / 2 + 14;
        for (int i = 0; i < n; i++) {
            HubModel.RungRow row = rungs.get(n - 1 - i);
            double k = n == 1 ? 0.5 : i / (double) (n - 1);
            double angle = Math.toRadians(235 - 110 * k);
            int gx = cx + (int) Math.round(radius * Math.cos(angle)) - 4;
            int gy = cy + (int) Math.round(radius * Math.sin(angle)) - 4;
            t.drawRungMark(f.g, gx, gy, row.status());
            Component name = PageKit.rung(row);
            String text = net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(name.getString(), Math.max(24, Math.min(120, gx - 6 - area.x())), f.font()::width); // never wider than the room left of its mark
            int tw = BookKit.smallWidth(f, text);
            int color = row.status() == net.ofatech.supernaturalbloodlines.shared.hub.RungStatus.LOCKED ? t.muted() : row.status() == net.ofatech.supernaturalbloodlines.shared.hub.RungStatus.CURRENT ? t.accent() : t.ink();
            BookKit.small(f, Component.literal(text), Math.max(area.x(), gx - 4 - tw), gy + 1, tw + 4, color);
            Rect hit = new Rect(Math.max(area.x(), gx - 6 - tw), gy - 1, tw + 20, 11);
            PathActions.rungTarget(f, ladder, row, hit);
        }
    }

    /** The height the event lines take under the moon (0 without events). */
    private static int eventRoom(HubScreen.Frame f, Rect area) {
        int h = AlmanacEvents.height(f, AlmanacEvents.view(f), area.w() - 4, 9);
        return h == 0 ? 0 : h + 4;
    }

    private static void almanac(HubScreen.Frame f, Rect area) {
        HubTheme t = f.theme();
        int phase = HubDev.moonPhase(f.view.player().level());
        int y = area.bottom() - 12;
        int x = area.x() + 2;
        var view = AlmanacEvents.view(f);
        int step = AlmanacEvents.step(view, 10, 8);
        int day = 0;
        AlmanacEvents.link(f, x, y, step, 8);
        for (int p : HubAlmanac.phases(phase, HubAlmanac.ROW)) {
            KitDraw.icon(f.g, Icons.moon(p), x, y, 8, p == 0 ? 0xFFffd36b : t.muted());
            AlmanacEvents.ring(f, view, day++, x, y, 8);
            x += step;
        }
        int ey = y - 4 - AlmanacEvents.height(f, view, area.w() - 4, 9); // the event lines sit above the row, wrapped, in the theme's ink
        for (var e : view.lines()) {
            ey = BookKit.small(f, AlmanacEvents.line(e), area.x() + 2, ey, area.w() - 4, t.ink()) - 1;
        }
        Component line = PathActions.almanacLine(phase);
        int room = Math.max(30, area.right() - x - 8);
        BookKit.small(f, Component.literal(net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(line.getString(), room, s -> BookKit.smallWidth(f, s))), x + 4, y + 1, room, t.muted());
    }

    /** The right card: the next rung's name and its tasks as cards with a progress bar, then the reward. */
    private void card(HubScreen.Frame f, Rect col, HubModel.LadderView ladder) {
        HubTheme t = f.theme();
        int y = f.begin("right");
        if (ladder == null || ladder.atTop()) {
            f.end(PageKit.wrapped(f, Component.translatable(KEY + "complete"), col.x(), y, col.w(), t.ink()));
            return;
        }
        HubModel.RungRow active = ladder.active().orElseThrow();
        y = BookKit.caps(f, Component.translatable(KEY + "to_become"), col.x(), y, t.muted());
        y = PageKit.title(f, PageKit.rung(active), col.x(), y, col.w(), t.accent()) + 2;
        Optional<HubModel.TaskRow> chosen = HubModel.pick(ladder.tasks(), f.selectedTask());
        for (HubModel.TaskRow row : ladder.tasks()) {
            String id = row.task().id();
            boolean claimable = row.state() == TaskState.CLAIMABLE;
            List<String> lines = net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.wrap(PageKit.text(row.text()).getString(),
                    Math.max(40, col.w() - KitDraw.tagWidth(f.font(), SourceTag.of(row.source())) - 14), 2, f.font()::width);
            boolean two = lines.size() > 1;
            Rect box = new Rect(col.x(), y, col.w(), two ? 40 : 30);
            f.g.fill(box.x(), box.y(), box.right(), box.bottom(), TaskCard.border(t, row, QuestFilter.isChosen(chosen, id)));
            f.g.fill(box.x() + 1, box.y() + 1, box.right() - 1, box.bottom() - 1, 0xFF160a10);
            int tw = KitDraw.sourceTag(f.g, f.font(), t, SourceTag.of(row.source()), box.x() + 4, box.y() + 4);
            // a task with no text has no lines: draw what there is
            for (int k = 0; k < lines.size(); k++) {
                f.g.drawString(f.font(), lines.get(k), box.x() + tw + 8, box.y() + 5 + 10 * k, t.ink(), false);
            }
            int by = box.y() + (two ? 29 : 19);
            float frac = TaskCard.fraction(row);
            String count = TaskCard.count(row);
            int cw = BookKit.smallWidth(f, count);
            t.drawBar(f.g, new Rect(box.x() + 4, by, box.w() - 12 - cw, 5), frac);
            BookKit.small(f, Component.literal(count), box.right() - cw - 4, by - 1, cw + 4, claimable ? t.good() : t.muted());
            TaskCard.target(f, row, box);
            y += box.h() + 4;
        }
        y = BookKit.small(f, PathPage.reward(active), col.x(), y + 2, col.w(), t.accent());
        y = WardCard.render(f, col.x(), y, col.w());
        f.end(y);
    }

    /** The floating bar: the rung, its pips, a line, Track on HUD (needs the HUD part) and Claim reward. */
    private void bar(HubScreen.Frame f, HubModel.LadderView ladder) {
        if (ladder == null || ladder.atTop()) {
            return;
        }
        HubModel.RungRow active = ladder.active().orElseThrow();
        Detail.Builder d = PathActions.rung(ladder, null).body(Component.translatable(MOON + "bar", active.claimed(), active.tasks(), PathPage.reward(active)));
        PathActions.addActions(d, f, ladder, Component.translatable(MOON + "claim"));
        f.detail(d.build());
    }
}
