package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanRanks;
import net.ofatech.supernaturalbloodlines.core.group.clan.ExileRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupText;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.jetbrains.annotations.Nullable;

/** The clan's discipline (R270, R276): the leader promotes, demotes and exiles; the clock promotes a Night Walker after the days. Every action re-checks type, switch and rank. */
public final class ClanDiscipline {
    /** The mark a demoted member carries so the clock never promotes them again: a cooldown key of the clan's group, with no end. */
    private static final String NO_PROMOTE = "nodw:";

    /** The cooldown key of a member the head demoted; the one place the key is spelled. */
    public static String noPromoteKey(UUID member) {
        return NO_PROMOTE + member;
    }

    /** A member left or was removed: the mark that kept the clock from promoting them goes with them. Called from the one place every departure goes through. */
    public static void memberDeparted(GroupIndex groups, UUID leader, UUID member) {
        groups.existingGroupOf(leader).ifPresent(g -> groups.clearCooldown(g, noPromoteKey(member)));
    }

    private ClanDiscipline() {
    }

    private record Ctx(ServerPlayer p, MinecraftServer server, ClanScope.Clan clan, PackIndex index, GroupIndex groups) {
        boolean leads() {
            return p.getUUID().equals(clan.leader());
        }

        boolean isLeader(UUID target) {
            return target != null && target.equals(clan.leader());
        }

        GroupRank rankOf(UUID target) {
            return target == null ? GroupRank.NONE : groups.rankOf(target, GroupType.CLAN);
        }

        List<UUID> everyone() {
            return index.roster(clan.leader());
        }
    }

    /** The refusal when the player is not in a clan or clans are off, else null. */
    @Nullable
    public static Component gate(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        PackIndex packs = PackIndex.get(level);
        if (!packs.inPack(p.getUUID())) {
            return GroupText.error(Groups.typeOf(p), "no_group");
        }
        GroupType type = Groups.typeOfGroup(level, packs.leaderOf(p.getUUID()));
        if (ClanScope.of(level, p.getUUID()) == null) {
            return GroupText.error(type, "wrong_type");
        }
        return GroupsConfig.clanPerksOn() ? null : GroupText.error(GroupType.CLAN, "group_off");
    }

    private static Ctx ctx(ServerPlayer p) {
        return new Ctx(p, p.server, ClanScope.of(p.serverLevel(), p.getUUID()), PackIndex.get(p.serverLevel()), GroupIndex.get(p.serverLevel()));
    }

