package dev.corgitaco.enhancedcelestials2core;

import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarForecast;
import java.util.Optional;
import net.minecraft.world.level.Level;

/** A test stand-in with the signatures javap shows in Enhanced Celestials 2 core 2.0.3.4 (see EcReflectTest). Not the mod's code. */
public class EnhancedCelestials {
    public static LunarForecast forecast;

    public static Optional<LunarForecast> lunarForecastWorldData(Level level) {
        return Optional.ofNullable(forecast);
    }
}
