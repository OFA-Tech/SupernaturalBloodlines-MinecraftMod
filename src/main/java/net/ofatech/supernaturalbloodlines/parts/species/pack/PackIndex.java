package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Who is in whose pack, online or not (a pack survives its Alpha logging out), plus each Alpha's emissary and each
 * werewolf's anchor. The pack of a Beta also lives in {@code WolfState.packAlpha}; a {@link BloodlineAccess} listener keeps
 * this index in step with it, so the rules' own writes are covered.
 */
public final class PackIndex extends SavedData {
    private static final String NAME = "supernaturalbloodlines_packs";
    private static final String MEMBERS = "members";
    private static final String EMISSARIES = "emissaries";
    private static final String ANCHORS = "anchors";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    // beta -> alpha, insertion order is the order they joined
    private final Map<UUID, UUID> alphaOf = new LinkedHashMap<>();
    private final Map<UUID, UUID> emissaryOf = new HashMap<>();
    private final Map<UUID, UUID> anchorOf = new HashMap<>();

    PackIndex() {
    }

    /** One index per world: always the overworld's storage, whichever level is passed. */
    public static PackIndex get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(PackIndex::new, PackIndex::load, null), NAME);
    }

    /** Keeps the index in step with {@code WolfState.packAlpha}: on every change, and at login (heals a lost file). */
    public static void register() {
        BloodlineAccess.addListener((p, change) -> {
            if (!Objects.equals(change.before().wolf().packAlpha(), change.after().wolf().packAlpha())) {
                follow(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                follow(p);
            }
        });
    }

    private static void follow(ServerPlayer p) {
        PackIndex index = get(p.serverLevel());
        UUID alpha = PackAccess.wolf(p).packAlpha();
        if (!Objects.equals(index.alphaOf(p.getUUID()).orElse(null), alpha)) {
            index.leave(p.getUUID());
            if (alpha != null) {
                index.join(alpha, p.getUUID());
            }
        }
    }

    public Optional<UUID> alphaOf(UUID beta) {
        return Optional.ofNullable(alphaOf.get(beta));
    }

    public List<UUID> betasOf(UUID alpha) {
        List<UUID> out = new ArrayList<>();
        alphaOf.forEach((beta, a) -> {
            if (a.equals(alpha)) {
                out.add(beta);
            }
        });
        return out;
    }

    /** @return false when the beta already has an Alpha (a pack is never switched silently) or is the Alpha itself */
    public boolean join(UUID alpha, UUID beta) {
        if (alpha.equals(beta) || alphaOf.containsKey(beta)) {
            return false;
        }
        alphaOf.put(beta, alpha);
        setDirty();
        return true;
    }

    public void leave(UUID beta) {
        if (alphaOf.remove(beta) != null) {
            setDirty();
        }
    }

    /** The new Alpha takes every Beta of the old one (and stops being a Beta); the old Alpha's emissary is dropped. */
    public void transfer(UUID oldAlpha, UUID newAlpha) {
        alphaOf.remove(newAlpha);
        alphaOf.replaceAll((beta, a) -> a.equals(oldAlpha) ? newAlpha : a);
        alphaOf.remove(oldAlpha);
        emissaryOf.remove(oldAlpha);
        setDirty();
    }

    public Optional<UUID> emissaryOf(UUID alpha) {
        return Optional.ofNullable(emissaryOf.get(alpha));
    }

    public void setEmissary(UUID alpha, @Nullable UUID emissary) {
        put(emissaryOf, alpha, emissary);
    }

    public Optional<UUID> anchorOf(UUID member) {
        return Optional.ofNullable(anchorOf.get(member));
    }

    public void setAnchor(UUID member, @Nullable UUID anchor) {
        put(anchorOf, member, anchor);
    }

    private void put(Map<UUID, UUID> map, UUID key, @Nullable UUID value) {
        if (value == null ? map.remove(key) != null : !value.equals(map.put(key, value))) {
            setDirty();
        }
    }

    static PackIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        PackIndex i = new PackIndex();
        read(tag, MEMBERS, i.alphaOf);
        read(tag, EMISSARIES, i.emissaryOf);
        read(tag, ANCHORS, i.anchorOf);
        return i;
    }

    private static void read(CompoundTag tag, String name, Map<UUID, UUID> into) {
        for (Tag t : tag.getList(name, Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            into.put(NbtUtils.loadUUID(e.get(KEY)), NbtUtils.loadUUID(e.get(VALUE)));
        }
    }

    private static void write(CompoundTag tag, String name, Map<UUID, UUID> from) {
        ListTag list = new ListTag();
        from.forEach((k, v) -> {
            CompoundTag e = new CompoundTag();
            e.put(KEY, NbtUtils.createUUID(k));
            e.put(VALUE, NbtUtils.createUUID(v));
            list.add(e);
        });
        tag.put(name, list);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        write(tag, MEMBERS, alphaOf);
        write(tag, EMISSARIES, emissaryOf);
        write(tag, ANCHORS, anchorOf);
        return tag;
    }
}
