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
 * info pane) with its actions: Hide finished, Track on HUD (needs the HUD part) and Claim.
 */
public final class StageQuestsPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "quests.";
    private static final String BOOK = PageKit.HUB + "book.quests.";
    private final QuestFilter quests = new QuestFilter();

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.TaskRow> all = quests.all(f);
        Optional<HubModel.TaskRow> chosen = quests.chosen(f, all);

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
            List<HubModel.TaskRow> rows = quests.rows(ladder);
            if (rows.isEmpty()) {
                continue;
            }
            y = BookKit.caps(f, Component.translatable(BOOK + "next_step", PageKit.rung(ladder.active().orElseThrow())), x, y + 2, t.muted());
            for (HubModel.TaskRow row : rows) {
                y = card(f, row, x, y, w, chosen);
            }
        }
        List<HubModel.QuestView> skillQuests = HubModel.quests(f.view.skills(), f.view.state(), f.view.races());
        if (all.isEmpty() && skillQuests.isEmpty() && quests.filter() == 0) {
            y = EmptyState.render(f, new Rect(x, y, w, 0), y + 14, "quests", HubPage.PATH);
        } else if (all.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), x, y, w, t.muted()) + 4;
        }
        if (!skillQuests.isEmpty() && quests.filter() == 0) {
            y = BookKit.caps(f, Component.translatable(BOOK + "other"), x, y + 2, t.muted());
            for (HubModel.QuestView q : skillQuests) {
                for (HubModel.TaskRow row : q.tasks()) {
                    if (quests.shown(row)) {
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
        List<String> labels = quests.labels();
        int cx = x;
        for (int i = 0; i < labels.size(); i++) {
            int pw = SourceTag.width(f.font().width(labels.get(i)));
            int fill = i == 0 ? t.accent() : t.sourceColor(QuestFilter.FILTERS.get(i - 1).source());
            Rect pill = new Rect(cx, y, pw, 13);
            int text = BookKit.chip(f, pill, i == quests.filter(), f.hovered(pill));
            int dotY = pill.y() + (pill.h() - SourceTag.STRIPE_H) / 2;
            f.g.fill(pill.x() + SourceTag.STRIPE_X, dotY, pill.x() + SourceTag.STRIPE_X + SourceTag.STRIPE_W, dotY + SourceTag.STRIPE_H, fill);
            f.g.drawString(f.font(), labels.get(i), pill.x() + SourceTag.TEXT_X, pill.y() + 3, text, false);
            int idx = i;
            f.target("filter:" + i, pill, List.of(Component.literal(labels.get(i))), () -> quests.filter(idx));
            if (f.focused("filter:" + i)) {
                KitDraw.focusRing(f.g, pill, t.focusColor());
            }
            cx += pw + 4;
        }
        return y + 19;
    }

    private int card(HubScreen.Frame f, HubModel.TaskRow row, int x, int y, int w, Optional<HubModel.TaskRow> chosen) {
        HubTheme t = f.theme();
        boolean claimable = row.state() == TaskState.CLAIMABLE;
        Rect box = new Rect(x, y, w, 24);
        BookKit.card(f, box, TaskCard.edge(row.state(), QuestFilter.isChosen(chosen, row.task().id()), t.good(), t.accent()));
        int tw = KitDraw.sourceTag(f.g, f.font(), t, SourceTag.of(row.source()), box.x() + 5, box.y() + 7);
        float frac = TaskCard.fraction(row);
        String count = TaskCard.count(row);
        int cw = f.font().width(count);
        int barW = Math.max(30, Math.min(w / 3, 110));
        int textW = w - tw - barW - cw - 26;
        f.g.drawString(f.font(), net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(PageKit.text(row.text()).getString(), textW, f.font()::width), box.x() + tw + 10, box.y() + 8, t.ink(), false);
        t.drawBar(f.g, new Rect(box.right() - barW - cw - 12, box.y() + 10, barW, 5), frac);
        f.g.drawString(f.font(), count, box.right() - cw - 6, box.y() + 8, claimable ? t.good() : t.muted(), false);
        TaskCard.target(f, row, box);
        return y + 28;
    }

    private void detail(HubScreen.Frame f, HubModel.TaskRow row) {
        Detail.Builder d = Detail.of(PageKit.text(row.text())).body(QuestFilter.hintBody(row));
        if (row.counted() && row.target() > 1 && row.target() <= 8) {
            d.pips((int) row.progress(), (int) row.target());
        }
        d.action("hide", quests.hideLabel(), false, true, quests::toggleHideDone);
        d.action(TrackHud.action(row.task().id(), row.state() == TaskState.CLAIMED));
        String rite = net.ofatech.supernaturalbloodlines.shared.hub.RiteLinkRule.riteOf(row.task());
        if (rite != null) {
            d.action(new Detail.Action("open_book", Component.translatable(RiteTaskLink.LABEL), false, net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.RiteLinks.hasBook(),
                    net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.RiteLinks.tip(rite, null), null,
                    () -> net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.RiteLinks.open(rite)));
        }
        String id = row.task().id();
        d.action("claim", Component.translatable(BOOK + "claim"), true, row.state() == TaskState.CLAIMABLE, () -> f.claim(id));
        f.detail(d.build());
    }
}
