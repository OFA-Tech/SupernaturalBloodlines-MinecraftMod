package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.parts.artifacts.curios.CuriosSlots;

/** "Is this artifact worn": Curios slots first (when Curios is loaded), then inventory, armor and offhand. */
public final class ArtifactSlots {
    private ArtifactSlots() {
    }

    public static boolean isWorn(Player p, Item item) {
        return findWorn(p, item).isPresent();
    }

    /** The stack itself, so callers can read its data components (bound UUID, charges). */
    public static Optional<ItemStack> findWorn(Player p, Item item) {
        return findWorn(p, item, s -> true);
    }

    /** The first worn stack of {@code item} that passes {@code filter}: one copy that fails it must not hide another. */
    public static Optional<ItemStack> findWorn(Player p, Item item, Predicate<ItemStack> filter) {
        if (ModList.get().isLoaded("curios")) {
            Optional<ItemStack> curio = CuriosSlots.find(p, item, filter);
            if (curio.isPresent()) {
                return curio;
            }
        }
        return InventorySlots.find(p, item, filter);
    }
}
