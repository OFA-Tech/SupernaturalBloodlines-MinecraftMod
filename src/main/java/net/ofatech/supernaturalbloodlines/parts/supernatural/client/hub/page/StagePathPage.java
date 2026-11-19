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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LadderLabels;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * Path for Tensura's status window: the magic circle with the live model in the middle of the stage, the ladder as a row of hexagons under it (the
 * ones you have reached ticked, the current one open, the rest sealed), the moon almanac in a box along the bottom, and the next rung's name, its
 * tasks and the reward in the info pane with Track on HUD (needs the HUD part) and Claim reward as its buttons.
 */
public final class StagePathPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "path.";
    private static final String MOON = PageKit.HUB + "moon.";
    private static final int HEX = 18;

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
        BookKit.moonHead(f, Component.translatable(MOON + "path_of", PageKit.race(f.view.races().get(0))), null);
        Rect stage = f.region(BookKit.MOON);
        int top = f.begin(BookKit.MOON);
        top += LadderTabs.render(f, stage.x(), top, stage.w());
        int almanacH = almanacHeight(f, stage.w() - 12);
        int ladderH = ladder == null ? 0 : HEX + 22 + (staggered(f, ladder, stage) ? 18 : 0);
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
        int step = LadderLabels.step(stage.w(), n, HEX);
        int x0 = LadderLabels.x0(stage, n, HEX);
        List<LadderLabels.Placed> labels = LadderLabels.place(rungs.stream().map(r -> PageKit.rung(r).getString()).toList(), stage, HEX, y, s -> BookKit.smallWidth(f, s));
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
            LadderLabels.Placed label = labels.get(i);
            int tw = 0;
            for (int k = 0; k < label.lines().size(); k++) {
                Rect b = label.boxes().get(k);
                tw = Math.max(tw, b.w() - 4);
                BookKit.small(f, Component.literal(label.lines().get(k)), b.x(), b.y(), b.w(), current ? t.accent() : reached ? t.ink() : t.muted());
            }
            Rect hit = new Rect(x - 2, y, Math.max(HEX + 4, tw + 4), HEX + 4 + 8 * label.lines().size());
            PathActions.rungTarget(f, ladder, row, hit);
        }
    }

    /** Whether the rung names need two alternating rows to stay apart (a name's longest word does not fit the gap between rungs). */
    private static boolean staggered(HubScreen.Frame f, HubModel.LadderView ladder, Rect stage) {
        return LadderLabels.staggered(ladder.rungs().stream().map(r -> PageKit.rung(r).getString()).toList(), stage.w(), HEX, s -> BookKit.smallWidth(f, s));
    }

    /** The next moons in a box with the days to the full moon. */
    /** The width the full-moon line has beside the six moons of an almanac box {@code boxW} wide. */
    private static int fullMoonWidth(int step, int boxW) {
        return Math.max(40, boxW - 8 - HubAlmanac.ROW * step - 10 - 6);
    }

    /** The height of the almanac box: the full-moon line wrapped beside the moons, then every lunar event line wrapped under them. 22 as it was without either. */
    private static int almanacHeight(HubScreen.Frame f, int boxW) {
        var view = AlmanacEvents.view(f);
        int step = AlmanacEvents.step(view, 11, 8);
        int fullH = f.font().split(PathActions.almanacLine(f.view.player().level().getMoonPhase()), fullMoonWidth(step, boxW)).size() * PageKit.LINE;
        int eventH = AlmanacEvents.height(f, view, boxW - 16, PageKit.LINE);
        return view.isEmpty() && fullH <= PageKit.LINE ? 22 : 7 + Math.max(14, fullH) + eventH + 4;
    }

    private static void almanac(HubScreen.Frame f, Rect box) {
        HubTheme t = f.theme();
        BookKit.outline(f.g, box, (t.paper() & 0xFFFFFF) | 0xCC000000, (t.muted() & 0xFFFFFF) | 0x77000000);
        int phase = f.view.player().level().getMoonPhase();
        var view = AlmanacEvents.view(f);
        int step = AlmanacEvents.step(view, 11, 8);
        int x = box.x() + 8;
        int fullH = f.font().split(PathActions.almanacLine(phase), fullMoonWidth(step, box.w())).size() * PageKit.LINE;
        int y = view.isEmpty() && fullH <= PageKit.LINE ? box.y() + (box.h() - 8) / 2 : box.y() + 7;
        int day = 0;
        for (int p : HubAlmanac.phases(phase, HubAlmanac.ROW)) {
            KitDraw.icon(f.g, Icons.moon(p), x, y, 8, p == 0 ? 0xFFffd36b : t.muted());
            AlmanacEvents.ring(f, view, day++, x, y, 8);
            x += step;
        }
        Component line = PathActions.almanacLine(phase);
        PageKit.wrapped(f, line, x + 6, y, fullMoonWidth(step, box.w()), t.accent());
        int ey = y + Math.max(14, fullH);
        for (var e : view.lines()) { // wrapped, so the day count is never cut, in the theme's ink
            ey = PageKit.wrapped(f, AlmanacEvents.line(e), box.x() + 8, ey, box.w() - 16, t.ink());
        }
    }

    private void detail(HubScreen.Frame f, HubModel.LadderView ladder) {
        if (ladder == null || ladder.atTop()) {
            f.detail(Detail.of(Component.translatable(KEY + "complete_title")).body(Component.translatable(KEY + "complete")).build());
            return;
        }
        HubModel.RungRow active = ladder.active().orElseThrow();
        Detail.Builder d = PathActions.rung(ladder, Component.translatable(KEY + "to_become")).body(Component.translatable(KEY + "info_body", active.tasks()));
        for (HubModel.TaskRow row : ladder.tasks()) {
            d.row(PageKit.text(row.text()), SourceTag.of(row.source()), row.counted() ? row.progress() : row.state() == TaskState.OPEN ? 0 : 1, row.counted() ? row.target() : 1);
        }
        PathActions.addActions(d, f, ladder, Component.translatable(KEY + "claim_reward"));
        f.detail(d.build());
    }
}
