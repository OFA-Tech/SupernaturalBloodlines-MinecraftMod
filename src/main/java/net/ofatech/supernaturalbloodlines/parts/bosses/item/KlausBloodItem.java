package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodEffect;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;

/**
 * Klaus's Blood (R164): the blood of an Original hybrid (he is one), so it is taken under either {@code hybrid_transition_blood}, and Hybrid's Blood as
 * well: drinking it cures werewolf venom. Drunk by someone who then dies, it starts the hybrid transition in Klaus's line; the transition death still
 * applies. It is an item, not a Sire's Blood bottle: it records no source on the stack, the source is always Klaus. If his line is dead it only cures.
 */
public final class KlausBloodItem extends Item {
    private static final int DRINK_TICKS = 32;

    public KlausBloodItem(Properties properties) {
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
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.klaus_blood.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        entity.removeEffect(SpeciesRegistries.HYBRID_VENOM);
        if (entity instanceof ServerPlayer player && level.getServer() != null) {
            if (LineIndex.get(level.getServer()).isDead(NpcOriginal.KLAUS.uuid())) {
                // his line is gone (he died to white oak): the blood still cures, but there is no sire left to turn anyone for
                player.displayClientMessage(Component.translatable("item.supernaturalbloodlines.klaus_blood.dead_line"), true);
            } else {
                player.setData(SpeciesRegistries.BLOOD_SOURCE, NpcOriginal.KLAUS.uuid());
                player.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
            }
        }
        return entity instanceof Player player ? ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)) : stack;
    }
}