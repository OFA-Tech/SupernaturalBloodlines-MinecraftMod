package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * Where a compass must not keep a target: on the ground and in a container. {@link VampireCompassItem#inventoryTick} already clears
 * one that is carried but not held; an item entity and a stack in a container are not ticked as inventory items, so they are covered here.
 * <ul>
 *   <li>Any compass that joins the level as an item entity (a toss, a death drop, a dispenser) is emptied on joining.</li>
 *   <li>When a player closes a container, every compass in its slots that the player is not holding is emptied (one put into a chest,
 *       a shulker box or the ender chest while it was open).</li>
 * </ul>
 * Not covered: a compass put somewhere without a player's container (an item frame, a hopper line); and compasses saved in a container by
 * an earlier version keep their last target until a player takes them out and carries them.
 */
public final class CompassPrivacy {
    private CompassPrivacy() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel() instanceof ServerLevel && e.getEntity() instanceof ItemEntity item) {
                clearIfCompass(item.getItem());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerContainerEvent.Close e) -> {
            for (Slot slot : e.getContainer().slots) {
                ItemStack stack = slot.getItem();
                if (stack.getItem() instanceof VampireCompassItem && !VampireCompassItem.heldBy(stack, e.getEntity())) {
                    VampireCompassItem.clearTarget(stack);
                }
            }
        });
    }

    private static void clearIfCompass(ItemStack stack) {
        if (stack.getItem() instanceof VampireCompassItem) {
            VampireCompassItem.clearTarget(stack);
        }
    }
}
