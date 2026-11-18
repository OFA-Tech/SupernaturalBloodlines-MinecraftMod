package net.ofatech.supernaturalbloodlines.parts.celestials;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

/**
 * Enhanced Celestials 2 by reflection: the mod is All Rights Reserved and has no public API artifact, so nothing of it is a build
 * dependency. Everything is resolved once in {@link #create}; a member that is missing (the API is young) makes {@code create} throw and
 * the part logs one warning and stays without a mood. The names and signatures were read from core 2.0.3.4 with javap.
 */
final class EcReflect implements EcAdapter {
    private static final String CORE = "dev.corgitaco.enhancedcelestials2core.";
    private static final ResourceLocation EVENT_REGISTRY = ResourceLocation.parse("enhancedcelestials2core:lunar/event");

    private final Method forecastOf;
    private final Method isEventActive;
    private final Method currentHolder;
    private final Method upcomingByDays;
    private final Method setTonight;
    private final Method removeEvent;
    private final Method nameColor;
    private final Method register;
    private final Class<?> listenerType;

    private EcReflect(Class<?> core, Class<?> forecast, Class<?> event, Class<?> events, Class<?> listener) throws ReflectiveOperationException {
        forecastOf = core.getMethod("lunarForecastWorldData", Level.class);
        isEventActive = forecast.getMethod("isEventActive");
        currentHolder = forecast.getMethod("currentLunarEventHolder");
        upcomingByDays = forecast.getMethod("upcomingEventsByDaysInAdvance");
        setTonight = forecast.getMethod("setLunarEventTonight", Holder.class);
        removeEvent = forecast.getMethod("removeLunarEvent", long.class);
        nameColor = event.getMethod("getNameColor");
        listenerType = listener;
        register = events.getMethod("registerLunarEventChanged", listener);
    }

    static EcReflect create() throws ReflectiveOperationException {
        ClassLoader loader = EcReflect.class.getClassLoader();
        return new EcReflect(Class.forName(CORE + "EnhancedCelestials", true, loader),
                Class.forName(CORE + "api.lunarevent.LunarForecast", true, loader),
                Class.forName(CORE + "api.lunarevent.LunarEvent", true, loader),
                Class.forName(CORE + "api.EnhancedCelestialsEvents", true, loader),
                Class.forName(CORE + "api.lunarevent.LunarEventChangeListener", true, loader));
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

    private Event eventOf(Holder<?> holder) {
        ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElseThrow();
        Optional<TextColor> color = call(nameColor, holder.value());
        return new Event(id, color.map(TextColor::getValue).orElse(-1));
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
            int day = ((Number) e.getKey()).intValue();
            if (day >= 0 && day < days) {
                out.put(day, eventOf((Holder<?>) e.getValue()));
            }
        }
        return out;
    }

    @Override
    public boolean start(ServerLevel level, ResourceLocation id) {
        Optional<?> f = call(forecastOf, null, level);
        if (f.isEmpty() || (Boolean) call(isEventActive, f.get())) {
            return false;
        }
        Registry<?> events = level.registryAccess().registry(ResourceKey.createRegistryKey(EVENT_REGISTRY)).orElse(null);
        Optional<? extends Holder<?>> holder = events == null ? Optional.empty() : events.getHolder(id);
        if (holder.isEmpty()) {
            return false;
        }
        call(setTonight, f.get(), holder.get());
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

    @Override
    public void onChanged(Change listener) {
        Object proxy = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] {listenerType}, (self, method, args) -> {
            if (method.getName().equals("onLunarEventChanged")) {
                // (ServerLevel, previous, new): read from LunarForecast.notifyEventSwitched, which passes the old holder first
                listener.changed((ServerLevel) args[0], idOf((Holder<?>) args[1]), idOf((Holder<?>) args[2]));
                return null;
            }
            return switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(self);
                case "equals" -> self == args[0];
                case "toString" -> "SupernaturalBloodlines lunar listener";
                default -> null;
            };
        });
        call(register, null, proxy);
    }

    private static ResourceLocation idOf(Holder<?> holder) {
        return holder.unwrapKey().map(ResourceKey::location).orElseThrow();
    }
}
