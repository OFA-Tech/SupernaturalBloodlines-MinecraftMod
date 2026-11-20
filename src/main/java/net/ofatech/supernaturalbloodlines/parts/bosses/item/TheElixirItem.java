package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

/**
 * Elijah's reward (R168). It does nothing in the hand: carried in the inventory, it brings its holder back from the next death that is not a line death
 * or {@code /kill}, once ({@code BossEvents17d}).
 */
public final class TheElixirItem extends Item {
    public TheElixirItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.the_elixir.tooltip").withStyle(ChatFormatting.GRAY));
    }
}