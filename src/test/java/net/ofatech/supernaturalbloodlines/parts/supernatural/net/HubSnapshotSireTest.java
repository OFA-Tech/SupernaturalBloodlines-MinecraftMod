package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

class HubSnapshotSireTest {
    private static HubSnapshotPayload withSire(Optional<String> sire) {
        return new HubSnapshotPayload(new HubSnapshotPayload.Family(sire, false, List.of(), List.of(), List.of(), HubSocial.Origin.NONE), List.of(), 0, Map.of(), List.of());
    }

    @Test
    void aSireTheClientMayNotBeToldIsAnEmptyNameAndStillASire() {
        assertTrue(withSire(Optional.of("")).hasSire());
        assertTrue(withSire(Optional.of("Klaus")).hasSire());
        assertFalse(withSire(Optional.empty()).hasSire());
    }
}
