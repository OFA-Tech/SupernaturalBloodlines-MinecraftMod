package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import org.junit.jupiter.api.Test;

/** A vote answer names the vote it answers (one vote per kind is open in a coven): the rows take the kind. */
class CovenVoteRowsTest {
    @Test
    void theVoteRowsTakeTheKindOfTheirVote() {
        for (String id : new String[] {"C9", "C10"}) {
            AdminCommands.Row row = AdminCommands.byId(id).orElseThrow();
            assertEquals(1, row.args().size(), id + " must take exactly the kind");
            assertEquals("kind", row.args().get(0).name());
            for (VoteKind kind : VoteKind.values()) {
                Optional<String> command = AdminCommands.command(row, java.util.List.of(kind.id()));
                assertTrue(command.isPresent(), id + " refused " + kind.id());
                assertTrue(command.get().endsWith(" " + kind.id()), command.get());
            }
            assertTrue(AdminCommands.command(row, java.util.List.of()).isEmpty(), id + " ran without a kind");
            assertTrue(AdminCommands.command(row, java.util.List.of("shun now")).isEmpty(), id + " took a space in the kind");
        }
        assertEquals("group vote yes <kind>", AdminCommands.byId("C9").orElseThrow().form());
        assertEquals("group vote no <kind>", AdminCommands.byId("C10").orElseThrow().form());
    }
}