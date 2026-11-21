package net.ofatech.supernaturalbloodlines.parts.hud.net;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.ActionCooldown;

/** What the client was last told about each action's cooldown (written by the payload handler, read by the slot). Empty on a server. */
public final class ActionCooldowns {
    private record Started(long at, int ticks) {
    }

    private static final Map<ResourceLocation, Started> STARTED = new ConcurrentHashMap<>();

    private ActionCooldowns() {
    }

    public static void set(ResourceLocation action, long now, int ticks) {
        STARTED.put(action, new Started(now, ticks));
    }

    public static int total(ResourceLocation action) {
        Started s = STARTED.get(action);
        return s == null ? 0 : s.ticks();
    }

    public static int left(ResourceLocation action, long now) {
        Started s = STARTED.get(action);
        return s == null ? 0 : ActionCooldown.left(s.at(), s.ticks(), now);
    }

    /** A new world: its clock starts again, so an old cooldown means nothing. */
    public static void clear() {
        STARTED.clear();
    }
}
