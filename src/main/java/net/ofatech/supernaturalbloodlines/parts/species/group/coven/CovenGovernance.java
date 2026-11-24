package net.ofatech.supernaturalbloodlines.parts.species.group.coven;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenRules;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenRules.Role;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteTally;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.Vote;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.jetbrains.annotations.Nullable;

/** The coven's government (R250, R251, R259, R260, R261): the Regent, the Elders, the votes, the shunning and the call for a new Regent. */
public final class CovenGovernance {
    public static final String SHUN_SCOPE = "type:coven";
    public static final String ELECTION = "election";
    private static final long HOUR = 60L * 60 * 1000;
    private static final long DAY = 24 * HOUR;
    public static final long VOTE_LIFETIME = 7 * DAY;

    private CovenGovernance() {
    }

    /** The coven the player is in and everything the actions need. */
    private record Ctx(ServerPlayer p, MinecraftServer server, UUID leader, UUID group, PackIndex index, GroupIndex groups) {
        Role roleOf(UUID id) {
            return id.equals(leader) ? Role.REGENT : groups.rankOf(id, GroupType.COVEN) == GroupRank.ELDER ? Role.ELDER : Role.MEMBER;
        }

        List<UUID> electors() {
            List<UUID> out = new ArrayList<>();
            out.add(leader);
            index.betasOf(leader).stream().filter(id -> roleOf(id) == Role.ELDER).forEach(out::add);
            return out;
        }

        List<UUID> everyone() {
            List<UUID> out = new ArrayList<>(index.betasOf(leader));
            out.add(leader);
            return out;
        }
    }

    @Nullable
    private static Ctx ctx(ServerPlayer p) {
        UUID leader = CircleLink.covenOf(p);
        if (leader == null) {
            return null;
        }
        GroupIndex groups = GroupIndex.get(p.serverLevel());
        UUID group = groups.existingGroupOf(leader).orElse(null);
        return group == null ? null : new Ctx(p, p.server, leader, group, PackIndex.get(p.serverLevel()), groups);
    }

    @Nullable
    private static Ctx ctxOf(MinecraftServer server, UUID leader) {
        GroupIndex groups = GroupIndex.get(server.overworld());
        UUID group = groups.existingGroupOf(leader).orElse(null);
        return group == null ? null : new Ctx(null, server, leader, group, PackIndex.get(server.overworld()), groups);
    }

