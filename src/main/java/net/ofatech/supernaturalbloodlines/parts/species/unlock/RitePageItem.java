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
 * A page torn from an old grimoire (plan {@code rites-unlock}). Right-click reads it: it teaches the rites of its lore, every time, and is never used up
 * (it works like the two grimoires, so it can be handed on). A page that teaches nothing says so.
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
        if (RiteUnlocks.learnLore(p, loreOf(stack)) == 0) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.rites.nothing_new"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    /** The line that says what reading a page teaches: its rites by name, or that it is blank. */
    public static Component teaches(String lore) {
        List<String> rites = RiteUnlockTable.taughtBy(lore);
        if (rites.isEmpty()) {
            return Component.translatable("item.supernaturalbloodlines.rite_page.tooltip_blank");
        }
        Component list = Component.empty();
        for (int i = 0; i < rites.size(); i++) {
            list = ((net.minecraft.network.chat.MutableComponent) list).append(i == 0 ? Component.empty() : Component.literal(", "))
                    .append(Component.translatable("rite.supernaturalbloodlines." + rites.get(i)));
        }
        return Component.translatable("item.supernaturalbloodlines.rite_page.tooltip", list);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String lore = loreOf(stack);
        lines.add(Component.translatable("item.supernaturalbloodlines.rite_page.lore." + (lore.isEmpty() ? "blank" : lore)).withStyle(ChatFormatting.GRAY));
        lines.add(teaches(lore).copy().withStyle(ChatFormatting.DARK_GRAY));
    }
}