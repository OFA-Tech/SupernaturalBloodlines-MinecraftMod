package net.ofatech.supernaturalbloodlines.parts.species.rite;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;

/** What the two line rites read from their ingredients. */
final class LineRites {
    private LineRites() {
    }

    /** Whose blood each bound bottle of Sire's Blood among the ingredients is, in order. */
    static List<UUID> bloodSources(List<ItemStack> consumed) {
        return consumed.stream().filter(s -> s.is(SpeciesRegistries.SIRES_BLOOD.get())).map(SiresBloodItem::sourceOf).filter(Objects::nonNull).toList();
    }
}
