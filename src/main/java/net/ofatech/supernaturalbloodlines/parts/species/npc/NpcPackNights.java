package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The ledger of the NPC packs (R143): the night each player was last rolled for, and how many packs this level has spawned in the night it last
 * spawned one. Saved with the world, so a restart cannot roll a player twice in one night. A bad entry costs the entry, not the file.
 */
public final class NpcPackNights extends SavedData {
    private static final String NAME = "supernaturalbloodlines_npc_pack_nights";
    private final Map<UUID, Long> last = new HashMap<>();
    private long countNight = -1;
    private int count;
    private long nextPackId = 1;

    public static NpcPackNights get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(NpcPackNights::new, NpcPackNights::load, null), NAME);
    }

    /** The night {@code p} was last rolled for, or -1. */
    public long lastNight(UUID p) {
        return last.getOrDefault(p, -1L);
    }

    public void rolled(UUID p, long night) {
        last.put(p, night);
        setDirty();
    }

    /** How many packs this level spawned in {@code night}. */
    public int packsIn(long night) {
        return night == countNight ? count : 0;
    }

    public void packSpawned(long night) {
        if (night != countNight) {
            countNight = night;
            count = 0;
        }
        count++;
        setDirty();
    }

    public long newPackId() {
        setDirty();
        return nextPackId++;
    }

    public static NpcPackNights load(CompoundTag tag, HolderLookup.Provider registries) {
        NpcPackNights n = new NpcPackNights();
        n.countNight = tag.contains("count_night") ? tag.getLong("count_night") : -1;
        n.count = tag.getInt("count");
        n.nextPackId = Math.max(1, tag.getLong("next_pack"));
        for (var e : tag.getCompound("last").getAllKeys()) {
            try {
                n.last.put(UUID.fromString(e), tag.getCompound("last").getLong(e));
            } catch (IllegalArgumentException bad) {
                // a bad key costs that entry only
            }
        }
        return n;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag map = new CompoundTag();
        last.forEach((id, night) -> map.putLong(id.toString(), night));
        tag.put("last", map);
        tag.putLong("count_night", countNight);
        tag.putInt("count", count);
        tag.putLong("next_pack", nextPackId);
        return tag;
    }
}
