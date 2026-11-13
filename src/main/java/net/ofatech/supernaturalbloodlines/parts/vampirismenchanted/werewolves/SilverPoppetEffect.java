package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves;

import net.favouriteless.enchanted.api.poppets.PlayerPoppetEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Only ever instantiated when Werewolves is loaded. It reads Werewolves' tags by id and links none of its classes. */
public final class SilverPoppetEffect implements PlayerPoppetEffect {
    private static final TagKey<DamageType> BITE = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("werewolves:bite"));
    private static final ResourceLocation SILVER_BOLT = ResourceLocation.parse("werewolves:crossbow_arrow_silver_bolt");
    private static final TagKey<Item> SILVER = TagKey.create(Registries.ITEM, ResourceLocation.parse("werewolves:type/silver"));

    @Override
    public boolean protectsAgainst(DamageSource source) {
        if (source.is(BITE)) {
            return true;
        }
        if (isSilver(source.getWeaponItem())) {
            return true;
        }
        // for an arrow getWeaponItem() is the bow; the silver is the ammunition
        return source.getDirectEntity() instanceof AbstractArrow arrow && isSilver(arrow.getPickupItemStackOrigin());
    }

    private static boolean isSilver(ItemStack stack) {
        // the silver bolt is not in werewolves:type/silver, so it is named by id
        return stack != null && !stack.isEmpty() && (stack.is(SILVER) || stack.is(BuiltInRegistries.ITEM.get(SILVER_BOLT)));
    }

    @Override
    public void protect(Player player) {
        // nothing to add: Enchanted zeroes the incoming damage itself when a poppet triggers, and only on a lethal hit
    }
}
