package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProbesTest {
    @Test
    void theNoSwitchProbeIsKnownAndAlwaysOpen() {
        Optional<AddonProbe> probe = Probes.of(Probes.UNIQUE_NO_SWITCH);
        assertTrue(probe.isPresent(), "a Unique whose addon has no on/off switch needs a probe that asks nothing");
        assertEquals(Optional.empty(), probe.get().missingHook());
        assertTrue(probe.get().roadOpen());
        assertEquals("unique.no_switch", Probes.UNIQUE_NO_SWITCH);
    }

    @Test
    void anUnknownIdIsStillNoProbe() {
        assertEquals(Optional.empty(), Probes.of("unique.no_such_thing"));
        assertEquals(Optional.empty(), Probes.of(""));
        assertTrue(Probes.of(Probes.OWN_SAGE_GREAT_SAGE).isPresent());
    }
}
