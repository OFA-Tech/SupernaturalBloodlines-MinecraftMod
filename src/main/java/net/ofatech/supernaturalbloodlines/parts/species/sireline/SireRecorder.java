package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import com.mojang.authlib.GameProfile;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Writes down who sired whom. A player's sire lives in {@code BloodlineState.sire}, a creature's in the
 * {@code supernaturalbloodlines:sire} attachment; the {@link SirelineIndex} follows both. The first sire wins: nothing
 * here overwrites one. A player's index entry is kept by a {@link BloodlineAccess} listener, so a sire set by the rules
 * (a hybrid's completion) or cleared by them (a cure) is covered without a call from the rule's owner.
 */
public final class SireRecorder {
    private static final String PLAYER = "minecraft:player";
    private static final List<BiConsumer<UUID, LivingEntity>> LISTENERS = new CopyOnWriteArrayList<>();

    private SireRecorder() {
    }

    /** Called with (sire id, child) whenever a child gets a sire, whichever way (player or creature). */
    public static void addListener(BiConsumer<UUID, LivingEntity> listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void register() {
        BloodlineAccess.addListener(SireRecorder::onBloodlineChange);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(SireRecorder::onConversion);
        // last, so a death something else cancelled is not counted
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, SireRecorder::onDeath);
    }

    /** A sired creature that dies leaves the tree; a player stays (they come back, and their line goes on). */
    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!dead.level().isClientSide && !(dead instanceof Player) && dead.hasData(SpeciesRegistries.SIRE)) {
            SirelineIndex.get((ServerLevel) dead.level()).remove(dead.getUUID());
        }
    }

    /** @return false when the child already has a sire (the first wins), is the sire itself, or this is a client */
    public static boolean record(Entity sire, LivingEntity child) {
        return record(sire.getUUID(), nameOf(sire), child);
    }

    /** For a sire who is not here (offline): the id and the name to show for it. */
    public static boolean record(UUID sire, @Nullable String sireName, LivingEntity child) {
        return record(sire, sireName, child, typeOf(child), nameOf(child));
    }

    static boolean record(UUID sire, @Nullable String sireName, LivingEntity child, String childType, String childName) {
        if (child.level().isClientSide || sire.equals(child.getUUID())) {
            return false;
        }
        SirelineIndex index = SirelineIndex.get((ServerLevel) child.level());
        if (child instanceof ServerPlayer p) {
            BloodlineState s = BloodlineAccess.get(p);
            if (s.sire() != null) {
                return false;
            }
            // the bloodline listener indexes and notifies
            BloodlineAccess.set(p, s.toBuilder().sire(sire).build());
        } else {
            if (child.hasData(SpeciesRegistries.SIRE)) {
                return false;
            }
            child.setData(SpeciesRegistries.SIRE, sire);
            index.add(sire, child.getUUID(), childType, childName);
            fire(sire, child);
        }
        if (sireName != null) {
            index.setName(sire, sireName);
        }
        return true;
    }

    private static void fire(UUID sire, LivingEntity child) {
        for (BiConsumer<UUID, LivingEntity> l : LISTENERS) {
            try {
                l.accept(sire, child);
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.error("Sireline listener failed", e);
            }
        }
    }

    private static void onBloodlineChange(ServerPlayer p, BloodlineAccess.BloodlineChange change) {
        UUID before = change.before().sire();
        UUID after = change.after().sire();
        if (Objects.equals(before, after)) {
            return;
        }
        SirelineIndex index = SirelineIndex.get(p.serverLevel());
        if (after == null) {
            index.remove(p.getUUID());
        } else if (!after.equals(p.getUUID())) {
            index.add(after, p.getUUID(), PLAYER, nameOf(p));
            String sireName = nameOfId(p.server, after);
            if (sireName != null) {
                index.setName(after, sireName);
            }
            fire(after, p);
        }
    }

    /** The infector's name: the live one when the server knows it, else the one noted at the infection. */
    static String knownName(MinecraftServer server, InfectionSire inf) {
        String live = nameOfId(server, inf.sire());
        return live != null ? live : inf.name();
    }

    /** A player's name, online or from the profile cache; null when the server does not know it. */
    @Nullable
    static String nameOfId(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        GameProfileCache cache = server.getProfileCache();
        return cache == null ? null : cache.get(id).map(GameProfile::getName).orElse(null);
    }

    /** Idempotent: heals the index if it ever missed a change (lost file, state set while the part was off) and refreshes the name. */
    public static void sync(ServerPlayer p) {
        SirelineIndex index = SirelineIndex.get(p.serverLevel());
        UUID sire = BloodlineAccess.get(p).sire();
        if (sire == null) {
            index.remove(p.getUUID());
        } else if (!sire.equals(p.getUUID()) && !index.sireOf(p.getUUID()).map(sire::equals).orElse(false)) {
            index.add(sire, p.getUUID(), PLAYER, nameOf(p));
        } else {
            index.setName(p.getUUID(), nameOf(p));
        }
    }

    /**
     * Vampirism replaces a creature by its converted form (or back, when cured) through a conversion event. The infector
     * remembered on the old creature becomes the sire of the new one; a cured creature leaves the index.
     */
    private static void onConversion(LivingConversionEvent.Post e) {
        LivingEntity old = e.getEntity();
        LivingEntity converted = e.getOutcome();
        if (old.level().isClientSide) {
            return;
        }
        if (converted instanceof IConvertedCreature<?>) {
            InfectionSire pending = InfectionTracker.pending(old);
            // the special handler copies the old creature's data into the new one: the pending record must not follow
            converted.removeData(SpeciesRegistries.INFECTION_SIRE);
            if (pending != null) {
                record(pending.sire(), knownName(((ServerLevel) old.level()).getServer(), pending), converted, typeOf(old), nameOf(old));
            }
        } else if (old instanceof IConvertedCreature<?> && old.hasData(SpeciesRegistries.SIRE)) {
            SirelineIndex.get((ServerLevel) old.level()).remove(old.getUUID());
            converted.removeData(SpeciesRegistries.SIRE);
        }
    }

    private static String typeOf(LivingEntity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
    }

    static String nameOf(Entity e) {
        return e instanceof ServerPlayer p ? p.getGameProfile().getName() : e.getName().getString();
    }
}
