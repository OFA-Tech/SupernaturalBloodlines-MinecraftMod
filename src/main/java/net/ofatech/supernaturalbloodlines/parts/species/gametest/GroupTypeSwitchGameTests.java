package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20a, R241: a leader who changes kind changes the group's type, and nobody and nothing is lost. */
@PrefixGameTestTemplate(false)
public final class GroupTypeSwitchGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String CHANGED = "supernaturalbloodlines.group.type_changed.coven";

    private GroupTypeSwitchGameTests() {
    }

    /** A hybrid-kind leader of a pack (open founding stands in for the Alpha rank, which needs Werewolves). */
    private static TalkingDuelist hybridLeader(GameTestHelper helper, double x) {
        TalkingDuelist p = human(helper, x);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
        return p;
    }

    private static GroupType stored(GameTestHelper helper, UUID leader) {
        GroupIndex g = GroupIndex.get(helper.getLevel());
        return g.typeOf(g.existingGroupOf(leader).orElseThrow());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHybridLeaderWhoBecomesAHumanSwitchesToACoven(GameTestHelper helper) {
        TalkingDuelist leader = hybridLeader(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(leader, a);
        GroupsConfig.override("open_founding", true);
        try {
            helper.assertTrue(join(leader, a) && stored(helper, leader.getUUID()) == GroupType.PACK, "no pack to begin with: " + Groups.typeOf(leader));
            BloodlineAccess.set(leader, BloodlineState.NONE);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.COVEN && Groups.typeOf(a) == GroupType.COVEN, "the group is " + stored(helper, leader.getUUID()));
            helper.assertTrue(a.received(CHANGED), "the member was not told");
        } finally {
            clean(helper, leader, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSwitchLosesNobody(GameTestHelper helper) {
        TalkingDuelist leader = hybridLeader(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(leader, a, b);
        GroupsConfig.override("open_founding", true);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, a) && join(leader, b), "the members could not join");
            List<UUID> before = index.betasOf(leader.getUUID());
            BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.COVEN, "no switch");
            helper.assertTrue(index.betasOf(leader.getUUID()).equals(before), "the members changed: " + index.betasOf(leader.getUUID()));
            BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.PACK && index.betasOf(leader.getUUID()).equals(before), "the way back lost someone");
        } finally {
            clean(helper, leader, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSwitchIsToldOnce(GameTestHelper helper) {
        TalkingDuelist leader = hybridLeader(helper, 1.5);
        TalkingDuelist here = human(helper, 2.5);
        TalkingDuelist away = human(helper, 3.5);
        online(leader, here, away);
        GroupsConfig.override("open_founding", true);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, here) && join(leader, away), "the members could not join");
            online(leader, here); // the second member is away from here on
            BloodlineAccess.set(leader, BloodlineState.NONE);
            for (int i = 0; i < 5; i++) {
                GroupSwitcher.sweep(helper.getLevel().getServer());
            }
            helper.assertTrue(here.count(CHANGED) == 1, "the online member was told " + here.count(CHANGED) + " times");
            helper.assertTrue(index.noticeCount(away.getUUID()) == 1, "the member who was away has " + index.noticeCount(away.getUUID()) + " notices waiting");
        } finally {
            clean(helper, leader, here, away);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void switchBackRestoresDormantRows(GameTestHelper helper) {
        TalkingDuelist leader = human(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(leader, a);
        VampirismAPI.factionPlayerHandler(leader).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, a) && stored(helper, leader.getUUID()) == GroupType.CLAN, "no clan: " + Groups.typeOf(leader));
            groups.setRank(a.getUUID(), GroupRank.DAY_WALKER);
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.COVEN, "no switch to a coven");
            helper.assertTrue(groups.activeRankOf(a.getUUID(), GroupType.COVEN) == GroupRank.NONE, "a clan rank shows in a coven");
            helper.assertTrue(groups.rankOf(a.getUUID()) == GroupRank.DAY_WALKER, "the clan rank was deleted by the switch");
            VampirismAPI.factionPlayerHandler(leader).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(stored(helper, leader.getUUID()) == GroupType.CLAN && groups.activeRankOf(a.getUUID(), GroupType.CLAN) == GroupRank.DAY_WALKER, "the rank did not come back");
        } finally {
            clean(helper, leader, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineLeaderIsNotSwitched(GameTestHelper helper) {
        TalkingDuelist leader = hybridLeader(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(leader, a);
        GroupsConfig.override("open_founding", true);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, a), "the member could not join");
            UUID group = groups.existingGroupOf(leader.getUUID()).orElseThrow();
            BloodlineAccess.set(leader, BloodlineState.NONE);
            groups.setType(group, GroupType.PACK, 1L);
            online(a);
            GroupSwitcher.sweep(helper.getLevel().getServer());
            helper.assertTrue(groups.typeOf(group) == GroupType.PACK, "an offline leader's group was switched");
        } finally {
            clean(helper, leader, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void typeSinceIsStamped(GameTestHelper helper) {
        TalkingDuelist leader = hybridLeader(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(leader, a);
        GroupsConfig.override("open_founding", true);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            PackIndex.overrideClock(() -> 1_000L);
            helper.assertTrue(join(leader, a), "the member could not join");
            UUID group = groups.existingGroupOf(leader.getUUID()).orElseThrow();
            helper.assertTrue(groups.typeSince(group) == 1_000L, "the first stamp is " + groups.typeSince(group));
            PackIndex.overrideClock(() -> 9_000L);
            BloodlineAccess.set(leader, BloodlineState.NONE);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(groups.typeSince(group) == 9_000L, "the switch was not stamped: " + groups.typeSince(group));
        } finally {
            clean(helper, leader, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void allianceSurvivesATypeSwitch(GameTestHelper helper) {
        TalkingDuelist hybrid = hybridLeader(helper, 1.5);
        TalkingDuelist w = GroupTestKit.witch(helper, 2.5);
        ServerPlayer hm = human(helper, 3.5);
        ServerPlayer wm = human(helper, 4.5);
        online(hybrid, w, hm, wm);
        GroupsConfig.override("open_founding", true);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(hybrid, hm) && join(w, wm), "the groups were not made");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand.allyInvite(hybrid, w) == null
                    && net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand.allyAccept(w, net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand.allyRequests().tokenOf(hybrid.getUUID(), w.getUUID()).orElseThrow()) == null, "no alliance");
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GroupSwitcher.refresh(hybrid);
            helper.assertTrue(stored(helper, hybrid.getUUID()) == GroupType.COVEN, "no switch");
            helper.assertTrue(index.alliedPlayers(hm.getUUID(), wm.getUUID()), "the alliance did not survive the switch");
        } finally {
            clean(helper, hybrid, w, hm, wm);
        }
        helper.succeed();
    }
}
