package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EclipseSignalTest {
    /** One test, because the listeners are static and there is no way back. */
    @Test
    void nobodyAnnouncesWithoutAListenerAndABrokenOneDoesNotStopTheOthers() {
        assertFalse(EclipseSignal.fire(null, true), "no listener: the eclipse part sends its own notice");
        List<String> seen = new ArrayList<>();
        EclipseSignal.add((server, on) -> {
            throw new IllegalStateException("broken listener");
        });
        assertFalse(EclipseSignal.fire(null, true), "a broken listener announced nothing");
        EclipseSignal.add((server, on) -> {
            seen.add("on=" + on);
            return on;
        });
        assertTrue(EclipseSignal.fire(null, true), "the second listener announced the start");
        assertFalse(EclipseSignal.fire(null, false), "it did not announce the end");
        assertEquals(List.of("on=true", "on=false"), seen);
    }
}
