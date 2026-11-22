package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.function.BooleanSupplier;

/**
 * Whether the player turned UI motion off (the client setting {@code reduce_motion}). The part that owns the config installs the reader, so the HUD and
 * the boss bars can ask without depending on the hub part; until then motion is on. Pure.
 */
public final class ReduceMotion {
    private static volatile BooleanSupplier reader = () -> false;

    private ReduceMotion() {
    }

    public static void install(BooleanSupplier setting) {
        reader = setting;
    }

    public static boolean on() {
        return reader.getAsBoolean();
    }
}
