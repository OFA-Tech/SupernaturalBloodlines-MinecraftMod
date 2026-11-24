package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoneFailuresTest {
    @Test
    void aFailedActionIsLoggedOncePerActionAndNamesIt() {
        List<String> warnings = new ArrayList<>();
        StoneFailures f = new StoneFailures(warnings::add, m -> { });
        f.failed("his construct screen", new IllegalStateException("gone"));
        f.failed("his construct screen", new IllegalStateException("gone"));
        f.failed("Pray", new IllegalStateException("gone"));
        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("his construct screen"));
    }

    @Test
    void aFailedReadOfHisPowersIsHandedToTheProbeEveryTime() {
        List<String> broke = new ArrayList<>();
        StoneFailures f = new StoneFailures(m -> { }, broke::add);
        f.broken("reading his wheel state", new ArrayIndexOutOfBoundsException(9));
        f.broken("reading his wheel state", new ArrayIndexOutOfBoundsException(9));
        assertEquals(2, broke.size(), "the probe keeps it to one warning; this only forwards");
        assertTrue(broke.get(0).contains("reading his wheel state"));
    }

    @Test
    void aRuntimeFailureTurnsTheProbeOffOnceAndGivesHisKeyBack() {
        List<String> warnings = new ArrayList<>();
        FakeStoneApi api = new FakeStoneApi();
        InfinityProbe probe = new InfinityProbe(() -> true, () -> true, () -> api, warnings::add);
        assertEquals(net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State.ACTIVE, probe.state());
        assertTrue(net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.wheelKeyCovered(probe.state()));
        StoneFailures failures = new StoneFailures(warnings::add, probe::broke);
        failures.broken("reading his wheel state", new IllegalStateException("x"));
        failures.broken("reading his wheel state", new IllegalStateException("x"));
        assertEquals(net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.State.OFF_BROKEN, probe.state());
        assertEquals(null, probe.api(), "the stones are plain Tensura skills again");
        assertTrue(!net.ofatech.supernaturalbloodlines.core.wheel.StoneRules.wheelKeyCovered(probe.state()), "KeyCover gives his key back on the next tick");
        assertTrue(!probe.soulControlCovered());
        assertEquals(1, warnings.size(), "one warning: " + warnings);
        assertTrue(warnings.get(0).startsWith("Infinity Stones compat off: reading his wheel state"));
    }
}