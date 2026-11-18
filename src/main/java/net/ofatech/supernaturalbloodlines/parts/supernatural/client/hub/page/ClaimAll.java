package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;

/** The tasks Home's "Claim all" presses: every claimable task of every ladder, once. Each goes out as the single claim does, and the server checks each again. */
final class ClaimAll {
    private ClaimAll() {}

    static List<String> ids(List<HubModel.LadderView> ladders) {
        Set<String> out = new LinkedHashSet<>();
        for (HubModel.LadderView l : ladders) {
            for (HubModel.TaskRow r : l.tasks()) {
                if (r.state() == TaskState.CLAIMABLE) {
                    out.add(r.task().id());
                }
            }
        }
        return List.copyOf(out);
    }
}

