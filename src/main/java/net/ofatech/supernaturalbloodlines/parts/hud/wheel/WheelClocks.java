package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.wheel.CooldownClock;

/** One {@link CooldownClock} per player, and the game time the providers read (a test can replace it). */
public final class WheelClocks {
    private static final Map<UUID, CooldownClock> CLOCKS = new ConcurrentHashMap<>();
    private static LongSupplier overrideNow;

    private WheelClocks() {
    }

    public static CooldownClock of(ServerPlayer p) {
        return CLOCKS.computeIfAbsent(p.getUUID(), k -> new CooldownClock());
    }

    public static long now(ServerPlayer p) {
        LongSupplier o = overrideNow;
        return o != null ? o.getAsLong() : p.level().getGameTime();
    }

    /** Test seam: the game time providers see. {@code null} puts the real one back. */
    public static void setNowForTest(LongSupplier now) {
        overrideNow = now;
    }

    public static void forget(ServerPlayer p) {
        CLOCKS.remove(p.getUUID());
    }
}
