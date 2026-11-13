package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;

/** The vervain crop: a wheat-like crop on farmland; the loot table gives the herb when ripe and the seeds always. */
public class VervainBlock extends CropBlock {
    public VervainBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ArtifactItems.VERVAIN_SEEDS.get();
    }
}
