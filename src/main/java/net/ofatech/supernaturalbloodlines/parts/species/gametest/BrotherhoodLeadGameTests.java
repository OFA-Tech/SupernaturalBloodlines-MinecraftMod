package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodLead;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodMarks;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodRegistry;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyBackend;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;

/** Plan 20d Task 5 (R291): the mark is recorded, and the lead moves to the highest mark at once. Marks are set by hand, a death is not looped. */
@PrefixGameTestTemplate(false)
public final class BrotherhoodLeadGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = PackAbsence.DAY_MILLIS;

    private BrotherhoodLeadGameTests() {
    }

    private static BloodlineState hof(int kills) {
        return BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(kills).build();
    }

    private static TalkingDuelist marked(GameTestHelper helper, double x, int kills) {
        TalkingDuelist p = human(helper, x);
        BloodlineAccess.set(p, hof(kills));
        return p;
    }

    private static PackIndex packs(GameTestHelper helper) {
        return PackIndex.get(helper.getLevel());
    }

    private static boolean leads(GameTestHelper helper, ServerPlayer p) {
        return packs(helper).isLeader(p.getUUID()) && !packs(helper).betasOf(p.getUUID()).isEmpty();
    }

    private static boolean ledBy(GameTestHelper helper, ServerPlayer member, ServerPlayer leader) {
        return packs(helper).alphaOf(member.getUUID()).map(leader.getUUID()::equals).orElse(false);
    }

    // ---- the takeover ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMarkedMemberPassesTheFounderAtOnce(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(founder, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(founder, brother), "no brotherhood");
            UUID group = groups.existingGroupOf(founder.getUUID()).orElseThrow();
            BloodlineAccess.set(brother, hof(1));
            helper.assertTrue(leads(helper, brother) && ledBy(helper, founder, brother), "the lead did not move at once");
            helper.assertTrue(groups.existingGroupOf(brother.getUUID()).map(group::equals).orElse(false) && groups.existingGroupOf(founder.getUUID()).isEmpty(), "the group id did not follow the lead");
            helper.assertTrue(groups.rankOf(founder.getUUID(), GroupType.BROTHERHOOD) == GroupRank.NONE, "the old leader holds a rank");
            helper.assertTrue(Groups.typeOf(founder) == GroupType.BROTHERHOOD, "the group is no brotherhood any more");
        } finally {
            clean(helper, founder, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFounderLeadsWhileNobodyIsHigher(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist a = marked(helper, 2.5, 0);
        TalkingDuelist b = human(helper, 3.5);
        online(founder, a, b);
        try {
            helper.assertTrue(join(founder, a) && join(founder, b), "no brotherhood");
            BloodlineAccess.set(a, hof(0));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, founder), "the founder lost the lead to nobody");
        } finally {
            clean(helper, founder, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTieKeepsTheLeader(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 2);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(2));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "a tie moved the lead");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHumanLeaderIsPassedByTheFirstMarkedHunter(GameTestHelper helper) {
        TalkingDuelist leader = human(helper, 1.5);
        TalkingDuelist hunter = human(helper, 2.5);
        TalkingDuelist other = human(helper, 3.5);
        online(leader, hunter, other);
        GroupsConfig.override("open_founding", true);
        try {
            helper.assertTrue(join(leader, hunter) && join(leader, other), "no brotherhood");
            BloodlineAccess.set(other, hof(0));
            helper.assertTrue(leads(helper, leader), "mark 0 passed a human");
            BloodlineAccess.set(hunter, hof(1));
            helper.assertTrue(leads(helper, hunter) && ledBy(helper, leader, hunter), "the first marked hunter did not take the lead from a human");
        } finally {
            clean(helper, leader, hunter, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHighestOfTwoPassesTheLeader(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist low = human(helper, 2.5);
        TalkingDuelist high = human(helper, 3.5);
        online(founder, low, high);
        try {
            helper.assertTrue(join(founder, low) && join(founder, high), "no brotherhood");
            BloodlineAccess.set(low, hof(3));
            helper.assertTrue(leads(helper, low), "the first mark did not lead");
            BloodlineAccess.set(high, hof(5));
            helper.assertTrue(leads(helper, high) && ledBy(helper, low, high) && ledBy(helper, founder, high), "the highest does not lead");
        } finally {
            clean(helper, founder, low, high);
        }
        helper.succeed();
    }

    private static final class FakeParty implements PartyBackend {
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
            parties.get(owner).add(member);
            return true;
        }

        @Override
        public boolean remove(MinecraftServer server, UUID owner, UUID member) {
            parties.get(owner).remove(member);
            return true;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTakeoverGoesThroughHandOver(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        TalkingDuelist allyLeader = GroupTestKit.hunter(helper, 3.5);
        TalkingDuelist allyMember = human(helper, 4.5);
        online(founder, brother, allyLeader, allyMember);
        FakeParty fake = new FakeParty();
        PartyLink.setBackend(fake);
        try {
            helper.assertTrue(join(founder, brother) && join(allyLeader, allyMember), "no groups");
            helper.assertTrue(packs(helper).ally(founder.getUUID(), allyLeader.getUUID()), "no alliance");
            BloodlineAccess.set(brother, hof(1));
            helper.assertTrue(leads(helper, brother), "the lead did not move");
            helper.assertTrue(packs(helper).alliesOf(brother.getUUID()).contains(allyLeader.getUUID()), "the alliance did not follow the new leader");
            helper.assertTrue(fake.parties.containsKey(brother.getUUID()) && fake.parties.get(brother.getUUID()).contains(founder.getUUID()), "the party link was not made for the new leader");
        } finally {
            PartyLink.setBackend(null);
            clean(helper, founder, brother, allyLeader, allyMember);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyoneIsToldAndOfflineMembersAtLogin(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        TalkingDuelist here = human(helper, 3.5);
        TalkingDuelist away = human(helper, 4.5);
        online(founder, brother, here, away);
        try {
            helper.assertTrue(join(founder, brother) && join(founder, here) && join(founder, away), "no brotherhood");
            online(founder, brother, here);
            BloodlineAccess.set(brother, hof(1));
            helper.assertTrue(brother.received("supernaturalbloodlines.brotherhood.lead_now"), "the new leader was not told");
            helper.assertTrue(here.received("supernaturalbloodlines.brotherhood.lead_lost"), "an online member was not told");
            helper.assertTrue(packs(helper).noticeCount(away.getUUID()) == 1, "the member who is away has " + packs(helper).noticeCount(away.getUUID()) + " notices waiting");
            helper.assertTrue(packs(helper).takeNotices(away.getUUID()).get(0)[0].equals("supernaturalbloodlines.brotherhood.lead_lost"), "the waiting notice is not the lead one");
        } finally {
            clean(helper, founder, brother, here, away);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOldLeaderGetsTheLostLeadNotice(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(founder, brother);
        try {
            helper.assertTrue(join(founder, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(1));
            helper.assertTrue(founder.received("supernaturalbloodlines.brotherhood.lead_you"), "the old leader was not told");
        } finally {
            clean(helper, founder, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAbsentOldLeaderIsToldAtLogin(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(founder, brother);
        try {
            helper.assertTrue(join(founder, brother), "no brotherhood");
            online(brother);
            BloodlineAccess.set(brother, hof(1));
            helper.assertTrue(leads(helper, brother), "the lead did not move");
            var notes = packs(helper).takeNotices(founder.getUUID());
            helper.assertTrue(notes.size() == 1 && notes.get(0)[0].equals("supernaturalbloodlines.brotherhood.lead_you"), "the absent old leader's notice is " + notes.size());
        } finally {
            clean(helper, founder, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeadersMarkResetPassesTheLead(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 5);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(1));
            helper.assertTrue(leads(helper, leader), "a lower mark took the lead");
            BloodlineAccess.applyEvent(leader, BloodlineEvent.KILLED_BY_PREY, SpeciesContext.of(leader, null));
            helper.assertTrue(BrotherhoodMarks.of(leader) == 0, "the mark was not reset: " + BrotherhoodMarks.of(leader));
            helper.assertTrue(leads(helper, brother), "the member with a mark did not take the lead after the leader's reset");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeadersMarkDroppingByTheCodePassesIt(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 5);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(4));
            helper.assertTrue(leads(helper, leader), "a lower mark took the lead");
            BloodlineAccess.set(leader, hof(3));
            helper.assertTrue(leads(helper, brother), "a mark that passed the leader's did not take the lead");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    // ---- who cannot ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineMemberCannotTakeItButTakesItAtLogin(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 2);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            online(leader);
            BloodlineAccess.set(brother, hof(5));
            groups.setMark(brother.getUUID(), 5);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "an offline member took the lead");
            online(leader, brother);
            BrotherhoodRegistry.onMarkChanged(brother);
            helper.assertTrue(leads(helper, brother), "the check at login did not run");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderOfflineIsComparedByTheRecordedMark(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 0);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            online(brother);
            groups.setMark(leader.getUUID(), 5);
            BloodlineAccess.set(brother, hof(3));
            helper.assertTrue(leads(helper, leader), "the recorded mark of the absent leader was ignored: 3 passed 5");
            groups.setMark(leader.getUUID(), 2);
            BrotherhoodLead.check(helper.getLevel().getServer(), brother.getUUID());
            helper.assertTrue(leads(helper, brother), "3 did not pass a recorded 2");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCodeBreakerCannotTakeIt(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 1);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            groups.setBreaker(brother.getUUID(), new BreakerRow(PackIndex.now() + DAY, List.of()));
            BloodlineAccess.set(brother, hof(9));
            helper.assertTrue(leads(helper, leader), "a Code-breaker took the lead");
            groups.clearBreaker(brother.getUUID());
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, brother), "the lead did not move when the breaker day ended");
        } finally {
            clean(helper, leader, brother);
            groups.clearBreaker(brother.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoDoesNotFitCannotTakeIt(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 2.5);
        online(leader, vampire);
        try {
            helper.assertTrue(join(leader, vampire), "a vampire could not join");
            BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().markKills(9).build());
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "a vampire member took the lead");
        } finally {
            clean(helper, leader, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMarkOfAnotherSpeciesIsNothing(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist potential = human(helper, 2.5);
        TalkingDuelist witch = human(helper, 3.5);
        online(leader, potential, witch);
        try {
            helper.assertTrue(join(leader, potential) && join(leader, witch), "no brotherhood");
            BloodlineAccess.set(potential, BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).markKills(9).build());
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).markKills(9).build());
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "a leftover mark of another species passed the leader");
            helper.assertTrue(BrotherhoodMarks.of(potential) == 0 && BrotherhoodMarks.of(witch) == 0, "a mark of another species counts");
        } finally {
            clean(helper, leader, potential, witch);
        }
        helper.succeed();
    }

    // ---- switches and dormancy ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leadByMarkOffKeepsTheLeader(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupsConfig.override("lead_by_mark", false);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(5));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "the lead moved with lead_by_mark off");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brotherhoodOffKeepsTheLeader(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            GroupsConfig.override("brotherhood_enabled", false);
            BloodlineAccess.set(brother, hof(5));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "the lead moved with brotherhood_enabled off");
            GroupsConfig.override("brotherhood_enabled", null);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, brother), "the lead did not move once brotherhoods were on again");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackAndAClanNeverMoveByMark(GameTestHelper helper) {
        TalkingDuelist clanLeader = GroupTestKit.vampire(helper, 1.5);
        TalkingDuelist clanMember = human(helper, 2.5);
        TalkingDuelist hybrid = human(helper, 3.5);
        TalkingDuelist packMember = human(helper, 4.5);
        online(clanLeader, clanMember, hybrid, packMember);
        GroupsConfig.override("open_founding", true);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(join(clanLeader, clanMember) && join(hybrid, packMember), "no clan or pack");
            BloodlineAccess.set(clanMember, hof(9));
            BloodlineAccess.set(packMember, hof(9));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(!BrotherhoodLead.check(helper.getLevel().getServer(), clanMember.getUUID()) && !BrotherhoodLead.check(helper.getLevel().getServer(), packMember.getUUID()), "check moved a lead");
            helper.assertTrue(leads(helper, clanLeader) && leads(helper, hybrid), "a clan or a pack moved by mark");
            helper.assertTrue(GroupIndex.get(helper.getLevel()).markOf(clanMember.getUUID()).isEmpty(), "a clan member's mark was recorded");
        } finally {
            clean(helper, clanLeader, clanMember, hybrid, packMember);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTypeSwitchPausesItAndKeepsTheMarksRows(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(leader, hof(0));
            BloodlineAccess.set(brother, hof(0));
            groups.setMark(brother.getUUID(), 4);
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GroupSwitcher.refresh(leader);
            helper.assertTrue(Groups.typeOf(leader) == GroupType.COVEN, "the group did not become a coven");
            BloodlineAccess.set(brother, hof(9));
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, leader), "the lead moved in a coven");
            helper.assertTrue(groups.markOf(brother.getUUID()).isPresent(), "the marks row was deleted by the switch");
            BloodlineAccess.set(leader, hof(0));
            GroupSwitcher.refresh(leader);
            helper.assertTrue(Groups.typeOf(leader) == GroupType.BROTHERHOOD, "the brotherhood did not come back");
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(groups.markOf(brother.getUUID()).orElse(-1) == 9 && leads(helper, brother), "the marks row did not refresh or the lead did not move when the type returned");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMarksRowFollowsTheMark(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 20);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(4));
            helper.assertTrue(groups.markOf(brother.getUUID()).orElse(-1) == 4, "row " + groups.markOf(brother.getUUID()));
            BloodlineAccess.set(brother, hof(7));
            helper.assertTrue(groups.markOf(brother.getUUID()).orElse(-1) == 7, "row " + groups.markOf(brother.getUUID()));
            BloodlineAccess.set(brother, hof(40));
            helper.assertTrue(groups.markOf(brother.getUUID()).orElse(-1) == 20, "the row is not capped: " + groups.markOf(brother.getUUID()));
            BloodlineAccess.set(brother, BloodlineState.NONE);
            helper.assertTrue(groups.markOf(brother.getUUID()).orElse(-1) == 0, "the row after losing the species: " + groups.markOf(brother.getUUID()));
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoLeavesLosesTheRow(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 20);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(4));
            groups.setBreaker(brother.getUUID(), new BreakerRow(PackIndex.now() + DAY, List.of(PackIndex.now())));
            helper.assertTrue(groups.markOf(brother.getUUID()).isPresent(), "no row to begin with");
            helper.assertTrue(PackCommand.leave(brother) == null, "could not leave");
            helper.assertTrue(groups.markOf(brother.getUUID()).isEmpty(), "a leaver keeps the mark row");
            helper.assertTrue(groups.breakerOf(brother.getUUID()).map(r -> r.strikes().size()).orElse(0) == 1, "leaving cleared the strikes");
        } finally {
            groups.clearBreaker(brother.getUUID());
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoChecksAreIdempotent(GameTestHelper helper) {
        TalkingDuelist founder = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist brother = human(helper, 2.5);
        online(founder, brother);
        try {
            helper.assertTrue(join(founder, brother), "no brotherhood");
            BloodlineAccess.set(brother, hof(1));
            int told = founder.count("supernaturalbloodlines.brotherhood.lead_you");
            helper.assertTrue(!BrotherhoodLead.check(helper.getLevel().getServer(), brother.getUUID()) && !BrotherhoodLead.check(helper.getLevel().getServer(), founder.getUUID()), "a second check moved the lead");
            helper.assertTrue(leads(helper, brother) && founder.count("supernaturalbloodlines.brotherhood.lead_you") == told && told == 1, "the old leader was told " + told + " times");
        } finally {
            clean(helper, founder, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCheckRunsAtLoginAndInTheSweep(GameTestHelper helper) {
        TalkingDuelist leader = marked(helper, 1.5, 1);
        TalkingDuelist brother = human(helper, 2.5);
        online(leader, brother);
        try {
            helper.assertTrue(join(leader, brother), "no brotherhood");
            online(leader);
            BloodlineAccess.set(brother, hof(6));
            helper.assertTrue(leads(helper, leader), "the member took it while away");
            online(leader, brother);
            BrotherhoodRegistry.sweep(helper.getLevel().getServer());
            helper.assertTrue(leads(helper, brother), "the sweep did not move the lead");
        } finally {
            clean(helper, leader, brother);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoJoinsWithTheHighestMarkTakesItAtOnce(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        TalkingDuelist newcomer = marked(helper, 2.5, 3);
        online(leader, newcomer);
        try {
            helper.assertTrue(join(leader, newcomer), "could not join");
            helper.assertTrue(leads(helper, newcomer) && ledBy(helper, leader, newcomer), "the newcomer with a mark did not take the lead at once");
        } finally {
            clean(helper, leader, newcomer);
        }
        helper.succeed();
    }

    // ---- the absence heir ----

    private static void build(GameTestHelper helper, UUID gone, long seen, ServerPlayer... members) {
        PackIndex index = packs(helper);
        long[] at = {1_000L};
        PackIndex.overrideClock(() -> at[0]);
        for (ServerPlayer m : members) {
            index.join(gone, m.getUUID());
            at[0] += 1_000L;
        }
        PackIndex.overrideClock(null);
        GroupIndex.get(helper.getLevel()).ensure(gone, GroupType.BROTHERHOOD, 1_000L);
        index.setLastSeen(gone, seen);
    }

    private static void drop(GameTestHelper helper, UUID gone) {
        packs(helper).leave(gone);
        packs(helper).takeNotices(gone);
        GroupIndex.get(helper.getLevel()).clearRanks(gone);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAbsenceHeirSkipsABreakerAndAMemberWhoDoesNotFit(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist breaker = marked(helper, 1.5, 9);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 2.5);
        TalkingDuelist fair = marked(helper, 3.5, 2);
        online(breaker, vampire, fair);
        BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().markKills(15).build());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, now - 8 * DAY, breaker, vampire, fair);
            groups.setBreaker(breaker.getUUID(), new BreakerRow(now + DAY, List.of()));
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1, "the absent leader was not deposed");
            helper.assertTrue(leads(helper, fair), "the heir is not the only member who counts: " + packs(helper).leaderOf(breaker.getUUID()));
        } finally {
            groups.clearBreaker(breaker.getUUID());
            drop(helper, gone);
            clean(helper, breaker, vampire, fair);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAbsenceHeirIsStillTheHighestOnlineMark(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist low = marked(helper, 1.5, 3);
        TalkingDuelist high = marked(helper, 2.5, 8);
        online(low, high);
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, now - 8 * DAY, low, high);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1 && leads(helper, high), "the highest mark does not lead");
        } finally {
            drop(helper, gone);
            clean(helper, low, high);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineMarkNeverInherits(GameTestHelper helper) {
        UUID gone = UUID.randomUUID();
        TalkingDuelist away = marked(helper, 1.5, 15);
        TalkingDuelist here = marked(helper, 2.5, 1);
        online(here);
        long now = System.currentTimeMillis();
        try {
            build(helper, gone, now - 8 * DAY, away, here);
            packs(helper).setLastSeen(away.getUUID(), now - 8 * DAY);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1 && leads(helper, here), "an offline mark inherited");
        } finally {
            drop(helper, gone);
            clean(helper, away, here);
        }
        helper.succeed();
    }
}
