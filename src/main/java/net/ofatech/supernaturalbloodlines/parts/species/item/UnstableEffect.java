package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

/** Marks a failing hybrid transition: when it ends, the player dies. Milk and the like cannot cure it. */
public final class UnstableEffect extends MobEffect {
    public static final int DURATION_TICKS = 1200;

    public UnstableEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B0082);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    }
}
