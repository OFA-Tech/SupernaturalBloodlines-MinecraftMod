package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminHandoffTest {
    @Test
    void aPersonOfferedByAProfileIsTakenByTheAdminPageOnceWithTheirIdAndName() {
        AdminHandoff.take();
        UUID id = new UUID(4, 2);
        AdminHandoff.offer(id, "Derek");
        assertEquals(Optional.of(new AdminHandoff.Person(id, "Derek")), AdminHandoff.take());
        assertTrue(AdminHandoff.take().isEmpty(), "taken twice");
    }
}

