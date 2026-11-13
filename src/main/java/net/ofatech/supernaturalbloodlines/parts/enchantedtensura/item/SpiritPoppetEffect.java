package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.item;

import net.favouriteless.enchanted.api.poppets.PlayerPoppetEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class SpiritPoppetEffect implements PlayerPoppetEffect {
    private static final TagKey<DamageType> IS_SPIRITUAL = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("tensura:is_spiritual"));

    @Override
    public boolean protectsAgainst(DamageSource source) {
        return source.is(IS_SPIRITUAL);
    }

    @Override
    public void protect(Player player) {
        // nothing to add: Enchanted zeroes the incoming damage itself when a poppet triggers, and only on a lethal hit
    }
}
