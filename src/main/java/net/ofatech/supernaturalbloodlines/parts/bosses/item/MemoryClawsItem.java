package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules;
import net.ofatech.supernaturalbloodlines.parts.species.profile.ProfileKnowledge;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Knowledge;

/**
 * Peter's reward (R182). Right-click another player who consents and the user learns that player's sire and genes: they are written into the user's own profile
 * knowledge (never the pack's pool), so what the user reads on that player's Profile page is what the discovery rules already show of them. The rules are
 * {@link MemoryClawsRules}; this class asks the world the questions and tells the people involved.
 */
public final class MemoryClawsItem extends Item {
    public static final String LEARNED = "item.supernaturalbloodlines.memory_claws.learned";
    public static final String USED_ON = "item.supernaturalbloodlines.memory_claws.used_on";

    public MemoryClawsItem(Properties properties) {
        super(properties.durability(MemoryClawsRules.USES).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer user) || !(user.level() instanceof ServerLevel level)) {
            return target instanceof Player ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        MemoryClawsRules.Target kind = target instanceof ServerPlayer ? MemoryClawsRules.Target.PLAYER : MemoryClawsRules.Target.OTHER;
        boolean consent = target instanceof ServerPlayer t
                && LinkRules.consents(false, t.isShiftKeyDown(), t.getMainHandItem().is(Items.LEAD), level.getServer().isPvpAllowed());
        boolean mayHarm = target instanceof ServerPlayer t && user.canHarmPlayer(t);
        MemoryClawsRules.Verdict verdict = MemoryClawsRules.verdict(kind, target == user, consent, mayHarm);
        String refusal = MemoryClawsRules.message(verdict);
        if (refusal != null) {
            user.displayClientMessage(Component.translatable(refusal), true);
            return InteractionResult.SUCCESS;
        }
        ServerPlayer subject = (ServerPlayer) target;
        // the user's own record only: no pack pool (null), so a pack-mate learns nothing from it
        ProfileKnowledge.get(level).discover(null, user.getUUID(), subject.getUUID(), Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS));
        user.displayClientMessage(Component.translatable(LEARNED, subject.getDisplayName()), true);
        subject.displayClientMessage(Component.translatable(USED_ON, user.getDisplayName()), true);
        user.getCooldowns().addCooldown(this, MemoryClawsRules.COOLDOWN_TICKS);
        stack.hurtAndBreak(1, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    /** Used on the air they say what they are for: nothing is spent. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(MemoryClawsRules.SELF), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.memory_claws.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.memory_claws.tooltip_consent").withStyle(ChatFormatting.DARK_GRAY));
    }
}