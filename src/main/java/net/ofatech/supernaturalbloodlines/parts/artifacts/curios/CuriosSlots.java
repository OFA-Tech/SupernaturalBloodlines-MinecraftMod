package net.ofatech.supernaturalbloodlines.parts.artifacts.curios;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/** The only class that links Curios. Call it only behind {@code ModList.isLoaded("curios")} (see ArtifactSlots). */
public final class CuriosSlots {
    private CuriosSlots() {
    }

    public static boolean isEquipped(Player p, Item item) {
        return CuriosApi.getCuriosInventory(p).map(h -> h.isEquipped(item)).orElse(false);
    }

    public static Optional<ItemStack> find(Player p, Item item) {
        return find(p, item, s -> true);
    }

    public static Optional<ItemStack> find(Player p, Item item, Predicate<ItemStack> filter) {
        return CuriosApi.getCuriosInventory(p).flatMap(h -> h.findFirstCurio(s -> s.is(item) && filter.test(s))).map(SlotResult::stack);
    }
}
