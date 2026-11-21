package net.ofatech.supernaturalbloodlines.parts.species.skill;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** One saved cooldown per action id, shared by every timed action of the species part. It survives death and a relog. */
public final class ActionClock {
    private ActionClock() {
    }

    public static boolean ready(ServerPlayer p, ResourceLocation action) {
        return left(p, action) == 0;
    }

    /** Starts the cooldown and tells the HUD slot. */
    public static void start(ServerPlayer p, ResourceLocation action, int ticks) {
        Map<String, Long> next = new HashMap<>(p.getData(SpeciesRegistries.ACTION_READY));
        next.put(action.toString(), p.level().getGameTime() + ticks);
        p.setData(SpeciesRegistries.ACTION_READY, next);
        ExtraActions.cooldownStarted(p, action, ticks);
    }

    public static int left(ServerPlayer p, ResourceLocation action) {
        long readyAt = p.getData(SpeciesRegistries.ACTION_READY).getOrDefault(action.toString(), 0L);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, readyAt - p.level().getGameTime()));
    }
}
