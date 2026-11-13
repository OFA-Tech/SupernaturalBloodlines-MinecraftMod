package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
 * the bloodline state (set when the sire is recorded, cleared when the sire is), a creature's follows {@link SireRecorder}. A Tensura
 * owner the child already has is never replaced, and an owner is only cleared while it is still the sire this mod linked.
 */
public final class SubordinateLink {
    private static final Set<UUID> WARNED = new HashSet<>();

    private SubordinateLink() {}

    public static void register() {
        BloodlineAccess.addListener(SubordinateLink::onChange);
        SireRecorder.addListener(SubordinateLink::onSired);
        // catches a sire that was set while the part (or Tensura) was not there
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
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
        UUID before = change.before().sire();
        if (!Objects.equals(before, change.after().sire()) && before != null) {
            unlink(p, before);
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
     * and no Tensura owner gets the sire as owner. A player who is not (a cured player can keep a stale sire) loses the link again, but
     * only while the owner is that sire.
     */
    public static void sync(ServerPlayer p) {
        UUID sire = BloodlineAccess.get(p).sire();
        if (sire == null || !SpeciesTensuraConfig.sireLink()) {
            return;
        }
        if (Supernatural.is(p)) {
            link(p, sire);
        } else {
            unlink(p, sire);
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
            if (WARNED.add(child.getUUID())) {
                SupernaturalBloodlines.LOGGER.info("species_tensura: {} keeps its Tensura owner {}, sire {} is not linked", child.getUUID(), owner, sire);
            }
            return false;
        }
        existence.setPermanentOwner(sire);
        existence.markDirty();
        return sire.equals(existence.getPermanentOwner());
    }

    /** Clears the owner only while it is still the sire this bond linked: a different owner is Tensura's own. */
    static void unlink(LivingEntity child, UUID sire) {
        IExistence existence = TensuraStorages.getExistenceFrom(child);
        if (sire.equals(existence.getPermanentOwner())) {
            existence.setPermanentOwner(null);
            existence.markDirty();
        }
    }
}
