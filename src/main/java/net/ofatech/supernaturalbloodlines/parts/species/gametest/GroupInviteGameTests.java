package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.hunter;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupCommand;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupGate;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20a, R240 to R243, R249: founding, joining, leaving, kicking and allying in groups of every type. Runs with or without Werewolves. */
@PrefixGameTestTemplate(false)
public final class GroupInviteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GroupInviteGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchInvitesAndAHumanJoinsACoven(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            helper.assertTrue(join(w, h), "the human could not join the coven");
            helper.assertTrue(Groups.typeOf(h) == GroupType.COVEN, "the member's group is " + Groups.typeOf(h));
            helper.assertTrue(Groups.typeOf(w) == GroupType.COVEN, "the leader's group is " + Groups.typeOf(w));
            helper.assertTrue(w.received("supernaturalbloodlines.group.invited.coven") && h.received("supernaturalbloodlines.group.joined.coven"), "the coven was not named: " + w.received());
        } finally {
            clean(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireFoundsAClan(GameTestHelper helper) {
        TalkingDuelist v = vampire(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(v, h);
        try {
            helper.assertTrue(join(v, h) && Groups.typeOf(h) == GroupType.CLAN, "no clan: " + Groups.typeOf(v));
            helper.assertTrue(GroupIndex.get(helper.getLevel()).existingGroupOf(v.getUUID()).map(g -> GroupIndex.get(helper.getLevel()).typeOf(g)).orElse(null) == GroupType.CLAN, "the stored type is not CLAN");
        } finally {
            clean(helper, v, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHunterFoundsABrotherhoodAndAHumanJoins(GameTestHelper helper) {
        TalkingDuelist hu = hunter(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(hu, h);
        try {
            helper.assertTrue(join(hu, h) && Groups.typeOf(h) == GroupType.BROTHERHOOD, "no brotherhood: " + Groups.typeOf(hu));
        } finally {
            clean(helper, hu, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireCannotFoundABrotherhood(GameTestHelper helper) {
        TalkingDuelist v = vampire(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(v, h);
        try {
            helper.assertTrue(!GroupGate.fit(v).hunter(), "a vampire fits the hunters");
            helper.assertTrue(join(v, h) && Groups.typeOf(v) != GroupType.BROTHERHOOD && Groups.typeOf(v) == GroupType.CLAN, "a vampire's group is " + Groups.typeOf(v));
        } finally {
            clean(helper, v, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHumanCannotFoundAGroup(GameTestHelper helper) {
        TalkingDuelist a = human(helper, 1.5);
        TalkingDuelist b = human(helper, 2.5);
        online(a, b);
        try {
            var refused = PackCommand.invite(a, b);
            helper.assertTrue(refused != null && key(refused).equals("supernaturalbloodlines.group.not_fit.coven"), "the refusal was " + key(refused));
            helper.assertTrue(PackIndex.get(helper.getLevel()).betasOf(a.getUUID()).isEmpty(), "a group was made");
        } finally {
            clean(helper, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void openFoundingLetsAHumanLeadACoven(GameTestHelper helper) {
        TalkingDuelist a = human(helper, 1.5);
        TalkingDuelist b = human(helper, 2.5);
        online(a, b);
        GroupsConfig.override("open_founding", true);
        try {
            helper.assertTrue(join(a, b) && Groups.typeOf(b) == GroupType.COVEN, "open founding did not lift the gate");
        } finally {
            clean(helper, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClanCannotBeFoundedWhenClansAreOff(GameTestHelper helper) {
        TalkingDuelist v = vampire(helper, 1.5);
        TalkingDuelist w = witch(helper, 2.5);
        TalkingDuelist h = human(helper, 3.5);
        TalkingDuelist h2 = human(helper, 4.5);
        online(v, w, h, h2);
        GroupsConfig.override("clans_enabled", false);
        try {
            var refused = PackCommand.invite(v, h);
            helper.assertTrue(refused != null && key(refused).equals("supernaturalbloodlines.group.group_off.clan"), "the refusal was " + key(refused));
            helper.assertTrue(join(w, h2), "switching clans off stopped a coven");
        } finally {
            clean(helper, v, w, h, h2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void maxMembersRefusesTheInviteAtTheCap(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(w, a, b);
        GroupsConfig.override("max_members", 1);
        try {
            helper.assertTrue(join(w, a), "the first member could not join");
            var refused = PackCommand.invite(w, b);
            helper.assertTrue(refused != null && key(refused).equals("supernaturalbloodlines.group.full.coven"), "the refusal was " + key(refused));
            helper.assertTrue(PackCommand.kick(w, a.getUUID(), "a") == null, "a full group could not kick");
        } finally {
            clean(helper, w, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kickAndLeaveWorkInACoven(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(w, a, b);
        PackIndex index = PackIndex.get(helper.getLevel());
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(w, a) && join(w, b), "the members could not join");
            groups.setRank(a.getUUID(), GroupRank.ELDER);
            helper.assertTrue(PackCommand.leave(a) == null && index.alphaOf(a.getUUID()).isEmpty(), "leaving did not work");
            helper.assertTrue(groups.rankOf(a.getUUID()) == GroupRank.NONE, "a rank stayed after leaving");
            helper.assertTrue(a.received("supernaturalbloodlines.group.left.coven"), "the leaver was not told in coven words");
            helper.assertTrue(PackCommand.kickNamed(w, b.getUUID().toString()) == null && index.alphaOf(b.getUUID()).isEmpty(), "the kick did not work");
            helper.assertTrue(PackCommand.leave(w) != null, "a leader left their own coven");
        } finally {
            clean(helper, w, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void allyAcrossTypes(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist v = vampire(helper, 2.5);
        TalkingDuelist wm = human(helper, 3.5);
        TalkingDuelist vm = human(helper, 4.5);
        online(w, v, wm, vm);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(w, wm) && join(v, vm), "the groups were not made");
            helper.assertTrue(PackCommand.allyInvite(w, v) == null, "the alliance request was refused");
            String token = PackCommand.allyRequests().tokenOf(w.getUUID(), v.getUUID()).orElseThrow();
            helper.assertTrue(PackCommand.allyAccept(v, token) == null, "the alliance was refused");
            helper.assertTrue(index.alliedPlayers(wm.getUUID(), vm.getUUID()) && index.alliedPlayers(vm.getUUID(), wm.getUUID()), "the members are not allied");
            helper.assertTrue(Groups.typeOf(wm) == GroupType.COVEN && Groups.typeOf(vm) == GroupType.CLAN, "the types changed");
        } finally {
            clean(helper, w, v, wm, vm);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alliedMembersCannotHurtEachOtherAcrossTypes(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist v = vampire(helper, 2.5);
        TalkingDuelist wm = human(helper, 3.5);
        TalkingDuelist vm = human(helper, 4.5);
        online(w, v, wm, vm);
        try {
            helper.assertTrue(join(w, wm) && join(v, vm), "the groups were not made");
            helper.assertTrue(PackCommand.allyInvite(w, v) == null, "the alliance request was refused");
            helper.assertTrue(PackCommand.allyAccept(v, PackCommand.allyRequests().tokenOf(w.getUUID(), v.getUUID()).orElseThrow()) == null, "the alliance was refused");
            helper.assertTrue(hurt(wm, vm).isCanceled() && hurt(vm, wm).isCanceled(), "an ally was hurt");
            TalkingDuelist stranger = human(helper, 5.5);
            helper.assertTrue(!hurt(stranger, vm).isCanceled(), "a stranger was protected from");
            clean(helper, stranger);
        } finally {
            clean(helper, w, v, wm, vm);
        }
        helper.succeed();
    }

    private static LivingIncomingDamageEvent hurt(ServerPlayer attacker, ServerPlayer victim) {
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(victim, new net.neoforged.neoforge.common.damagesource.DamageContainer(attacker.damageSources().playerAttack(attacker), 1f)));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWolfOnlyLiteralAnswersWrongTypeInACoven(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(w, a);
        try {
            helper.assertTrue(join(w, a), "the coven was not made");
            CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
            GroupCommand.register(d);
            helper.assertTrue(d.getRoot().getChild("group").getChild("anchor") == null && d.getRoot().getChild("group").getChild("heal") == null, "the group tree has a wolf literal");
            helper.assertTrue(d.execute("group challenge accept", w.createCommandSourceStack()) == 0 && w.received("supernaturalbloodlines.group.wrong_type.coven"),
                    "a challenge in a coven did not answer wrong_type");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("the command did not parse: " + e.getMessage());
        } finally {
            clean(helper, w, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheViewersOwnGroupIsCarried(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist v = vampire(helper, 2.5);
        TalkingDuelist wm = human(helper, 3.5);
        TalkingDuelist vm = human(helper, 4.5);
        TalkingDuelist alone = human(helper, 5.5);
        online(w, v, wm, vm, alone);
        try {
            helper.assertTrue(join(w, wm) && join(v, vm), "the groups were not made");
            var view = net.ofatech.supernaturalbloodlines.parts.species.group.GroupViews.of(wm);
            var ids = view.members().stream().map(r -> r.id().orElseThrow()).toList();
            helper.assertTrue(view.hasGroup() && view.type() == GroupType.COVEN && ids.equals(java.util.List.of(w.getUUID(), wm.getUUID())), "the view lists " + ids);
            helper.assertTrue(view.viewerRank() == net.ofatech.supernaturalbloodlines.core.group.GroupView.MEMBER && net.ofatech.supernaturalbloodlines.parts.species.group.GroupViews.of(w).viewerRank() == net.ofatech.supernaturalbloodlines.core.group.GroupView.LEADER, "the viewer's rank is wrong");
            var none = net.ofatech.supernaturalbloodlines.parts.species.group.GroupViews.of(alone);
            helper.assertTrue(!none.hasGroup() && none.members().isEmpty(), "a player with no group sees people");
        } finally {
            clean(helper, w, v, wm, vm, alone);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAliasesAreTheSameTree(GameTestHelper helper) {
        CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
        GroupCommand.register(d);
        var group = d.getRoot().getChild("group");
        for (String alias : new String[] {"clan", "coven", "brotherhood"}) {
            helper.assertTrue(d.getRoot().getChild(alias) != null && d.getRoot().getChild(alias).getRedirect() == group, "/" + alias + " is not an alias of /group");
        }
        helper.assertTrue(d.getRoot().getChild("sb").getChild("group").getRedirect() == group, "/sb group is not the same tree");
        helper.succeed();
    }
}
