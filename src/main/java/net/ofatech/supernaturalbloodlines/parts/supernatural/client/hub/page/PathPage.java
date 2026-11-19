package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubView;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/** Path: the left page is your story and your ladders (rungs as chapters), the right page the next rung's tasks with their claim stamps. */
public final class PathPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "path.";
    private final boolean book;

    /** The shared Path page. */
    public PathPage() {
        this(false);
    }

    /**
     * @param book the Grimoire's Path page: the moon almanac under the ladders, the sigil emblem (the live model) over the tasks, a footnote reward and
     *             the Track on HUD link; everything else is the same page
     */
    public PathPage(boolean book) {
        this.book = book;
    }

    @Override
    public void render(HubScreen.Frame f) {
        story(f, book);
        next(f, book);
    }

    /** The task "Track on HUD" follows on this ladder: the first one still open, or claimable when none is open. */
    public static Optional<HubModel.TaskRow> trackable(HubModel.LadderView ladder) {
        if (ladder == null || ladder.atTop()) {
            return Optional.empty();
        }
        return ladder.tasks().stream().filter(r -> r.state() == TaskState.OPEN).findFirst().or(() -> ladder.tasks().stream().filter(r -> r.state() == TaskState.CLAIMABLE).findFirst());
    }

    private static void story(HubScreen.Frame f, boolean book) {
        HubView v = f.view;
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        // with lunar events the almanac is pinned at the foot of the page (the scrolling part never hides it); without them it stays in the flow, as before
        boolean pin = book && !AlmanacEvents.view(f).isEmpty();
        int pinH = pin ? almanacHeight(f, r) : 0;
        if (pin) {
            f.reserve(HubLayout.MAIN, pinH + 4);
        }
        int y = f.begin(HubLayout.MAIN);
        f.g.drawString(f.font(), Component.translatable(KEY + "book_of"), r.x(), y, t.muted(), false);
        y = PageKit.title(f, v.player().getName(), r.x(), y + PageKit.LINE, r.w(), t.accent());
        y = PageKit.wrapped(f, subtitle(v), r.x(), y + 1, r.w(), t.muted());
        Optional<Component> tensura = tensuraLine();
        if (tensura.isPresent()) {
            y = PageKit.wrapped(f, tensura.get(), r.x(), y, r.w(), t.muted());
        }
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        boolean many = v.ladders().size() > 1;
        for (HubModel.LadderView ladder : LadderTabs.shown(f)) {
            if (many) {
                f.g.drawString(f.font(), PageKit.race(ladder.race()).copy().withStyle(ChatFormatting.BOLD), r.x(), y, t.accent(), false);
                y += PageKit.LINE + 1;
            }
            for (HubModel.RungRow row : ladder.rungs()) {
                y = rung(f, ladder, row, r, y);
            }
            y += 3;
        }
        t.drawRule(f.g, r.x(), y, r.w());
        y += 4;
        y = PageKit.wrapped(f, Component.translatable(KEY + "sp", v.state().sp(), v.state().spEarned()), r.x(), y, r.w(), t.ink());
        if (book && !pin) {
            y = almanac(f, r, y + 4);
        }
        f.end(y);
        if (pin) {
            almanac(f, r, r.bottom() - pinH);
        }
    }

    /** The height {@link #almanac} takes in {@code r}: the label, the moons with room for rings, the full-moon line and every event line, wrapped. */
    private static int almanacHeight(HubScreen.Frame f, Rect r) {
        var view = AlmanacEvents.view(f);
        int lift = view.byDay().isEmpty() ? 0 : 4;
        int fullH = f.font().split(PathActions.almanacLine(f.view.player().level().getMoonPhase()), r.w()).size() * PageKit.LINE;
        return PageKit.LINE + 14 + 2 * lift + fullH + AlmanacEvents.height(f, view, r.w(), PageKit.LINE);
    }

    /** The almanac: the next six moons from the level's day, and when the full moon rises. */
    private static int almanac(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        var level = f.view.player().level();
        int phase = level.getMoonPhase();
        f.g.drawString(f.font(), Component.translatable(KEY + "almanac"), r.x(), y, t.muted(), false);
        int x = r.x();
        var view = AlmanacEvents.view(f);
        int lift = view.byDay().isEmpty() ? 0 : 4; // room for the rings above and below the moons
        int step = AlmanacEvents.step(view, 14, 12);
        int day = 0;
        AlmanacEvents.link(f, x, y + PageKit.LINE + lift, step, 12);
        for (int p : HubAlmanac.phases(phase, HubAlmanac.ROW)) {
            KitDraw.icon(f.g, Icons.moon(p), x, y + PageKit.LINE + lift, 12, p == 0 ? 0xFFB8860B : t.ink());
            AlmanacEvents.ring(f, view, day++, x, y + PageKit.LINE + lift, 12);
            x += step;
        }
        Component line = PathActions.almanacLine(phase);
        y = PageKit.wrapped(f, line, r.x(), y + PageKit.LINE + 14 + 2 * lift, r.w(), t.accent());
        for (var e : view.lines()) {
            y = PageKit.wrapped(f, AlmanacEvents.line(e), r.x(), y, r.w(), t.ink());
        }
        return y;
    }

    private static Component subtitle(HubView v) {
        MutableComponent races = Component.empty();
        for (int i = 0; i < v.races().size(); i++) {
            races.append(i == 0 ? Component.empty() : Component.literal(" \u00b7 ")).append(PageKit.race(v.races().get(i)));
        }
        sireLine(v).ifPresent(sire -> races.append(Component.literal(" \u00b7 ")).append(sire));
        return races;
    }

    /** "sired by X" (or "(bond severed)"), when the snapshot names a sire. Shared with the themes' own Path views. */
    static Optional<Component> sireLine(HubView v) {
        return v.snapshot().sire().map(sire -> Component.translatable(v.snapshot().severed() ? KEY + "sire_severed" : KEY + "sire", sire));
    }

    /** "Tensura: race, EP" with Tensura and its part on. */
    static Optional<Component> tensuraLine() {
        return HubHooks.tensura().map(info -> Component.translatable(KEY + "tensura", info.race(), String.format("%,d", (long) info.ep())));
    }

    private static int rung(HubScreen.Frame f, HubModel.LadderView ladder, HubModel.RungRow row, Rect r, int y) {
        HubTheme t = f.theme();
        boolean current = row.status() == RungStatus.CURRENT;
        int h = current ? PageKit.LINE + 5 : PageKit.LINE + 1;
        t.drawRungMark(f.g, r.x(), y, row.status());
        int color = row.status() == RungStatus.LOCKED ? t.muted() : current ? t.accent() : t.ink();
        Component status = status(ladder, row);
        int sw = f.font().width(status);
        String name = LabelFit.cut(PageKit.rung(row).getString(), r.w() - HubTheme.MARK - 4 - sw - 4, f.font()::width);
        f.g.drawString(f.font(), name, r.x() + HubTheme.MARK + 4, y + 1, color, false);
        f.g.drawString(f.font(), status, r.right() - sw, y + 1, current ? t.accent() : t.muted(), false);
        if (current && row.tasks() > 0) {
            t.drawBar(f.g, new Rect(r.x() + HubTheme.MARK + 4, y + PageKit.LINE + 1, Math.min(70, r.w() / 2), 4), row.claimed() / (float) row.tasks());
        }
        Rect hit = new Rect(r.x(), y, r.w(), h - 1);
        String id = "rung:" + ladder.race().id() + ":" + row.id();
        f.target(id, hit, rungTooltip(ladder, row), null);
        if (f.focused(id)) {
            t.drawFocus(f.g, hit);
        }
        return y + h;
    }

    private static Component status(HubModel.LadderView ladder, HubModel.RungRow row) {
        return switch (row.status()) {
            case REACHED -> Component.translatable(KEY + "reached");
            case CURRENT -> Component.translatable(KEY + "of", row.claimed(), row.tasks());
            case LOCKED -> Component.translatable(apex(ladder, row) ? KEY + "apex" : KEY + "sealed");
        };
    }

    /** The last rung of a creature ladder is its apex. */
    static boolean apex(HubModel.LadderView ladder, HubModel.RungRow row) {
        return row.index() == ladder.rungs().size() - 1 && apexLadder(ladder.race());
    }

    private static boolean apexLadder(SupernaturalRace race) {
        return race != SupernaturalRace.HUMAN && race != SupernaturalRace.GHOUL;
    }

    /** A rung's tooltip: its name, where it stands (the apex says so), and its reward. Shared with the themes' own Path views and Blood Moon's gems. */
    public static List<Component> rungTooltip(HubModel.LadderView ladder, HubModel.RungRow row) {
        List<Component> out = new ArrayList<>();
        out.add(PageKit.rung(row));
        out.add(Component.translatable(KEY + "rung_state." + row.status().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        if (apex(ladder, row)) {
            out.add(Component.translatable(KEY + "rung_apex").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (row.index() > 0) {
            out.add(reward(row).copy().withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    /** "Reward: +N skill points" (and EP with Tensura). Shared with the themes' own Path views. */
    public static Component reward(HubModel.RungRow row) {
        return row.ep() > 0 && ModList.get().isLoaded("tensura")
                ? Component.translatable(KEY + "reward_ep", row.sp(), String.format("%,d", row.ep()))
                : Component.translatable(KEY + "reward", row.sp());
    }

    /** A Ghoul's turning: what it is and the time left. Returns the y under it. Shared with the themes' own Path views. */
    static int turning(HubScreen.Frame f, Rect r, int y) {
        HubTheme t = f.theme();
        y = PageKit.title(f, Component.translatable(KEY + "turning"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y = PageKit.wrapped(f, Component.translatable(KEY + "turning_text"), r.x(), y + 6, r.w(), t.ink());
        int ticks = f.view.bloodline().turningTicksLeft();
        if (ticks > 0) {
            // the clock large, as the Blood Moon's grey moon has it
            String clock = StageGhoulPage.clock(ticks);
            var pose = f.g.pose();
            pose.pushPose();
            pose.translate(r.x() + (r.w() - f.font().width(clock) * 2.5f) / 2f, y + 10, 0);
            pose.scale(2.5f, 2.5f, 1f);
            f.g.drawString(f.font(), clock, 0, 0, t.accent(), false);
            pose.popPose();
            y += 38;
            y = PageKit.wrapped(f, Component.translatable(PageKit.HUB + "ghoul.left"), r.x(), y, r.w(), t.muted());
        }
        return y;
    }

    private static void next(HubScreen.Frame f, boolean book) {
        HubView v = f.view;
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.SIDE);
        // the bookmarks hang in the right page's header band (the margin note is on the left page), so they never touch it
        LadderTabs.render(f, r.x(), r.y() - LadderTabs.BAND, r.w());
        int y = f.begin(HubLayout.SIDE);
        if (v.ghoul()) {
            f.end(turning(f, r, y));
            return;
        }
        boolean any = false;
        for (HubModel.LadderView ladder : LadderTabs.shown(f)) {
            if (ladder.atTop()) {
                continue;
            }
            any = true;
            HubModel.RungRow active = ladder.active().orElseThrow();
            f.g.drawString(f.font(), Component.translatable(KEY + "to_become"), r.x(), y, t.muted(), false);
            y = PageKit.title(f, PageKit.rung(active), r.x(), y + PageKit.LINE, r.w(), t.accent());
            t.drawRule(f.g, r.x(), y + 2, r.w());
            y += 6;
            if (book && ladder == LadderTabs.shown(f).get(0)) {
                int size = Math.min(r.w(), 78);
                f.emblem(new Rect(r.x() + (r.w() - size) / 2, y, size, size));
                y += size + 4;
            }
            for (HubModel.TaskRow task : ladder.tasks()) {
                y = PageKit.taskRow(f, task, r.x(), y, r.w(), true, null, false);
            }
            y = PageKit.wrapped(f, book ? Component.literal("* ").append(reward(active)) : reward(active), r.x(), y + 2, r.w(), t.muted()) + 6;
            if (book) {
                Optional<HubModel.TaskRow> trackable = trackable(ladder);
                BookKit.trackLink(f, "panel:track:" + ladder.race(), trackable.map(row -> row.task().id()).orElse(""), trackable.isEmpty(), r.x(), y);
                y += 14;
            }
        }
        if (!any) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "complete"), r.x(), y, r.w(), t.ink());
        }
        f.end(y);
    }
}
