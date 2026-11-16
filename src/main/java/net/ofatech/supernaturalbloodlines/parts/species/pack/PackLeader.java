package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * Who leads an Alpha Pack (plan 14). The index keeps each member's last seen strength; this reads it fresh for a player who
 * is online, asks the index to recalculate and, when the leadership moved, points every online Beta at the new leader.
 */
public final class PackLeader {
    private PackLeader() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer p && !p.level().isClientSide) {
                refresh(p);
            }
        });
    }

    /** The strength this player has right now: an Alpha of the werewolves by rank and level, 0 for a Beta or Omega, -1 for anyone who is no werewolf (a Packmate). */
    public static int strengthNow(ServerPlayer p) {
        if (PackAccess.wolf(p).lineage() == net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage.NONE) {
            return -1;
        }
        if (!PackAccess.isAlpha(PackAccess.wolf(p).rank()) || !VampirismSupernatural.isWerewolf(p)) {
            return 0;
        }
        return PackLeadership.strength(PackAccess.wolf(p).rank(), VampirismAPI.factionPlayerHandler(p).getCurrentLevel());
    }

    /** Records the player's strength and recalculates their pack's leader. */
    public static void refresh(ServerPlayer p) {
        if (p.level().isClientSide) {
            return;
        }
        PackIndex index = PackIndex.get(p.serverLevel());
        index.setStrength(p.getUUID(), strengthNow(p));
        recalc(p, p.getUUID());
    }

    /** Recalculates the pack {@code member} is in; true when the leader changed. */
    public static boolean recalc(ServerPlayer any, UUID member) {
        PackIndex index = PackIndex.get(any.serverLevel());
        UUID old = index.leaderOf(member);
        List<UUID> before = index.betasOf(old);
        var next = index.recalc(member);
        if (next.isEmpty()) {
            return false;
        }
        // the old leader and every Beta now follow the new one
        for (UUID id : before) {
            SparkHandler.repoint(any.server, id, next.get());
        }
        return true;
    }
}
