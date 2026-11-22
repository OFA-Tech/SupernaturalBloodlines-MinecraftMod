package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

class HubSnapshotIdsTest {
    private static final UUID SIRE = new UUID(1, 1);
    private static final UUID MEMBER = new UUID(2, 2);

    @Test
    void theIndexIsBuiltOnceAndThePeopleOutrankTheSameNameInThePack() {
        var snap = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE,
                List.of(new HubSocial.Member("Klaus", "beta", true, Optional.of(MEMBER)), new HubSocial.Member("Derek", "beta", true, Optional.of(MEMBER)), new HubSocial.Member("Nobody", "beta", true, Optional.empty())),
                0, Map.of(), List.of(new HubSocial.Person("Klaus", SIRE)));
        assertEquals(Optional.of(SIRE), snap.idOf("Klaus"));
        assertEquals(Optional.of(MEMBER), snap.idOf("Derek"));
        assertTrue(snap.idOf("Nobody").isEmpty(), "a member with no id is not clickable");
        assertTrue(snap.idOf("Unknown").isEmpty());
        assertTrue(snap.ids() == snap.ids(), "the same map every time");
    }

    @Test
    void theIdsAreNotPartOfTheWireFormat() {
        var snap = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, Map.of(), List.of(new HubSocial.Person("A", SIRE)));
        assertEquals(snap, new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, Map.of(), List.of(new HubSocial.Person("A", SIRE))));
        assertEquals(Map.of("A", SIRE), snap.ids());
    }
}

