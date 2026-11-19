package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Source;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcIndex;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * The Tensura bridge of the pack's NPC members (R149): a Tensura subordinate of a pack member, or of a Brother of a pack member, is itself a member of that
 * pack. It keeps Tensura's own orders (nothing here ever calls {@code setBehaviour}, {@code setOwnerCommand} or {@code setOrderedToSit}; our order tick skips it);
 * it gets the pack's Regeneration, the Roar and the Surge, the protection of friends, and counts for {@code npc_weight_tensura} toward the pack bonus. Only one
 * step down counts: the subordinate of a subordinate does not join. At most {@code max_tensura_members_per_pack} per pack. A subordinate leaves the moment its owner
 * is no longer in a pack, the owner changes, or {@code tensura_subordinates_join} is turned off: its bond is removed and nothing else about it is touched.
 * Checked when a subordinate joins a level and every 200 ticks for the loaded ones. 0 mixins.
 */
public final class NpcSubordinates {
    public static final int SCAN_PERIOD = 200;

    private NpcSubordinates() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide && e.getEntity() instanceof Mob m && m instanceof ISubordinate) {
                evaluate(m);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % SCAN_PERIOD == 0) {
                for (ServerLevel level : e.getServer().getAllLevels()) {
                    scan(level);
                }
            }
        });
    }

    /** Looks at every loaded subordinate of a level once: it joins, stays or leaves. */
    public static void scan(ServerLevel level) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob m && m instanceof ISubordinate && m.isAlive()) {
                evaluate(m);
            }
        }
    }

    /**
     * The player whose pack this subordinate belongs to now: its Tensura owner when that is a player in a pack, or the owner of the Brother that is its Tensura
     * owner; null when none. A subordinate of a Tensura member does not count (one step only).
     */
    @Nullable
    static UUID packOwnerOf(ServerLevel level, UUID tensuraOwner) {
        if (tensuraOwner == null) {
            return null;
        }
        PackIndex packs = PackIndex.get(level);
        if (packs.inPack(tensuraOwner)) {
            return tensuraOwner;
        }
        return NpcIndex.get(level).of(tensuraOwner).filter(e -> e.source() == Source.BROTHER && packs.inPack(e.owner())).map(NpcIndex.Entry::owner).orElse(null);
    }

    /** Joins, stays or leaves; returns whether the subordinate is a member afterwards. */
    public static boolean evaluate(Mob m) {
        if (!(m.level() instanceof ServerLevel level) || !(m instanceof ISubordinate sub)) {
            return false;
        }
        var existing = NpcMembers.bondOf(m);
        if (existing.isPresent() && existing.get().source() != Source.TENSURA) {
            return true; // a Brother that is also somebody's subordinate stays a Brother
        }
        UUID owner = SpeciesTensuraConfig.subordinatesJoin() && sub.isTame() ? packOwnerOf(level, sub.getOwnerUUID()) : null;
        if (owner == null) {
            if (existing.isPresent()) {
                NpcMembers.unbind(m);
            }
            return false;
        }
        if (existing.isPresent() && existing.get().owner().equals(owner)) {
            NpcMembers.touch(m);
            return true;
        }
        if (existing.isEmpty() && countInPack(level, owner) >= Math.max(0, SpeciesTensuraConfig.maxTensuraMembers())) {
            return false;
        }
        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
        NpcMembers.bind(m, ownerPlayer, NpcBond.tensura(owner, level.getGameTime()));
        return true;
    }

    private static int countInPack(ServerLevel level, UUID player) {
        int n = 0;
        NpcIndex index = NpcIndex.get(level);
        for (UUID id : PackIndex.get(level).pack(player)) {
            n += index.count(id, Source.TENSURA);
        }
        return n;
    }

    /** For tests: the bond a subordinate has, if any. */
    public static boolean isMember(Mob m) {
        return m.hasData(SpeciesRegistries.NPC_BOND) && m.getData(SpeciesRegistries.NPC_BOND).bonded();
    }
}
