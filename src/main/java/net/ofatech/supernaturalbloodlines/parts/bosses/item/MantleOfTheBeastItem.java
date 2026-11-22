package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The Mantle of the Beast (R191): see {@code MantleRules}. A cloak for the Curios back slot (or carried, where Curios is not installed). */
public final class MantleOfTheBeastItem extends Item {
    public MantleOfTheBeastItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.mantle_of_the_beast.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.mantle_of_the_beast.who").withStyle(ChatFormatting.GRAY));
    }
}
