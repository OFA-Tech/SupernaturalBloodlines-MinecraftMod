package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

/** The Valet's tricorn: a hat with no armour value, which the Host of the Beast wears and any player may. */
public final class SebastienTricornItem extends Item implements Equipable {
    public SebastienTricornItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
}
