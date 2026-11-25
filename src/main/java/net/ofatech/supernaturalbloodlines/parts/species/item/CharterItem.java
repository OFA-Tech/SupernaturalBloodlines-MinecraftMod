package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The Clan Charter: a token whose truth is a row of the server's group index. It has no use of its own; holding it at midnight is the Game of Kings. */
public final class CharterItem extends Item {
    public CharterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.supernaturalbloodlines.clan_charter.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.supernaturalbloodlines.clan_charter.tooltip_night").withStyle(ChatFormatting.DARK_GRAY));
    }
}
