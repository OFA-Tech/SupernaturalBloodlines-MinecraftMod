package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * A bottle of Hybrid's Blood: the only cure for werewolf venom (R42v). Only drinkable while poisoned, so it is never
 * wasted. It is deliberately not in {@code #supernaturalbloodlines:blood_drinks}.
 */
public final class HybridBloodItem extends Item {
    private static final int DRINK_TICKS = 32;

    public HybridBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.hasEffect(SpeciesRegistries.HYBRID_VENOM)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        entity.removeEffect(SpeciesRegistries.HYBRID_VENOM);
        return entity instanceof Player player ? ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)) : stack;
    }
}
