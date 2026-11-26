package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.LeadByMark;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** R291: the moment an online brother's mark is strictly higher than the leader's, the lead moves to them, through {@link GroupLead#handOver}. */
public final class BrotherhoodLead {
    private BrotherhoodLead() {
    }

    /** Checks the group {@code anyMember} belongs to (as a leader or a member); true when the lead moved. */
    public static boolean check(MinecraftServer server, UUID anyMember) {
        if (!GroupsConfig.leadByMark() || !GroupsConfig.brotherhoodPerksOn()) {
            return false;
        }
        ServerLevel level = server.overworld();
        PackIndex packs = PackIndex.get(level);
        UUID leader = packs.leaderOf(anyMember);
        if (!BrotherhoodScope.isBrotherhood(level, leader) || packs.betasOf(leader).isEmpty()) {
            return false;
        }
        ServerPlayer leaderOnline = SireCall.player(server, leader);
        // an absent leader whose mark was never recorded (a group from before this update) is not a 0 to be passed
        if (leaderOnline == null && GroupIndex.get(level).markOf(leader).isEmpty()) {
            return false;
        }
        int leaderMark = leaderOnline != null ? BrotherhoodMarks.of(leaderOnline) : BrotherhoodMarks.recorded(level, leader);
        List<LeadByMark.Candidate> candidates = new ArrayList<>();
        for (UUID id : packs.betasOf(leader)) {
            ServerPlayer p = SireCall.player(server, id);
            boolean online = p != null;
            candidates.add(new LeadByMark.Candidate(id, online ? BrotherhoodMarks.of(p) : BrotherhoodMarks.recorded(level, id), online,
                    online && BrotherhoodScope.appliesTo(level, leader, p), Codes.isBreaker(server, id), packs.inPackSince(id).orElse(Long.MAX_VALUE)));
        }
        Optional<UUID> next = LeadByMark.pick(leader, leaderMark, candidates);
        if (next.isEmpty()) {
            return false;
        }
        List<UUID> members = packs.betasOf(leader);
        GroupLead.handOver(server, packs, leader, next.get(), GroupType.BROTHERHOOD);
        String name = BrotherhoodText.nameOf(server, next.get());
        String unknown = name == null ? ".someone" : "";
        String shown = name == null ? "" : name;
        for (UUID id : members) {
            PackMembers.tell(server, id, BrotherhoodText.key(id.equals(next.get()) ? "lead_now" : "lead_lost" + unknown), id.equals(next.get()) ? "" : shown);
        }
        PackMembers.tell(server, leader, BrotherhoodText.key("lead_you" + unknown), shown);
        BrotherhoodViews.push(server, next.get());
        return true;
    }
}
