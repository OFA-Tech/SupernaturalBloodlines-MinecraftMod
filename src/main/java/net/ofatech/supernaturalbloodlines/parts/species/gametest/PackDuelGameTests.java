package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Plan 14, the leadership duel (owner ruling): a co-Alpha challenges, the leader accepts, the first defeat decides, nobody dies. */
@PrefixGameTestTemplate(false)
public final class PackDuelGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private PackDuelGameTests() {
    }

    private static ServerPlayer alpha(GameTestHelper helper, int level, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.ALPHA).geneRolled(true).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    /** A player who is no werewolf: their kill is no spark steal, which a werewolf Alpha's would be. */
    private static ServerPlayer human(GameTestHelper helper, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        return p;
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
    }

    private static void clean(GameTestHelper helper, ServerPlayer... players) {
        PackIndex index = PackIndex.get(helper.getLevel());
        SireCall.overridePlayerLookup(null);
        PackDuel.reset();
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            index.takeDissolved(p.getUUID());
            index.takeNotice(p.getUUID());
            p.removeAllEffects();
            p.setHealth(p.getMaxHealth());
        }
    }

    private static boolean join(ServerPlayer leader, ServerPlayer target) {
        if (PackCommand.invite(leader, target) != null) {
            return false;
        }
        return PackCommand.accept(target, PackCommand.requests().tokenOf(leader.getUUID(), target.getUUID()).orElseThrow()) == null;
    }

    /** What the opponent's blow does at the end of a fight: would kill {@code loser}. @return the event, after the listeners ran */
    private static LivingDeathEvent finish(ServerPlayer winner, ServerPlayer loser) {
        LivingDeathEvent e = new LivingDeathEvent(loser, winner.damageSources().playerAttack(winner));
        NeoForge.EVENT_BUS.post(e);
        return e;
    }

    /** A whole duel the challenger wins; true when it was fought and settled. For the tests that need a new leader. */
    static boolean win(ServerPlayer challenger, ServerPlayer leader) {
        if (PackDuel.challenge(challenger, leader.getUUID().toString()) != null || PackDuel.accept(leader) != null) {
            return false;
        }
        return finish(challenger, leader).isCanceled();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theChallengerWhoWinsLeadsAndNobodyLosesTheSpark(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = TestPlayers.createDuelist(helper);
        online(leader, second, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && join(leader, beta), "the pack could not be built");
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null, "the challenge was refused");
            helper.assertTrue(PackDuel.board().on(second.getUUID()).isEmpty(), "the duel is on before it was accepted");
            helper.assertTrue(PackDuel.accept(leader) == null, "the leader could not accept");

            LivingDeathEvent e = finish(second, leader);
            helper.assertTrue(e.isCanceled(), "the loser would have died");
            helper.assertTrue(leader.getHealth() == 1f && leader.isAlive(), "the loser is at " + leader.getHealth());
            helper.assertTrue(index.isLeader(second.getUUID()) && index.alphaOf(leader.getUUID()).map(second.getUUID()::equals).orElse(false), "the winner does not lead");
            helper.assertTrue(second.getUUID().equals(index.leaderOf(beta.getUUID())), "the pack did not follow the winner");
            WolfState lost = BloodlineAccess.get(leader).wolf();
            WolfState won = BloodlineAccess.get(second).wolf();
            helper.assertTrue(lost.rank() == Rank.ALPHA && won.rank() == Rank.ALPHA && !won.stoleSpark(), "someone's spark changed: " + lost + " / " + won);
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel was not settled");
        } finally {
            clean(helper, leader, second, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeaderWhoWinsStaysTheLeader(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 9, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the duel did not start");
            helper.assertTrue(finish(leader, second).isCanceled() && second.getHealth() == 1f, "the challenger was not spared");
            helper.assertTrue(index.isLeader(leader.getUUID()) && index.alphaOf(second.getUUID()).isPresent(), "the lead moved");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelNeedsASecondAlphaAndTheRightLeaderAndOnlyOneAtATime(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer third = alpha(helper, 2, 3.5);
        ServerPlayer beta = TestPlayers.createDuelist(helper);
        ServerPlayer outsider = alpha(helper, 4, 4.5);
        online(leader, second, third, beta, outsider);
        try {
            helper.assertTrue(join(leader, second) && join(leader, third) && join(leader, beta), "the pack could not be built");
            helper.assertTrue(PackDuel.challenge(leader, second.getUUID().toString()) != null, "the leader challenged someone");
            helper.assertTrue(PackDuel.challenge(beta, leader.getUUID().toString()) != null, "a Beta challenged the leader");
            helper.assertTrue(PackDuel.challenge(outsider, leader.getUUID().toString()) != null, "an Alpha of no pack challenged the leader");
            helper.assertTrue(PackDuel.challenge(second, third.getUUID().toString()) != null, "a co-Alpha challenged another co-Alpha");
            helper.assertTrue(PackDuel.challenge(second, "nobody") != null, "challenged a name nobody has");
            helper.assertTrue(PackDuel.accept(leader) != null, "accepted a challenge nobody made");

            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null, "the challenge was refused");
            helper.assertTrue(PackDuel.challenge(third, leader.getUUID().toString()) != null, "a second challenge for the same leader");
            helper.assertTrue(PackDuel.accept(second) != null, "the challenger accepted their own challenge");
            helper.assertTrue(PackDuel.accept(leader) == null, "the leader could not accept");
        } finally {
            clean(helper, leader, second, third, beta, outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aThirdPartyKillingEitherOneCallsItOffAndTheDeathStands(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer bystander = human(helper, 3.5);
        online(leader, second, bystander);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the duel did not start");
            LivingDeathEvent e = finish(bystander, second);
            helper.assertTrue(!e.isCanceled(), "a third party's kill was turned into a defeat");
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel went on after one of them was killed");
            helper.assertTrue(index.isLeader(leader.getUUID()), "the lead moved");

            // the world killing them, with nobody to blame, calls it off too
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the second duel did not start");
            LivingDeathEvent fall = new LivingDeathEvent(leader, leader.damageSources().fall());
            NeoForge.EVENT_BUS.post(fall);
            helper.assertTrue(!fall.isCanceled() && PackDuel.board().all().isEmpty(), "a fall was turned into a defeat or the duel went on");
        } finally {
            clean(helper, leader, second, bystander);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelRunsOutAndLeavingOrLoggingOutCallsItOff(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer human = human(helper, 3.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            long now = helper.getLevel().getGameTime();

            // a challenge from long ago has run out
            PackDuel.board().challenge(second.getUUID(), leader.getUUID(), now - 5000);
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty(), "an old challenge was kept");
            helper.assertTrue(PackDuel.accept(leader) != null, "an old challenge was accepted");

            // logging out (the same call the logout event makes)
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the duel did not start");
            PackDuel.callOff(helper.getLevel().getServer(), second.getUUID());
            helper.assertTrue(PackDuel.board().all().isEmpty() && !finish(human, second).isCanceled(), "the duel survived a logout");

            // leaving the pack
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the third duel did not start");
            helper.assertTrue(PackCommand.leave(second) == null, "the second Alpha could not leave");
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel survived a challenger who left the pack");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpentFightIsNotADuelAndTheLoserKeepsOneHeartWhileShielded(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer human = human(helper, 3.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            // no duel: a death is an ordinary death
            helper.assertTrue(!finish(human, leader).isCanceled(), "a death outside a duel was cancelled");
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null, "the challenge was refused");
            // asked but not accepted: still an ordinary death, and the challenge goes
            helper.assertTrue(!finish(human, leader).isCanceled() && PackDuel.board().all().isEmpty(), "an unaccepted challenge turned a death into a defeat");
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the duel did not start");
            finish(second, leader);
            helper.assertTrue(leader.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "the loser is not shielded for a moment");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }
}
