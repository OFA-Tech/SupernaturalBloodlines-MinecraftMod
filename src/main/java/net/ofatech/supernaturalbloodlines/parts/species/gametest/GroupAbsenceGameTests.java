package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyBackend;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;

/** Plan 20a, R244: an absent leader loses the lead to the heir of the group's type. Runs with or without Werewolves. */
@PrefixGameTestTemplate(false)
public final class GroupAbsenceGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = PackAbsence.DAY_MILLIS;

    private GroupAbsenceGameTests() {
    }

    /** A group of {@code type} led by somebody who is not online, with the members joined in the order given (the first has been in longest). */
    private static void build(GameTestHelper helper, UUID gone, GroupType type, long seen, ServerPlayer... members) {
        PackIndex index = PackIndex.get(helper.getLevel());
        long[] at = {1_000L};
        PackIndex.overrideClock(() -> at[0]);
        for (ServerPlayer m : members) {
            index.join(gone, m.getUUID());
            at[0] += 1_000L;
        }
        PackIndex.overrideClock(null);
        GroupIndex.get(helper.getLevel()).ensure(gone, type, 1_000L);
        index.setLastSeen(gone, seen);
    }

    private static void drop(GameTestHelper helper, UUID gone) {
        PackIndex index = PackIndex.get(helper.getLevel());
        index.leave(gone);
        index.takeNotices(gone);
        GroupIndex.get(helper.getLevel()).clearRanks(gone);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClanLeaderAbsentSevenDaysLosesTheLeadToADayWalker(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist night = human(helper, 1.5);
        TalkingDuelist first = vampire(helper, 2.5);
        TalkingDuelist second = vampire(helper, 3.5);
        online(night, first, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, GroupType.CLAN, now - 8 * DAY, night, first, second);
            groups.setRank(first.getUUID(), GroupRank.DAY_WALKER);
            groups.setRank(second.getUUID(), GroupRank.DAY_WALKER);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1, "the absent leader was not deposed");
            helper.assertTrue(index.isLeader(first.getUUID()) && index.alphaOf(gone).map(first.getUUID()::equals).orElse(false), "the longest Day Walker does not lead: " + index.leaderOf(second.getUUID()));
            helper.assertTrue(groups.rankOf(gone, GroupType.CLAN) == GroupRank.DAY_WALKER && groups.rankOf(first.getUUID(), GroupType.CLAN) == GroupRank.NONE, "the ranks did not follow the lead");
            helper.assertTrue(groups.existingGroupOf(first.getUUID()).map(groups::typeOf).orElse(null) == GroupType.CLAN, "the clan id did not follow the leader");
            helper.assertTrue(first.received("supernaturalbloodlines.pack.absent_lead_now") && night.received("supernaturalbloodlines.pack.absent_leader"), "the members were not told");
            helper.assertTrue(index.noticeCount(gone) == 1, "the old leader was not told");
        } finally {
            drop(helper, gone);
            clean(helper, night, first, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCovenLeaderAbsentSevenDaysLosesTheLeadToAnElder(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist plain = human(helper, 1.5);
        TalkingDuelist elder = human(helper, 2.5);
        online(plain, elder);
        PackIndex index = PackIndex.get(helper.getLevel());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, GroupType.COVEN, now - 8 * DAY, plain, elder);
            groups.setRank(elder.getUUID(), GroupRank.ELDER);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1 && index.isLeader(elder.getUUID()), "the Elder does not lead");
            helper.assertTrue(groups.rankOf(gone, GroupType.COVEN) == GroupRank.ELDER && index.alphaOf(gone).map(elder.getUUID()::equals).orElse(false), "the old leader is not an Elder of the new one");
        } finally {
            drop(helper, gone);
            clean(helper, plain, elder);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherhoodLeaderAbsentLosesTheLeadToTheHighestMark(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist low = human(helper, 1.5);
        TalkingDuelist high = human(helper, 2.5);
        online(low, high);
        BloodlineAccess.set(low, BloodlineState.NONE.toBuilder().markKills(3).build());
        BloodlineAccess.set(high, BloodlineState.NONE.toBuilder().markKills(8).build());
        PackIndex index = PackIndex.get(helper.getLevel());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, GroupType.BROTHERHOOD, now - 8 * DAY, low, high);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1 && index.isLeader(high.getUUID()), "the highest mark does not lead");
            helper.assertTrue(index.alphaOf(gone).map(high.getUUID()::equals).orElse(false) && groups.rankOf(gone, GroupType.BROTHERHOOD) == GroupRank.NONE, "the old leader is not a plain Brother");
        } finally {
            drop(helper, gone);
            clean(helper, low, high);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noHeirChangesNothing(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist a = human(helper, 1.5);
        TalkingDuelist b = human(helper, 2.5);
        online(a, b);
        PackIndex index = PackIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, GroupType.COVEN, now - 30 * DAY, a, b);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 0 && index.isLeader(gone), "a coven with no Elder changed hands");
            GroupIndex.get(helper.getLevel()).setRank(a.getUUID(), GroupRank.DAY_WALKER);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 0 && index.isLeader(gone), "a clan rank led a coven");
        } finally {
            drop(helper, gone);
            clean(helper, a, b);
        }
        helper.succeed();
    }

    /** A pack's absence path (the spark rule) is reached without Werewolves too, and with no second Alpha it changes nothing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAbsentPackLeaderWithNoSecondAlphaKeepsTheLead(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist a = human(helper, 1.5);
        online(a);
        PackIndex index = PackIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, GroupType.PACK, now - 30 * DAY, a);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 0 && index.isLeader(gone), "a pack with no second Alpha changed hands");
        } finally {
            drop(helper, gone);
            clean(helper, a);
        }
        helper.succeed();
    }

    /** Open Parties and Claims, as far as the link uses it. */
    private static final class FakeParties implements PartyBackend {
        final Map<UUID, UUID> ids = new HashMap<>();
        final Map<UUID, Set<UUID>> parties = new HashMap<>();

        @Override
        public Found find(MinecraftServer server, UUID owner, UUID id) {
            return id.equals(ids.get(owner)) ? new Found(true, new Party(owner, new HashSet<>(parties.get(owner)))) : new Found(true, null);
        }

        @Override
        public Optional<UUID> create(MinecraftServer server, UUID owner) {
            ids.put(owner, UUID.randomUUID());
            parties.put(owner, new HashSet<>(Set.of(owner)));
            return Optional.of(ids.get(owner));
        }

        @Override
        public boolean add(MinecraftServer server, UUID owner, UUID member) {
            if (!parties.containsKey(owner)) {
                return false;
            }
            parties.get(owner).add(member);
            return true;
        }

        @Override
        public boolean remove(MinecraftServer server, UUID owner, UUID member) {
            parties.getOrDefault(owner, new HashSet<>()).remove(member);
            return true;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClanIsMirroredIntoThePartyToo(GameTestHelper helper) {
        TalkingDuelist leader = vampire(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(leader, a);
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        try {
            helper.assertTrue(GroupTestKit.join(leader, a), "the clan could not be built");
            PartyLink.sync(helper.getLevel().getServer(), leader.getUUID(), false);
            helper.assertTrue(fake.parties.getOrDefault(leader.getUUID(), Set.of()).equals(Set.of(leader.getUUID(), a.getUUID())), "the party is " + fake.parties);
        } finally {
            PartyLink.setBackend(null);
            clean(helper, leader, a);
        }
        helper.succeed();
    }
}
