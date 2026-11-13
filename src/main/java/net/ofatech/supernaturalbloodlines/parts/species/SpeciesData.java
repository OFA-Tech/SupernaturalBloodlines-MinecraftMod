package net.ofatech.supernaturalbloodlines.parts.species;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide species facts: who the Originals (ORIGINAL and ORIGINAL_HYBRID) are, online or not. */
public final class SpeciesData extends SavedData {
    private static final String NAME = "supernaturalbloodlines_species";
    private static final String ORIGINALS = "originals";

    private final Set<UUID> originals = new HashSet<>();

    SpeciesData() {
    }

    public static SpeciesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SpeciesData::new, SpeciesData::load, null), NAME);
    }

    static SpeciesData load(CompoundTag tag, HolderLookup.Provider registries) {
        SpeciesData d = new SpeciesData();
        for (Tag t : tag.getList(ORIGINALS, Tag.TAG_INT_ARRAY)) {
            d.originals.add(NbtUtils.loadUUID(t));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        originals.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put(ORIGINALS, list);
        return tag;
    }

    public int count() {
        return originals.size();
    }

    public void setOriginal(UUID player, boolean original) {
        if (original ? originals.add(player) : originals.remove(player)) {
            setDirty();
        }
    }
}
