package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.item;

import net.favouriteless.enchanted.api.poppets.PlayerPoppetEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class SunPoppetEffect implements PlayerPoppetEffect {
    private static final ResourceKey<DamageType> SUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:sun_damage"));

    @Override
    public boolean protectsAgainst(DamageSource source) {
        return source.is(SUN_DAMAGE);
    }

    @Override
    public void protect(Player player) {
        // nothing to add: Enchanted zeroes the incoming damage itself when a poppet triggers, and only on a lethal hit
    }
}
