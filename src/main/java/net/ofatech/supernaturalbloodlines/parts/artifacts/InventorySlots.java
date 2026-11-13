package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Vanilla lookup: main inventory (hotbar included), then armor, then offhand. Never touches Curios. */
final class InventorySlots {
    private InventorySlots() {
    }

    static Optional<ItemStack> find(Player p, Item item) {
        Inventory inv = p.getInventory();
        for (NonNullList<ItemStack> list : java.util.List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : list) {
                if (!s.isEmpty() && s.is(item)) {
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }
}
