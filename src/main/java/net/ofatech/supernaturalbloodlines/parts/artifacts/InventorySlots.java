package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Vanilla lookup: main inventory (hotbar included), then armor, then offhand. Never touches Curios. */
final class InventorySlots {
    private InventorySlots() {
    }

    static boolean has(Player p, TagKey<Item> tag) {
        Inventory inv = p.getInventory();
        for (NonNullList<ItemStack> list : java.util.List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : list) {
                if (!s.isEmpty() && s.is(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    static Optional<ItemStack> find(Player p, Item item, Predicate<ItemStack> filter) {
        Inventory inv = p.getInventory();
        for (NonNullList<ItemStack> list : java.util.List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : list) {
                if (!s.isEmpty() && s.is(item) && filter.test(s)) {
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }
}
