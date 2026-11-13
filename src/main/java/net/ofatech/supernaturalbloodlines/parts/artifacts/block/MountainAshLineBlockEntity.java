package net.ofatech.supernaturalbloodlines.parts.artifacts.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;

/**
 * The one fact a line remembers: whether a pack Emissary laid it. Such a line also stops the mobs in
 * {@code #supernaturalbloodlines:supernatural_mobs}. {@link MountainAshLineBlock#setPlacedBy} sets it when the placer is
 * an Emissary (asked through {@code EmissaryLines}); without the species part it stays false.
 */
public class MountainAshLineBlockEntity extends BlockEntity {
    private static final String EMISSARY = "emissary";

    private boolean emissary;

    public MountainAshLineBlockEntity(BlockPos pos, BlockState state) {
        super(MountainAsh.LINE_ENTITY.get(), pos, state);
    }

    public boolean isEmissary() {
        return emissary;
    }

    public void setEmissary(boolean emissary) {
        if (this.emissary != emissary) {
            this.emissary = emissary;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (emissary) {
            tag.putBoolean(EMISSARY, true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        emissary = tag.getBoolean(EMISSARY);
    }
}
