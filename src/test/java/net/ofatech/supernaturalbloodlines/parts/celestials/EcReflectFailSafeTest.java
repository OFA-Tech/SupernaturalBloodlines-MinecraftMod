package net.ofatech.supernaturalbloodlines.parts.celestials;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.corgitaco.enhancedcelestials2core.EnhancedCelestials;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarForecast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Review test (tester): when a member of Enhanced Celestials throws at runtime, {@link EcReflect} must surface it as an
 * IllegalStateException, which {@code LunarMoods} turns into "no mood". Stand-ins only, no real EC.
 */
class EcReflectFailSafeTest {
    @AfterEach
    void clear() {
        EnhancedCelestials.forecast = null;
    }

    @Test
    void aThrowingActiveCheckIsAnIllegalStateException() throws Exception {
        EnhancedCelestials.forecast = new LunarForecast() {
            @Override
            public boolean isEventActive() {
                throw new UnsupportedOperationException("changed in a new EC");
            }
        };
        EcReflect ec = EcReflect.create();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ec.active(null));
        assertTrue(e.getMessage().contains("isEventActive"), e.getMessage());
    }

    @Test
    void aThrowingScheduleIsAnIllegalStateExceptionToo() throws Exception {
        EnhancedCelestials.forecast = new LunarForecast() {
            @Override
            public it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap<net.minecraft.core.Holder<
                    dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarEvent>> upcomingEventsByDaysInAdvance() {
                throw new IllegalStateException("changed in a new EC");
            }
        };
        EcReflect ec = EcReflect.create();
        assertThrows(IllegalStateException.class, () -> ec.upcoming(null, 6));
    }
}
