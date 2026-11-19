package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Source;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * The pack's NPC members (R148, R149): the one place that binds a mob to a player, releases it, finds a player's members and weighs them. It touches no
 * Werewolves or Tensura class, so the pack bonus can use it with neither mod loaded.
 */
public final class NpcMembers {
    private static final String KEY = "supernaturalbloodlines.npc.";

    private NpcMembers() {
    }

    public static Optional<NpcBond> bondOf(Entity e) {
        return e instanceof Mob m && m.hasData(SpeciesRegistries.NPC_BOND) && m.getData(SpeciesRegistries.NPC_BOND).bonded()
                ? Optional.of(m.getData(SpeciesRegistries.NPC_BOND)) : Optional.empty();
    }

    /** The live mob of a member, in whatever dimension it is loaded; empty when its chunk is not loaded. */
    public static Optional<Mob> live(MinecraftServer server, UUID npc) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(npc) instanceof Mob m && m.isAlive() && bondOf(m).isPresent()) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /**
     * Binds {@code mob} to {@code owner}: the bond on the entity, the ledger entry, a mob that stays, one that is no longer a wild wolf of a night's pack.
     * A Brother is named "(owner)'s Brother" unless it already has a name.
     */
    public static void bind(Mob mob, ServerPlayer ownerPlayer, NpcBond bond) {
        ServerLevel level = (ServerLevel) mob.level();
        mob.setData(SpeciesRegistries.NPC_BOND, bond);
        mob.removeData(SpeciesRegistries.NPC_MARK);
        if (bond.source() == Source.BROTHER) {
            // a wolf taken in is no longer a wild wolf of a night's pack: it stays, and drops its fight and its feral bonus. A Tensura subordinate is left to Tensura.
            NpcPackBrain.release(mob);
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.setPersistenceRequired();
            if (!mob.hasCustomName()) {
                mob.setCustomName(Component.translatable(KEY + "brother_name", ownerPlayer.getName()));
            }
        }
        touch(mob);
    }

    /** Takes the bond off a mob and forgets it; the mob goes back to the wild (and leaves with a night like any wild wolf). */
    public static void release(Mob mob) {
        ServerLevel level = (ServerLevel) mob.level();
        NpcIndex.get(level).remove(mob.getUUID());
        mob.removeData(SpeciesRegistries.NPC_BOND);
        mob.setTarget(null);
        mob.setData(SpeciesRegistries.NPC_MARK, NpcMark.wild(0, Math.floorDiv(level.getDayTime(), 24000L)));
    }

    /** Takes the bond off a mob and forgets it, touching nothing else: a Tensura subordinate that is no longer a member. */
    public static void unbind(Mob mob) {
        NpcIndex.get((ServerLevel) mob.level()).remove(mob.getUUID());
        mob.removeData(SpeciesRegistries.NPC_BOND);
    }

    /** Writes what is seen of a member now into the ledger (name, kind, place, time). */
    public static void touch(Mob mob) {
        NpcBond bond = mob.getData(SpeciesRegistries.NPC_BOND);
        if (!bond.bonded() || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        String name = mob.hasCustomName() ? mob.getCustomName().getString() : mob.getType().getDescription().getString();
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        NpcIndex.get(level).put(mob.getUUID(), bond.owner(), bond.source(), name, type, level.dimension().location().toString(), mob.blockPosition().getX(),
                mob.blockPosition().getY(), mob.blockPosition().getZ(), level.getGameTime());
    }

    /** All members of every player in {@code pack}, from the ledger (loaded or not). */
    public static List<NpcIndex.Entry> ofPack(ServerLevel level, List<UUID> pack) {
        NpcIndex index = NpcIndex.get(level);
        List<NpcIndex.Entry> out = new ArrayList<>();
        for (UUID id : pack) {
            out.addAll(index.ownedBy(id));
        }
        return out;
    }

    /** Brothers the player has, and the Brothers the whole pack has: what the recruiting caps count. */
    public static int brothers(ServerLevel level, UUID owner) {
        return NpcIndex.get(level).count(owner, Source.BROTHER);
    }

    public static int brothersInPack(ServerLevel level, UUID player) {
        int n = 0;
        for (UUID id : PackIndex.get(level).pack(player)) {
            n += brothers(level, id);
        }
        return n;
    }

    /** What a member counts for toward the pack bonus. */
    public static double weight(NpcBond bond) {
        SpeciesConfig.MemberTuning t = SpeciesConfig.members();
        return NpcRules.weight(bond.source(), t.weightBrother(), t.weightTensura());
    }

    /**
     * The loaded members of every player in {@code alpha}'s pack that are within {@code range} blocks of the Alpha in the Alpha's dimension: who the pack
     * bonus, the Roar and the Surge reach.
     */
    public static List<Mob> near(ServerPlayer alpha, double range) {
        List<Mob> out = new ArrayList<>();
        ServerLevel level = alpha.serverLevel();
        for (NpcIndex.Entry e : ofPack(level, PackIndex.get(level).pack(alpha.getUUID()))) {
            if (level.getEntity(e.npc()) instanceof Mob m && m.isAlive() && bondOf(m).isPresent() && alpha.distanceToSqr(m) <= range * range) {
                out.add(m);
            }
        }
        return out;
    }

    public static Optional<ServerPlayer> owner(MinecraftServer server, UUID owner) {
        return Optional.ofNullable(SireCall.player(server, owner));
    }

    /** What a member does now: its order, or REST while its owner is offline or dead. */
    public static Order effectiveOrder(MinecraftServer server, NpcBond bond) {
        ServerPlayer o = SireCall.player(server, bond.owner());
        return NpcRules.effectiveOrder(bond.order(), o != null, o != null && o.isAlive() && !o.isSpectator());
    }
}
