package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupGate;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodRegistry;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodScope;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20d Task 4 (R290, R298): a plain human leads a brotherhood, who fits, the one scope question, and the type following the leader. */
@PrefixGameTestTemplate(false)
public final class BrotherhoodScopeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NOW_BROTHERHOOD = "supernaturalbloodlines.group.type_changed.brotherhood";
    private static final String NOW_COVEN = "supernaturalbloodlines.group.type_changed.coven";
    private static final String NOW_CLAN = "supernaturalbloodlines.group.type_changed.clan";

    private BrotherhoodScopeGameTests() {
    }

    private static GroupType stored(GameTestHelper helper, UUID leader) {
        GroupIndex g = GroupIndex.get(helper.getLevel());
        return g.typeOf(g.existingGroupOf(leader).orElseThrow());
    }

    private static boolean applies(GameTestHelper helper, ServerPlayer leader, ServerPlayer member) {
        return BrotherhoodScope.appliesTo(helper.getLevel(), leader.getUUID(), member);
    }

    private static TalkingDuelist hunter(GameTestHelper helper, double x) {
        return GroupTestKit.hunter(helper, x);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHunterAndAPlainHumanFit(GameTestHelper helper) {
        TalkingDuelist h = hunter(helper, 1.5);
        TalkingDuelist p = human(helper, 2.5);
        try {
            helper.assertTrue(BrotherhoodScope.fits(h), "a hunter does not fit");
            helper.assertTrue(BrotherhoodScope.fits(p), "a plain human does not fit");
            helper.assertTrue(Groups.isPlainHuman(p), "isPlainHuman is wrong");
        } finally {
            clean(helper, h, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchAVampireAWerewolfAndAHybridDoNotFit(GameTestHelper helper) {
        TalkingDuelist witch = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 2.5);
        TalkingDuelist wolf = human(helper, 3.5);
        TalkingDuelist hybrid = human(helper, 4.5);
        try {
            BloodlineAccess.set(wolf, BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().lineage(WolfState.Lineage.BORN).build()).build());
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            for (ServerPlayer p : List.of(witch, vampire, wolf, hybrid)) {
                helper.assertTrue(!BrotherhoodScope.fits(p) && !Groups.isPlainHuman(p), p.getGameProfile().getName() + " fits");
            }
        } finally {
            clean(helper, witch, vampire, wolf, hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPotentialFits(GameTestHelper helper) {
        TalkingDuelist p = human(helper, 1.5);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).build());
            helper.assertTrue(BrotherhoodScope.fits(p), "a Potential does not fit");
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void appliesToNeedsABrotherhood(GameTestHelper helper) {
        TalkingDuelist brother = hunter(helper, 1.5);
        TalkingDuelist b = human(helper, 2.5);
        TalkingDuelist witch = GroupTestKit.witch(helper, 3.5);
        TalkingDuelist c = human(helper, 4.5);
        TalkingDuelist vamp = GroupTestKit.vampire(helper, 5.5);
        TalkingDuelist d = human(helper, 6.5);
        online(brother, b, witch, c, vamp, d);
        try {
            helper.assertTrue(join(brother, b) && join(witch, c) && join(vamp, d), "the groups could not be made");
            helper.assertTrue(applies(helper, brother, b) && applies(helper, brother, brother), "the same member in a brotherhood: no");
            helper.assertTrue(!applies(helper, witch, c) && !applies(helper, vamp, d), "the same member in a coven or a clan: yes");
            helper.assertTrue(!applies(helper, brother, c), "a fitting member of another group received this brotherhood's perks");
        } finally {
            clean(helper, brother, b, witch, c, vamp, d);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void appliesToIsFalseWhenBrotherhoodsAreOff(GameTestHelper helper) {
        TalkingDuelist brother = hunter(helper, 1.5);
        TalkingDuelist b = human(helper, 2.5);
        online(brother, b);
        try {
            helper.assertTrue(join(brother, b) && applies(helper, brother, b), "no brotherhood to begin with");
            GroupsConfig.override("brotherhood_enabled", false);
            helper.assertTrue(!applies(helper, brother, b), "brotherhood_enabled false still applies");
            GroupsConfig.override("brotherhood_enabled", null);
            GroupsConfig.override("groups_enabled", false);
            helper.assertTrue(!applies(helper, brother, b), "groups_enabled false still applies");
        } finally {
            clean(helper, brother, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ofAnswersNullInAnotherType(GameTestHelper helper) {
        TalkingDuelist witch = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist c = human(helper, 2.5);
        TalkingDuelist loner = human(helper, 3.5);
        online(witch, c, loner);
        try {
            helper.assertTrue(join(witch, c), "no coven");
            helper.assertTrue(BrotherhoodScope.of(helper.getLevel(), witch.getUUID()) == null && BrotherhoodScope.of(helper.getLevel(), c.getUUID()) == null, "a coven answers as a brotherhood");
            helper.assertTrue(BrotherhoodScope.of(helper.getLevel(), loner.getUUID()) == null, "a player in no group answers as a brotherhood");
        } finally {
            clean(helper, witch, c, loner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ofAnswersTheLeaderAndGroupForAMember(GameTestHelper helper) {
        TalkingDuelist brother = hunter(helper, 1.5);
        TalkingDuelist b = human(helper, 2.5);
        online(brother, b);
        try {
            helper.assertTrue(join(brother, b), "no brotherhood");
            var forMember = BrotherhoodScope.of(helper.getLevel(), b.getUUID());
            var forLeader = BrotherhoodScope.of(helper.getLevel(), brother.getUUID());
            UUID group = GroupIndex.get(helper.getLevel()).existingGroupOf(brother.getUUID()).orElseThrow();
            helper.assertTrue(forMember != null && forMember.leader().equals(brother.getUUID()) && forMember.group().equals(group), "member: " + forMember);
            helper.assertTrue(forLeader != null && forLeader.equals(forMember), "leader: " + forLeader);
        } finally {
            clean(helper, brother, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHumanLedCovenSwitchesOnceAndKeepsItsRows(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist elder = human(helper, 2.5);
        TalkingDuelist member = human(helper, 3.5);
        online(leader, elder, member);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, elder) && join(leader, member) && stored(helper, leader.getUUID()) == GroupType.COVEN, "no coven: " + Groups.typeOf(leader));
            groups.setRank(elder.getUUID(), GroupRank.ELDER);
            BloodlineAccess.set(leader, BloodlineState.NONE);
            GroupSwitcher.refresh(leader);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.BROTHERHOOD, "the group is " + stored(helper, leader.getUUID()));
            helper.assertTrue(elder.count(NOW_BROTHERHOOD) == 1 && member.count(NOW_BROTHERHOOD) == 1 && leader.count(NOW_BROTHERHOOD) == 1, "the notice was sent "
                    + elder.count(NOW_BROTHERHOOD) + "/" + member.count(NOW_BROTHERHOOD) + "/" + leader.count(NOW_BROTHERHOOD) + " times");
            helper.assertTrue(groups.rankOf(elder.getUUID(), GroupType.COVEN) == GroupRank.ELDER, "the Elder row was deleted by the switch");
            helper.assertTrue(groups.rankOf(elder.getUUID(), GroupType.BROTHERHOOD) == GroupRank.NONE, "the Elder row shows in the brotherhood");
            BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.COVEN && groups.rankOf(elder.getUUID(), GroupType.COVEN) == GroupRank.ELDER, "the coven did not come back with its row");
        } finally {
            clean(helper, leader, elder, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTensuraRaceIsNotAPlainHuman(GameTestHelper helper) {
        TalkingDuelist leader = human(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(leader, a);
        SpeciesEligibility.HumanLikeCheck before = SpeciesEligibility.check();
        GroupsConfig.override("open_founding", true);
        try {
            SpeciesEligibility.setCheck(p -> false);
            helper.assertTrue(join(leader, a), "could not join");
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.COVEN && Groups.derive(leader) == GroupType.COVEN, "a race that is not human-like leads " + stored(helper, leader.getUUID()));
            helper.assertTrue(!BrotherhoodScope.fits(a), "a race that is not human-like fits");
            SpeciesEligibility.setCheck(before);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.BROTHERHOOD, "a human-like leader did not switch it");
        } finally {
            SpeciesEligibility.setCheck(before);
            clean(helper, leader, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHunterWhoLeavesTheHuntersKeepsTheBrotherhood(GameTestHelper helper) {
        TalkingDuelist leader = hunter(helper, 1.5);
        TalkingDuelist member = human(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member) && stored(helper, leader.getUUID()) == GroupType.BROTHERHOOD, "no brotherhood: " + Groups.typeOf(leader));
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.BROTHERHOOD && Groups.typeOf(member) == GroupType.BROTHERHOOD, "the group is " + stored(helper, leader.getUUID()));
            helper.assertTrue(member.count(NOW_COVEN) == 0 && member.count(NOW_BROTHERHOOD) == 0, "a switch was announced");
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderWhoBecomesAVampireSwitchesToAClan(GameTestHelper helper) {
        TalkingDuelist leader = hunter(helper, 1.5);
        TalkingDuelist member = human(helper, 2.5);
        online(leader, member);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, member), "no brotherhood");
            groups.setMark(member.getUUID(), 3);
            List<UUID> before = index.betasOf(leader.getUUID());
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            VampirismAPI.factionPlayerHandler(leader).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.CLAN, "the group is " + stored(helper, leader.getUUID()));
            helper.assertTrue(index.betasOf(leader.getUUID()).equals(before), "the members changed");
            helper.assertTrue(groups.markOf(member.getUUID()).orElse(-1) == 3, "the dormant mark row was touched");
            helper.assertTrue(member.count(NOW_CLAN) == 1, "the member was told " + member.count(NOW_CLAN) + " times");
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderWhoBecomesAWitchSwitchesToACoven(GameTestHelper helper) {
        TalkingDuelist leader = hunter(helper, 1.5);
        TalkingDuelist member = human(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member), "no brotherhood");
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.COVEN && member.count(NOW_COVEN) == 1, "the group is " + stored(helper, leader.getUUID()));
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void joiningTheHuntersMakesAWitchLedGroupABrotherhood(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist member = human(helper, 2.5);
        online(leader, member);
        try {
            helper.assertTrue(join(leader, member) && stored(helper, leader.getUUID()) == GroupType.COVEN, "no coven");
            VampirismAPI.factionPlayerHandler(leader).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.BROTHERHOOD && member.count(NOW_BROTHERHOOD) == 1, "the group is " + stored(helper, leader.getUUID()));
        } finally {
            clean(helper, leader, member);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void openFoundingLetsAHumanLeadABrotherhood(GameTestHelper helper) {
        TalkingDuelist p = human(helper, 1.5);
        TalkingDuelist other = human(helper, 2.5);
        online(p, other);
        GroupsConfig.override("open_founding", true);
        try {
            helper.assertTrue(join(p, other) && Groups.typeOf(other) == GroupType.BROTHERHOOD, "the group is " + Groups.typeOf(other));
        } finally {
            clean(helper, p, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void foundingIsOffWhenBrotherhoodsAreOff(GameTestHelper helper) {
        TalkingDuelist h = hunter(helper, 1.5);
        TalkingDuelist other = human(helper, 2.5);
        online(h, other);
        GroupsConfig.override("brotherhood_enabled", false);
        try {
            helper.assertTrue(GroupGate.canLead(h).equals(Optional.of("group_off")), "a hunter may found: " + GroupGate.canLead(h));
            helper.assertTrue(PackCommand.invite(h, other) != null, "the invite was sent");
        } finally {
            clean(helper, h, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackAndAClanAreUnaffected(GameTestHelper helper) {
        TalkingDuelist vamp = GroupTestKit.vampire(helper, 1.5);
        TalkingDuelist hybrid = human(helper, 2.5);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(Groups.derive(vamp) == GroupType.CLAN, "a vampire derives " + Groups.derive(vamp));
            helper.assertTrue(Groups.derive(hybrid) == GroupType.PACK, "a hybrid derives " + Groups.derive(hybrid));
        } finally {
            clean(helper, vamp, hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRegistrationRunsWithoutWerewolves(GameTestHelper helper) {
        TalkingDuelist h = hunter(helper, 1.5);
        TalkingDuelist p = human(helper, 2.5);
        online(h, p);
        try {
            helper.assertTrue(BrotherhoodRegistry.registered(), "the brotherhood registry was not registered");
            helper.assertTrue(join(h, p) && Groups.typeOf(p) == GroupType.BROTHERHOOD, "a hunter could not found a brotherhood with a human member");
        } finally {
            clean(helper, h, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoDoesNotFitStaysAMember(GameTestHelper helper) {
        TalkingDuelist h = hunter(helper, 1.5);
        TalkingDuelist v = GroupTestKit.vampire(helper, 2.5);
        online(h, v);
        try {
            helper.assertTrue(join(h, v), "a vampire could not join (allowed today, with no perks)");
            helper.assertTrue(Groups.typeOf(v) == GroupType.BROTHERHOOD && !applies(helper, h, v) && applies(helper, h, h), "the vampire member is read as " + applies(helper, h, v));
        } finally {
            clean(helper, h, v);
        }
        helper.succeed();
    }
}
