package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubView;

/** Quests: every active task (both ladders) and the open skill quests on the left; the chosen task in full, with its claim stamp, on the right. */
public final class QuestsPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "quests.";

    @Override
    public void render(HubScreen.Frame f) {
        HubView v = f.view;
        HubTheme t = f.theme();
        List<HubModel.TaskRow> all = new ArrayList<>();
        v.ladders().forEach(l -> all.addAll(l.tasks()));
        Optional<HubModel.TaskRow> chosen = HubModel.pick(all, f.selectedTask());

        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        for (HubModel.LadderView ladder : v.ladders()) {
            if (ladder.atTop()) {
                continue;
            }
            Component head = PageKit.race(ladder.race()).copy().append(" \u00b7 ").append(PageKit.rung(ladder.active().orElseThrow()));
            y = PageKit.wrapped(f, head.copy().withStyle(ChatFormatting.BOLD), r.x(), y, r.w(), t.accent()) + 1;
            for (HubModel.TaskRow row : ladder.tasks()) {
                String id = row.task().id();
                boolean sel = chosen.map(c -> c.task().id().equals(id)).orElse(false);
                y = PageKit.taskRow(f, row, r.x(), y, r.w(), false, () -> f.select(id), sel);
            }
            y += 3;
        }
        if (all.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted()) + 4;
        }
        List<HubModel.QuestView> quests = HubModel.quests(v.skills(), v.state(), v.races());
        if (!quests.isEmpty()) {
            t.drawRule(f.g, r.x(), y, r.w());
            y = PageKit.wrapped(f, Component.translatable(KEY + "skill_quests").withStyle(ChatFormatting.BOLD), r.x(), y + 4, r.w(), t.accent()) + 1;
            for (HubModel.QuestView q : quests) {
                y = PageKit.wrapped(f, Component.translatableWithFallback(PageKit.HUB + "quest." + q.id(), q.name()), r.x(), y, r.w(), t.ink());
                for (HubModel.TaskRow row : q.tasks()) {
                    y = PageKit.taskRow(f, row, r.x() + 4, y, r.w() - 4, false, null, false);
                }
            }
        }
        f.end(y);

        detail(f, chosen);
    }

    private static void detail(HubScreen.Frame f, Optional<HubModel.TaskRow> chosen) {
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.SIDE);
        int y = f.begin(HubLayout.SIDE);
        if (chosen.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "choose"), r.x(), y, r.w(), t.muted());
            f.end(y);
            return;
        }
        HubModel.TaskRow row = chosen.get();
        f.g.drawString(f.font(), PageKit.sourceName(row.source()), r.x(), y, t.sourceColor(row.source()), false);
        y = PageKit.wrapped(f, PageKit.text(row.text()), r.x(), y + PageKit.LINE + 2, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        if (row.counted() && row.target() > 1) {
            t.drawBar(f.g, new Rect(r.x(), y, r.w(), 6), row.progress() / (float) row.target());
            y = PageKit.wrapped(f, Component.translatable(PageKit.HUB + "task.progress", row.progress(), row.target()), r.x(), y + 8, r.w(), t.ink());
        }
        for (HubModel.TaskRow m : row.members()) {
            y = PageKit.taskRow(f, m, r.x(), y, r.w(), false, null, false);
        }
        String hint = row.text().hint();
        if (hint != null && I18n.exists(hint)) {
            y = PageKit.wrapped(f, Component.translatable(hint), r.x(), y + 2, r.w(), t.muted());
        }
        y = RiteTaskLink.add(f, row, r.x(), y, r.w());
        y = PageKit.wrapped(f, Component.translatable(PageKit.HUB + "task.state." + row.state().name().toLowerCase(java.util.Locale.ROOT)),
                r.x(), y + 4, r.w(), row.state() == TaskState.CLAIMABLE ? t.good() : t.muted());
        if (row.state() == TaskState.CLAIMABLE) {
            Rect s = new Rect(r.x() + (r.w() - HubTheme.STAMP) / 2, y + 6, HubTheme.STAMP, HubTheme.STAMP);
            String id = row.task().id();
            t.drawStamp(f.g, s.x(), s.y(), f.hovered(s) || f.focused("claim:" + id));
            f.target("claim:" + id, s, List.of(Component.translatable(PageKit.HUB + "task.claim"), PageKit.text(row.text())), () -> f.claim(id));
            if (f.focused("claim:" + id)) {
                t.drawFocus(f.g, s);
            }
            y = PageKit.centered(f, Component.translatable(PageKit.HUB + "task.claim"), r, s.bottom() + 3, t.accent());
        }
        f.end(y);
    }
}
