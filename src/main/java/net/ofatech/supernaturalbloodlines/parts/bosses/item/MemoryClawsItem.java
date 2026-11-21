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
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules;
import net.ofatech.supernaturalbloodlines.parts.species.profile.SpeciesProfiles;

/**
 * Peter's reward (R183). Right-click another player who agrees on purpose (sneaking, a lead in the main hand, looking at you, PvP on, and you may hurt them) and you are
 * given a snapshot of their sire and genes as they knew them then, shown on their Profile page. The rules are {@link MemoryClawsRules}; this class asks the world the
 * questions and tells the people involved. The cooldown is kept here, first and with the same words for every target, because a vanilla cooldown stops a use of the item
 * and not a use on somebody.
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
        if (user.getCooldowns().isOnCooldown(this)) {
            int seconds = MemoryClawsRules.secondsLeft(user.getCooldowns().getCooldownPercent(this, 0F));
            user.displayClientMessage(Component.translatable(MemoryClawsRules.WAIT, Math.max(1, seconds)), true);
            return InteractionResult.SUCCESS;
        }
        MemoryClawsRules.Target kind = target instanceof ServerPlayer ? MemoryClawsRules.Target.PLAYER : MemoryClawsRules.Target.OTHER;
        boolean pvp = level.getServer().isPvpAllowed();
        boolean consent = false;
        boolean facing = false;
        boolean mayHarm = false;
        if (target instanceof ServerPlayer t) {
            consent = LinkRules.consents(false, t.isShiftKeyDown(), t.getMainHandItem().is(Items.LEAD), pvp);
            facing = looksAt(t, user);
            mayHarm = user.canHarmPlayer(t);
        }
        MemoryClawsRules.Verdict verdict = MemoryClawsRules.verdict(kind, target == user, pvp, consent, facing, mayHarm);
        String refusal = MemoryClawsRules.message(verdict);
        if (refusal != null) {
            user.displayClientMessage(Component.translatable(refusal), true);
            return InteractionResult.SUCCESS;
        }
        ServerPlayer subject = (ServerPlayer) target;
        SpeciesProfiles.snapshot(user, subject);
        user.displayClientMessage(Component.translatable(LEARNED, subject.getDisplayName()), true);
        Component told = Component.translatable(USED_ON, user.getDisplayName());
        subject.displayClientMessage(told, true);
        subject.sendSystemMessage(told);
        user.getCooldowns().addCooldown(this, MemoryClawsRules.COOLDOWN_TICKS);
        stack.hurtAndBreak(1, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    /** Whether {@code looker} is looking at {@code user}: the look and the way to their eyes agree, and there is a clear line. Standing on the same spot is looking. */
    private static boolean looksAt(ServerPlayer looker, ServerPlayer user) {
        Vec3 to = user.getEyePosition().subtract(looker.getEyePosition());
        double length = to.length();
        double dot = length < 1.0E-3 ? 1.0 : looker.getLookAngle().dot(to.scale(1.0 / length));
        return MemoryClawsRules.facing(dot, looker.hasLineOfSight(user));
    }

    /** Used on the air they say how they are used: nothing is spent. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(MemoryClawsRules.USAGE), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.memory_claws.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.memory_claws.tooltip_consent").withStyle(ChatFormatting.GRAY));
    }
}
