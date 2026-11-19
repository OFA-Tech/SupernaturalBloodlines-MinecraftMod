package net.ofatech.supernaturalbloodlines.parts.species.npc;

import de.teamlapen.werewolves.api.entities.werewolf.WerewolfTransformable;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Keeps the ledger ({@link NpcIndex}) true to the world (R148). A member that leaves the world for good (discarded, not killed, not unloaded, not changing
 * dimension) is forgotten and its owner told, so a ghost entry never fills the caps. The Werewolves mod turns a wolf into a human (and back) by creating a NEW
 * entity beside the old one and discarding the old one: the bond is copied across (same token), so the member survives the transformation. A wolf that joins
 * a level and does not match the ledger is put right: an entry with no bond is removed, and a bond with no entry is taken off (the wolf was released while it was
 * not loaded, or its entry was lost: that member is freed).
 */
public final class NpcLedger {
    private static final String KEY = "supernaturalbloodlines.npc.";

    private NpcLedger() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide && e.getEntity() instanceof Mob m && e.getLevel() instanceof ServerLevel) {
                onJoin(m);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (!e.getLevel().isClientSide && e.getEntity() instanceof Mob m && e.getLevel() instanceof ServerLevel) {
                onLeave(m);
            }
        });
    }

    /** A mob that joins a level (a loaded chunk, a spawn): the ledger and the bond must agree. */
    public static void onJoin(Mob m) {
        ServerLevel level = (ServerLevel) m.level();
        NpcIndex index = NpcIndex.get(level);
        var bond = NpcMembers.bondOf(m);
        if (bond.isPresent() && index.of(m.getUUID()).isEmpty()) {
            // a bond with no entry: its owner freed it while it was out of reach (the entry goes at once, the bond stays on the unloaded wolf), or the ledger lost it.
            // Either way it is freed, never written down again: shortcut, a member whose entry was lost is freed, which is cheaper than a list of the released.
            if (bond.get().source() == NpcBond.Source.BROTHER) {
                NpcMembers.release(m);
            } else {
                NpcMembers.unbind(m); // a Tensura subordinate: NpcSubordinates looks at it next and joins it again if it still belongs
            }
        } else if (bond.isEmpty() && index.of(m.getUUID()).isPresent()) {
            index.remove(m.getUUID()); // an entry for a wolf that has no bond (it was replaced, or the bond was lost): a ghost
        }
    }

    /** A mob that leaves a level: unloading and a change of dimension keep it; a transformation moves it; anything else forgets it. */
    public static void onLeave(Mob m) {
        if (NpcMembers.bondOf(m).isEmpty()) {
            return;
        }
        Entity.RemovalReason reason = m.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) {
            return;
        }
        ServerLevel level = (ServerLevel) m.level();
        NpcIndex index = NpcIndex.get(level);
        if (index.of(m.getUUID()).isEmpty()) {
            return;
        }
        if (reason == Entity.RemovalReason.KILLED) {
            index.remove(m.getUUID()); // NpcOrders tells the owner of a death
            return;
        }
        Optional<Mob> successor = successor(level, m);
        if (successor.isPresent()) {
            carryOver(index, m, successor.get());
            return;
        }
        NpcBond bond = m.getData(SpeciesRegistries.NPC_BOND);
        String name = index.of(m.getUUID()).get().name();
        index.remove(m.getUUID());
        if (bond.source() == NpcBond.Source.BROTHER) {
            ServerPlayer owner = SireCall.player(level.getServer(), bond.owner());
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable(KEY + "lost", name));
            } else {
                PackIndex.get(level).addNotice(bond.owner(), KEY + "lost", name);
            }
        }
    }

    /**
     * The entity that took the place of {@code old} in a Werewolves transformation: a werewolf that can transform, of another type, a moment old, where the old one
     * stands and with no bond of its own.
     */
    public static Optional<Mob> successor(ServerLevel level, Mob old) {
        return level.getEntitiesOfClass(Mob.class, old.getBoundingBox().inflate(1.5),
                c -> c != old && c.isAlive() && c.getType() != old.getType() && c instanceof WerewolfTransformable && c.tickCount <= 1 && NpcMembers.bondOf(c).isEmpty())
                .stream().min(java.util.Comparator.comparingDouble(c -> c.distanceToSqr(old))).map(c -> c);
    }

    /** The member goes on in the new body: its bond, its name, its token. */
    static void carryOver(NpcIndex index, Mob old, Mob now) {
        now.setData(SpeciesRegistries.NPC_BOND, old.getData(SpeciesRegistries.NPC_BOND));
        now.setPersistenceRequired();
        if (old.hasCustomName()) {
            now.setCustomName(old.getCustomName());
            now.setCustomNameVisible(old.isCustomNameVisible());
        }
        index.rekey(old.getUUID(), now.getUUID());
        NpcMembers.touch(now);
    }
}
