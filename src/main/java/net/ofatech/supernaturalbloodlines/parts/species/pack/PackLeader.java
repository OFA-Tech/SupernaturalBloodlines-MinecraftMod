package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * What an Alpha is worth when a pack has to pick a new leader (plan 14, owner). The index keeps each member's last seen strength;
 * this reads it fresh for a player who is online. **It never moves the leadership:** a joining Alpha, or one who levels up, does
 * not take over. The strongest is only the successor when the leader leaves, loses the spark or gives it up; otherwise the lead
 * changes hands in a {@link PackDuel}.
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

    /** Records the player's strength. The leader stays the leader. */
    public static void refresh(ServerPlayer p) {
        if (!p.level().isClientSide) {
            PackIndex.get(p.serverLevel()).setStrength(p.getUUID(), strengthNow(p));
        }
    }
}