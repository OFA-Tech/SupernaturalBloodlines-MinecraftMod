package net.ofatech.supernaturalbloodlines.parts.species.gametest;

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
        NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(victim, new net.neoforged.neoforge.common.damagesource.DamageContainer(attacker.damageSources().playerAttack(attacker), 1f)));
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
        try {
            helper.assertTrue(join(leader, second) && start(second, leader), "the duel did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(second));
            helper.assertTrue(PackDuel.board().all().isEmpty() && index.isLeader(leader.getUUID()), "the leader lost the lead to somebody who left");
            helper.assertTrue(leader.received("supernaturalbloodlines.pack.duel.forfeit"), "the leader was not told");
        } finally {
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
            helper.assertTrue(PackDuel.board().all().isEmpty() && leader.received(OFF + "far"), "the duel went on out of range, or nobody was told");
            second.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 1.5)));
            net.minecraft.network.chat.Component refused = PackDuel.challenge(second, id(leader));
            helper.assertTrue(refused != null && refused.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().endsWith("duel_cooldown"),
                    "walking out of the duel did not bar the pair: " + refused);
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }
}
