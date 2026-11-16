package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * The Grimoire's Quests: a filter row (All and one per mod), the tasks grouped under the rung they belong to, then the skill quests, on the left;
 * the chosen task on the facing page with its source, a drop-cap line, its progress, its reward by a leader, the secondary links and the CLAIM seal.
 */
public final class BookQuestsPage implements HubPageView {
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

        Rect r = f.region(BookKit.MAIN);
        int y = BookKit.headNoRule(f, BookKit.MAIN, Component.translatable(KEY + "title"), null);
        List<String> labels = new ArrayList<>(List.of(Component.translatable(BOOK + "all").getString()));
        FILTERS.forEach(s -> labels.add(s.label()));
        y = BookKit.filterRow(f, labels, filter, r.x(), y + 1, r.w(), i -> filter = i);
        t.drawRule(f.g, r.x(), y, r.w());
        y += 6;
        for (HubModel.LadderView ladder : f.view.ladders()) {
            if (ladder.atTop()) {
                continue;
            }
            List<HubModel.TaskRow> rows = ladder.tasks().stream().filter(this::shown).toList();
            if (rows.isEmpty()) {
                continue;
            }
            y = BookKit.caps(f, Component.translatable(BOOK + "next_step", PageKit.rung(ladder.active().orElseThrow())), r.x(), y, t.muted());
            for (HubModel.TaskRow row : rows) {
                String id = row.task().id();
                boolean sel = chosen.map(c -> c.task().id().equals(id)).orElse(false);
                y = PageKit.taskRow(f, row, r.x(), y, r.w(), false, () -> f.select(id), sel);
            }
            y += 3;
        }
        if (all.isEmpty()) {
            y = EmptyState.render(f, r, y + 10, "quests", HubPage.PATH);
        }
        List<HubModel.QuestView> quests = HubModel.quests(f.view.skills(), f.view.state(), f.view.races());
        if (!quests.isEmpty() && filter == 0) {
            y = BookKit.caps(f, Component.translatable(BOOK + "other"), r.x(), y + 2, t.muted());
            for (HubModel.QuestView q : quests) {
                y = PageKit.wrapped(f, Component.translatableWithFallback(PageKit.HUB + "quest." + q.id(), q.name()), r.x(), y, r.w(), t.ink());
                for (HubModel.TaskRow row : q.tasks()) {
                    if (shown(row)) {
                        y = PageKit.taskRow(f, row, r.x() + 4, y, r.w() - 4, false, null, false);
                    }
                }
            }
        }
        f.end(y);
        detail(f, chosen);
    }

    private void detail(HubScreen.Frame f, Optional<HubModel.TaskRow> chosen) {
        HubTheme t = f.theme();
        Rect s = f.region(BookKit.SIDE);
        if (chosen.isPresent()) {
            f.reserve(BookKit.SIDE, BookKit.SEAL + 8); // the CLAIM seal is pinned at the foot of the page
        }
        int y = f.begin(BookKit.SIDE);
        if (chosen.isEmpty()) {
            f.end(PageKit.wrapped(f, Component.translatable(KEY + "choose"), s.x(), y, s.w(), t.muted()));
            return;
        }
        HubModel.TaskRow row = chosen.get();
        y = PageKit.title(f, PageKit.text(row.text()), s.x(), y, s.w(), t.accent());
        SourceTag tag = SourceTag.of(row.source());
        int tw = KitDraw.sourceTag(f.g, f.font(), t, tag, s.x(), y);
        HubModel.LadderView owner = f.view.ladders().stream().filter(l -> l.tasks().stream().anyMatch(x -> x.task().id().equals(row.task().id()))).findFirst().orElse(null);
        if (owner != null && owner.active().isPresent()) {
            BookKit.small(f, Component.translatable(BOOK + "for_step", PageKit.rung(owner.active().get())), s.x() + tw + 4, y + 2, s.w() - tw - 4, t.muted());
        }
        t.drawRule(f.g, s.x(), y + 13, s.w());
        y += 19;
        String hint = row.text().hint();
        y = hint != null && I18n.exists(hint) ? BookKit.dropCap(f, Component.translatable(hint), s.x(), y, s.w(), t.ink())
                : PageKit.wrapped(f, Component.translatable(PageKit.HUB + "task.state." + row.state().name().toLowerCase(java.util.Locale.ROOT)), s.x(), y, s.w(), t.ink()) + 3;
        y = BookKit.caps(f, Component.translatable(BOOK + "progress"), s.x(), y + 4, t.muted());
        if (row.counted() && row.target() > 1) {
            if (row.target() <= 8) {
                for (int i = 0; i < row.target(); i++) {
                    KitDraw.shape(f.g, Shapes.Shape.ROUND, s.x() + i * 11, y, 9, t.accent());
                    KitDraw.shape(f.g, Shapes.Shape.ROUND, s.x() + i * 11 + 1, y + 1, 7, i < row.progress() ? t.accent() : t.paper());
                }
                y += 12;
            } else {
                t.drawBar(f.g, new Rect(s.x(), y, s.w(), 6), row.progress() / (float) row.target());
                y += 9;
            }
            y = PageKit.wrapped(f, Component.translatable(PageKit.HUB + "task.progress", row.progress(), row.target()), s.x(), y, s.w(), t.muted());
        }
        for (HubModel.TaskRow m : row.members()) {
            y = PageKit.taskRow(f, m, s.x(), y, s.w(), false, null, false);
        }
        y += 4;
        if (owner != null && owner.active().isPresent()) {
            String reward = PathPage.reward(owner.active().get()).getString();
            int colon = reward.indexOf(": ");
            String[] parts = (colon >= 0 ? reward.substring(colon + 2) : reward).split(", ");
            for (int i = 0; i < parts.length; i++) {
                y = BookKit.leader(f, i == 0 ? Component.translatable(BOOK + "reward") : Component.empty(), Component.literal(parts[i] + (i == parts.length - 1 ? " *" : "")), s.x(), y, s.w(), t.good());
            }
        }
        y += 6;
        int x = s.x();
        x += BookKit.link(f, "link:track", Component.translatable(PageKit.HUB + "path.track"), x, y, List.of(Component.translatable(PageKit.HUB + "path.track_soon")), null) + 12;
        BookKit.link(f, "link:hide", Component.translatable(BOOK + (hideDone ? "show" : "hide")), x, y, List.of(Component.translatable(BOOK + (hideDone ? "show" : "hide"))),
                () -> hideDone = !hideDone);
        f.end(y + 16);
        String id = row.task().id();
        boolean claimable = row.state() == TaskState.CLAIMABLE;
        int sy = s.bottom() - BookKit.SEAL;
        BookKit.seal(f, "claim:" + id, Component.translatable(BOOK + "claim"), claimable, s.right() - BookKit.SEAL, sy, () -> f.claim(id),
                List.of(Component.translatable(PageKit.HUB + "task.claim"), PageKit.text(row.text())));
        BookKit.small(f, Component.translatable(BOOK + "stamp_note"), s.x(), sy + 6, s.w() - BookKit.SEAL - 8, t.muted());
    }
}
