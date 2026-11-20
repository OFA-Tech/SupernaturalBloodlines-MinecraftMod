package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.TheCureItem;

/**
 * Rebekah's reward (R171): the cure an Original drinks willingly, with no rite and no desiccation. It is The Cure with another event: one drink clears every
 * vampire, werewolf and species layer ({@code DRANK_THE_TRUE_CURE}); the Original is then human, and the ones they sired keep their line (a cured Original
 * is not a line death).
 */
public final class TheTrueCureItem extends TheCureItem {
    public TheTrueCureItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant().food(new FoodProperties.Builder()
                .nutrition(0).saturationModifier(0f).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build()));
    }

    @Override
    protected BloodlineEvent event() {
        return BloodlineEvent.DRANK_THE_TRUE_CURE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.supernaturalbloodlines.the_true_cure.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
