package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import com.mojang.brigadier.CommandDispatcher;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader;

/** Plan 20a on a Werewolves world: a pack is founded and kept as before, and a leader's switch loses no wolf. */
@PrefixGameTestTemplate(false)
public final class GroupWolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GroupWolfGameTests() {
    }

    private static TalkingDuelist wolf(GameTestHelper helper, Rank rank, java.util.UUID packAlpha, int level, double x) {
        TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).protectPath(false).packAlpha(packAlpha)
                .betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    private static void becomeAWitch(TalkingDuelist p) {
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlphaStillFoundsAPackExactlyAsBefore(GameTestHelper helper) {
        TalkingDuelist alpha = wolf(helper, Rank.ALPHA, null, 5, 1.5);
        TalkingDuelist guest = human(helper, 2.5);
        online(alpha, guest);
        try {
            helper.assertTrue(join(alpha, guest), "the pack could not be founded");
            helper.assertTrue(Groups.typeOf(guest) == GroupType.PACK, "the group is " + Groups.typeOf(guest));
            helper.assertTrue(alpha.received("supernaturalbloodlines.pack.invited") && guest.received("supernaturalbloodlines.pack.joined"), "the old pack wording changed");
            helper.assertTrue(PackCommand.invite(guest, alpha) != null, "a Packmate founded a pack");
        } finally {
            clean(helper, alpha, guest);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWolfBetaSurvivesTheirLeadersSwitch(GameTestHelper helper) {
        TalkingDuelist leader = human(helper, 1.5);
        BloodlineAccess.set(leader, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
        TalkingDuelist beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        PackIndex index = PackIndex.get(helper.getLevel());
        online(leader, beta);
        try {
            index.join(leader.getUUID(), beta.getUUID());
            GroupIndex.get(helper.getLevel()).ensure(leader.getUUID(), GroupType.PACK, PackIndex.now());
            helper.assertTrue(Groups.typeOf(beta) == GroupType.PACK, "no pack to begin with");
            BloodlineAccess.set(leader, BloodlineState.NONE);
            GroupSwitcher.refresh(leader);
            helper.assertTrue(Groups.typeOf(beta) == GroupType.BROTHERHOOD, "the group is " + Groups.typeOf(beta) + ", the leader derives " + Groups.derive(leader));
            helper.assertTrue(index.alphaOf(beta.getUUID()).map(leader.getUUID()::equals).orElse(false), "the Beta left the index");
            helper.assertTrue(leader.getUUID().equals(BloodlineAccess.get(beta).wolf().packAlpha()) && BloodlineAccess.get(beta).wolf().rank() == Rank.BETA, "the wolf state changed: " + BloodlineAccess.get(beta).wolf());
        } finally {
            clean(helper, leader, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void slashPackStillWorks(GameTestHelper helper) {
        CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
        PackCommand.register(d);
        var pack = d.getRoot().getChild("pack");
        for (String literal : new String[] {"invite", "accept", "leave", "kick", "ally", "challenge", "info", "heal", "protect", "emissary", "roar", "anchor"}) {
            helper.assertTrue(pack.getChild(literal) != null, "/pack lost " + literal);
        }
        helper.assertTrue(d.getRoot().getChild("sb").getChild("pack").getRedirect() == pack, "/sb pack is not the same tree");
        helper.succeed();
    }

    /** A wolf Alpha who is a plain member of somebody's coven has no pack: the profile shows no pack bonus (it is a pack's). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWolfAlphaInACovenShowsNoPackBonus(GameTestHelper helper) {
        TalkingDuelist witch = GroupTestKit.witch(helper, 1.5);
        TalkingDuelist alpha = wolf(helper, Rank.ALPHA, null, 5, 2.5);
        online(witch, alpha);
        try {
            var alone = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, alpha.getUUID()).facts();
            helper.assertTrue(alone.containsKey(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.BONUS), "an Alpha with no group has a pack bonus to show: " + alone.keySet());
            helper.assertTrue(join(witch, alpha), "the Alpha could not join the coven");
            var facts = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, alpha.getUUID()).facts();
            helper.assertTrue(!facts.containsKey(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.BONUS), "a member of a coven shows a pack bonus: " + facts.get(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.BONUS));
            var group = facts.get(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.PACK);
            helper.assertTrue(group != null && group.size() == 2 && group.get(1).equals("coven"), "the group fact is " + group);
        } finally {
            clean(helper, witch, alpha);
        }
        helper.succeed();
    }}