    public static String name(MinecraftServer server, UUID id) {
        String n = PackAccess.nameOf(server, id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    @Nullable
    private static UUID match(Ctx c, String word) {
        return c.everyone().stream().filter(id -> id.toString().equalsIgnoreCase(word) || name(c.server, id).equalsIgnoreCase(word)).findFirst().orElse(null);
    }

    /** The names a discipline command may take: the members of the player's clan. */
    public static List<String> namesFor(ServerPlayer p) {
        if (ClanScope.of(p.serverLevel(), p.getUUID()) == null) {
            return List.of();
        }
        Ctx c = ctx(p);
        return c.everyone().stream().filter(id -> !id.equals(p.getUUID())).map(id -> name(p.server, id)).toList();
    }

    private static Component refusal(ClanRanks.Refusal r) {
        return ClanText.error(switch (r) {
            case NOT_LEADER -> "leader_only";
            case IS_LEADER -> "leader_target";
            case NOT_A_MEMBER -> "not_member";
            case ALREADY_DAY_WALKER -> "already_day_walker";
            case NOT_A_DAY_WALKER -> "not_day_walker";
        });
    }

    private static void refresh(MinecraftServer server, UUID id) {
        ServerPlayer online = SireCall.player(server, id);
        if (online != null) {
            HubRequests.changed(online);
        }
    }

    @Nullable
    public static Component promote(ServerPlayer p, String word) {
        Component gate = gate(p);
        if (gate != null) {
            return gate;
        }
        Ctx c = ctx(p);
        UUID target = match(c, word);
        Optional<ClanRanks.Refusal> no = ClanRanks.mayPromote(c.leads(), c.rankOf(target), c.isLeader(target), target != null);
        if (no.isPresent()) {
            return refusal(no.get());
        }
        if (!ClanScope.mayBeDayWalker(p.serverLevel(), target)) {
            return ClanText.error("cannot_promote");
        }
        c.groups().setRank(target, GroupRank.DAY_WALKER);
        PackMembers.tell(c.server(), target, ClanText.key("promoted"), name(c.server(), p.getUUID()));
        p.sendSystemMessage(ClanText.of("promoted_ok", name(c.server(), target)));
        refresh(c.server(), target);
        HubRequests.changed(p);
        return null;
    }

    @Nullable
    public static Component demote(ServerPlayer p, String word) {
        Component gate = gate(p);
        if (gate != null) {
            return gate;
        }
        Ctx c = ctx(p);
        UUID target = match(c, word);
        Optional<ClanRanks.Refusal> no = ClanRanks.mayDemote(c.leads(), c.rankOf(target), c.isLeader(target), target != null);
        if (no.isPresent()) {
            return refusal(no.get());
        }
        Component locked = Contests.lockRefusal(p.serverLevel(), c.clan().group(), target);
        if (locked != null) {
            return locked;
        }
        c.groups().clearRank(target, GroupType.CLAN);
        c.groups().setCooldown(c.clan().group(), noPromoteKey(target), Long.MAX_VALUE);
        PackMembers.tell(c.server(), target, ClanText.key("demoted"), name(c.server(), p.getUUID()));
        p.sendSystemMessage(ClanText.of("demoted_ok", name(c.server(), target)));
        refresh(c.server(), target);
        HubRequests.changed(p);
        return null;
    }

    /** The leader sends a member away and bars them from this clan for {@code exile_days}; an ordinary kick has no bar. */
    @Nullable
    public static Component exile(ServerPlayer p, String word) {
        Component gate = gate(p);
        if (gate != null) {
            return gate;
        }
        Ctx c = ctx(p);
        UUID target = match(c, word);
        Optional<ClanRanks.Refusal> no = ClanRanks.mayExile(c.leads(), c.isLeader(target), target != null);
        if (no.isPresent()) {
            return refusal(no.get());
        }
        Component locked = Contests.lockRefusal(p.serverLevel(), c.clan().group(), target);
        if (locked != null) {
            return locked;
        }
        String targetName = name(c.server(), target);
        // the ban is written only once the member is really out: a failed removal must not leave a bar behind
        if (!PackMembers.remove(c.server(), target, ClanText.key("exiled"), name(c.server(), p.getUUID()))) {
            return ClanText.error("not_member");
        }
        long until = ExileRules.until(PackIndex.now(), GroupsConfig.exileDays());
        c.groups().ban(c.clan().group().toString(), target, until);
        PackMembers.tell(c.server(), target, ClanText.key("exiled_until"), ExileRules.words(GroupsConfig.exileDays(), until));
        p.sendSystemMessage(ClanText.of("exiled_ok", targetName, ExileRules.words(GroupsConfig.exileDays(), until)));
        PartyLink.sync(c.server(), p.getUUID(), true);
        HubRequests.changed(p);
        return null;
    }

    /** Every 200 ticks: each Night Walker who has been in a clan for the days, and was never demoted, becomes a Day Walker. */
    public static void sweep(MinecraftServer server) {
        GroupIndex.get(server.overworld()).pruneBans(PackIndex.now());
        if (!GroupsConfig.clanPerksOn()) {
            return;
        }
        int days = GroupsConfig.daywalkerDays();
        if (days <= 0) {
            return;
        }
        ServerLevel level = server.overworld();
        GroupIndex groups = GroupIndex.get(level);
        PackIndex packs = PackIndex.get(level);
        long now = PackIndex.now();
        for (UUID leader : List.copyOf(groups.leaders())) {
            if (!ClanScope.isClan(level, leader)) {
                continue;
            }
            UUID group = groups.existingGroupOf(leader).orElse(null);
            if (group == null) {
                continue;
            }
            for (UUID member : packs.betasOf(leader)) {
                if (groups.rankOf(member, GroupType.CLAN) != GroupRank.NONE || !ClanScope.mayBeDayWalker(level, member)) {
                    continue;
                }
                boolean demoted = groups.cooldownUntil(group, noPromoteKey(member)) > 0;
                long since = packs.inPackSince(member).orElse(now);
                if (ClanRanks.autoPromotionDue(since, now, days, demoted)) {
                    groups.setRank(member, GroupRank.DAY_WALKER);
                    PackMembers.tell(server, member, ClanText.key("auto_promoted"), String.valueOf(days));
                    refresh(server, member);
                }
            }
        }
    }
}
