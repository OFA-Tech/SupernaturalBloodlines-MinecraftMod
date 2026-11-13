package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;

/**
 * R101: a sire is their sired's Tensura permanent owner, so Tensura treats the sired as the sire's subordinate. A player's link follows
 * the bloodline state (set when the sire is recorded, cleared when the sire is), a creature's follows {@link SireRecorder}.
 * <p>
 * The linked sire is also written to the child's persistent data ({@link #KEY}). An owner is cleared only while it still equals that
 * stored sire: an owner Tensura set (a naming, a taming) or one the child already had is never ours to remove, and is never
 * overwritten either. The link is dropped when the sire is gone, the child is no longer supernatural, or the switch is off; the switch
 * is read when a player logs in or their bloodline changes, so turn it off and have everyone log in once before disabling the part.
 * Nothing runs while the whole part is off, so its links stay.
 */
public final class SubordinateLink {
    /** The sire this mod made the child's owner; absent when the owner (if any) is not ours. */
    public static final String KEY = SupernaturalBloodlines.MODID + ":linked_sire";
    private static final int MAX_WARNED = 1024;
    private static final Set<UUID> WARNED = new HashSet<>();

    private SubordinateLink() {}

    public static void register() {
        BloodlineAccess.addListener(SubordinateLink::onChange);
        SireRecorder.addListener(SubordinateLink::onSired);
        // catches a sire set while the part (or Tensura) was not there, and applies a switch that was turned off
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> WARNED.remove(e.getEntity().getUUID()));
        // persistent data is not carried over a death by itself
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone e) -> {
            if (e.getOriginal().getPersistentData().hasUUID(KEY)) {
                e.getEntity().getPersistentData().putUUID(KEY, e.getOriginal().getPersistentData().getUUID(KEY));
            }
        });
        // a plain vampire is supernatural through its faction: joining or leaving it moves the bond
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer p) {
                sync(p);
            }
        });
    }

    private static void onChange(ServerPlayer p, BloodlineAccess.BloodlineChange change) {
        if (!Objects.equals(change.before().sire(), change.after().sire())) {
            unlink(p);
        }
        sync(p);
    }

    private static void onSired(UUID sire, LivingEntity child) {
        if (child instanceof Player || !SpeciesTensuraConfig.sireLink() || !link(child, sire)) {
            return;
        }
        // a tamable Tensura creature also becomes the sire's tamed follower, when the sire is here to be the owner
        if (child instanceof ISubordinate sub && !sub.isTame()) {
            ServerPlayer online = SireCall.player(child.getServer(), sire);
            if (online != null) {
                sub.tame(online);
            }
        }
    }

    /**
     * Idempotent. A player with a sire who is still supernatural (a species, a vampire or werewolf, a Tensura undead or daemon race)
     * and no Tensura owner gets the sire as owner. With no sire, no longer supernatural (a cured player can keep a stale sire), or the
     * switch off, the link this mod made is taken away again.
     */
    public static void sync(ServerPlayer p) {
        UUID sire = BloodlineAccess.get(p).sire();
        if (sire != null && SpeciesTensuraConfig.sireLink() && Supernatural.is(p)) {
            link(p, sire);
        } else {
            unlink(p);
        }
    }

    /** @return whether the sire is the child's permanent owner now */
    static boolean link(LivingEntity child, UUID sire) {
        IExistence existence = TensuraStorages.getExistenceFrom(child);
        UUID owner = existence.getPermanentOwner();
        if (sire.equals(owner)) {
            return true;
        }
        if (owner != null) {
            if (WARNED.size() >= MAX_WARNED) {
                WARNED.clear();
            }
            if (WARNED.add(child.getUUID())) {
                SupernaturalBloodlines.LOGGER.info("species_tensura: {} keeps its Tensura owner {}, sire {} is not linked", child.getUUID(), owner, sire);
            }
            return false;
        }
        existence.setPermanentOwner(sire);
        existence.markDirty();
        if (!sire.equals(existence.getPermanentOwner())) {
            return false;
        }
        child.getPersistentData().putUUID(KEY, sire);
        return true;
    }

    /** Clears the owner only while it still equals the sire this mod linked; forgets the stored sire either way. */
    static void unlink(LivingEntity child) {
        CompoundTag data = child.getPersistentData();
        if (!data.hasUUID(KEY)) {
            return;
        }
        UUID linked = data.getUUID(KEY);
        data.remove(KEY);
        IExistence existence = TensuraStorages.getExistenceFrom(child);
        if (linked.equals(existence.getPermanentOwner())) {
            existence.setPermanentOwner(null);
            existence.markDirty();
        }
    }
}