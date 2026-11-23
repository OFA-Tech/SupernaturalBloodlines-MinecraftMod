package net.ofatech.supernaturalbloodlines.parts.species.blood;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/**
 * What vampire blood in the system does while it lasts (R207): Regeneration II, which is the faster healing, and a little less hunger. It is for anyone the blood
 * marks, however it got there (a bottle, a bite, a forced drink).
 */
public final class VampireBloodBuff {
    /** Longer than the 20 ticks between two refreshes, so the effect never flickers off. */
    static final int REFRESH_TICKS = 60;
    static final float EXHAUSTION_EASED = 0.15f;

    private VampireBloodBuff() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) {
                tick(p);
            }
        });
    }

    /** One pass of the buff for one player (every 20 ticks of their own). */
    public static void tick(ServerPlayer p) {
        if (!SpeciesConfig.vampireBloodBuffEnabled() || !p.isAlive() || BloodlineAccess.get(p).vampireBloodUntil() <= p.level().getGameTime()) {
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REFRESH_TICKS, 1, true, false));
        FoodData food = p.getFoodData();
        food.setExhaustion(Math.max(0f, food.getExhaustionLevel() - EXHAUSTION_EASED));
    }
}
