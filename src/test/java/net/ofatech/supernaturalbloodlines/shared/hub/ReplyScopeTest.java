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
    void theDebugRowsAreExactlyTheAdminActionsOfTheCommandTable() {
        for (var r : net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands.ALL) {
            assertTrue(ReplyScope.isDebug(r.id()) == (r.ui() == net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands.Ui.ADMIN_ACTION), r.id());
        }
        assertFalse(ReplyScope.isDebug("nonsense"));
        // an id that merely looks like a debug row (the old "starts with D" rule) but is not in the table is not one
        assertFalse(ReplyScope.isDebug("D99"));
        assertFalse(ReplyScope.isDebug("B27"));
        assertTrue(ReplyScope.shownOn("D99", HubPage.HOME));
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

