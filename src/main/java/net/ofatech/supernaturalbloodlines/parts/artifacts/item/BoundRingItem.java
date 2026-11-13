package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;

/** The Sun Ring and the Moon Ring: worn items that belong to one player (data component {@code bound_to}). */
public class BoundRingItem extends Item {
    public BoundRingItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        UUID owner = stack.get(ArtifactItems.BOUND_TO.get());
        if (owner == null) {
            lines.add(Component.translatable("tooltip.supernaturalbloodlines.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        String name = FMLEnvironment.dist == Dist.CLIENT ? ClientNames.of(owner) : null;
        lines.add(Component.translatable("tooltip.supernaturalbloodlines.bound_to", name != null ? name : owner.toString())
                .withStyle(ChatFormatting.GRAY));
    }
}
