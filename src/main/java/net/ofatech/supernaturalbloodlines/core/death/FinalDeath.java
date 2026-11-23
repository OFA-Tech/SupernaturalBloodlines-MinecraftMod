package net.ofatech.supernaturalbloodlines.core.death;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

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

    /**
     * Savers from other mods (a revive skill, a death-cancelling passive: Return by Death, Immortality, Phoenix and so on) do not ask {@link #is}. Last of
     * all listeners, a chosen death that one of them cancelled is taken back and the health put back to zero, so the saver may have healed.
     */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, FinalDeath::takeBackSave);
    }

    static void takeBackSave(LivingDeathEvent e) {
        if (e.isCanceled() && is(e.getEntity())) {
            e.setCanceled(false);
            e.getEntity().setHealth(0.0F);
        }
    }
}
