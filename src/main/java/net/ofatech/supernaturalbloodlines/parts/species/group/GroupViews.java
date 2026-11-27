package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.GroupWire;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesHub;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/** The group the viewer is in, as the hub draws it: only that group's own people, never another group's. */
public final class GroupViews {
    private GroupViews() {
    }

    public static GroupView of(ServerPlayer p) {
        PackIndex packs = PackIndex.get(p.serverLevel());
        UUID me = p.getUUID();
        if (!packs.inPack(me)) {
            return GroupView.none(Groups.derive(p));
        }
        UUID leader = packs.leaderOf(me);
        GroupIndex groups = GroupIndex.get(p.serverLevel());
        GroupType type = Groups.typeOfGroup(p.serverLevel(), leader);
        List<GroupView.Row> rows = new ArrayList<>();
        if (type == GroupType.PACK) {
            for (HubSocial.Member m : SpeciesHub.pack(p, p.server)) {
                rows.add(new GroupView.Row(GroupView.clip(m.name(), GroupWire.MAX_NAME), GroupView.clip(m.rank(), GroupWire.MAX_RANK), GroupRank.NONE, m.online(), m.id()));
            }
        } else {
            rows.add(row(p, leader, "leader", GroupRank.NONE));
            for (UUID id : packs.betasOf(leader)) {
                GroupRank rank = groups.rankOf(id, type);
                rows.add(row(p, id, rank != GroupRank.NONE ? rank.id() : type == GroupType.BROTHERHOOD && isPotential(p, id) ? "potential" : "member", rank));
            }
            for (UUID ally : packs.alliesOf(leader)) {
                rows.add(row(p, ally, "ally", GroupRank.NONE));
            }
        }
        GroupRank mine = groups.rankOf(me, type);
        byte viewer = me.equals(leader) ? GroupView.LEADER : mine == GroupRank.DAY_WALKER ? GroupView.DAY_WALKER : mine == GroupRank.ELDER ? GroupView.ELDER : GroupView.MEMBER;
        long since = groups.existingGroupOf(leader).map(groups::typeSince).orElse(0L);
        int days = since <= 0 ? 0 : (int) Math.max(0, (PackIndex.now() - since) / PackAbsence.DAY_MILLIS);
        String name = PackAccess.nameOf(p.server, leader);
        return new GroupView(type, true, viewer, GroupView.clip(name == null ? leader.toString().substring(0, 8) : name, GroupWire.MAX_NAME), days, rows);
    }

    /**
     * A brotherhood's member row says so for a Potential who is here: the one word the spec takes from a kind. Only the Potential and brothers who fit read
     * it (a prey member must not learn who will be the next hunter); somebody who is away is a Brother to everyone.
     */
    private static boolean isPotential(ServerPlayer viewer, UUID id) {
        if (!viewer.getUUID().equals(id) && !net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodScope.fits(viewer)) {
            return false;
        }
        ServerPlayer member = SireCall.player(viewer.server, id);
        return member != null && net.ofatech.supernaturalbloodlines.core.BloodlineAccess.get(member).species() == net.ofatech.supernaturalbloodlines.core.bloodline.Species.POTENTIAL;
    }

    private static GroupView.Row row(ServerPlayer viewer, UUID id, String rank, GroupRank groupRank) {
        String name = PackAccess.nameOf(viewer.server, id);
        return new GroupView.Row(GroupView.clip(name == null ? id.toString() : name, GroupWire.MAX_NAME), GroupView.clip(rank, GroupWire.MAX_RANK), groupRank,
                SireCall.player(viewer.server, id) != null, java.util.Optional.of(id));
    }
}
