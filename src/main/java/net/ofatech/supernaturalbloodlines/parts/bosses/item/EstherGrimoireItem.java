package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;

/**
 * Finn's reward (R177): Esther's Grimoire. Right-click reads it: it teaches the Linking Spell and the Rite of Severance (plan rites-unlock), to every
 * reader and every time, and is never used up. The Linking Spell rite asks for it and gives it back.
 */
public final class EstherGrimoireItem extends Item {
    public EstherGrimoireItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (RiteUnlocks.learnLore(p, RiteUnlockTable.ESTHER) == 0) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.rites.nothing_new"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.esther_grimoire.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.esther_grimoire.tooltip_who").withStyle(ChatFormatting.DARK_GRAY));
    }
}
