package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import java.util.Collections;
import java.util.HashSet;
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

    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingPotentialChoice = new HashSet<>();
    private final Set<UUID> liftedCurses = new HashSet<>();

    FiveRoster() {
    }

    /** One roster per world: always read from the overworld's storage, whichever level is passed. */
    public static FiveRoster get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(FiveRoster::new, FiveRoster::load, null), NAME);
    }

    static FiveRoster load(CompoundTag tag, HolderLookup.Provider registries) {
        FiveRoster r = new FiveRoster();
        read(tag, MEMBERS, r.members);
        read(tag, PENDING, r.pendingPotentialChoice);
        read(tag, LIFTED, r.liftedCurses);
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
        write(tag, PENDING, pendingPotentialChoice);
        write(tag, LIFTED, liftedCurses);
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

    public Set<UUID> pendingPotentialChoice() {
        return Collections.unmodifiableSet(pendingPotentialChoice);
    }

    public void setPendingPotentialChoice(UUID id, boolean pending) {
        change(pendingPotentialChoice, id, pending);
    }

    public Set<UUID> liftedCurses() {
        return Collections.unmodifiableSet(liftedCurses);
    }

    public void setLiftedCurse(UUID id, boolean lifted) {
        change(liftedCurses, id, lifted);
    }
}
