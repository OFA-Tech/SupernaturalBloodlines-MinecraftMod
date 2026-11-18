package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReplyScopeTest {
    @Test
    void anOpsDebugReplyIsDrawnOnTheAdminPageAndNowhereElse() {
        for (String row : new String[] {"D1", "D4", "D8", "B2", "B7"}) {
            assertTrue(ReplyScope.shownOn(row, HubPage.ADMIN), row);
            for (HubPage p : HubPage.values()) {
                if (p != HubPage.ADMIN) {
                    assertFalse(ReplyScope.shownOn(row, p), row + " on " + p);
                }
            }
        }
    }

    @Test
    void aPlayersOwnCommandsAnswerWhereTheyWereRun() {
        for (String row : new String[] {"B3", "B4", "P1", "P3", "P8", "P15"}) {
            for (HubPage p : HubPage.values()) {
                assertTrue(ReplyScope.shownOn(row, p), row + " on " + p);
            }
        }
    }
}

