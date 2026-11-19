package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EclipseSignalTest {
    private List<EclipseSignal.Listener> production;

    /** The listeners the running game has (the Celestials part's own, when it is on) are put back after each test, so a test never leaves the game without them. */
    @BeforeEach
    void save() {
        production = EclipseSignal.snapshot();
    }

    @AfterEach
    void restore() {
        EclipseSignal.restore(production);
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthersOrTheEclipse() {
        EclipseSignal.clear();
        List<String> seen = new ArrayList<>();
        EclipseSignal.fire(null, true);
        EclipseSignal.add((server, on) -> {
            throw new IllegalStateException("EC changed");
        });
        EclipseSignal.add((server, on) -> seen.add(on ? "start" : "end"));
        EclipseSignal.fire(null, true);
        EclipseSignal.fire(null, false);
        assertEquals(List.of("start", "end"), seen);
    }

    @Test
    void whatWasInstalledBeforeATestIsBackAfterIt() {
        EclipseSignal.Listener installed = (server, on) -> {
        };
        EclipseSignal.add(installed);
        List<EclipseSignal.Listener> saved = EclipseSignal.snapshot();
        EclipseSignal.clear();
        assertEquals(0, EclipseSignal.snapshot().size());
        EclipseSignal.restore(saved);
        assertTrue(EclipseSignal.snapshot().contains(installed), "the listener did not come back");
    }
}
