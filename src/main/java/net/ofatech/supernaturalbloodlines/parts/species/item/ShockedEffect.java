package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Electricity's mark on a supernatural: Slowness I (-15% speed, the vanilla amount). What else it does (no natural
 * regeneration, no Werewolves forms) is in {@code Electricity} and {@code FormBlocks}.
 */
public final class ShockedEffect extends MobEffect {
    public static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "effect.shocked");

    public ShockedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2E94E);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SLOW_ID, -0.15, Operation.ADD_MULTIPLIED_TOTAL);
    }
}
