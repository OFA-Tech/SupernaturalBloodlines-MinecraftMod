package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Electricity;

/** The hunter's Taser: a hit shocks a supernatural (see {@link Electricity}). Every hit spends a charge; redstone repairs it in an anvil. */
public final class Taser extends Item {
    public static final int DURABILITY = 16;

    public Taser(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            Electricity.shockIfSupernatural(target);
        }
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.REDSTONE);
    }
}
