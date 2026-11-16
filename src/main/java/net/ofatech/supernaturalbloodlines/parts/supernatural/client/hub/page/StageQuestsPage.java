package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * Quests for the Blood Moon and Tensura: coloured source pills as filters, one rounded card per task (its mod tag, the task, a progress bar and the
 * count) grouped under the rung they belong to, then the skill quests. The chosen task goes to the theme's detail (the Blood Moon's bar, Tensura's
 * info pane) with its actions: Hide finished, Track on HUD (waits for the HUD update) and Claim.
 */
public final class StageQuestsPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "quests.";
    private static final String BOOK = PageKit.HUB + "book.quests.";
    private static final List<SourceTag> FILTERS = List.of(SourceTag.SB, SourceTag.VMP, SourceTag.WW, SourceTag.ENC, SourceTag.TEN);

    private int filter;
    private boolean hideDone;

    private boolean shown(HubModel.TaskRow row) {
        if (hideDone && row.state() == TaskState.CLAIMED) {
            return false;
        }
        return filter == 0 || SourceTag.of(row.source()) == FILTERS.get(filter - 1);
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.TaskRow> all = new ArrayList<>();
        f.view.ladders().forEach(l -> all.addAll(l.tasks().stream().filter(this::shown).toList()));
        Optional<HubModel.TaskRow> chosen = HubModel.pick(all, f.selectedTask());

        BookKit.moonHead(f, Component.translatable(KEY + "title"), Component.translatable(BOOK + "subtitle"));
        Rect s = f.region(BookKit.FULL);
        int y = f.begin(BookKit.FULL);
        int w = Math.min(s.w(), 480);
        int x = s.x() + (s.w() - w) / 2;
        y = pills(f, x, y + 1, w);
        for (HubModel.LadderView ladder : f.view.ladders()) {
            if (ladder.atTop()) {
                continue;
            }
            List<HubModel.TaskRow> rows = ladder.tasks().stream().filter(this::shown).toList();
            if (rows.isEmpty()) {
                continue;
            }
            y = BookKit.caps(f, Component.translatable(BOOK + "next_step", PageKit.rung(ladder.active().orElseThrow())), x, y + 2, t.muted());
            for (HubModel.TaskRow row : rows) {
                y = card(f, row, x, y, w, chosen);
            }
        }
        List<HubModel.QuestView> quests = HubModel.quests(f.view.skills(), f.view.state(), f.view.races());
        if (all.isEmpty() && quests.isEmpty() && filter == 0) {
            y = EmptyState.render(f, new Rect(x, y, w, 0), y + 14, "quests", HubPage.PATH);
        } else if (all.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), x, y, w, t.muted()) + 4;
        }
        if (!quests.isEmpty() && filter == 0) {
            y = BookKit.caps(f, Component.translatable(BOOK + "other"), x, y + 2, t.muted());
            for (HubModel.QuestView q : quests) {
                for (HubModel.TaskRow row : q.tasks()) {
                    if (shown(row)) {
                        y = card(f, row, x, y, w, chosen);
                    }
                }
            }
        }
        f.end(y);
        chosen.ifPresent(row -> detail(f, row));
    }

    /** The source filters as coloured pills, the chosen one outlined in white. */
    private int pills(HubScreen.Frame f, int x, int y, int w) {
        HubTheme t = f.theme();
        List<String> labels = new ArrayList<>(List.of(Component.translatable(BOOK + "all").getString()));
        FILTERS.forEach(tag -> labels.add(tag.label()));
        int cx = x;
        for (int i = 0; i < labels.size(); i++) {
            int pw = f.font().width(labels.get(i)) + 14;
            int fill = i == 0 ? t.accent() : t.sourceColor(FILTERS.get(i - 1).source());
            Rect pill = new Rect(cx, y, pw, 13);
            f.g.fill(pill.x(), pill.y(), pill.right(), pill.bottom(), i == filter ? 0xFFFFFFFF : (t.paper() | 0xFF000000));
            f.g.fill(pill.x() + 1, pill.y() + 1, pill.right() - 1, pill.bottom() - 1, i == filter || f.hovered(pill) ? fill : (fill & 0xFFFFFF) | 0xAA000000);
            f.g.drawString(f.font(), labels.get(i), pill.x() + 7, pill.y() + 3, Contrast.bestText(fill), false);
            int idx = i;
            f.target("filter:" + i, pill, List.of(Component.literal(labels.get(i))), () -> filter = idx);
            if (f.focused("filter:" + i)) {
                KitDraw.focusRing(f.g, pill, t.focusColor());
            }
            cx += pw + 4;
        }
        return y + 19;
    }

    private int card(HubScreen.Frame f, HubModel.TaskRow row, int x, int y, int w, Optional<HubModel.TaskRow> chosen) {
        HubTheme t = f.theme();
        String id = row.task().id();
        boolean sel = chosen.map(c -> c.task().id().equals(id)).orElse(false);
        boolean claimable = row.state() == TaskState.CLAIMABLE;
        Rect box = new Rect(x, y, w, 24);
        f.g.fill(box.x(), box.y(), box.right(), box.bottom(), claimable ? t.good() : sel ? t.accent() : (t.muted() & 0xFFFFFF) | 0x66000000);
        f.g.fill(box.x() + 1, box.y() + 1, box.right() - 1, box.bottom() - 1, t.paper());
        int tw = KitDraw.sourceTag(f.g, f.font(), t, SourceTag.of(row.source()), box.x() + 5, box.y() + 7);
        float frac = row.counted() && row.target() > 0 ? Math.min(1f, row.progress() / (float) row.target()) : row.state() == TaskState.CLAIMED || claimable ? 1f : 0f;
        String count = row.counted() && row.target() > 1 ? row.progress() + "/" + row.target() : claimable || row.state() == TaskState.CLAIMED ? "\u2713" : "";
        int cw = f.font().width(count);
        int barW = Math.max(30, Math.min(w / 3, 110));
        int textW = w - tw - barW - cw - 26;
        f.g.drawString(f.font(), f.font().plainSubstrByWidth(PageKit.text(row.text()).getString(), textW), box.x() + tw + 10, box.y() + 8, t.ink(), false);
        t.drawBar(f.g, new Rect(box.right() - barW - cw - 12, box.y() + 10, barW, 5), frac);
        f.g.drawString(f.font(), count, box.right() - cw - 6, box.y() + 8, claimable ? t.good() : t.muted(), false);
        f.target("task:" + id, box, PageKit.tooltip(row), () -> f.select(id));
        if (f.focused("task:" + id)) {
            KitDraw.focusRing(f.g, box, t.focusColor());
        }
        return y + 28;
    }

    private void detail(HubScreen.Frame f, HubModel.TaskRow row) {
        String hint = row.text().hint();
        Component body = hint != null && I18n.exists(hint) ? Component.translatable(hint)
                : Component.translatable(PageKit.HUB + "task.state." + row.state().name().toLowerCase(java.util.Locale.ROOT));
        Detail.Builder d = Detail.of(PageKit.text(row.text())).body(body);
        if (row.counted() && row.target() > 1 && row.target() <= 8) {
            d.pips((int) row.progress(), (int) row.target());
        }
        d.action("hide", Component.translatable(BOOK + (hideDone ? "show" : "hide")), false, true, () -> hideDone = !hideDone);
        d.action(TrackHud.action(row.task().id(), row.state() == TaskState.CLAIMED));
        String id = row.task().id();
        d.action("claim", Component.translatable(BOOK + "claim"), true, row.state() == TaskState.CLAIMABLE, () -> f.claim(id));
        f.detail(d.build());
    }
}
