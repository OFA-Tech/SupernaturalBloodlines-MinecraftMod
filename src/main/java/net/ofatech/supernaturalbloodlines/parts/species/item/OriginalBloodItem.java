package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
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
 * A bottle of an Original's blood. Our own drinkable item: werewolves cannot drink Vampirism's blood bottles. Drinking
 * records whose blood it was and gives the {@code original_blood} effect; the last drink wins.
 */
public final class OriginalBloodItem extends Item {
    private static final int DRINK_TICKS = 32;

    public OriginalBloodItem(Properties properties) {
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
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT)) {
            return InteractionResultHolder.pass(stack); // blood from nobody
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        UUID source = stack.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT);
        if (source != null && entity instanceof ServerPlayer player) {
            player.setData(SpeciesRegistries.BLOOD_SOURCE, source);
            player.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
        }
        return entity instanceof Player player ? ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)) : stack;
    }
}
