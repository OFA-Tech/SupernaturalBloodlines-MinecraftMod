package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * A short blade: what it does to Originals and to non-humans who use it is in {@code AshDagger}. The Cursed one (R205) is the same blade with the
 * same numbers (it only does not burn), so what lodges where is the one difference, and the tooltip says it.
 */
public class AshDaggerItem extends Item {
    private final boolean cursed;

    public AshDaggerItem(boolean cursed) {
        super(properties(cursed));
        this.cursed = cursed;
    }

    private static Item.Properties properties(boolean cursed) {
        Item.Properties p = new Item.Properties().stacksTo(1).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
        return cursed ? p.fireResistant() : p;
    }

    public boolean cursed() {
        return cursed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(cursed ? "tooltip.supernaturalbloodlines.cursed_white_oak_ash_dagger" : "tooltip.supernaturalbloodlines.white_oak_ash_dagger")
                .withStyle(ChatFormatting.GRAY));
    }
}
