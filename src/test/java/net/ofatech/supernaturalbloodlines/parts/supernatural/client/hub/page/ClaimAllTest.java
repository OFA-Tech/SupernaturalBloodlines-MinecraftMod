package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import org.junit.jupiter.api.Test;

class ClaimAllTest {
    private static HubModel.TaskRow row(String id, TaskState state) {
        TaskSpec spec = new TaskSpec(id, null, Map.of(), 1, null, null, false, List.of());
        return new HubModel.TaskRow(SupernaturalRace.HUMAN, spec, state, 0, 1, false, "species", new HubModel.TaskText(id, id, List.of(), null, null), List.of());
    }

    private static HubModel.LadderView ladder(HubModel.TaskRow... rows) {
        return new HubModel.LadderView(SupernaturalRace.HUMAN, List.of(), Optional.empty(), List.of(rows));
    }

    @Test
    void onlyClaimableTasksAreClaimedAndEachOnce() {
        var a = ladder(row("one", TaskState.CLAIMABLE), row("open", TaskState.OPEN), row("done", TaskState.CLAIMED), row("frozen", TaskState.FROZEN));
        var b = ladder(row("two", TaskState.CLAIMABLE), row("one", TaskState.CLAIMABLE));
        assertEquals(List.of("one", "two"), ClaimAll.ids(List.of(a, b)));
        assertEquals(List.of(), ClaimAll.ids(List.of(ladder(row("open", TaskState.OPEN)))));
    }
}

