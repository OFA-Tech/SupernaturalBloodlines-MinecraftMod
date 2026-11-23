package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * A short blade: what it does to Originals and to non-humans who use it is in {@code AshDagger}. The Cursed one (R205) is the same blade with the
 * same numbers, so nothing but who it lodges in tells them apart.
 */
public class AshDaggerItem extends Item {
    private final boolean cursed;

    public AshDaggerItem(boolean cursed) {
        super(new Item.Properties().stacksTo(1).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
        this.cursed = cursed;
    }

    public boolean cursed() {
        return cursed;
    }
}
