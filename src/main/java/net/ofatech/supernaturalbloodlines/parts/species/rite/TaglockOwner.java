package net.ofatech.supernaturalbloodlines.parts.species.rite;

import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.common.items.component.EDataComponents;
import net.favouriteless.enchanted.common.items.component.EntityRefData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Who a filled taglock belongs to, read the way Enchanted's own rites do ({@code EDataComponents.ENTITY_REF}). The owner is looked up
 * by UUID among the entities loaded in the rite's level, then among the players online anywhere. An offline player or an unloaded
 * villager cannot be found, and the rites that need the owner refuse: their state lives in the player's save, which is not read while
 * they are away (listed in Known Limits).
 */
final class TaglockOwner {
    private TaglockOwner() {
    }

    /** The owner's UUID from the first filled taglock among {@code items}; null when none carries one. */
    @Nullable
    static UUID uuid(List<ItemStack> items) {
        for (ItemStack stack : items) {
            EntityRefData ref = stack.get(EDataComponents.ENTITY_REF.get());
            if (ref != null) {
                return ref.uuid();
            }
        }
        return null;
    }

    /** The owner if it is loaded in {@code level} or is a player online, else null. */
    @Nullable
    static LivingEntity find(ServerLevel level, UUID id) {
        Entity e = level.getEntity(id);
        if (e == null) {
            e = level.getServer().getPlayerList().getPlayer(id);
        }
        return e instanceof LivingEntity living && !living.isRemoved() ? living : null;
    }
}
