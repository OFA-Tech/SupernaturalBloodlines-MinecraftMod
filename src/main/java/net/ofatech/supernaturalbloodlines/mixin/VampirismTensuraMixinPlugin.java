package net.ofatech.supernaturalbloodlines.mixin;

import net.ofatech.supernaturalbloodlines.core.Part;

/** Applies the vampirism_tensura mixins only when every mod the part requires is installed and its switch is on. */
public final class VampirismTensuraMixinPlugin extends PartMixinPlugin {
    public VampirismTensuraMixinPlugin() {
        super(Part.VAMPIRISM_TENSURA);
    }
}
