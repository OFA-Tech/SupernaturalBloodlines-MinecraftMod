package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.server.level.ServerPlayer;
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
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionHandler;

/**
 * A bottle of Doppelgänger Blood: drunk during the hybrid transition it completes it. Only drinkable by someone in a
 * stable transition, so it is never wasted.
 */
public final class DoppelgangerBloodItem extends Item {
    private static final int DRINK_TICKS = 32;

    public DoppelgangerBloodItem(Properties properties) {
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
        BloodlineState s = BloodlineAccess.get(player);
        if (!s.inTransition() || s.unstable()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            TransitionHandler.drankDoppelganger(player);
        }
        return entity instanceof Player player ? ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)) : stack;
    }
}
