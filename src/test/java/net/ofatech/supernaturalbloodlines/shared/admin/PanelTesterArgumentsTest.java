package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Reviewer-written (tester panel). Checks the hub's argument gate against the forms the server will actually run. */
class PanelTesterArgumentsTest {
    private static final String STEVE = "123e4567-e89b-42d3-a456-426614174000";

    @Test
    void aNumberThatBrigadierCannotReadIsRefusedBeforeItRuns() {
        AdminCommands.Row sp = AdminCommands.byId("D7").orElseThrow();
        // Brigadier's integer reader takes '-' but not '+', so "+5" would only fail later in chat
        assertTrue(AdminCommands.command(sp, List.of(STEVE, "+5")).isEmpty(), "'+5' was sent to the server");
    }

    @Test
    void aSignedNumberInRangeIsStillAccepted() {
        AdminCommands.Row sp = AdminCommands.byId("D7").orElseThrow();
        assertTrue(AdminCommands.command(sp, List.of(STEVE, "-5")).isPresent(), "'-5' must stay allowed");
    }
}
