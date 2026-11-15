package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Most payloads one player may send per second (20 ticks), counted from the first payload of a window. Every payload counts, a refused one as
 * much as one that worked, so a flood of refusals cannot get past it. No game classes.
 */
public final class RateLimiter {
    private static final long WINDOW_TICKS = 20;

    private static final class Window {
        long start;
        int count;
    }

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(UUID player, long nowTick, int limitPerSecond) {
        Window w = windows.computeIfAbsent(player, k -> new Window());
        synchronized (w) {
            // a clock that went backwards (a restarted tick counter) starts a new window too
            if (nowTick < w.start || nowTick - w.start >= WINDOW_TICKS) {
                w.start = nowTick;
                w.count = 0;
            }
            return ++w.count <= limitPerSecond;
        }
    }

    public void forget(UUID player) {
        windows.remove(player);
    }
}
