package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanTime;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.ContestRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.jetbrains.annotations.Nullable;

/**
 * The Game of Kings (R272, R277): a Day Walker challenges, the head cannot refuse, and at the first in-game midnight after the minimum the online Day Walker
 * who holds the Charter leads. The lead only ever moves through {@link GroupLead#handOver}. The row is the clock: it carries the day time it started at.
 */
public final class Contests {
    public static final String COOLDOWN_KEY = "contest";
    private static LongSupplier dayTimeOverride;
    private static boolean resolving;
    private static boolean registered;

    private Contests() {
    }

    /** For tests: pins the overworld day time the contests read; null restores the world's. */
    public static void overrideDayTime(LongSupplier supplier) {
        dayTimeOverride = supplier;
    }

    static long dayTime(MinecraftServer server) {
        return dayTimeOverride != null ? dayTimeOverride.getAsLong() : server.overworld().getDayTime();
    }

    static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        GroupSwitcher.onSwitched(s -> {
            if (GroupIndex.get(s.server().overworld()).contestOf(s.group()).isPresent()) {
                voidContest(s.server(), s.leader(), s.group());
            }
        });
        PackIndex.onLeadershipMoved((old, next) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || resolving) {
                return;
            }
            GroupIndex groups = GroupIndex.get(server.overworld());
            for (UUID who : new UUID[] {old, next}) {
                groups.existingGroupOf(who).filter(g -> groups.contestOf(g).isPresent()).ifPresent(g -> voidContest(server, next, g));
            }
        });
    }

    /**
     * The refusal when {@code target} is the challenger or the holder of the Charter of a contest of this clan that is pending or running, else null:
     * the head cannot cancel a Game of Kings by sending its people away.
     */
    @Nullable
    public static Component lockRefusal(ServerLevel level, UUID group, UUID target) {
        GroupIndex groups = GroupIndex.get(level);
        Optional<ContestRow> row = groups.contestOf(group);
        if (row.isEmpty()) {
            return null;
        }
        boolean holder = groups.charterOf(group).flatMap(CharterRow::holder).filter(target::equals).isPresent();
        return row.get().challenger().equals(target) || holder ? ClanText.error("contest_running") : null;
    }

    private static Component noDecline(ServerPlayer p) {
        Component gate = ClanDiscipline.gate(p);
        return gate != null ? gate : ClanText.error("no_decline");
    }

    /** The leader cannot decline: a clan's {@code challenge decline} only says so. */
    public static Component decline(ServerPlayer p) {
        return noDecline(p);
    }

    private static void tellAll(MinecraftServer server, UUID leader, String key, String arg) {
        Charters.tellClan(server, leader, key, arg);
        Charters.refresh(server, leader);
    }

    /**
     * Why this player may not challenge their clan's head now, or empty when they may. The one answer the challenge command and the hub's card both read, so the
     * button and the command can never disagree.
     */
    public static Optional<ContestRules.Refusal> refusalFor(ServerLevel level, UUID player) {
        ClanScope.Clan clan = ClanScope.of(level, player);
        if (clan == null) {
            return Optional.of(ContestRules.Refusal.NOT_DAY_WALKER);
        }
        GroupIndex groups = GroupIndex.get(level);
        PackIndex packs = PackIndex.get(level);
        long now = PackIndex.now();
        int days = packs.inPackSince(player).map(t -> (int) Math.min(Integer.MAX_VALUE, Math.max(0, (now - t) / ClanTime.DAY))).orElse(0);
        boolean charter = groups.charterOf(clan.group()).filter(r -> r.state() != CharterState.LOST).isPresent();
        return ContestRules.mayChallenge(ClanScope.dayWalker(level, player) ? GroupRank.DAY_WALKER : GroupRank.NONE, days, packs.roster(clan.leader()).size(),
                SireCall.player(level.getServer(), clan.leader()) != null, charter, groups.contestOf(clan.group()).isPresent(), groups.cooldownUntil(clan.group(), COOLDOWN_KEY), now,
                GroupsConfig.challengerMinDays(), GroupsConfig.charterMinMembers());
    }

    @Nullable
    public static Component challenge(ServerPlayer p) {
        Component gate = ClanDiscipline.gate(p);
        if (gate != null) {
            return gate;
        }
        if (!GroupsConfig.charterActive()) {
            return ClanText.error("charter_off");
        }
        ServerLevel level = p.serverLevel();
        MinecraftServer server = p.server;
        ClanScope.Clan clan = ClanScope.of(level, p.getUUID());
        if (p.getUUID().equals(clan.leader())) {
            return ClanText.error("challenge_leader");
        }
        GroupIndex groups = GroupIndex.get(level);
        long now = PackIndex.now();
        Optional<ContestRules.Refusal> no = refusalFor(level, p.getUUID());
        if (no.isPresent()) {
            long cooldown = groups.cooldownUntil(clan.group(), COOLDOWN_KEY);
            return no.get() == ContestRules.Refusal.COOLDOWN
                    ? ClanText.error("challenge_cooldown", Math.max(1, (cooldown - now + ClanTime.DAY - 1) / ClanTime.DAY))
                    : ClanText.error("challenge_" + no.get().id());
        }
        boolean headStored = groups.charterOf(clan.group()).filter(r -> r.state() == CharterState.STORED && r.byHead()).isPresent();
        groups.setContest(clan.group(), new ContestRow(p.getUUID(), ContestRow.Phase.PENDING, now, now + GroupsConfig.contestAcceptMinutes() * 60_000L, 0, 0, headStored));
        tellAll(server, clan.leader(), ClanText.key("challenged"), ClanDiscipline.name(server, p.getUUID()));
        return null;
    }

    @Nullable
    public static Component accept(ServerPlayer p) {
        Component gate = ClanDiscipline.gate(p);
        if (gate != null) {
            return gate;
        }
        ClanScope.Clan clan = ClanScope.of(p.serverLevel(), p.getUUID());
        if (!p.getUUID().equals(clan.leader())) {
            return ClanText.error("leader_only");
        }
        Optional<ContestRow> row = GroupIndex.get(p.serverLevel()).contestOf(clan.group());
        if (row.isEmpty() || row.get().phase() != ContestRow.Phase.PENDING) {
            return ClanText.error("challenge_none");
        }
        start(p.server, clan.leader(), clan.group(), row.get());
        return null;
    }

    private static void start(MinecraftServer server, UUID leader, UUID group, ContestRow pending) {
        GroupIndex groups = GroupIndex.get(server.overworld());
        long now = PackIndex.now();
        groups.setContest(group, new ContestRow(pending.challenger(), ContestRow.Phase.RUNNING, pending.openedAt(), pending.acceptBy(), now, dayTime(server), pending.headStored()));
        groups.setCooldown(group, COOLDOWN_KEY, now + GroupsConfig.contestCooldownDays() * ClanTime.DAY);
        tellAll(server, leader, ClanText.key("contest_started"), "");
    }

    /** Ends a contest with no winner and no new cooldown. */
    static void voidContest(MinecraftServer server, UUID leader, UUID group) {
        GroupIndex.get(server.overworld()).clearContest(group);
        tellAll(server, leader, ClanText.key("contest_void"), "");
    }

    private static Map<UUID, UUID> leaders(GroupIndex groups) {
        Map<UUID, UUID> out = new HashMap<>();
        for (UUID leader : groups.leaders()) {
            groups.existingGroupOf(leader).ifPresent(g -> out.put(g, leader));
        }
        return out;
    }

    public static void tick(MinecraftServer server) {
        tick(server, dayTime(server));
    }

    /** One look at every contest: the accept deadline, the end, and the reasons it ends with no winner. */
    public static void tick(MinecraftServer server, long dayTime) {
        GroupIndex groups = GroupIndex.get(server.overworld());
        if (groups.contests().isEmpty()) {
            return;
        }
        Map<UUID, UUID> leaderOf = leaders(groups);
        long now = PackIndex.now();
        for (Map.Entry<UUID, ContestRow> e : new ArrayList<>(groups.contests().entrySet())) {
            UUID group = e.getKey();
            ContestRow row = e.getValue();
            UUID leader = leaderOf.get(group);
            if (leader == null) {
                groups.clearContest(group);
                PackMembers.tell(server, row.challenger(), ClanText.key("contest_void"), "");
                continue;
            }
            if (!GroupsConfig.charterActive() || !ClanScope.isClan(server.overworld(), leader)) {
                voidContest(server, leader, group);
                continue;
            }
            if (row.phase() == ContestRow.Phase.PENDING) {
                if (!Charters.isMember(server, row.challenger(), group)) {
                    voidContest(server, leader, group); // the challenger left while it waited: nobody is left to challenge, and no cooldown is earned
                    continue;
                }
                // a head who is away at the deadline is waited for: it starts at their next login, never by itself without them
                if (ContestRules.autoStart(row.acceptBy(), now) && SireCall.player(server, leader) != null) {
                    start(server, leader, group, row);
                }
                continue;
            }
            if (groups.charterOf(group).filter(r -> r.state() == CharterState.LOST).isPresent()) {
                voidContest(server, leader, group);
                continue;
            }
            if (ContestRules.due(row.startedDayTime(), dayTime, row.startedAt(), now, GroupsConfig.contestMinMinutes(), GroupsConfig.contestMaxHours())) {
                resolve(server, leader, group);
            }
        }
    }

    private static void resolve(MinecraftServer server, UUID leader, UUID group) {
        CharterScan.scanNow(server);
        GroupIndex groups = GroupIndex.get(server.overworld());
        PackIndex packs = PackIndex.get(server.overworld());
        CharterRow charter = groups.charterOf(group).orElse(null);
        ContestRules.Holder holder;
        if (charter == null || charter.holder().isEmpty()) {
            holder = new ContestRules.Holder(charter == null ? CharterState.LOST : charter.state(), null, false, false, GroupRank.NONE, false);
        } else {
            UUID id = charter.holder().get();
            ServerPlayer online = SireCall.player(server, id);
            GroupRank rank = ClanScope.dayWalker(server.overworld(), id) ? GroupRank.DAY_WALKER : GroupRank.NONE;
            holder = new ContestRules.Holder(charter.state(), id, online != null, online != null && online.isAlive(), rank, Charters.isMember(server, id, group));
        }
        ContestRow contest = groups.contestOf(group).orElseThrow();
        ServerPlayer challengerOnline = SireCall.player(server, contest.challenger());
        ContestRules.Holder challenger = new ContestRules.Holder(CharterState.HELD, contest.challenger(), challengerOnline != null, challengerOnline != null && challengerOnline.isAlive(),
                ClanScope.dayWalker(server.overworld(), contest.challenger()) ? GroupRank.DAY_WALKER : GroupRank.NONE, Charters.isMember(server, contest.challenger(), group));
        ContestRules.Outcome outcome = ContestRules.resolve(holder, leader, challenger, contest.headStored());
        groups.clearContest(group);
        if (outcome.newLeader().isEmpty()) {
            tellAll(server, leader, ClanText.key("contest_nochange." + outcome.reason()), "");
            long until = groups.cooldownUntil(group, COOLDOWN_KEY);
            long days = Math.max(1, (until - PackIndex.now() + ClanTime.DAY - 1) / ClanTime.DAY);
            tellAll(server, leader, ClanText.key(days == 1 ? "contest_cooldown.one" : "contest_cooldown"), String.valueOf(days));
            return;
        }
        UUID next = outcome.newLeader().get();
        resolving = true;
        try {
            GroupLead.handOver(server, packs, leader, next, GroupType.CLAN);
        } finally {
            resolving = false;
        }
        tellAll(server, next, ClanText.key(outcome.forfeit() ? "contest_new_head_forfeit" : "contest_new_head"), ClanDiscipline.name(server, next));
    }

    /** The head's card while a challenge waits for an answer: kind, challenger, no token (the accept command takes none) and the time left. */
    public static List<HubRequests.Request> requestsFor(ServerPlayer p) {
        ClanScope.Clan clan = ClanScope.of(p.serverLevel(), p.getUUID());
        if (clan == null || !p.getUUID().equals(clan.leader()) || !GroupsConfig.charterActive()) {
            return List.of();
        }
        Optional<ContestRow> row = GroupIndex.get(p.serverLevel()).contestOf(clan.group()).filter(r -> r.phase() == ContestRow.Phase.PENDING);
        if (row.isEmpty()) {
            return List.of();
        }
        int ticks = (int) Math.max(1, Math.min(Integer.MAX_VALUE, (row.get().acceptBy() - PackIndex.now()) / 50));
        return List.of(new HubRequests.Request(HubRequests.Kind.CHARTER_CONTEST, row.get().challenger(), ClanDiscipline.name(p.server, row.get().challenger()), "", ticks));
    }
}
