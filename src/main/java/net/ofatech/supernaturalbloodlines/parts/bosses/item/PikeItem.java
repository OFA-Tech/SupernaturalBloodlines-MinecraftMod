package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/** The Valet's Pike (R188, R189): an iron spear that never breaks, made by the Rite of the Valet Pike. See {@code PikeRules}. */
public final class PikeItem extends SwordItem {
    public PikeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.valet_pike.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.supernaturalbloodlines.valet_pike.key").withStyle(ChatFormatting.GRAY));
    }
}