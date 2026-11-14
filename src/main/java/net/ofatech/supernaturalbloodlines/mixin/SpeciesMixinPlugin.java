package net.ofatech.supernaturalbloodlines.mixin;

import net.ofatech.supernaturalbloodlines.core.Part;

/** Applies the species mixins (witch bonus) only when Vampirism and Enchanted are installed and the species switch is on. */
public final class SpeciesMixinPlugin extends PartMixinPlugin {
    public SpeciesMixinPlugin() {
        super(Part.SPECIES);
    }
}
