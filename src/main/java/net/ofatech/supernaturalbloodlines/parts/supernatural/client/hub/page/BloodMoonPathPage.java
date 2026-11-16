package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

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

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.LadderView> ladders = f.view.ladders();
        HubModel.LadderView ladder = ladders.isEmpty() ? null : ladders.get(0);
        BookKit.moonHead(f, Component.translatable(MOON + "path_of", PageKit.race(f.view.races().get(0))), subtitle(f, ladder));

        Rect stage = f.region(BookKit.MOON);
        Rect col = f.region("right");
        int y0 = f.begin(BookKit.MOON);
        int labels = 78;
        Rect area = new Rect(stage.x(), y0, Math.max(60, col.x() - 14 - stage.x()), stage.bottom() - y0);
        int size = Math.max(60, Math.min(area.w() - labels, area.h() - 34));
        Rect moon = new Rect(area.x() + labels + Math.max(0, (area.w() - labels - size) / 2), area.y() + (area.h() - 18 - size) / 2, size, size);
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
            String text = f.font().plainSubstrByWidth(name.getString(), 120);
            int tw = BookKit.smallWidth(f, text);
            int color = row.status() == net.ofatech.supernaturalbloodlines.shared.hub.RungStatus.LOCKED ? t.muted() : row.status() == net.ofatech.supernaturalbloodlines.shared.hub.RungStatus.CURRENT ? t.accent() : t.ink();
            BookKit.small(f, Component.literal(text), Math.max(area.x(), gx - 4 - tw), gy + 1, tw + 4, color);
            Rect hit = new Rect(Math.max(area.x(), gx - 6 - tw), gy - 1, tw + 20, 11);
            String id = "phase:" + ladder.race().id() + ":" + row.id();
            f.target(id, hit, PathPage.rungTooltip(ladder, row), null);
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
        }
    }

    private static void almanac(HubScreen.Frame f, Rect area) {
        HubTheme t = f.theme();
        int phase = f.view.player().level().getMoonPhase();
        int y = area.bottom() - 12;
        int x = area.x() + 2;
        for (int p : HubAlmanac.phases(phase, 6)) {
            KitDraw.icon(f.g, Icons.moon(p), x, y, 8, p == 0 ? 0xFFffd36b : t.muted());
            x += 10;
        }
        int days = HubAlmanac.daysToFull(phase);
        BookKit.small(f, days == 0 ? Component.translatable(KEY + "full_moon_today") : Component.translatable(KEY + "full_moon_in", days), x + 4, y + 1, 120, t.muted());
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
            boolean sel = chosen.map(c -> c.task().id().equals(id)).orElse(false);
            boolean claimable = row.state() == TaskState.CLAIMABLE;
            Rect box = new Rect(col.x(), y, col.w(), 30);
            f.g.fill(box.x(), box.y(), box.right(), box.bottom(), claimable ? t.good() : sel ? t.accent() : t.muted());
            f.g.fill(box.x() + 1, box.y() + 1, box.right() - 1, box.bottom() - 1, 0xFF160a10);
            int tw = KitDraw.sourceTag(f.g, f.font(), t, SourceTag.of(row.source()), box.x() + 4, box.y() + 4);
            String text = f.font().plainSubstrByWidth(PageKit.text(row.text()).getString(), box.w() - tw - 14);
            f.g.drawString(f.font(), text, box.x() + tw + 8, box.y() + 5, t.ink(), false);
            float frac = row.counted() && row.target() > 0 ? Math.min(1f, row.progress() / (float) row.target()) : row.state() == TaskState.CLAIMED || claimable ? 1f : 0f;
            String count = row.counted() && row.target() > 1 ? row.progress() + "/" + row.target() : claimable ? "\u2713" : "";
            int cw = BookKit.smallWidth(f, count);
            t.drawBar(f.g, new Rect(box.x() + 4, box.y() + 19, box.w() - 12 - cw, 5), frac);
            BookKit.small(f, Component.literal(count), box.right() - cw - 4, box.y() + 18, cw + 4, claimable ? t.good() : t.muted());
            f.target("task:" + id, box, PageKit.tooltip(row), () -> f.select(id));
            if (f.focused("task:" + id)) {
                KitDraw.focusRing(f.g, box, t.focusColor());
            }
            y += 34;
        }
        y = BookKit.small(f, PathPage.reward(active), col.x(), y + 2, col.w(), t.accent());
        f.end(y);
    }

    /** The floating bar: the rung, its pips, a line, Track on HUD (waits for the HUD update) and Claim reward. */
    private void bar(HubScreen.Frame f, HubModel.LadderView ladder) {
        if (ladder == null || ladder.atTop()) {
            return;
        }
        HubModel.RungRow active = ladder.active().orElseThrow();
        Optional<HubModel.TaskRow> claimable = ladder.tasks().stream().filter(r -> r.state() == TaskState.CLAIMABLE).findFirst();
        Detail.Builder d = Detail.of(PageKit.rung(active)).pips(active.claimed(), Math.max(active.tasks(), 1))
                .body(Component.translatable(MOON + "bar", active.claimed(), active.tasks(), PathPage.reward(active)));
        d.action(new Detail.Action("track", Component.translatable(KEY + "track"), false, false, Component.translatable(KEY + "track_soon"), null, () -> { }));
        d.action("claim", Component.translatable(MOON + "claim"), true, claimable.isPresent(), () -> claimable.ifPresent(r -> f.claim(r.task().id())));
        f.detail(d.build());
    }
}
