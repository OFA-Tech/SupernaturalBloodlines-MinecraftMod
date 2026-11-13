package net.ofatech.supernaturalbloodlines.shared.werewolves;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * "Does this player hold or wear an anchor item" (R94): an item in {@code #supernaturalbloodlines:anchor}. The species
 * part does not require artifacts, so it asks here. Without a replacement the inventory (hotbar, armor, offhand) is
 * searched; the artifacts part replaces the lookup with {@code ArtifactSlots}, which also covers Curios.
 */
public final class AnchorItems {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "anchor"));

    private static volatile Predicate<ServerPlayer> worn = AnchorItems::inInventory;

    private AnchorItems() {
    }

    /** Replaces the default inventory lookup (artifacts: Curios slots first, then the inventory). Returns the one it replaced. */
    public static Predicate<ServerPlayer> setWornLookup(Predicate<ServerPlayer> lookup) {
        Predicate<ServerPlayer> previous = worn;
        worn = lookup;
        return previous;
    }

    public static boolean holdsOrWears(ServerPlayer p) {
        return worn.test(p);
    }

    public static boolean inInventory(ServerPlayer p) {
        for (NonNullList<ItemStack> list : List.of(p.getInventory().items, p.getInventory().armor, p.getInventory().offhand)) {
            for (ItemStack s : list) {
                if (!s.isEmpty() && s.is(TAG)) {
                    return true;
                }
            }
        }
        return false;
    }
}
