package net.ofatech.supernaturalbloodlines.parts.celestials;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced Celestials 2 by reflection: the mod is All Rights Reserved and has no public API artifact, so nothing of it is a build
 * dependency. Everything is resolved once in {@link #create}; a member that is missing (the API is young) makes {@code create} throw and
 * the part logs one warning and stays without a mood. The names and signatures were read from core 2.0.3.4 with javap. EC's change
 * listener is deliberately not used: nothing needs it, and a listener runs inside EC's own tick.
 */
final class EcReflect implements EcAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EcReflect.class);
    private static final String CORE = "dev.corgitaco.enhancedcelestials2core.";
    private static final ResourceLocation EVENT_REGISTRY = ResourceLocation.parse("enhancedcelestials2core:lunar/event");

    private final Method forecastOf;
    private final Method isEventActive;
    private final Method currentHolder;
    private final Method upcomingByDays;
    private final Method setTonight;
    private final Method removeEvent;
    private final Method nameColor;
    private volatile boolean warnedMissingEvent;

    private EcReflect(Class<?> core, Class<?> forecast, Class<?> event) throws ReflectiveOperationException {
        forecastOf = core.getMethod("lunarForecastWorldData", Level.class);
        isEventActive = forecast.getMethod("isEventActive");
        currentHolder = forecast.getMethod("currentLunarEventHolder");
        upcomingByDays = forecast.getMethod("upcomingEventsByDaysInAdvance");
        setTonight = forecast.getMethod("setLunarEventTonight", Holder.class);
        removeEvent = forecast.getMethod("removeLunarEvent", long.class);
        nameColor = event.getMethod("getNameColor");
    }

    static EcReflect create() throws ReflectiveOperationException {
        ClassLoader loader = EcReflect.class.getClassLoader();
        return create(Class.forName(CORE + "EnhancedCelestials", true, loader),
                Class.forName(CORE + "api.lunarevent.LunarForecast", true, loader),
                Class.forName(CORE + "api.lunarevent.LunarEvent", true, loader));
    }

    /** Looks every member up on these three classes. Throws when one is missing, so there is no half-working adapter. */
    static EcReflect create(Class<?> core, Class<?> forecast, Class<?> event) throws ReflectiveOperationException {
        return new EcReflect(core, forecast, event);
    }

    private static <T> T call(Method m, Object on, Object... args) {
        try {
            @SuppressWarnings("unchecked")
            T result = (T) m.invoke(on, args);
            return result;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(m.getName() + " threw " + e.getCause(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(m.getName() + " cannot be called: " + e, e);
        }
    }

    private static ResourceLocation idOf(Holder<?> holder) {
        return holder.unwrapKey().map(ResourceKey::location).orElseThrow();
    }

    private Event eventOf(Holder<?> holder) {
        Optional<TextColor> color = call(nameColor, holder.value());
        return new Event(idOf(holder), color.map(TextColor::getValue).orElse(-1));
    }

    @Override
    public Optional<Event> active(Level level) {
        Optional<?> f = call(forecastOf, null, level);
        if (f.isEmpty() || !(Boolean) call(isEventActive, f.get())) {
            return Optional.empty();
        }
        return Optional.of(eventOf(call(currentHolder, f.get())));
    }

    @Override
    public Map<Integer, Event> upcoming(Level level, int days) {
        Optional<?> f = call(forecastOf, null, level);
        Map<Integer, Event> out = new TreeMap<>();
        if (f.isEmpty()) {
            return out;
        }
        Map<?, ?> byDays = call(upcomingByDays, f.get());
        for (Map.Entry<?, ?> e : byDays.entrySet()) {
            try {
                int day = ((Number) e.getKey()).intValue();
                if (day >= 0 && day < days) {
                    out.put(day, eventOf((Holder<?>) e.getValue()));
                }
            } catch (RuntimeException bad) {
                // one entry we cannot read (an event without a key) does not hide the others
                LOGGER.debug("A scheduled lunar event could not be read: {}", bad.toString());
            }
        }
        return out;
    }

    private Holder<?> holderOf(ServerLevel level, ResourceLocation id) {
        Registry<?> events = level.registryAccess().registry(ResourceKey.createRegistryKey(EVENT_REGISTRY)).orElse(null);
        return events == null ? null : events.getHolder(id).orElse(null);
    }

    @Override
    public boolean knows(ServerLevel level, ResourceLocation id) {
        return holderOf(level, id) != null;
    }

    @Override
    public boolean start(ServerLevel level, ResourceLocation id) {
        Optional<?> f = call(forecastOf, null, level);
        if (f.isEmpty() || (Boolean) call(isEventActive, f.get())) {
            return false;
        }
        Holder<?> holder = holderOf(level, id);
        if (holder == null) {
            if (!warnedMissingEvent) {
                warnedMissingEvent = true;
                LOGGER.warn("Enhanced Celestials has no lunar event {} (is this mod's datapack disabled?), so our eclipse is not shown as one", id);
            }
            return false;
        }
        call(setTonight, f.get(), holder);
        return true;
    }

    @Override
    public boolean stop(ServerLevel level, ResourceLocation id) {
        Optional<?> f = call(forecastOf, null, level);
        if (f.isEmpty() || !(Boolean) call(isEventActive, f.get()) || !eventOf(call(currentHolder, f.get())).id().equals(id)) {
            return false;
        }
        return call(removeEvent, f.get(), 0L);
    }
}
