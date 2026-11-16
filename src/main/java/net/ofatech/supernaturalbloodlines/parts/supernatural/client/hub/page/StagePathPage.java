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
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * Path for Tensura's status window: the magic circle with the live model in the middle of the stage, the ladder as a row of hexagons under it (the
 * ones you have reached ticked, the current one open, the rest sealed), the moon almanac in a box along the bottom, and the next rung's name, its
 * tasks and the reward in the info pane with Track on HUD (waits for the HUD update) and Claim reward as its buttons.
 */
public final class StagePathPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "path.";
    private static final String MOON = PageKit.HUB + "moon.";
    private static final int HEX = 18;

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.LadderView> ladders = f.view.ladders();
        HubModel.LadderView ladder = ladders.isEmpty() ? null : ladders.get(0);
        BookKit.moonHead(f, Component.translatable(MOON + "path_of", PageKit.race(f.view.races().get(0))), null);
        Rect stage = f.region(BookKit.MOON);
        int top = f.begin(BookKit.MOON);
        int almanacH = 22;
        int ladderH = ladder == null ? 0 : HEX + 16;
        int size = Math.max(50, Math.min(stage.w() - 20, stage.bottom() - top - almanacH - ladderH - 8));
        Rect circle = new Rect(stage.x() + (stage.w() - size) / 2, top + 2, size, size);
        f.emblem(circle);
        int y = circle.bottom() + 6;
        if (ladder != null) {
            ladder(f, ladder, stage, y);
            y += ladderH;
        }
        almanac(f, new Rect(stage.x() + 6, Math.max(y, stage.bottom() - almanacH), stage.w() - 12, almanacH - 2));
        f.end(stage.bottom());
        detail(f, ladder);
    }

    /** The ladder: one hexagon per rung with a line through them, the name under each. */
    private void ladder(HubScreen.Frame f, HubModel.LadderView ladder, Rect stage, int y) {
        HubTheme t = f.theme();
        List<HubModel.RungRow> rungs = ladder.rungs();
        int n = rungs.size();
        int span = Math.min(stage.w() - 16, n * 70);
        int step = n <= 1 ? 0 : (span - HEX) / (n - 1);
        int x0 = stage.x() + (stage.w() - (step * (n - 1) + HEX)) / 2;
        f.g.fill(x0 + HEX / 2, y + HEX / 2, x0 + HEX / 2 + step * (n - 1), y + HEX / 2 + 1, (t.muted() & 0xFFFFFF) | 0x99000000);
        for (int i = 0; i < n; i++) {
            HubModel.RungRow row = rungs.get(i);
            int x = x0 + i * step;
            boolean reached = row.status() == RungStatus.REACHED;
            boolean current = row.status() == RungStatus.CURRENT;
            int edge = reached ? t.good() : current ? t.accent() : (t.muted() & 0xFFFFFF) | 0xAA000000;
            KitDraw.shape(f.g, Shapes.Shape.HEX, x, y, HEX, edge);
            KitDraw.shape(f.g, Shapes.Shape.HEX, x + 2, y + 2, HEX - 4, t.paper());
            KitDraw.icon(f.g, reached ? Icons.CHECK : current ? Icons.DOT : Icons.LOCK, x + (HEX - 10) / 2, y + (HEX - 10) / 2, 10, edge);
            Component name = PageKit.rung(row);
            String text = f.font().plainSubstrByWidth(name.getString(), Math.max(40, step + 16));
            int tw = BookKit.smallWidth(f, text);
            int lx = Math.max(stage.x(), Math.min(stage.right() - tw - 4, x + HEX / 2 - tw / 2));
            BookKit.small(f, Component.literal(text), lx, y + HEX + 3, tw + 4, current ? t.accent() : reached ? t.ink() : t.muted());
            Rect hit = new Rect(x - 2, y, Math.max(HEX + 4, tw + 4), HEX + 12);
            String id = "phase:" + ladder.race().id() + ":" + row.id();
            f.target(id, hit, PathPage.rungTooltip(ladder, row), null);
            if (f.focused(id)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
        }
    }

    /** The next moons in a box with the days to the full moon. */
    private static void almanac(HubScreen.Frame f, Rect box) {
        HubTheme t = f.theme();
        BookKit.outline(f.g, box, (t.paper() & 0xFFFFFF) | 0xCC000000, (t.muted() & 0xFFFFFF) | 0x77000000);
        int phase = f.view.player().level().getMoonPhase();
        int x = box.x() + 8;
        int y = box.y() + (box.h() - 8) / 2;
        for (int p : HubAlmanac.phases(phase, 6)) {
            KitDraw.icon(f.g, Icons.moon(p), x, y, 8, p == 0 ? 0xFFffd36b : t.muted());
            x += 11;
        }
        int days = HubAlmanac.daysToFull(phase);
        Component line = days == 0 ? Component.translatable(KEY + "full_moon_today") : Component.translatable(KEY + "full_moon_in", days);
        f.g.drawString(f.font(), f.font().plainSubstrByWidth(line.getString(), box.right() - x - 10), x + 6, y, t.accent(), false);
    }

    private void detail(HubScreen.Frame f, HubModel.LadderView ladder) {
        if (ladder == null || ladder.atTop()) {
            f.detail(Detail.of(Component.translatable(KEY + "complete_title")).body(Component.translatable(KEY + "complete")).build());
            return;
        }
        HubModel.RungRow active = ladder.active().orElseThrow();
        Optional<HubModel.TaskRow> claimable = ladder.tasks().stream().filter(r -> r.state() == TaskState.CLAIMABLE).findFirst();
        Detail.Builder d = Detail.of(PageKit.rung(active)).kicker(Component.translatable(KEY + "to_become")).pips(active.claimed(), Math.max(active.tasks(), 1))
                .body(Component.translatable(KEY + "info_body", active.tasks())).reward(PathPage.reward(active));
        for (HubModel.TaskRow row : ladder.tasks()) {
            d.row(PageKit.text(row.text()), SourceTag.of(row.source()), row.counted() ? row.progress() : row.state() == TaskState.OPEN ? 0 : 1, row.counted() ? row.target() : 1);
        }
        d.action(new Detail.Action("track", Component.translatable(KEY + "track"), false, false, Component.translatable(KEY + "track_soon"), null, () -> { }));
        d.action("claim", Component.translatable(KEY + "claim_reward"), true, claimable.isPresent(), () -> claimable.ifPresent(r -> f.claim(r.task().id())));
        f.detail(d.build());
    }
}
