package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EclipseSignalTest {
    @AfterEach
    void clear() {
        EclipseSignal.clear();
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthersOrTheEclipse() {
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
}
