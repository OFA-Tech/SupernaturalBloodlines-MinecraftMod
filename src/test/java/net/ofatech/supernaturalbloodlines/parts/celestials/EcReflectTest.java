package net.ofatech.supernaturalbloodlines.parts.celestials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.corgitaco.enhancedcelestials2core.EnhancedCelestials;
import dev.corgitaco.enhancedcelestials2core.api.EnhancedCelestialsEvents;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarEvent;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarForecast;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The reflection against stand-in classes that have the names and signatures javap shows in Enhanced Celestials 2 core 2.0.3.4
 * (the stubs are in src/test, under the same package names). It proves the lookups, the call order of the listener (level, previous, new), and the
 * fail-soft; it cannot prove the real mod, and {@code start} needs a ServerLevel, so it is covered by the game tests' fake only.
 */
class EcReflectTest {
    private static final ResourceKey<Registry<LunarEvent>> KEY = ResourceKey.createRegistryKey(ResourceLocation.parse("enhancedcelestials2core:lunar/event"));
    private static MappedRegistry<LunarEvent> REGISTRY = new MappedRegistry<>(KEY, Lifecycle.stable());
    private final LunarForecast forecast = new LunarForecast();

    private static Holder<LunarEvent> holder(String ns, String path, int color) {
        LunarEvent event = new LunarEvent();
        if (color >= 0) {
            event.nameColor = Optional.of(TextColor.fromRgb(color));
        }
        return Registry.registerForHolder(REGISTRY, ResourceLocation.fromNamespaceAndPath(ns, path), event);
    }

    @BeforeEach
    void install() {
        REGISTRY = new MappedRegistry<>(KEY, Lifecycle.stable()); // a fresh registry: an id can only be registered once
        EnhancedCelestials.forecast = forecast;
    }

    @AfterEach
    void clear() {
        EnhancedCelestials.forecast = null;
    }

    @Test
    void anEventThatIsNotRunningIsNotActive() throws Exception {
        EcReflect ec = EcReflect.create();
        forecast.current = holder("enhancedcelestials2core", "default", -1);
        assertTrue(ec.active(null).isEmpty());
        EnhancedCelestials.forecast = null;
        assertTrue(ec.active(null).isEmpty(), "no forecast on this level");
    }

    @Test
    void theRunningEventHasItsIdAndColour() throws Exception {
        EcReflect ec = EcReflect.create();
        forecast.active = true;
        forecast.current = holder("enhancedcelestials2defaultlunarevents", "blood_moon", 0xFF5555);
        EcAdapter.Event e = ec.active(null).orElseThrow();
        assertEquals(ResourceLocation.parse("enhancedcelestials2defaultlunarevents:blood_moon"), e.id());
        assertEquals(0xFF5555, e.color());
        assertEquals("lunar.event.enhancedcelestials2defaultlunarevents.blood_moon", e.nameKey());
        forecast.current = holder("x", "no_colour", -1);
        assertEquals(-1, ec.active(null).orElseThrow().color());
    }

    @Test
    void theScheduleIsByDaysFromTodayAndCutAtTheHorizon() throws Exception {
        EcReflect ec = EcReflect.create();
        forecast.upcoming.put(3L, holder("a", "one", 1));
        forecast.upcoming.put(0L, holder("a", "tonight", 2));
        forecast.upcoming.put(40L, holder("a", "far", 3));
        var two = ec.upcoming(null, 6);
        assertEquals(List.of(0, 3), new ArrayList<>(two.keySet()));
        assertEquals("tonight", two.get(0).id().getPath());
        assertEquals(3, ec.upcoming(null, 100).size());
    }

    @Test
    void stopEndsOnlyOurOwnEventToday() throws Exception {
        EcReflect ec = EcReflect.create();
        ResourceLocation ours = ResourceLocation.parse("supernaturalbloodlines:lunar_eclipse");
        forecast.active = true;
        forecast.current = holder("enhancedcelestials2defaultlunarevents", "blood_moon", -1);
        assertFalse(ec.stop(null, ours), "ended somebody else's event");
        assertTrue(forecast.active);
        forecast.current = holder("supernaturalbloodlines", "lunar_eclipse", -1);
        assertTrue(ec.stop(null, ours));
        assertEquals(0, forecast.removed, "today's event is the one removed");
        assertFalse(ec.stop(null, ours), "nothing is running any more");
    }

    @Test
    void theListenerGetsLevelPreviousNewInThatOrder() throws Exception {
        EcReflect ec = EcReflect.create();
        List<String> seen = new ArrayList<>();
        ec.onChanged((level, previous, next) -> seen.add(previous + " -> " + next));
        EnhancedCelestialsEvents.fireLunarEventChanged(null, holder("a", "before", -1), holder("a", "after", -1));
        assertEquals(List.of("a:before -> a:after"), seen);
    }

}
