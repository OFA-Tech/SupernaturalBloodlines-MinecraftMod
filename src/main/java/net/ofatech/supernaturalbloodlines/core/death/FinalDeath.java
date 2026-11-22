package net.ofatech.supernaturalbloodlines.core.death;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;

/**
 * A death the player chose (Give up in the dying state): nothing that saves a player from dying (the Elixir, the Resurrection Ring) may take it back. The
 * code that causes such a death runs it through {@link #run}; the savers ask {@link #is}. Shared by the parts, so none of them needs another.
 */
public final class FinalDeath {
    private static final Set<UUID> ENDING = ConcurrentHashMap.newKeySet();

    private FinalDeath() {
    }

    public static void run(Entity who, Runnable death) {
        boolean outer = ENDING.add(who.getUUID());
        try {
            death.run();
        } finally {
            if (outer) {
                ENDING.remove(who.getUUID());
            }
        }
    }

    public static boolean is(Entity who) {
        return ENDING.contains(who.getUUID());
    }
}
