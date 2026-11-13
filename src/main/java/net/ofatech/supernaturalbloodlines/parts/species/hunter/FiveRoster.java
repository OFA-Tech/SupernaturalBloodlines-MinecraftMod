package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The Five: who holds a mark, online or not (logging out keeps the slot). Also the hunters waiting to be chosen
 * as a potential, and the killers whose Hunter's Curse was lifted.
 */
public final class FiveRoster extends SavedData {
    private static final String NAME = "supernaturalbloodlines_five";
    private static final String MEMBERS = "members";
    private static final String PENDING = "pending_potential_choice";
    private static final String LIFTED = "lifted_curses";
    private static final String CURSED = "cursed";
    private static final String PENDING_ID = "id";
    private static final String PENDING_COUNT = "count";

    private final Set<UUID> members = new HashSet<>();
    // marked hunter (by UUID) -> how many potentials their lost marks still owe the world (the same hunter can lose it twice)
    private final Map<UUID, Integer> pendingPotentialChoice = new HashMap<>();
    // cursed killers whose curse was lifted while they were offline; applied at their next login
    private final Set<UUID> liftedCurses = new HashSet<>();
    // everyone who got the curse and has not been freed: tells which offline players to free when it lifts
    private final Set<UUID> cursed = new HashSet<>();

    FiveRoster() {
    }

    /** One roster per world: always read from the overworld's storage, whichever level is passed. */
    public static FiveRoster get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(FiveRoster::new, FiveRoster::load, null), NAME);
    }

    static FiveRoster load(CompoundTag tag, HolderLookup.Provider registries) {
        FiveRoster r = new FiveRoster();
        read(tag, MEMBERS, r.members);
        for (Tag t : tag.getList(PENDING, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            int count = entry.getInt(PENDING_COUNT);
            if (count > 0) {
                r.pendingPotentialChoice.put(NbtUtils.loadUUID(entry.get(PENDING_ID)), count);
            }
        }
        read(tag, LIFTED, r.liftedCurses);
        read(tag, CURSED, r.cursed);
        return r;
    }

    private static void read(CompoundTag tag, String key, Set<UUID> into) {
        for (Tag t : tag.getList(key, Tag.TAG_INT_ARRAY)) {
            into.add(NbtUtils.loadUUID(t));
        }
    }

    private static void write(CompoundTag tag, String key, Set<UUID> from) {
        ListTag list = new ListTag();
        from.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put(key, list);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag, MEMBERS, members);
        ListTag pending = new ListTag();
        pendingPotentialChoice.forEach((id, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.put(PENDING_ID, NbtUtils.createUUID(id));
            entry.putInt(PENDING_COUNT, count);
            pending.add(entry);
        });
        tag.put(PENDING, pending);
        write(tag, LIFTED, liftedCurses);
        write(tag, CURSED, cursed);
        return tag;
    }

    private boolean change(Set<UUID> set, UUID id, boolean present) {
        boolean changed = present ? set.add(id) : set.remove(id);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public int size() {
        return members.size();
    }

    public boolean contains(UUID id) {
        return members.contains(id);
    }

    /** @return false when they were already in */
    public boolean add(UUID id) {
        return change(members, id, true);
    }

    public void remove(UUID id) {
        change(members, id, false);
    }

    /** Marked hunter who lost the mark → potentials still owed because of it. */
    public Map<UUID, Integer> pendingPotentialChoice() {
        return Collections.unmodifiableMap(pendingPotentialChoice);
    }

    public void addPendingPotentialChoice(UUID lostMarkOf) {
        pendingPotentialChoice.merge(lostMarkOf, 1, Integer::sum);
        setDirty();
    }

    /** One owed potential was chosen. */
    public void resolvePendingPotentialChoice(UUID lostMarkOf) {
        pendingPotentialChoice.computeIfPresent(lostMarkOf, (id, n) -> n > 1 ? n - 1 : null);
        setDirty();
    }

    public void clearPendingPotentialChoice(UUID lostMarkOf) {
        if (pendingPotentialChoice.remove(lostMarkOf) != null) {
            setDirty();
        }
    }

    public Set<UUID> liftedCurses() {
        return Collections.unmodifiableSet(liftedCurses);
    }

    public void setLiftedCurse(UUID id, boolean lifted) {
        change(liftedCurses, id, lifted);
    }

    public Set<UUID> cursed() {
        return Collections.unmodifiableSet(cursed);
    }

    public void setCursed(UUID id, boolean isCursed) {
        change(cursed, id, isCursed);
    }
}
