package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;

/**
 * Every ward of the world (R300), in one saved file on the overworld: the model is {@link WardTable}, this keeps it. It is keyed by owner and by id,
 * never read from a chunk, so a ward whose chunk is not loaded still falls the moment its owner does (R307).
 */
public final class WardIndex extends SavedData {
    private static final String NAME = "supernaturalbloodlines_wards";
    private static final String WARDS = "wards";

    private final WardTable table = new WardTable();

    public static WardIndex get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(WardIndex::new, WardIndex::load, null), NAME);
    }

    public WardTable table() {
        return table;
    }

    public Optional<WardTable.Ward> add(WardRules.Kind kind, String dimension, int x, int y, int z, int half, UUID owner, int maxPerOwner) {
        Optional<WardTable.Ward> ward = table.add(kind, dimension, x, y, z, half, owner, maxPerOwner, WardIndex::newId);
        ward.ifPresent(w -> setDirty());
        return ward;
    }

    public boolean drop(String id, UUID requester) {
        boolean dropped = table.drop(id, requester);
        if (dropped) {
            setDirty();
        }
        return dropped;
    }

    public Optional<WardRules.InviteResult> inviteAll(UUID owner, UUID who, int max) {
        Optional<WardRules.InviteResult> result = table.inviteAll(owner, who, max);
        if (result.filter(r -> r == WardRules.InviteResult.ADDED).isPresent()) {
            setDirty();
        }
        return result;
    }

    public boolean uninviteAll(UUID owner, UUID who) {
        boolean removed = table.uninviteAll(owner, who);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** @return how many wards of the owner were removed */
    public int dropAllOf(UUID owner) {
        int dropped = table.dropAllOf(owner);
        if (dropped > 0) {
            setDirty();
        }
        return dropped;
    }

    /** @return how many wards fell now */
    public int fallAllOf(UUID owner) {
        int fell = table.fallAllOf(owner);
        if (fell > 0) {
            setDirty();
        }
        return fell;
    }

    private static String newId() {
        return String.format("%08x", java.util.concurrent.ThreadLocalRandom.current().nextInt());
    }

    public static WardIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        WardIndex index = new WardIndex();
        for (Tag t : tag.getList(WARDS, Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            WardRules.Kind kind;
            try {
                kind = WardRules.Kind.valueOf(c.getString("kind"));
            } catch (IllegalArgumentException unknown) {
                continue;
            }
            WardTable.Ward ward = new WardTable.Ward(c.getString("id"), kind, c.getString("dim"), c.getInt("x"), c.getInt("y"), c.getInt("z"),
                    c.getInt("half"), NbtUtils.loadUUID(c.get("owner")));
            ward.invited().clear();
            for (Tag who : c.getList("invited", Tag.TAG_INT_ARRAY)) {
                ward.invited().add(NbtUtils.loadUUID(who));
            }
            ward.invited().add(ward.owner());
            ward.setFallen(c.getBoolean("fallen"));
            index.table.restore(ward);
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (WardTable.Ward ward : table.all()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", ward.id());
            c.putString("kind", ward.kind().name());
            c.putString("dim", ward.dimension());
            c.putInt("x", ward.cx());
            c.putInt("y", ward.cy());
            c.putInt("z", ward.cz());
            c.putInt("half", ward.half());
            c.put("owner", NbtUtils.createUUID(ward.owner()));
            ListTag invited = new ListTag();
            ward.invited().forEach(id -> invited.add(NbtUtils.createUUID(id)));
            c.put("invited", invited);
            c.putBoolean("fallen", ward.fallen());
            list.add(c);
        }
        tag.put(WARDS, list);
        return tag;
    }
}
