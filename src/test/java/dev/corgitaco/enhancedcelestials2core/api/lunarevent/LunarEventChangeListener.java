package dev.corgitaco.enhancedcelestials2core.api.lunarevent;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;

/** A test stand-in (see EcReflectTest). */
public interface LunarEventChangeListener {
    void onLunarEventChanged(ServerLevel level, Holder<LunarEvent> previous, Holder<LunarEvent> now);
}
