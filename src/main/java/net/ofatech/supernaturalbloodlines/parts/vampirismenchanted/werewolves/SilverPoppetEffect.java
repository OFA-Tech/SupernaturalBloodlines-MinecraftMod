package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves;

import net.favouriteless.enchanted.api.poppets.PlayerPoppetEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Only ever instantiated when Werewolves is loaded. It reads Werewolves' tags by id and links none of its classes. */
public final class SilverPoppetEffect implements PlayerPoppetEffect {
    private static final TagKey<DamageType> BITE = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("werewolves:bite"));
    private static final TagKey<Item> SILVER = TagKey.create(Registries.ITEM, ResourceLocation.parse("werewolves:type/silver"));

    @Override
    public boolean protectsAgainst(DamageSource source) {
        if (source.is(BITE)) {
            return true;
        }
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && weapon.is(SILVER);
    }

    @Override
    public void protect(Player player) {
        // nothing to add: Enchanted zeroes the incoming damage itself when a poppet triggers, and only on a lethal hit
    }
}
