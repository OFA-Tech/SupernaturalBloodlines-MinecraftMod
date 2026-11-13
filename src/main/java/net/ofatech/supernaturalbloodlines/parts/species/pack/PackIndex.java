package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
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
    private static final String FORMER = "former_alphas";
    private static final String DISSOLVED = "dissolved";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    // beta -> alpha, insertion order is the order they joined
    private final Map<UUID, UUID> alphaOf = new LinkedHashMap<>();
    private final Map<UUID, UUID> emissaryOf = new HashMap<>();
    private final Map<UUID, UUID> anchorOf = new HashMap<>();
    // players who lost the spark (stolen, passed on, given up): an offline Alpha is still an Alpha unless they are here
    private final Set<UUID> formerAlphas = new HashSet<>();
    // Betas of a pack that dissolved while they were away: they become Omegas at their next login
    private final Set<UUID> dissolved = new HashSet<>();

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
            if (PackAccess.isAlpha(change.after().wolf().rank())) {
                get(p.serverLevel()).clearFormerAlpha(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                login(p);
            }
        });
    }

    /**
     * Login: a Beta whose pack dissolved while they were away is an Omega now, and one whose Alpha lost the spark to
     * someone else (the index moved the pack, their own state still names the old Alpha) follows the index.
     */
    public static void login(ServerPlayer p) {
        PackIndex index = get(p.serverLevel());
        if (index.takeDissolved(p.getUUID())) {
            PackAccess.applyPack(p, PackEvent.LEFT_PACK, PackContext.defaults());
        }
        WolfState w = PackAccess.wolf(p);
        UUID indexed = index.alphaOf(p.getUUID()).orElse(null);
        if (w.packAlpha() != null && indexed != null && !indexed.equals(w.packAlpha()) && index.isFormerAlpha(w.packAlpha())) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().packAlpha(indexed).build()).build());
        }
        follow(p);
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

    /**
     * The new Alpha takes every Beta of the old one (and stops being a Beta); the old Alpha's emissary is dropped and
     * they are remembered as having lost the spark.
     */
    public void transfer(UUID oldAlpha, UUID newAlpha) {
        alphaOf.remove(newAlpha);
        alphaOf.replaceAll((beta, a) -> a.equals(oldAlpha) ? newAlpha : a);
        alphaOf.remove(oldAlpha);
        emissaryOf.remove(oldAlpha);
        formerAlphas.add(oldAlpha);
        formerAlphas.remove(newAlpha);
        setDirty();
    }

    /** Nobody can take the pack over (no Beta is around): its Betas become Omegas when they next log in. */
    public void dissolve(UUID alpha) {
        for (UUID beta : betasOf(alpha)) {
            alphaOf.remove(beta);
            dissolved.add(beta);
        }
        emissaryOf.remove(alpha);
        formerAlphas.add(alpha);
        setDirty();
    }

    /** True once, for a Beta whose pack dissolved while they were away. */
    public boolean takeDissolved(UUID beta) {
        boolean was = dissolved.remove(beta);
        if (was) {
            setDirty();
        }
        return was;
    }

    public boolean isFormerAlpha(UUID id) {
        return formerAlphas.contains(id);
    }

    public void markFormerAlpha(UUID id) {
        if (formerAlphas.add(id)) {
            setDirty();
        }
    }

    public void clearFormerAlpha(UUID id) {
        if (formerAlphas.remove(id)) {
            setDirty();
        }
    }

    public Optional<UUID> emissaryOf(UUID alpha) {
        return Optional.ofNullable(emissaryOf.get(alpha));
    }

    /** The Alpha who named {@code emissary}, if anyone did. */
    public Optional<UUID> emissaryAlpha(UUID emissary) {
        return emissaryOf.entrySet().stream().filter(e -> e.getValue().equals(emissary)).map(Map.Entry::getKey).findFirst();
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
        readIds(tag, FORMER, i.formerAlphas);
        readIds(tag, DISSOLVED, i.dissolved);
        return i;
    }

    private static void readIds(CompoundTag tag, String name, Set<UUID> into) {
        for (Tag t : tag.getList(name, Tag.TAG_INT_ARRAY)) {
            into.add(NbtUtils.loadUUID(t));
        }
    }

    private static void writeIds(CompoundTag tag, String name, Set<UUID> from) {
        ListTag list = new ListTag();
        from.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put(name, list);
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
        writeIds(tag, FORMER, formerAlphas);
        writeIds(tag, DISSOLVED, dissolved);
        return tag;
    }
}
