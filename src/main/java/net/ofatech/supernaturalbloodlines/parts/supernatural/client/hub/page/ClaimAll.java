package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;

/** The tasks Home's "Claim all" presses: every claimable task of every ladder, once. Each goes out as the single claim does, and the server checks each again. */
final class ClaimAll {
    private ClaimAll() {}

    /** How long the button stays shut after a press when the server never answers. */
    static final long TIMEOUT_MS = 5000;

    /**
     * Whether a press is still waiting for the server: some task that was sent is still claimable in what the client has, and it was sent a moment ago.
     * Until then the button stays shut, so one press cannot be sent twice.
     */
    static boolean waiting(Set<String> sent, long sentAt, List<String> claimableNow, long now) {
        return !sent.isEmpty() && now - sentAt < TIMEOUT_MS && claimableNow.stream().anyMatch(sent::contains);
    }

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

