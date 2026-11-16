package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import org.junit.jupiter.api.Test;

class HubProgressTest {
    private static HubModel.RungRow rung(int i, RungStatus s, int claimed, int tasks) {
        return new HubModel.RungRow("r" + i, "R" + i, i, s, claimed, tasks, 0, 0);
    }

    private static HubModel.LadderView ladder(HubModel.RungRow... rows) {
        return new HubModel.LadderView(SupernaturalRace.HYBRID, List.of(rows), Optional.empty(), List.of());
    }

    @Test
    void theEmblemRingFillsWithTheRungsReachedAndTheActiveRungsClaims() {
        assertEquals(0f, HubModel.progress(ladder(rung(0, RungStatus.REACHED, 0, 0), rung(1, RungStatus.CURRENT, 0, 2), rung(2, RungStatus.LOCKED, 0, 2))));
        assertEquals(0.25f, HubModel.progress(ladder(rung(0, RungStatus.REACHED, 0, 0), rung(1, RungStatus.CURRENT, 1, 2), rung(2, RungStatus.LOCKED, 0, 2))));
        assertEquals(0.5f, HubModel.progress(ladder(rung(0, RungStatus.REACHED, 0, 0), rung(1, RungStatus.REACHED, 2, 2), rung(2, RungStatus.LOCKED, 0, 2))));
        assertEquals(1f, HubModel.progress(ladder(rung(0, RungStatus.REACHED, 0, 0), rung(1, RungStatus.REACHED, 2, 2), rung(2, RungStatus.REACHED, 2, 2))));
    }

    @Test
    void aLadderWithOneRungDoesNotDivideByZero() {
        assertEquals(0f, HubModel.progress(ladder(rung(0, RungStatus.REACHED, 0, 0))));
    }
}
