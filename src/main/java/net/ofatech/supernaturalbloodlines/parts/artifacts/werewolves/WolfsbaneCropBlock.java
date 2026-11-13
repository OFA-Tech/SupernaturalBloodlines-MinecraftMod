package net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves;

import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** A wolfsbane crop: a wheat-like crop on farmland; the loot table gives the plant when ripe and the seeds always. */
public class WolfsbaneCropBlock extends CropBlock {
    private final Supplier<? extends ItemLike> seeds;

    public WolfsbaneCropBlock(Properties properties, Supplier<? extends ItemLike> seeds) {
        super(properties);
        this.seeds = seeds;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }
}
