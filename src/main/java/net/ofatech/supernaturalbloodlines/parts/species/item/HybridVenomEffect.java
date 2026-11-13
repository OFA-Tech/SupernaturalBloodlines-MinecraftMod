package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.Venom;

/**
 * Werewolf venom on a vampire (R42v). Pulses every 40 ticks and once more on its last tick (see {@link Venom#pulse}).
 * Milk and the like cannot cure it; only Hybrid's Blood removes it.
 */
public final class HybridVenomEffect extends MobEffect {
    private static final int PULSE_TICKS = 40;

    public HybridVenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B8E23);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % PULSE_TICKS == 0 || duration == 1;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        MobEffectInstance instance = entity.getEffect(SpeciesRegistries.HYBRID_VENOM);
        Venom.pulse(entity, instance == null ? PULSE_TICKS : instance.getDuration());
        return true;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    }
}
