package net.ofatech.supernaturalbloodlines.parts.artifacts.block;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;

/**
 * What a line remembers: whether a pack Emissary laid it, and who placed it ({@code owner}, for Ashwarden, S-WIT-3). Such a line also stops the mobs in
 * {@code #supernaturalbloodlines:supernatural_mobs}. {@link MountainAshLineBlock#setPlacedBy} sets it when the placer is
 * an Emissary (asked through {@code EmissaryLines}); without the species part it stays false.
 */
public class MountainAshLineBlockEntity extends BlockEntity {
    private static final String EMISSARY = "emissary";

    private static final String OWNER = "owner";

    private boolean emissary;
    @Nullable
    private UUID owner;

    public MountainAshLineBlockEntity(BlockPos pos, BlockState state) {
        super(MountainAsh.LINE_ENTITY.get(), pos, state);
    }

    public boolean isEmissary() {
        return emissary;
    }

    /** The player who placed the line, null for one placed by something else or before this was recorded. */
    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        if (!java.util.Objects.equals(this.owner, owner)) {
            this.owner = owner;
            setChanged();
        }
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
        if (owner != null) {
            tag.putUUID(OWNER, owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        emissary = tag.getBoolean(EMISSARY);
        owner = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }
}
