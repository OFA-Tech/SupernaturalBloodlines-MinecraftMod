package net.ofatech.supernaturalbloodlines.parts.species.skill;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.blood.DailyUses;
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
        startSilent(p, action, ticks);
        ExtraActions.cooldownStarted(p, action, ticks);
    }

    /** The same clock for something that is not a HUD action (a limit on an effect): nothing is told to a slot. */
    public static void startSilent(ServerPlayer p, ResourceLocation key, int ticks) {
        Map<String, Long> next = new HashMap<>(p.getData(SpeciesRegistries.ACTION_READY));
        next.put(key.toString(), p.level().getGameTime() + ticks);
        p.setData(SpeciesRegistries.ACTION_READY, next);
    }

    /** How many times this action was used in the current game day (a day is 24000 ticks). */
    public static int usesToday(ServerPlayer p, ResourceLocation action) {
        Map<String, Long> m = p.getData(SpeciesRegistries.ACTION_READY);
        return DailyUses.usesToday(m.getOrDefault(action + "#day", -1L), m.getOrDefault(action + "#uses", 0L), p.level().getGameTime());
    }

    /** Counts one use today. */
    public static void countUse(ServerPlayer p, ResourceLocation action) {
        int uses = usesToday(p, action) + 1;
        Map<String, Long> next = new HashMap<>(p.getData(SpeciesRegistries.ACTION_READY));
        next.put(action + "#day", today(p));
        next.put(action + "#uses", (long) uses);
        p.setData(SpeciesRegistries.ACTION_READY, next);
    }

    private static long today(ServerPlayer p) {
        return DailyUses.dayOf(p.level().getGameTime());
    }

    public static int left(ServerPlayer p, ResourceLocation action) {
        long readyAt = p.getData(SpeciesRegistries.ACTION_READY).getOrDefault(action.toString(), 0L);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, readyAt - p.level().getGameTime()));
    }
}
