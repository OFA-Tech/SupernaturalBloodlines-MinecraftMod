package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;

/** Worn; brings its wearer back from a supernatural killer. The uses left are the {@code charges} component. */
public class ResurrectionRingItem extends Item {
    public ResurrectionRingItem(DataComponentType<Integer> charges, int initialCharges) {
        super(new Item.Properties().stacksTo(1).component(charges, initialCharges));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.supernaturalbloodlines.charges", stack.getOrDefault(ArtifactItems.CHARGES.get(), 0))
                .withStyle(ChatFormatting.GRAY));
    }
}
