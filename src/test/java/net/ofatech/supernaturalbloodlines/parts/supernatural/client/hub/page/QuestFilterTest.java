package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import org.junit.jupiter.api.Test;

class QuestFilterTest {
    private static HubModel.TaskRow row(String id, TaskState state) {
        TaskSpec spec = new TaskSpec(id, null, Map.of(), 1, null, null, false, List.of());
        return new HubModel.TaskRow(SupernaturalRace.HUMAN, spec, state, 0, 1, false, "species", new HubModel.TaskText(id, id, List.of(), null, null), List.of());
    }

    @Test
    void aSkillQuestCardCanBeThePickedOne() {
        HubModel.TaskRow ladder = row("ladder", TaskState.OPEN);
        HubModel.TaskRow skill = row("skill", TaskState.OPEN);
        assertEquals("skill", QuestFilter.choose(List.of(ladder), List.of(skill), "skill").orElseThrow().task().id());
    }

    @Test
    void withNothingPickedTheLadderTaskComesFirstThenTheSkillQuest() {
        HubModel.TaskRow ladder = row("ladder", TaskState.OPEN);
        HubModel.TaskRow skill = row("skill", TaskState.OPEN);
        assertEquals("ladder", QuestFilter.choose(List.of(ladder), List.of(skill), null).orElseThrow().task().id());
        assertEquals("skill", QuestFilter.choose(List.of(), List.of(skill), null).orElseThrow().task().id());
    }

    @Test
    void aClaimableSkillQuestIsChosenBeforeAnOpenLadderTask() {
        HubModel.TaskRow ladder = row("ladder", TaskState.OPEN);
        HubModel.TaskRow skill = row("skill", TaskState.CLAIMABLE);
        assertEquals("skill", QuestFilter.choose(List.of(ladder), List.of(skill), null).orElseThrow().task().id());
    }
}
