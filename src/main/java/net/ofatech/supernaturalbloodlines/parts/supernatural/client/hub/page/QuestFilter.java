package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;

/**
 * What the Quests page of every theme shares: the source filter (All, then one per mod), "hide finished", which tasks that leaves, the one chosen, and the line
 * that explains a task. The Grimoire draws the result as a book page, the Blood Moon and Tensura as cards; neither decides what is shown.
 */
final class QuestFilter {
    static final List<SourceTag> FILTERS = List.of(SourceTag.SB, SourceTag.VMP, SourceTag.WW, SourceTag.ENC, SourceTag.TEN);
    private static final String BOOK = PageKit.HUB + "book.quests.";

    private int filter;
    private boolean hideDone;

    int filter() {
        return filter;
    }

    void filter(int index) {
        filter = index;
    }

    boolean hideDone() {
        return hideDone;
    }

    void toggleHideDone() {
        hideDone = !hideDone;
    }

    /** "Show finished" while they are hidden, else "Hide finished". */
    Component hideLabel() {
        return Component.translatable(BOOK + (hideDone ? "show" : "hide"));
    }

    /** The labels of the filter row: All first, then each mod's. */
    List<String> labels() {
        List<String> labels = new ArrayList<>(List.of(Component.translatable(BOOK + "all").getString()));
        FILTERS.forEach(s -> labels.add(s.label()));
        return labels;
    }

    boolean shown(HubModel.TaskRow row) {
        if (hideDone && row.state() == TaskState.CLAIMED) {
            return false;
        }
        return filter == 0 || SourceTag.of(row.source()) == FILTERS.get(filter - 1);
    }

    /** The rows of {@code ladder} that pass the filter. */
    List<HubModel.TaskRow> rows(HubModel.LadderView ladder) {
        return ladder.tasks().stream().filter(this::shown).toList();
    }

    /** Every task of every ladder that passes the filter. */
    List<HubModel.TaskRow> all(HubScreen.Frame f) {
        List<HubModel.TaskRow> all = new ArrayList<>();
        f.view.ladders().forEach(l -> all.addAll(rows(l)));
        return all;
    }

    /** The tasks of the "Other" skill quests that pass the filter: only the All filter lists them. */
    List<HubModel.TaskRow> skillQuestRows(HubScreen.Frame f) {
        List<HubModel.TaskRow> rows = new ArrayList<>();
        if (filter == 0) {
            for (HubModel.QuestView q : HubModel.quests(f.view.skills(), f.view.state(), f.view.races())) {
                q.tasks().stream().filter(this::shown).forEach(rows::add);
            }
        }
        return rows;
    }

    /**
     * The chosen task: the one the player picked if it is still shown, else the first. The skill quests listed under "Other" can be picked too, so they are among
     * the candidates (after the ladder tasks).
     */
    Optional<HubModel.TaskRow> chosen(HubScreen.Frame f, List<HubModel.TaskRow> all) {
        return choose(all, skillQuestRows(f), f.selectedTask());
    }

    /** Pure: the picked task among the ladder tasks and the skill-quest tasks, else the first of the ladder tasks, else the first skill-quest task. */
    static Optional<HubModel.TaskRow> choose(List<HubModel.TaskRow> ladder, List<HubModel.TaskRow> skill, String picked) {
        List<HubModel.TaskRow> candidates = new ArrayList<>(ladder);
        candidates.addAll(skill);
        return HubModel.pick(candidates, picked);
    }

    static boolean isChosen(Optional<HubModel.TaskRow> chosen, String id) {
        return chosen.map(c -> c.task().id().equals(id)).orElse(false);
    }

    /** The text that tells what a task asks: its own hint when the language has one, else its state. */
    static Component hintBody(HubModel.TaskRow row) {
        String hint = row.text().hint();
        return hint != null && I18n.exists(hint) ? Component.translatable(hint)
                : Component.translatable(PageKit.HUB + "task.state." + row.state().name().toLowerCase(java.util.Locale.ROOT));
    }
}
