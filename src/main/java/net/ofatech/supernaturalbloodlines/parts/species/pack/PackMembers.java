package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Taking someone out of a pack, and telling them: now when they are online, at their next login when they are not. */
public final class PackMembers {
    private PackMembers() {
    }

    /**
     * Removes {@code member} from their pack. A werewolf Beta becomes an Omega now, or at their next login when away.
     * @param reasonKey a lang key taking one argument ({@code arg}); it is shown now, or kept for the next login
     * @return false if they were in no pack
     */
    public static boolean remove(MinecraftServer server, UUID member, String reasonKey, String arg) {
        return remove(server, member, reasonKey, arg, false);
    }

    /** {@link #remove(MinecraftServer, UUID, String, String)}; {@code voluntary} is somebody who left the pack's party themselves, which is leaving by choice. */
    public static boolean remove(MinecraftServer server, UUID member, String reasonKey, String arg, boolean voluntary) {
        PackIndex index = PackIndex.get(server.overworld());
        UUID leader = index.leaderOf(member);
        if (!index.kick(member, voluntary)) {
            return false;
        }
        departed(server, leader, member, false);
        ServerPlayer online = SireCall.player(server, member);
        if (online != null) {
            WolfState w = PackAccess.wolf(online);
            if (w.lineage() != Lineage.NONE && !PackAccess.isAlpha(w.rank())) {
                PackAccess.applyPack(online, PackEvent.LEFT_PACK, PackContext.defaults());
            }
            // done now: left alone it would take them out of the next pack they join
            index.takeDissolved(member);
            online.sendSystemMessage(Component.translatable(reasonKey, arg));
        } else {
            index.addNotice(member, reasonKey, arg);
        }
        return true;
    }

    static void departed(MinecraftServer server, UUID leader, UUID member, boolean tellCircle) {
        var groups = net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.get(server.overworld());
        groups.clearRanks(member);
        net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenGovernance.memberDeparted(server, leader, member);
        net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.dropMember(server, member, tellCircle);
        ServerPlayer online = SireCall.player(server, member);
        if (online != null) {
            net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.changed(online);
        }
    }

    /** Tells {@code id} now when they are online, at their next login when not. */
    public static void tell(MinecraftServer server, UUID id, String key, String arg) {
        ServerPlayer online = SireCall.player(server, id);
        if (online != null) {
            online.sendSystemMessage(Component.translatable(key, arg));
        } else {
            PackIndex.get(server.overworld()).addNotice(id, key, arg);
        }
    }

    /** Tells each of {@code ids} that their pack now follows {@code leaderName}: at once when online, at their next login when not. */
    public static void moved(MinecraftServer server, java.util.Collection<UUID> ids, UUID newLeader) {
        PackIndex index = PackIndex.get(server.overworld());
        String name = PackAccess.nameOf(server, newLeader);
        String shown = name != null ? name : newLeader.toString().substring(0, 8);
        for (UUID id : ids) {
            if (id.equals(newLeader)) {
                continue;
            }
            ServerPlayer online = SireCall.player(server, id);
            if (online != null) {
                online.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.now_follows", shown));
            } else {
                index.addNotice(id, "supernaturalbloodlines.pack.now_follows", shown);
            }
        }
    }
}
