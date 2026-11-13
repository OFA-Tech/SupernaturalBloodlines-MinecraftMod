package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** A marker: while it lasts, dying counts as dying "with Original's Blood". The source is in the blood_source attachment. */
public final class OriginalBloodEffect extends MobEffect {
    public static final int DURATION_TICKS = 6000;

    public OriginalBloodEffect() {
        super(MobEffectCategory.NEUTRAL, 0x8B0000);
    }
}
