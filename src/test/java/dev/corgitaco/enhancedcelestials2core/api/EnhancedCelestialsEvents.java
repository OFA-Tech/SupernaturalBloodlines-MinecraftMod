package dev.corgitaco.enhancedcelestials2core.api;

import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarEvent;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarEventChangeListener;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;

/** A test stand-in (see EcReflectTest). */
public final class EnhancedCelestialsEvents {
    private static final List<LunarEventChangeListener> LISTENERS = new ArrayList<>();

    private EnhancedCelestialsEvents() {
    }

    public static void registerLunarEventChanged(LunarEventChangeListener listener) {
        LISTENERS.add(listener);
    }

    public static void fireLunarEventChanged(ServerLevel level, Holder<LunarEvent> previous, Holder<LunarEvent> now) {
        LISTENERS.forEach(l -> l.onLunarEventChanged(level, previous, now));
    }
}