    private static String name(MinecraftServer server, UUID id) {
        String n = PackAccess.nameOf(server, id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    @Nullable
    private static UUID match(MinecraftServer server, List<UUID> candidates, String word) {
        return candidates.stream().filter(id -> id.toString().equalsIgnoreCase(word) || name(server, id).equalsIgnoreCase(word)).findFirst().orElse(null);
    }

    /** The names a governance command may take: the members of the player's coven, the Regent too. */
    public static List<String> namesFor(ServerPlayer p) {
        Ctx c = ctx(p);
        return c == null ? List.of() : c.everyone().stream().filter(id -> !id.equals(p.getUUID())).map(id -> name(p.server, id)).toList();
    }

    private static void tellAll(Ctx c, String key, String arg) {
        for (UUID id : c.everyone()) {
            PackMembers.tell(c.server, id, CovenText.key(key), arg);
        }
    }

    private static void refreshCards(Ctx c) {
        for (UUID id : c.everyone()) {
            ServerPlayer online = SireCall.player(c.server, id);
            if (online != null) {
                HubRequests.changed(online);
            }
        }
    }

    private static long daysIn(Ctx c, UUID id) {
        return c.index.inPackSince(id).map(since -> Math.max(0, (PackIndex.now() - since) / DAY)).orElse(0L);
    }

    // ---- Elders ----

    @Nullable
    public static Component elderAdd(ServerPlayer p, String word) {
        Ctx c = ctx(p);
        if (c == null) {
            return CircleLink.notACoven(p);
        }
        if (c.roleOf(p.getUUID()) != Role.REGENT) {
            return CovenText.error("regent_only");
        }
        UUID target = match(c.server, c.index.betasOf(c.leader), word);
        if (target == null) {
            return CovenText.error("not_member", word);
        }
        if (c.roleOf(target) == Role.ELDER) {
            return CovenText.error("already_elder", word);
        }
        if (!CovenRules.mayBeElder(daysIn(c, target), GroupsConfig.elderMinDays())) {
            return CovenText.error("elder_days", word, GroupsConfig.elderMinDays());
        }
        long elders = c.electors().size() - 1;
        if (elders >= CovenRules.elderCap(c.index.betasOf(c.leader).size() + 1, GroupsConfig.membersPerElder())) {
            return CovenText.error("elder_cap");
        }
        return decide(c, VoteKind.ELDER_ADD, target);
    }

    @Nullable
    public static Component elderRemove(ServerPlayer p, String word) {
        Ctx c = ctx(p);
        if (c == null) {
            return CircleLink.notACoven(p);
        }
        if (c.roleOf(p.getUUID()) != Role.REGENT) {
            return CovenText.error("regent_only");
        }
        UUID target = match(c.server, c.index.betasOf(c.leader), word);
        if (target == null) {
            return CovenText.error("not_member", word);
        }
        if (c.roleOf(target) != Role.ELDER) {
            return CovenText.error("not_elder", word);
        }
        return decide(c, VoteKind.ELDER_REMOVE, target);
    }

    // ---- shunning ----

    @Nullable
    public static Component shun(ServerPlayer p, String word) {
        Ctx c = ctx(p);
        if (c == null) {
            return CircleLink.notACoven(p);
        }
        UUID target = match(c.server, c.everyone(), word);
        if (target == null) {
            return CovenText.error("not_member", word);
        }
        if (!CovenRules.mayCallShun(c.roleOf(p.getUUID()), c.roleOf(target))) {
            return CovenText.error(c.roleOf(target) == Role.REGENT ? "shun_regent" : "shun_not_allowed");
        }
        if (target.equals(p.getUUID())) {
            return CovenText.error("self");
        }
        if (!VoteTally.needsVote(c.electors().size()) && c.roleOf(p.getUUID()) != Role.REGENT) {
            return CovenText.error("regent_decides");
        }
        return decide(c, VoteKind.SHUN, target);
    }

    // ---- the Regent ----

    /** The coven route of {@code challenge <name>}: an Elder calls for the named Elder to become Regent. */
    @Nullable
    public static Component callElection(ServerPlayer p, String word) {
        Ctx c = ctx(p);
        if (c == null) {
            return CircleLink.notACoven(p);
        }
        if (c.roleOf(p.getUUID()) != Role.ELDER) {
            return CovenText.error("elder_only");
        }
        if (!VoteTally.mayCallElection(c.electors().size())) {
            return CovenText.error("too_few_electors");
        }
        UUID nominee = match(c.server, c.index.betasOf(c.leader), word);
        if (nominee == null) {
            return CovenText.error("not_member", word);
        }
        if (!CovenRules.mayCallElection(c.roleOf(p.getUUID()), c.roleOf(nominee), c.groups.cooldownUntil(c.group, ELECTION), PackIndex.now())) {
            return CovenText.error(c.roleOf(nominee) != Role.ELDER ? "nominee_not_elder" : "election_cooldown");
        }
        return open(c, VoteKind.REGENT, nominee);
    }

    // ---- votes ----

    /** The Regent alone when the electorate is under three; otherwise a vote opens and the caller has already said yes. */
    @Nullable
    private static Component decide(Ctx c, VoteKind kind, UUID subject) {
        if (!VoteTally.needsVote(c.electors().size())) {
            apply(c, kind, subject, c.p.getUUID());
            return null;
        }
        return open(c, kind, subject);
    }

    @Nullable
    private static Component open(Ctx c, VoteKind kind, UUID subject) {
        if (c.groups.openVote(c.group, kind).isPresent()) {
            return CovenText.error("vote_open");
        }
        Vote v = c.groups.openVote(c.group, kind, subject, c.p.getUUID(), PackIndex.now());
        c.groups.ballot(v, c.p.getUUID(), true);
        tellElectors(c, "vote_opened", kind, subject);
        refreshCards(c);
        resolve(c, v);
        return null;
    }

    private static void tellElectors(Ctx c, String key, VoteKind kind, UUID subject) {
        for (UUID id : c.electors()) {
            PackMembers.tell(c.server, id, CovenText.key(key + "." + kind.id()), name(c.server, subject));
        }
    }

    @Nullable
    public static Component vote(ServerPlayer p, boolean yes) {
        Ctx c = ctx(p);
        if (c == null) {
            return CircleLink.notACoven(p);
        }
        if (!c.electors().contains(p.getUUID())) {
            return CovenText.error("not_elector");
        }
        Vote open = c.groups.openVotes(c.group).stream().filter(v -> !v.ballots.containsKey(p.getUUID())).findFirst().orElse(null);
        if (open == null) {
            return CovenText.error("no_vote");
        }
        c.groups.ballot(open, p.getUUID(), yes);
        p.sendSystemMessage(CovenText.of("vote_counted"));
        HubRequests.changed(p);
        resolve(c, open);
        return null;
    }

    /** Counts a vote: it ends when it passed or failed, or is cancelled because what it is about is no longer true. */
    private static void resolve(Ctx c, Vote v) {
        if (v.cancelled) {
            return;
        }
        if (!stillValid(c, v)) {
            c.groups.cancelVote(v);
            refreshCards(c);
            return;
        }
        List<UUID> electors = c.electors();
        boolean everyone = electors.stream().allMatch(v.ballots::containsKey);
        boolean expired = PackIndex.now() - v.opened >= GroupsConfig.voteHours() * HOUR;
        int yes = (int) electors.stream().filter(id -> Boolean.TRUE.equals(v.ballots.get(id))).count();
        int no = (int) electors.stream().filter(id -> Boolean.FALSE.equals(v.ballots.get(id))).count();
        VoteTally.Result result = VoteTally.result(electors.size(), yes, no, everyone, expired);
        if (result == VoteTally.Result.PENDING) {
            return;
        }
        c.groups.closeVote(v);
        if (result == VoteTally.Result.PASSED) {
            apply(c, v.kind, v.subject, v.caller);
        } else {
            tellElectors(c, "vote_failed", v.kind, v.subject);
        }
        refreshCards(c);
    }

    private static boolean stillValid(Ctx c, Vote v) {
        List<UUID> members = c.index.betasOf(c.leader);
        if (!members.contains(v.subject)) {
            return false;
        }
        Role role = c.roleOf(v.subject);
        return switch (v.kind) {
            case ELDER_ADD -> role == Role.MEMBER;
            case ELDER_REMOVE, REGENT -> role == Role.ELDER;
            case SHUN -> role != Role.REGENT;
        };
    }

    private static void apply(Ctx c, VoteKind kind, UUID subject, UUID caller) {
        String who = name(c.server, subject);
        switch (kind) {
            case ELDER_ADD -> {
                c.groups.setRank(subject, GroupRank.ELDER);
                tellAll(c, "elder_added", who);
            }
            case ELDER_REMOVE -> {
                c.groups.clearRank(subject, GroupType.COVEN);
                tellAll(c, "elder_removed", who);
            }
            case SHUN -> {
                long until = CovenRules.banUntil(PackIndex.now(), GroupsConfig.shunDays());
                c.groups.ban(SHUN_SCOPE, subject, until);
                tellAll(c, "shun_done", who);
                PackMembers.remove(c.server, subject, CovenText.key("shunned"), name(c.server, c.leader));
                c.groups.clearRanks(subject);
                CircleLink.dropMember(c.server, subject, false);
                PartyLink.sync(c.server, c.leader, true);
            }
            case REGENT -> {
                c.groups.setCooldown(c.group, ELECTION, PackIndex.now() + GroupsConfig.electionCooldownDays() * DAY);
                String before = name(c.server, c.leader);
                List<UUID> everyone = c.everyone();
                GroupLead.handOver(c.server, c.index, c.leader, subject, GroupType.COVEN);
                for (UUID id : everyone) {
                    PackMembers.tell(c.server, id, CovenText.key(id.equals(subject) ? "regent_you" : "regent_new"), id.equals(subject) ? before : who);
                }
            }
        }
        refreshCards(c);
    }

    /** Every 200 ticks: counts the votes whose time is up, cancels the ones whose subject is gone, forgets what is over. */
    public static void sweep(MinecraftServer server) {
        GroupIndex groups = GroupIndex.get(server.overworld());
        long now = PackIndex.now();
        for (Vote v : groups.allVotes()) {
            UUID leader = groups.leaderOfGroup(v.group).orElse(null);
            Ctx c = leader == null ? null : ctxOf(server, leader);
            if (c == null || groups.typeOf(v.group) != GroupType.COVEN) {
                continue;
            }
            if (!v.cancelled) {
                resolveSwept(c, v);
            }
        }
        groups.prune(now, VOTE_LIFETIME);
    }

    private static void resolveSwept(Ctx c, Vote v) {
        if (!stillValid(c, v)) {
            c.groups.cancelVote(v);
            refreshCards(c);
            return;
        }
        List<UUID> electors = c.electors();
        boolean everyone = electors.stream().allMatch(v.ballots::containsKey);
        boolean expired = PackIndex.now() - v.opened >= GroupsConfig.voteHours() * HOUR;
        int yes = (int) electors.stream().filter(id -> Boolean.TRUE.equals(v.ballots.get(id))).count();
        if (VoteTally.result(electors.size(), yes, 0, everyone, expired) != VoteTally.Result.PENDING) {
            resolve(c, v);
        }
    }

    /** A type switch (or a dissolved group) cancels what is open; the rows stay and are ignored. */
    public static void cancelAll(MinecraftServer server, UUID group) {
        GroupIndex groups = GroupIndex.get(server.overworld());
        groups.openVotes(group).forEach(groups::cancelVote);
    }

    /** The cards for the hub's Home: the votes {@code p} may still answer. */
    public static List<HubRequests.Request> cardsFor(ServerPlayer p) {
        List<HubRequests.Request> out = new ArrayList<>();
        Ctx c = ctx(p);
        if (c == null || !c.electors().contains(p.getUUID())) {
            return out;
        }
        long now = PackIndex.now();
        for (Vote v : c.groups.openVotes(c.group)) {
            if (v.ballots.containsKey(p.getUUID())) {
                continue;
            }
            long leftMillis = Math.max(0, v.opened + GroupsConfig.voteHours() * HOUR - now);
            out.add(new HubRequests.Request(HubRequests.Kind.GROUP_VOTE, v.subject, name(p.server, v.subject), v.kind.id(), (int) Math.min(Integer.MAX_VALUE, leftMillis / 50)));
        }
        return out;
    }

    /** Who may be named in each governance command, for the suggestions of a typed command. */
    public static Map<String, List<String>> suggestions(ServerPlayer p) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("members", namesFor(p));
        return m;
    }

    public static boolean isCoven(ServerPlayer p) {
        return Groups.typeOf(p) == GroupType.COVEN;
    }
}