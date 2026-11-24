package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;

/** The one way the lead of a group moves to another member: the absence rule, a coven election and (20c) the Game of Kings all end here. */
public final class GroupLead {
    private GroupLead() {
    }

    /**
     * {@code next} leads, {@code old} stays in the group as the next rank down (a pack's second Alpha, a clan's Day Walker, a coven's Elder; a brotherhood's
     * ex-leader holds none). The people told, and how, are the caller's.
     */
    public static void handOver(MinecraftServer server, PackIndex index, UUID old, UUID next, GroupType type) {
        List<UUID> members = index.betasOf(old);
        index.promote(old, next);
        if (type == GroupType.PACK) {
            for (UUID id : members) {
                if (!id.equals(next)) {
                    SparkHandler.repoint(server, id, next);
                }
            }
        } else {
            GroupIndex groups = GroupIndex.get(server.overworld());
            if (type == GroupType.CLAN) {
                groups.setRank(old, GroupRank.DAY_WALKER);
            } else if (type == GroupType.COVEN) {
                groups.setRank(old, GroupRank.ELDER);
            } else {
                groups.clearRank(old, type);
            }
            groups.clearRank(next, type);
        }
        PartyLink.leadershipChanged(server, next);
    }
}