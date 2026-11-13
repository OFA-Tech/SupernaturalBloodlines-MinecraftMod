package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.Supernatural;

/**
 * Points to the nearest vampire, hybrid or Original within {@link #RANGE} blocks. It rides on the vanilla
 * {@code lodestone_tracker} component (untracked, so no lodestone is needed) which the server refreshes once a second;
 * with nobody in range the tracker is empty and the needle spins. The client side is {@code ArtifactsClient}.
 */
public class VampireCompassItem extends Item {
    public static final int RANGE = 64;
    public static final int PERIOD = 20;

    public VampireCompassItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide && holder.tickCount % PERIOD == 0) {
            update(stack, level, holder);
        }
    }

    /** A new target is not a new item: no re-equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    public static void update(ItemStack stack, Level level, Entity holder) {
        Optional<GlobalPos> target = nearestVampire(level, holder).map(e -> GlobalPos.of(level.dimension(), e.blockPosition()));
        LodestoneTracker tracker = new LodestoneTracker(target, false);
        // set only on a change, so the stack is not re-sent every second
        if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
            stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
    }

    private static Optional<LivingEntity> nearestVampire(Level level, Entity holder) {
        return level.getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(RANGE),
                        e -> e != holder && e.isAlive() && e.distanceToSqr(holder) <= (double) RANGE * RANGE && Supernatural.isVampiric(e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(holder)));
    }
}
