package net.ofatech.supernaturalbloodlines.parts.artifacts.accessories;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import io.wispforest.accessories.api.AccessoriesCapability;

/** The only class that links Accessories. Call it only behind {@code ModList.isLoaded("accessories")} (see ArtifactSlots). */
public final class AccessoriesSlots {
    private AccessoriesSlots() {
    }

    public static boolean isEquipped(Player p, Item item) {
        return AccessoriesCapability.get(p).map(cap -> {
            for (var entry : cap.getEquipped().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (!stacks.getStackInSlot(i).isEmpty() && stacks.getStackInSlot(i).is(item)) {
                        return true;
                    }
                }
            }
            return false;
        }).orElse(false);
    }

    public static boolean isEquipped(Player p, TagKey<Item> tag) {
        return AccessoriesCapability.get(p).map(cap -> {
            for (var entry : cap.getEquipped().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (!stacks.getStackInSlot(i).isEmpty() && stacks.getStackInSlot(i).is(tag)) {
                        return true;
                    }
                }
            }
            return false;
        }).orElse(false);
    }

    public static Optional<ItemStack> find(Player p, Item item) {
        return find(p, item, s -> true);
    }

    public static Optional<ItemStack> find(Player p, Item item, Predicate<ItemStack> filter) {
        return AccessoriesCapability.get(p).flatMap(cap -> {
            for (var entry : cap.getEquipped().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack s = stacks.getStackInSlot(i);
                    if (!s.isEmpty() && s.is(item) && filter.test(s)) {
                        return Optional.of(s);
                    }
                }
            }
            return Optional.empty();
        });
    }
}