package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Plan 14, the leadership duel (owner ruling): a co-Alpha challenges, the leader accepts, the first defeat decides, nobody dies. */
@PrefixGameTestTemplate(false)
public final class PackDuelGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String OFF = "supernaturalbloodlines.pack.duel.off.";

    private PackDuelGameTests() {
    }

    private static ServerPlayer asAlpha(GameTestHelper helper, ServerPlayer p, int level, double x) {
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.ALPHA).geneRolled(true).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    private static ServerPlayer alpha(GameTestHelper helper, int level, double x) {
        return asAlpha(helper, TestPlayers.createDuelist(helper), level, x);
    }

    private static TalkingDuelist talker(GameTestHelper helper, int level, double x) {
        return (TalkingDuelist) asAlpha(helper, TestPlayers.createTalkingDuelist(helper), level, x);
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
            if (p instanceof TalkingDuelist t) {
                t.putIn(helper.getLevel());
            }
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            index.takeDissolved(p.getUUID());
            index.takeNotices(p.getUUID());
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

    private static WolfState state(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    /** A blow from {@code attacker} on {@code victim}: what the board keeps as the combat credit of whatever comes next. */
    private static void hit(ServerPlayer attacker, ServerPlayer victim) {
        // the blow landed: the credit is given by the event that says so, not by one that may still be cancelled
        NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(victim, new net.neoforged.neoforge.common.damagesource.DamageContainer(attacker.damageSources().playerAttack(attacker), 1f)));
    }
    private static String id(ServerPlayer p) {
        return p.getUUID().toString();
    }

    private static boolean start(ServerPlayer challenger, ServerPlayer leader) {
        return PackDuel.challenge(challenger, id(leader)) == null && PackDuel.accept(leader) == null;
    }

    /** A whole duel the challenger wins; true when it was fought and settled. For the tests that need a new leader. */
    static boolean win(ServerPlayer challenger, ServerPlayer leader) {
        return start(challenger, leader) && finish(challenger, leader).isCanceled();
    }

    /** The challenge is one of the hub's requests: the leader is asked, with the time left, and it is gone once answered. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aChallengeIsARequestUntilItIsAnswered(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the pack could not be built");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(leader).isEmpty(), "a request before any challenge");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            var open = net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(leader);
            helper.assertTrue(open.size() == 1 && open.get(0).kind() == net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.CHALLENGE
                    && open.get(0).from().equals(second.getUUID()) && open.get(0).token().isEmpty() && open.get(0).ticksLeft() == DuelBoard.TIMEOUT_TICKS, "the leader's requests are " + open);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(second).isEmpty(), "the challenger was shown a request");
            helper.assertTrue(PackDuel.decline(leader) == null, "the decline failed");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(leader).isEmpty(), "an answered challenge is still a request");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
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
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
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
            helper.assertTrue(start(second, leader), "the duel did not start");
            helper.assertTrue(finish(leader, second).isCanceled() && second.getHealth() == 1f, "the challenger was not spared");
            helper.assertTrue(index.isLeader(leader.getUUID()) && index.alphaOf(second.getUUID()).isPresent(), "the lead moved");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anyOtherDeathOfADuelistIsHisDefeatToo(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            // the loser walks into lava: no totem of a duel, it is the defeat all the same
            helper.assertTrue(start(second, leader), "the duel did not start");
            hit(leader, second); // the leader hit them a moment ago: lava finishes what they started
            LivingDeathEvent lava = new LivingDeathEvent(second, second.damageSources().lava());
            NeoForge.EVENT_BUS.post(lava);
            helper.assertTrue(lava.isCanceled() && second.getHealth() == 1f, "the challenger escaped the defeat by dying to lava: " + second.getHealth());
            helper.assertTrue(index.isLeader(leader.getUUID()) && PackDuel.board().all().isEmpty(), "the leader did not win");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFallOfTheLeaderHandsTheLeadToTheChallenger(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            hit(second, leader);
            LivingDeathEvent fall = new LivingDeathEvent(leader, leader.damageSources().fall());
            NeoForge.EVENT_BUS.post(fall);
            helper.assertTrue(fall.isCanceled() && leader.getHealth() == 1f, "the leader escaped the defeat by falling");
            helper.assertTrue(index.isLeader(second.getUUID()), "the challenger does not lead after the leader's fall");
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
            helper.assertTrue(PackDuel.challenge(leader, id(second)) != null, "the leader challenged someone");
            helper.assertTrue(PackDuel.challenge(beta, id(leader)) != null, "a Beta challenged the leader");
            helper.assertTrue(PackDuel.challenge(outsider, id(leader)) != null, "an Alpha of no pack challenged the leader");
            helper.assertTrue(PackDuel.challenge(second, id(third)) != null, "a co-Alpha challenged another co-Alpha");
            helper.assertTrue(PackDuel.challenge(second, "nobody") != null, "challenged a name nobody has");
            helper.assertTrue(PackDuel.accept(leader) != null, "accepted a challenge nobody made");

            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            helper.assertTrue(PackDuel.challenge(third, id(leader)) != null, "a second challenge for the same leader");
            helper.assertTrue(PackDuel.accept(second) != null, "the challenger accepted their own challenge");
            helper.assertTrue(PackDuel.accept(leader) == null, "the leader could not accept");
        } finally {
            clean(helper, leader, second, third, beta, outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aThirdPlayerKillingEitherOneCallsItOffAndTheDeathStands(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        ServerPlayer bystander = human(helper, 3.5);
        online(leader, second, bystander);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(start(second, leader), "the duel did not start");
            LivingDeathEvent e = finish(bystander, second);
            helper.assertTrue(!e.isCanceled(), "a third player's kill was turned into a defeat");
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel went on after one of them was killed");
            helper.assertTrue(index.isLeader(leader.getUUID()), "the lead moved");
            helper.assertTrue(leader.received(OFF + "killed") && second.received(OFF + "killed"), "they were not told why it is off");
        } finally {
            clean(helper, leader, second, bystander);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelRunsOutAndLeavingOrLoggingOutCallsItOffWithAReason(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
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
            helper.assertTrue(leader.received(OFF + "timeout") && second.received(OFF + "timeout"), "the timeout was not told: " + leader.keys() + " " + second.keys() + " board " + PackDuel.board().all().size());
            PackDuel.reset();

            // the real logout event: a challenge nobody accepted yet is just called off
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            helper.assertTrue(PackDuel.board().all().isEmpty() && leader.received(OFF + "logout"), "the challenge survived a logout, or the one who stayed was not told");
            PackDuel.reset();

            // leaving the pack
            helper.assertTrue(start(second, leader), "the third duel did not start");
            helper.assertTrue(PackCommand.leave(second) == null, "the second Alpha could not leave");
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel survived a challenger who left the pack");
            helper.assertTrue(leader.received(OFF + "pack"), "nobody was told the pack changed");
        } finally {
            clean(helper, leader, second, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anUnacceptedChallengeIsNoDuelAndAnOrdinaryDeathStands(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer human = human(helper, 3.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(!finish(human, leader).isCanceled(), "a death outside a duel was cancelled");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            helper.assertTrue(!finish(human, leader).isCanceled() && PackDuel.board().all().isEmpty(), "an unaccepted challenge turned a death into a defeat");
        } finally {
            clean(helper, leader, second, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWinnerCannotFinishTheLoserForAMomentButEveryoneElseCan(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer human = human(helper, 3.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            finish(second, leader);
            helper.assertTrue(!leader.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "the loser got a totem's protection against everything");
            LivingIncomingDamageEvent fromWinner = new LivingIncomingDamageEvent(leader, new net.neoforged.neoforge.common.damagesource.DamageContainer(second.damageSources().playerAttack(second), 4f));
            NeoForge.EVENT_BUS.post(fromWinner);
            helper.assertTrue(fromWinner.isCanceled(), "the winner could hit the loser");
            LivingIncomingDamageEvent fromOther = new LivingIncomingDamageEvent(leader, new net.neoforged.neoforge.common.damagesource.DamageContainer(human.damageSources().playerAttack(human), 4f));
            NeoForge.EVENT_BUS.post(fromOther);
            helper.assertTrue(!fromOther.isCanceled(), "somebody else was stopped by the guard");
            LivingIncomingDamageEvent world = new LivingIncomingDamageEvent(leader, new net.neoforged.neoforge.common.damagesource.DamageContainer(leader.damageSources().lava(), 4f));
            NeoForge.EVENT_BUS.post(world);
            helper.assertTrue(!world.isCanceled(), "the guard stopped the world");
        } finally {
            clean(helper, leader, second, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSettledPairWaitsTenMinutesAndADeclinedOrIgnoredChallengeFiveMinutes(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer third = alpha(helper, 2, 3.5);
        online(leader, second, third);
        try {
            helper.assertTrue(join(leader, second) && join(leader, third), "the pack could not be built");
            helper.assertTrue(win(second, leader), "the first duel did not end");
            // second leads now: the old leader is the challenger of the same pair, and is barred as well
            helper.assertTrue(PackDuel.challenge(leader, id(second)) != null, "the same pair fought again at once");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) != null, "the same pair, the other way round, fought again at once");

            // the third Alpha asks the new leader and is turned down: they wait before asking again
            helper.assertTrue(PackDuel.challenge(third, id(second)) == null, "the third Alpha could not challenge");
            helper.assertTrue(PackDuel.decline(second) == null, "the leader could not decline");
            helper.assertTrue(PackDuel.board().all().isEmpty() && PackDuel.accept(second) != null, "a declined challenge can still be accepted");
            helper.assertTrue(PackDuel.challenge(third, id(second)) != null, "a declined challenger asked again at once");
            helper.assertTrue(PackDuel.decline(second) != null, "declined a challenge nobody made");
        } finally {
            clean(helper, leader, second, third);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void decliningTellsTheChallengerAndAnIgnoredChallengeBarsRetries(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null && PackDuel.decline(leader) == null, "the challenge was not declined");
            helper.assertTrue(second.received(OFF + "declined") && leader.received("supernaturalbloodlines.pack.duel.declined_you"), "they were not told");
            PackDuel.reset();

            // one nobody answered: it runs out, and asking again is barred
            long now = helper.getLevel().getGameTime();
            PackDuel.board().challenge(second.getUUID(), leader.getUUID(), now - DuelBoard.TIMEOUT_TICKS - 10);
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the old challenge was kept");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) != null, "a challenger whose challenge ran out asked again at once: t=" + helper.getLevel().getGameTime() + " o=" + helper.getLevel().getServer().overworld().getGameTime());
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aChallengeOrAnAcceptanceNeedsBothClose(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            second.setPos(helper.absoluteVec(new Vec3(100, 1, 1.5)));
            helper.assertTrue(PackDuel.challenge(second, id(leader)) != null && PackDuel.board().all().isEmpty(), "a challenge was made from 100 blocks away");

            second.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the close challenge was refused");
            second.setPos(helper.absoluteVec(new Vec3(100, 1, 1.5)));
            helper.assertTrue(PackDuel.accept(leader) != null && PackDuel.board().on(leader.getUUID()).isEmpty(), "a far acceptance started the duel");
            second.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
            helper.assertTrue(PackDuel.accept(leader) == null, "the challenge was lost by a refused acceptance");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherDimensionRefusesAChallengeAndCallsADuelOff(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        net.minecraft.server.level.ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(nether != null, "the test server has no nether");
            second.putIn(nether);
            helper.assertTrue(PackDuel.challenge(second, id(leader)) != null && PackDuel.board().all().isEmpty(), "a challenge crossed a dimension");
            second.putIn(helper.getLevel());
            helper.assertTrue(start(second, leader), "the duel did not start");
            second.putIn(nether);
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty() && leader.received(OFF + "far"), "the duel went on across dimensions or nobody was told");
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime()) > 0, "changing dimension in a duel that was on left the pair free to start again");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackThatChangedDuringTheFightIsNotPromotedAndTheChallengerIsToldWhenTheyNoLongerQualify(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        TalkingDuelist third = talker(helper, 2, 3.5);
        online(leader, second, third);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && join(leader, third), "the pack could not be built");
            // the challenger leaves the pack after the duel began: his win does not give him a pack he is not in
            helper.assertTrue(start(second, leader), "the duel did not start");
            index.leave(second.getUUID());
            helper.assertTrue(finish(second, leader).isCanceled(), "the defeat was not turned into 1 HP");
            helper.assertTrue(index.isLeader(leader.getUUID()) && index.alphaOf(third.getUUID()).map(leader.getUUID()::equals).orElse(false), "the pack changed hands");
            helper.assertTrue(!index.inPack(second.getUUID()), "the challenger was put in the pack");
            helper.assertTrue(leader.received(OFF + "pack"), "nobody was told it did not hold");
            PackDuel.reset();

            // the leader is asked by someone who has stopped being an Alpha: the answer says so, to both
            helper.assertTrue(join(leader, second), "the second Alpha could not join again");
            helper.assertTrue(PackDuel.challenge(third, id(leader)) == null, "the challenge was refused");
            BloodlineAccess.set(third, BloodlineAccess.get(third).toBuilder().wolf(BloodlineAccess.get(third).wolf().toBuilder().rank(Rank.BETA).build()).build());
            int before = third.received();
            helper.assertTrue(PackDuel.accept(leader) != null, "a challenge from a Beta was accepted");
            helper.assertTrue(third.received() > before && third.received(OFF + "pack"), "the challenger was not told it fell through");
        } finally {
            clean(helper, leader, second, third);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theChallengedLeaderIsToldHowToAnswer(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.asked") && leader.received("supernaturalbloodlines.pack.duel.accept"), "the leader got no question and no button");
            // the hub shows who is asking: the second Alpha is listed as the challenger for the leader only
            var asked = net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.of(leader).pack();
            helper.assertTrue(asked.stream().anyMatch(m -> m.rank().equals("coalpha_challenger")), "the leader's hub does not show who is asking: " + asked);
            var other = net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.of(second).pack();
            helper.assertTrue(other.stream().noneMatch(m -> m.rank().equals("coalpha_challenger")), "somebody else is shown the challenger");
            PackDuel.accept(leader);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.of(leader).pack().stream().noneMatch(m -> m.rank().equals("coalpha_challenger")), "the challenger is still shown once the duel is on");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeathByTheWorldWithNoBlowFromTheOpponentIsNoDefeatButAnEndToTheDuel(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(start(second, leader), "the duel did not start");
            // nobody has hit anybody: lava is a death, not a way to have the duel turned into a defeat and a life saved
            LivingDeathEvent lava = new LivingDeathEvent(second, second.damageSources().lava());
            NeoForge.EVENT_BUS.post(lava);
            helper.assertTrue(!lava.isCanceled(), "a death nobody had caused was turned into a defeat");
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()), "the duel went on or the lead moved");
            helper.assertTrue(leader.received(OFF + "died") && second.received(OFF + "died"), "they were not told why it is off: " + leader.keys());

            // a blow from long ago: the credit has run out
            PackDuel.reset();
            helper.assertTrue(start(second, leader), "the second duel did not start (a settled pair is barred)");
            PackDuel.board().recordHit(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime() - DuelBoard.COMBAT_CREDIT_TICKS - 5);
            LivingDeathEvent late = new LivingDeathEvent(second, second.damageSources().fall());
            NeoForge.EVENT_BUS.post(late);
            helper.assertTrue(!late.isCanceled(), "a hit from a minute ago still counted");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aDefeatByTheWorldWithCreditGivesFireResistanceAndAWayOutOfTheLava(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        BlockPos lava = new BlockPos(6, 1, 4);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            // a floor to stand on around the lava (the test box has none we can count on)
            for (int fx = 4; fx <= 8; fx++) {
                for (int fz = 2; fz <= 6; fz++) {
                    helper.setBlock(new BlockPos(fx, 0, fz), Blocks.STONE);
                }
            }
            helper.setBlock(lava, Blocks.LAVA);
            hit(leader, second);
            LivingDeathEvent e = new LivingDeathEvent(second, second.damageSources().lava());
            NeoForge.EVENT_BUS.post(e);
            helper.assertTrue(e.isCanceled() && second.getHealth() == 1f, "the defeat was not turned into 1 HP");
            helper.assertTrue(second.hasEffect(MobEffects.FIRE_RESISTANCE), "no Fire Resistance after a defeat by the world");
            helper.assertTrue(index.isLeader(leader.getUUID()), "the leader did not win");
            // the way out: the nearest block to stand on is no lava and has a solid block under it
            BlockPos safe = PackDuel.safeSpot(helper.getLevel(), helper.absolutePos(lava), second);
            helper.assertTrue(safe != null && !safe.equals(helper.absolutePos(lava)) && helper.getLevel().getFluidState(safe).isEmpty()
                    && helper.getLevel().getFluidState(safe.below()).isEmpty() && !helper.getLevel().getBlockState(safe.below()).isAir(), "no safe spot near the lava: " + safe);
        } finally {
            helper.setBlock(lava, Blocks.AIR);
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRefusedChallengeSaysHowManyMinutesRemain(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(win(second, leader), "the first duel did not end");
            net.minecraft.network.chat.Component refused = PackDuel.challenge(leader, id(second));
            helper.assertTrue(refused != null && refused.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().endsWith("duel_cooldown") && t.getArgs().length == 1 && t.getArgs()[0].equals(10L), "the refusal does not say 10 minutes: " + refused);
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aChallengeThatRanOutDoesNotKeepTheLeaderBusyBeforeTheSweep(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        ServerPlayer third = alpha(helper, 2, 3.5);
        online(leader, second, third);
        try {
            helper.assertTrue(join(leader, second) && join(leader, third), "the pack could not be built");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().challenge(second.getUUID(), leader.getUUID(), now - DuelBoard.TIMEOUT_TICKS - 5);
            helper.assertTrue(PackDuel.challenge(third, id(leader)) == null, "the leader was busy with a challenge that had run out");
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(second.received(OFF + "timeout"), "the one whose challenge ran out was never told");
        } finally {
            clean(helper, leader, second, third);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBoardIsClearedWhenTheWorldStops(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader) && !PackDuel.board().all().isEmpty(), "the duel did not start");
            PackDuel.onStopped();
            helper.assertTrue(PackDuel.board().all().isEmpty(), "a duel survived the stop of the world");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theVoidIsADeathWhoeverPushed(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            hit(leader, second); // credit from the leader, all the same: the void is nobody's win
            LivingDeathEvent e = new LivingDeathEvent(second, second.damageSources().fellOutOfWorld());
            NeoForge.EVENT_BUS.post(e);
            helper.assertTrue(!e.isCanceled(), "the void was turned into a defeat");
            helper.assertTrue(PackDuel.board().all().isEmpty() && leader.received(OFF + "died"), "the duel was not called off with a reason: " + leader.keys());
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowFromSomeoneElseGivesTheOpponentNoCredit(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer bystander = human(helper, 3.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            hit(bystander, second);
            LivingDeathEvent e = new LivingDeathEvent(second, second.damageSources().lava());
            NeoForge.EVENT_BUS.post(e);
            helper.assertTrue(!e.isCanceled(), "a blow from a third player was taken for the leader's");
        } finally {
            clean(helper, leader, second, bystander);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void noRescueThroughAWall(GameTestHelper helper) {
        ServerPlayer loser = alpha(helper, 3, 2.5);
        BlockPos lava = new BlockPos(6, 1, 4);
        online(loser);
        try {
            // a closed stone box around one cell of lava, the loser inside it: every spot outside is behind a wall
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        BlockPos p = lava.offset(dx, dy, dz);
                        boolean inside = dx == 0 && dz == 0 && dy >= 1 && dy <= 2;
                        helper.setBlock(p, inside ? (dy == 1 ? Blocks.LAVA : Blocks.AIR) : Blocks.STONE);
                    }
                }
            }
            loser.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5, 1.0, 4.5)));
            helper.assertTrue(PackDuel.safeSpot(helper.getLevel(), helper.absolutePos(lava), loser) == null, "a spot was found on the other side of a wall");
            // the same lava in the open has one
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 1; dy <= 3; dy++) {
                        helper.setBlock(lava.offset(dx, dy, dz), dx == 0 && dz == 0 && dy == 1 ? Blocks.LAVA : Blocks.AIR);
                    }
                }
            }
            helper.assertTrue(PackDuel.safeSpot(helper.getLevel(), helper.absolutePos(lava), loser) != null, "no spot in the open");
        } finally {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        helper.setBlock(lava.offset(dx, dy, dz), dy == 0 ? Blocks.STONE : Blocks.AIR);
                    }
                }
            }
            clean(helper, loser);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingAnOnDuelByLoggingOutIsADefeatAndTheWinnerLeadsIfTheyWereTheChallenger(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        SpeciesConfig.overrideDuelReconnectSeconds(0);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            // the leader walks out of the game in the middle of it: the challenger wins at once
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader));
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel was left on");
            helper.assertTrue(index.isLeader(second.getUUID()) && index.alphaOf(leader.getUUID()).map(second.getUUID()::equals).orElse(false), "the challenger did not take the lead");
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.forfeit"), "the winner was not told: " + second.keys());
            helper.assertTrue(state(leader).rank() == Rank.ALPHA && state(leader).sparks() == 1, "the forfeit cost a spark or a rank");
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime()) > 0, "the pair is not barred after a settled duel");
        } finally {
            SpeciesConfig.overrideDuelReconnectSeconds(null);
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aChallengerWhoLogsOutOfAnOnDuelLosesItAndTheLeaderStaysLeader(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        SpeciesConfig.overrideDuelReconnectSeconds(0);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()), "the leader lost the lead to somebody who left");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.forfeit"), "the leader was not told");
        } finally {
            SpeciesConfig.overrideDuelReconnectSeconds(null);
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void walkingOutOfRangeEndsAnOnDuelAndBarsThePair(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            second.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(200, 1, 1.5)));
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty() && leader.received(OFF + "range") && !leader.received(OFF + "far"), "the duel went on out of range, or nobody was told why: " + leader.keys());
            second.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 1.5)));
            net.minecraft.network.chat.Component refused = PackDuel.challenge(second, id(leader));
            helper.assertTrue(refused != null && refused.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().endsWith("duel_called_off"),
                    "the pair was not barred for a call-off: " + refused);
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowThatDidNotLandGivesNoCredit(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            // only the incoming event: a shield, a protected player or another mod may still cancel it, and then nothing landed
            LivingIncomingDamageEvent incoming = new LivingIncomingDamageEvent(second, new net.neoforged.neoforge.common.damagesource.DamageContainer(leader.damageSources().playerAttack(leader), 4f));
            NeoForge.EVENT_BUS.post(incoming);
            LivingDeathEvent lava = new LivingDeathEvent(second, second.damageSources().lava());
            NeoForge.EVENT_BUS.post(lava);
            helper.assertTrue(!lava.isCanceled(), "a blow that only arrived was given as credit");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void dyingOfSomethingElseOrToAThirdPlayerBarsThePairLikeWalkingOut(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer bystander = human(helper, 3.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(second, second.damageSources().lava())); // nobody hit them: DIED
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime()) > 0, "a death by the world left the pair free to start again");
            PackDuel.reset();
            helper.assertTrue(start(second, leader), "the next duel did not start");
            // a challenge nobody accepted is no duel: a death then bars nobody
            PackDuel.reset();
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(second, second.damageSources().lava()));
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime()) == 0, "a death barred the pair of a challenge nobody had accepted");
            PackDuel.reset();
            helper.assertTrue(start(second, leader), "the third duel did not start");
            finish(bystander, second); // killed by a third player: KILLED
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime()) > 0, "a kill by a third player left the pair free to start again");
        } finally {
            clean(helper, leader, second, bystander);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aServerThatIsStoppingSettlesNothingWhenItsPlayersAreSentAway(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            PackDuel.onStopping();
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the board was not cleared when the server began to stop");
            // and a duel that is somehow on while it stops is not settled by a logout either
            PackDuel.board().challenge(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime() + DuelBoard.PAIR_COOLDOWN_TICKS * 2);
            PackDuel.board().accept(leader.getUUID(), helper.getLevel().getGameTime() + DuelBoard.PAIR_COOLDOWN_TICKS * 2);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader));
            helper.assertTrue(index.isLeader(leader.getUUID()) && !second.received("supernaturalbloodlines.pack.duel.forfeit"), "the shutdown settled a duel: " + second.keys());
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).map(d -> d.pausedBy() == null).orElse(true) && !second.received("supernaturalbloodlines.pack.duel.disconnected"),
                    "the shutdown started a wait for somebody to reconnect");
            helper.assertTrue(index.noticeCount(leader.getUUID()) == 0, "the shutdown left a forfeit notice");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLoserWhoLoggedOutFindsOutHowItEndedAtTheNextLogin(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        SpeciesConfig.overrideDuelReconnectSeconds(0);
        try {
            // the leader logs out of a duel that the challenger would win: the lead moves
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader));
            java.util.List<String[]> now = index.takeNotices(leader.getUUID());
            helper.assertTrue(now.size() == 1 && now.get(0)[0].endsWith("duel.forfeit_lost_now") && now.get(0)[1].length() > 0, "no notice, or the wrong one, for the loser who left the lead: " + now.size());

            // the challenger logs out: the lead does not move, and the notice says who still leads
            PackDuel.reset();
            index.takeNotices(second.getUUID());
            PackIndex.get(helper.getLevel()).promote(second.getUUID(), leader.getUUID()); // the old leader leads again, for the second half
            helper.assertTrue(index.isLeader(leader.getUUID()), "the second half could not be set up");
            PackDuel.board().clear();
            PackDuel.board().challenge(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime() + DuelBoard.PAIR_COOLDOWN_TICKS * 3);
            PackDuel.board().accept(leader.getUUID(), helper.getLevel().getGameTime() + DuelBoard.PAIR_COOLDOWN_TICKS * 3);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            java.util.List<String[]> same = index.takeNotices(second.getUUID());
            helper.assertTrue(same.size() == 1 && same.get(0)[0].endsWith("duel.forfeit_lost_same"), "no notice, or the wrong one, for the challenger who left: " + same.size());
        } finally {
            SpeciesConfig.overrideDuelReconnectSeconds(null);
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    // ---- the reconnect window (owner ruling) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDisconnectPausesTheDuelAndComingBackInTimeResumesIt(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            DuelBoard.Duel d = PackDuel.board().on(leader.getUUID()).orElse(null);
            helper.assertTrue(d != null && d.pausedBy() != null && d.pausedBy().equals(second.getUUID()), "the duel was not paused for the one who left");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.disconnected") && !leader.received("supernaturalbloodlines.pack.duel.forfeit"),
                    "the opponent was not told they have a while to come back: " + leader.keys());
            helper.assertTrue(index.isLeader(leader.getUUID()), "the lead moved during the wait");
            // the rest of the world goes on and judges nothing about a duel that is waiting
            PackDuel.check(server);
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).isPresent(), "the check called off a duel that is waiting for somebody");

            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second));
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).map(x -> x.pausedBy() == null).orElse(false), "the duel is not running again");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.resumed") && second.received("supernaturalbloodlines.pack.duel.resumed_you"), "both were not told it goes on");
            // and it is a duel again: the first defeat settles it
            helper.assertTrue(finish(second, leader).isCanceled() && index.isLeader(second.getUUID()), "the resumed duel did not settle on the first defeat");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWindowThatRanOutIsAForfeitWithTheNoticeAtTheNextLogin(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            // the leader lost their connection long ago: their wait of 100 ticks began 5000 ticks back, and they are not here
            long now = helper.getLevel().getGameTime();
            DuelBoard.Duel d = PackDuel.board().on(second.getUUID()).orElseThrow();
            PackDuel.board().pause(leader.getUUID(), now - 5000, 100);
            online(second);
            helper.assertTrue(d.pausedBy() != null, "the duel is not paused");

            PackDuel.check(server);
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel is still there after the wait");
            helper.assertTrue(index.isLeader(second.getUUID()), "the challenger did not take the lead");
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.forfeit"), "the winner was not told: " + second.keys());
            java.util.List<String[]> notices = index.takeNotices(leader.getUUID());
            helper.assertTrue(notices.size() == 1 && notices.get(0)[0].endsWith("duel.forfeit_lost_now"), "no forfeit notice for the one who did not come back: " + notices.size());
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), now) > 0, "the pair is not barred after a settled duel");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void comingBackAfterTheWindowButBeforeTheSweepIsStillAForfeit(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().pause(second.getUUID(), now - 5000, 100); // away for 4900 ticks of a 100 tick wait
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second));
            helper.assertTrue(PackDuel.board().all().isEmpty() && !second.received("supernaturalbloodlines.pack.duel.resumed"), "a late return resumed the duel");
            helper.assertTrue(index.isLeader(leader.getUUID()), "the leader lost the lead to somebody who did not come back in time");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.forfeit"), "the leader was not told they won");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aReconnectWindowOfZeroIsTheOldInstantForfeit(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        SpeciesConfig.overrideDuelReconnectSeconds(0);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader));
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(second.getUUID()), "a window of 0 still waited");
            helper.assertTrue(!second.received("supernaturalbloodlines.pack.duel.disconnected"), "a window of 0 announced a wait");
        } finally {
            SpeciesConfig.overrideDuelReconnectSeconds(null);
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eachDuelistGetsOneWaitAndASecondDisconnectIsAForfeit(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).map(d -> d.pausedBy() != null).orElse(false), "the first disconnect was not waited for");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second));
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).map(d -> d.pausedBy() == null).orElse(false), "the duel did not resume");
            // the same duelist again: no second wait, they lose it
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            helper.assertTrue(PackDuel.board().all().isEmpty(), "a second disconnect was waited for again");
            helper.assertTrue(index.isLeader(leader.getUUID()) && leader.received("supernaturalbloodlines.pack.duel.forfeit"), "the one who stayed did not win it");
            helper.assertTrue(index.takeNotices(second.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.forfeit_lost_same")), "the one who left twice has no notice");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void youCannotRemoveSomebodyYouAreDuelingAndTheCommandSaysSo(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer third = alpha(helper, 2, 3.5);
        online(leader, second, third);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && join(leader, third), "the pack could not be built");
            helper.assertTrue(start(second, leader), "the duel did not start");
            net.minecraft.network.chat.Component refused = PackCommand.kick(leader, second.getUUID(), "Second");
            helper.assertTrue(refused != null && refused.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().endsWith("duel_no_kick"), "the leader removed their opponent: " + refused);
            helper.assertTrue(index.alphaOf(second.getUUID()).isPresent(), "the opponent is out of the pack");
            helper.assertTrue(PackCommand.kick(leader, third.getUUID(), "Third") == null, "somebody else could not be removed during the duel");
        } finally {
            clean(helper, leader, second, third);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeathWhileTheDuelWaitsForSomebodyEndsItAndSettlesNothing(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            hit(second, leader);
            LivingDeathEvent e = new LivingDeathEvent(leader, leader.damageSources().lava());
            NeoForge.EVENT_BUS.post(e);
            helper.assertTrue(!e.isCanceled(), "a death was turned into a defeat while the other one was away");
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()) && !index.isLeader(second.getUUID()), "the duel was settled for somebody who could not be there");
            helper.assertTrue(index.takeNotices(second.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.died")), "the one who was away was not left a notice that the duel ended");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theReturningDuelistAndTheOtherAreToldDifferently(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second));
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.resumed_you") && !second.received("supernaturalbloodlines.pack.duel.resumed"), "the one who came back was not welcomed back: " + second.keys());
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.resumed") && !leader.received("supernaturalbloodlines.pack.duel.resumed_you"), "the other was not told who is back: " + leader.keys());
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLateReturnIsToldDirectlyHowItEnded(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().pause(second.getUUID(), now - 5000, 100);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second));
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.forfeit_lost_same") && index.noticeCount(second.getUUID()) == 0,
                    "the one who came back too late was not told straight away how it ended (or a notice was left behind): " + second.keys());
            helper.assertTrue(!second.received("supernaturalbloodlines.pack.duel.resumed_you"), "a late return was welcomed back into a duel that was over");

        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    // ---- a window of their own for each duelist (round 7) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOtherOneLeavingRightAfterWinsNothingAndTheFirstWhoReturnsKeepsTheDuel(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second)); // A drops
            online(leader);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader)); // B leaves right after
            online();
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).map(d -> d.isAway(second.getUUID()) && d.isAway(leader.getUUID())).orElse(false), "both were not waited for");
            helper.assertTrue(index.isLeader(leader.getUUID()) && index.noticeCount(second.getUUID()) == 0 && index.noticeCount(leader.getUUID()) == 0, "leaving right after won (or lost) something");
            PackDuel.check(server);
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).isPresent(), "the check judged a duel that is waiting for both");

            // A returns within the window: B is away yet, so the duel stays paused and nobody has won
            online(second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second));
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.resumed_waiting"), "the first to return was not told the duel waits for the other: " + second.keys());
            helper.assertTrue(PackDuel.board().on(second.getUUID()).map(d -> d.paused() && d.isAway(leader.getUUID()) && !d.isAway(second.getUUID())).orElse(false), "the duel is not waiting for B alone");
            helper.assertTrue(index.isLeader(leader.getUUID()), "A's return gave B's lead away");

            // B returns too: it resumes, for both
            online(leader, second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(leader));
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).map(d -> !d.paused()).orElse(false), "the duel did not resume once both were back");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.resumed_you") && second.received("supernaturalbloodlines.pack.duel.resumed"), "both were not told it goes on");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOtherOneForfeitsOnlyIfTheyDoNotReturnInTheirOwnWindow(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second)); // A drops, real window
            PackDuel.board().pause(leader.getUUID(), now - 5000, 100); // B left a long time ago and never came back
            online(second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second)); // A is back in time; B's window is over
            helper.assertTrue(PackDuel.board().all().isEmpty(), "B's window ended with A here and the duel was not settled");
            helper.assertTrue(index.isLeader(second.getUUID()), "A, who came back, did not win: the lead is with " + index.leaderOf(second.getUUID()));
            helper.assertTrue(index.takeNotices(leader.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.forfeit_lost_now")), "B, who did not come back, has no notice");
            PackDuel.check(server);
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ifBothStayAwayPastTheirWindowsNobodyWinsAndThePairIsBarred(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().pause(second.getUUID(), now - 5000, 100); // A left first
            PackDuel.board().pause(leader.getUUID(), now - 4990, 100); // B left right after: both windows are over
            online();
            PackDuel.check(server);
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel was kept");
            helper.assertTrue(index.isLeader(leader.getUUID()), "somebody won a duel that both abandoned");
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), now) > 0, "the pair is not barred");
            helper.assertTrue(index.takeNotices(second.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.both_away")), "the first to leave has no notice");
            helper.assertTrue(index.takeNotices(leader.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.both_away")), "the second to leave has no notice");
            online(leader, second);
            net.minecraft.network.chat.Component refused = PackDuel.challenge(second, id(leader));
            helper.assertTrue(refused != null && refused.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().endsWith("duel_called_off"), "the pair was not barred for a call-off: " + refused);
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFirstToLeaveIsNotLostWhileTheOtherStillHasTimeAndOnlyThenIsSettled(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().pause(second.getUUID(), now - 5000, 100); // A's window is over
            PackDuel.board().pause(leader.getUUID(), now, 1200);        // B just left: a whole window to come
            online();
            PackDuel.check(server);
            helper.assertTrue(PackDuel.board().on(leader.getUUID()).isPresent() && index.isLeader(leader.getUUID()), "A forfeited, and B won by leaving, while B still has time");
            // B returns in time: A, whose window is over and who is away, forfeits at once
            online(leader);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(leader));
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()), "the duel was not settled for B, who came back");
            helper.assertTrue(index.takeNotices(second.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.forfeit_lost_same")), "A has no notice that they lost");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondDisconnectOfTheSameDuelistIsAForfeitEvenWhileTheOtherIsAway(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second)); // used their one wait
            online(leader, second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader)); // the leader drops: their own wait
            online(second);
            helper.assertTrue(PackDuel.board().on(second.getUUID()).map(d -> d.isAway(leader.getUUID())).orElse(false), "the leader's wait did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second)); // and the challenger again, their wait is spent
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()), "a second disconnect was waited for, or gave the lead to somebody who left twice");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelThatWasOnlyAskedForAnotherDimensionHasNoBarAndNoTenMinutesInItsText(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        net.minecraft.server.level.ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackDuel.challenge(second, id(leader)) == null, "the challenge was refused");
            second.putIn(nether);
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the challenge was kept");
            helper.assertTrue(leader.received(OFF + "far_asked") && !leader.received(OFF + "far"), "a challenge was told about a bar it never had: " + leader.keys());
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), helper.getLevel().getGameTime()) == 0, "a challenge nobody accepted barred the pair");
        } finally {
            second.putIn(helper.getLevel());
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelThatCouldNotBeSettledTellsTheWinnerWhoIsAwayAsWell(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            // the leader forfeits to a challenger who is not online to take the lead: nothing can be applied
            PackDuel.board().pause(leader.getUUID(), now - 5000, 100);
            online();
            PackDuel.check(server);
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel was kept");
            helper.assertTrue(index.takeNotices(leader.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.pack")), "the loser has no notice that it was called off");
            helper.assertTrue(index.takeNotices(second.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.pack")), "the winner, who is away, has no notice that it was called off");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    // ---- round 8: a late return waits for the other one; no double notice ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLateReturnWaitsForTheOtherOneWhoIsAwayWithTimeLeftAndThenForfeitsWhenTheyComeBack(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().pause(second.getUUID(), now - 5000, 100); // A's window is long over
            PackDuel.board().pause(leader.getUUID(), now, 1200);        // B is away with a whole window left
            online(second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second)); // A logs in late
            helper.assertTrue(PackDuel.board().on(second.getUUID()).isPresent(), "the duel was settled while B still has time");
            helper.assertTrue(index.isLeader(leader.getUUID()) && !second.received("supernaturalbloodlines.pack.duel.forfeit_lost_same"), "A forfeited, or the lead moved, before B had their window");
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.resumed_waiting"), "the late one was not told the duel waits for the other: " + second.keys());
            PackDuel.check(helper.getLevel().getServer());
            helper.assertTrue(PackDuel.board().on(second.getUUID()).isPresent(), "the next check settled it");

            // B comes back in time: A, who was late, forfeits it
            online(leader, second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(leader));
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel was kept after B came back");
            helper.assertTrue(index.isLeader(leader.getUUID()), "the lead moved to somebody who came back late");
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.forfeit_lost_same") && leader.received("supernaturalbloodlines.pack.duel.forfeit"), "the outcome was not told: " + second.keys() + " " + leader.keys());
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLateReturnWhoseOtherEndsAwayAsWellCallsItOffAndBarsThePair(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            long now = helper.getLevel().getGameTime();
            PackDuel.board().pause(second.getUUID(), now - 5000, 100);
            PackDuel.board().pause(leader.getUUID(), now - 4000, 100); // B's window ended as well, and B never came back
            online(second);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(second)); // A is here, late
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()), "the duel was settled for somebody, or kept");
            helper.assertTrue(second.received("supernaturalbloodlines.pack.duel.off.both_away") && index.takeNotices(leader.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.both_away")), "both were not told that nobody won");
            helper.assertTrue(PackDuel.board().cooldownLeft(second.getUUID(), leader.getUUID(), now) > 0, "the pair is not barred");
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aResultThatCouldNotBeAppliedTellsEachOnlinePlayerOnceAndTheOneWhoLeavesByANotice(GameTestHelper helper) {
        TalkingDuelist leader = talker(helper, 6, 1.5);
        TalkingDuelist second = talker(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        SpeciesConfig.overrideDuelReconnectSeconds(0);
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            // the challenger is no Alpha any more: the win cannot be applied
            BloodlineAccess.set(second, BloodlineAccess.get(second).toBuilder().wolf(WolfState.NONE).build());
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(leader)); // the leader leaves: an instant forfeit
            helper.assertTrue(PackDuel.board().all().isEmpty(), "the duel was kept");
            helper.assertTrue(second.count("supernaturalbloodlines.pack.duel.off.pack") == 1, "the online player was told " + second.count("supernaturalbloodlines.pack.duel.off.pack") + " times: " + second.keys());
            helper.assertTrue(index.noticeCount(second.getUUID()) == 0, "the online player was left a notice as well");
            helper.assertTrue(index.takeNotices(leader.getUUID()).stream().anyMatch(n -> n[0].endsWith("duel.off.pack")), "the one who disconnected has no notice for their next login");
        } finally {
            SpeciesConfig.overrideDuelReconnectSeconds(null);
            clean(helper, leader, second);
        }
        helper.succeed();
    }
}
