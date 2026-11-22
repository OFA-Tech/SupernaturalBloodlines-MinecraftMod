package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import org.junit.jupiter.api.Test;

class TaskCardTest {
    private static final int GOOD = 0xFF2F6E4A;
    private static final int ACCENT = 0xFF7A1218;

    @Test
    void aCardIsLitGreenWhenItCanBeClaimedTheAccentWhenChosenAndQuietOtherwise() {
        assertEquals(GOOD, TaskCard.edge(TaskState.CLAIMABLE, false, GOOD, ACCENT));
        assertEquals(GOOD, TaskCard.edge(TaskState.CLAIMABLE, true, GOOD, ACCENT), "claimable wins over chosen");
        assertEquals(ACCENT, TaskCard.edge(TaskState.OPEN, true, GOOD, ACCENT));
        assertEquals(0, TaskCard.edge(TaskState.OPEN, false, GOOD, ACCENT), "no line on a quiet card");
        assertEquals(0, TaskCard.edge(TaskState.CLAIMED, false, GOOD, ACCENT));
    }
}
