package net.ofatech.supernaturalbloodlines.parts.species.unlock;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * A page torn from an old grimoire (plan {@code rites-unlock}). Right-click reads it: it teaches the rites of its lore, then crumbles. A page whose lore
 * teaches nothing (a stale one) is not used up.
 */
public final class RitePageItem extends Item {
    public RitePageItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(String lore) {
        ItemStack stack = new ItemStack(SpeciesRegistries.RITE_PAGE.get());
        stack.set(SpeciesRegistries.RITE_LORE_COMPONENT.get(), lore);
        return stack;
    }

    public static String loreOf(ItemStack stack) {
        String lore = stack.get(SpeciesRegistries.RITE_LORE_COMPONENT.get());
        return lore == null ? "" : lore;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        String lore = loreOf(stack);
        if (RiteUnlockTable.taughtBy(lore).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        int fresh = RiteUnlocks.learnLore(p, lore);
        if (fresh == 0) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.rites.nothing_new"), true);
            return InteractionResultHolder.success(stack);
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String lore = loreOf(stack);
        lines.add(Component.translatable("item.supernaturalbloodlines.rite_page.lore." + (lore.isEmpty() ? "blank" : lore)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.rite_page.tooltip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
