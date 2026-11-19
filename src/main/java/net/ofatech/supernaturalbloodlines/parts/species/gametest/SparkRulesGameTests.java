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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrial;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Plan 18, the spark rules: two separate paths (Alpha and True Alpha), the warning and the protection before a spark is taken, the True Alpha's full
 * sparks, heal that destroys the spark, and the hand-overs (cure, duel, absence). Werewolves only. Each test names the rule it pins.
 */
@PrefixGameTestTemplate(false)
public final class SparkRulesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SparkRulesGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, UUID packAlpha, int level, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).packAlpha(packAlpha)
                .betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    private static ServerPlayer alpha(GameTestHelper helper, int level, double x) {
        return wolf(helper, Rank.ALPHA, null, level, x);
    }

    private static void protect(ServerPlayer p, boolean on) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(state(p).toBuilder().protectPath(on).build()).build());
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
        AlphaRoar.overridePlayers(s -> List.of(players));
    }

    private static void clean(GameTestHelper helper, ServerPlayer... players) {
        PackIndex index = PackIndex.get(helper.getLevel());
        SireCall.overridePlayerLookup(null);
        AlphaRoar.overridePlayers(null);
        AlphaRoar.reset();
        PackDuel.reset();
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            index.takeDissolved(p.getUUID());
            index.takeNotices(p.getUUID());
            p.removeAllEffects();
        }
    }

    private static WolfState state(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    private static boolean join(ServerPlayer leader, ServerPlayer target) {
        if (PackCommand.invite(leader, target) != null) {
            return false;
        }
        return PackCommand.accept(target, PackCommand.requests().tokenOf(leader.getUUID(), target.getUUID()).orElseThrow()) == null;
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, killer.damageSources().playerAttack(killer)));
    }

    // ---- rule 1 and 2: two paths, the warning, the protection ----

    /** The kill cannot show a dialog: the protection (on by default while the path is open) destroys the spark; with it off the spark is taken and the path ends. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aProtectedBetaWhoKillsAnAlphaDestroysTheSparkAndAnUnprotectedOneTakesItAndLosesThePath(GameTestHelper helper) {
        ServerPlayer first = alpha(helper, 5, 1.5);
        ServerPlayer second = alpha(helper, 5, 3.5);
        ServerPlayer killer = wolf(helper, Rank.OMEGA, null, 1, 2.5);
        online(first, second, killer);
        try {
            helper.assertTrue(PackRules.pathOpen(state(killer)) && state(killer).protectPath(), "the killer's path is not open and protected by default: " + state(killer));
            first.setLastHurtMob(killer); // a fight, not an innocent kill (blue eyes would close the path on their own)
            kill(killer, first);
            helper.assertTrue(state(killer).rank() == Rank.OMEGA && !state(killer).heldSpark() && !state(killer).stoleSpark(), "the protected killer took it: " + state(killer));
            helper.assertTrue(PackRules.pathOpen(state(killer)), "the path was closed by a destroyed spark");
            helper.assertTrue(state(first).rank() == Rank.OMEGA, "the spark was not destroyed: " + state(first));

            helper.assertTrue(PackCommand.protect(killer, false) == null && !state(killer).protectPath(), "the protection did not go off");
            kill(killer, second);
            helper.assertTrue(state(killer).rank() == Rank.ALPHA && state(killer).heldSpark() && state(killer).stoleSpark(), "the unprotected killer: " + state(killer));
            helper.assertTrue(!PackRules.pathOpen(state(killer)), "the path stayed open after taking a spark");
            helper.assertTrue(PackAccess.applyPack(killer, PackEvent.TRUE_ALPHA_EARNED, new PackContext(null, 10, 99_999_999L, 10, 1, 1, true)).refused(), "an Alpha earned the True Alpha rank");
        } finally {
            clean(helper, first, second, killer);
        }
        helper.succeed();
    }

    /** The warning hook: the trial refuses a Beta whose path is open until they were warned; once taken the spark closes the path for good. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAlphaTrialAsksFirstAndWinningItClosesThePath(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 2, 2.5);
        ServerPlayer blue = wolf(helper, Rank.BETA, leader.getUUID(), 2, 3.5);
        BloodlineAccess.set(blue, BloodlineAccess.get(blue).toBuilder().wolf(state(blue).toBuilder().killedInnocent(true).build()).build());
        online(leader, beta, blue);
        try {
            helper.assertTrue(AlphaTrial.win(beta, false) != null && state(beta).rank() == Rank.BETA, "the trial took a spark nobody was warned about");
            helper.assertTrue(AlphaTrial.win(blue, false) == null && state(blue).rank() == Rank.ALPHA, "a Beta with no path left was asked anyway");
            helper.assertTrue(AlphaTrial.win(beta, true) == null && state(beta).rank() == Rank.ALPHA, "the warned Beta could not win");
            helper.assertTrue(state(beta).heldSpark() && !PackRules.pathOpen(state(beta)), "winning did not close the path: " + state(beta));
        } finally {
            clean(helper, leader, beta, blue);
        }
        helper.succeed();
    }

    /** Protection covers kills only (owner ruling): an inheritance gives the spark to the strongest Beta even if they protect an open path, and that closes it; the pack goes on. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackInheritanceStillGivesTheSparkToAProtectedBetaAndClosesTheirPath(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer protectedBeta = wolf(helper, Rank.BETA, leader.getUUID(), 12, 2.5);
        ServerPlayer other = wolf(helper, Rank.BETA, leader.getUUID(), 2, 3.5);
        online(leader, protectedBeta, other);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(PackRules.pathOpen(state(protectedBeta)) && state(protectedBeta).protectPath(), "control: the strongest Beta protects an open path");
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false); // cured: the spark passes on
            helper.assertTrue(state(protectedBeta).rank() == Rank.ALPHA && state(protectedBeta).heldSpark() && !PackRules.pathOpen(state(protectedBeta)),
                    "the protected Beta did not inherit, or their path stayed open: " + state(protectedBeta));
            helper.assertTrue(index.leaderOf(other.getUUID()).equals(protectedBeta.getUUID()) && state(other).rank() == Rank.BETA, "the pack did not go on under the heir");
        } finally {
            clean(helper, leader, protectedBeta, other);
        }
        // protection never dissolves a pack: with only protected Betas the strongest still inherits
        ServerPlayer lead2 = alpha(helper, 6, 1.5);
        ServerPlayer only = wolf(helper, Rank.BETA, lead2.getUUID(), 9, 2.5);
        online(lead2, only);
        try {
            VampirismAPI.factionPlayerHandler(lead2).leaveFaction(false);
            helper.assertTrue(state(only).rank() == Rank.ALPHA && !PackRules.pathOpen(state(only)), "the only (protected) Beta did not inherit: " + state(only));
        } finally {
            clean(helper, lead2, only);
        }
        helper.succeed();
    }

    /** Duel and absence hand the lead between Alphas who hold a spark already: they warn nobody because nobody takes one. A Beta of the pack stays one with the path open. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelAndAnAbsenceForceNoSparkOnABeta(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        online(leader, second, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the pack could not be built");
            helper.assertTrue(PackDuelGameTests.win(second, leader) && index.isLeader(second.getUUID()), "the duel was not won");
            helper.assertTrue(state(beta).rank() == Rank.BETA && PackRules.pathOpen(state(beta)), "the duel gave the Beta a spark: " + state(beta));
            helper.assertTrue(state(leader).sparks() == 1 && state(second).sparks() == 1, "a spark moved in the duel");
        } finally {
            clean(helper, leader, second, beta);
        }
        UUID goneId = UUID.randomUUID();
        ServerPlayer strong = alpha(helper, 9, 1.5);
        ServerPlayer waiting = wolf(helper, Rank.BETA, goneId, 1, 2.5);
        online(strong, waiting);
        long now = System.currentTimeMillis();
        try {
            index.join(goneId, strong.getUUID());
            index.join(goneId, waiting.getUUID());
            index.setStrength(strong.getUUID(), 1009);
            index.setLastSeen(goneId, now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1 && index.isLeader(strong.getUUID()), "the absent leader was not deposed");
            helper.assertTrue(state(waiting).rank() == Rank.BETA && PackRules.pathOpen(state(waiting)), "the absence gave the Beta a spark: " + state(waiting));
        } finally {
            index.leave(goneId);
            index.takeNotices(goneId);
            clean(helper, strong, waiting);
        }
        helper.succeed();
    }

    // ---- rule 3: the True Alpha has full sparks by nature ----

    /** A True Alpha never steals (the Alpha they killed keeps the spark) and nobody steals from one. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaNeverStealsAndNothingIsTakenFromOne(GameTestHelper helper) {
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, null, 1, 1.5);
        ServerPlayer victim = alpha(helper, 5, 3.5);
        ServerPlayer thief = alpha(helper, 5, 5.5);
        online(truth, victim, thief);
        try {
            kill(truth, victim);
            helper.assertTrue(state(victim).rank() == Rank.ALPHA && state(victim).sparks() == 1, "the Alpha lost the spark to a True Alpha: " + state(victim));
            helper.assertTrue(state(truth).rank() == Rank.TRUE_ALPHA && !state(truth).stoleSpark() && !state(truth).heldSpark(), "the True Alpha took a spark: " + state(truth));
            kill(thief, truth);
            helper.assertTrue(state(truth).rank() == Rank.TRUE_ALPHA, "a True Alpha lost the rank: " + state(truth));
            helper.assertTrue(state(thief).sparks() == 1 && !state(thief).stoleSpark(), "the thief got something: " + state(thief));
        } finally {
            clean(helper, truth, victim, thief);
        }
        helper.succeed();
    }

    /** Active: the bonus and the top Roar tier of max_stacked_sparks, whatever the stored count; dormant (out of the werewolves): off. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anActiveTrueAlphaHoldsTheFullSparksAndADormantOneDoesNot(GameTestHelper helper) {
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, null, 1, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, truth.getUUID(), 1, 3.5);
        online(truth, beta);
        int cap = SpeciesConfig.maxStackedSparks();
        double full = 20 + Math.round(4 * 1.1 * PackRules.sparkScale(cap, cap) * 1000) / 1000.0;
        try {
            RankEffects.sync(truth);
            helper.assertTrue(state(truth).sparks() == 1, "the test needs a stored count of one");
            helper.assertTrue(PackIndex.get(helper.getLevel()).sparksOf(truth.getUUID()) == cap, "the pack index does not hold the full count: " + PackIndex.get(helper.getLevel()).sparksOf(truth.getUUID())); // what the pack's rows show
            helper.assertTrue(Math.abs(truth.getMaxHealth() - full) < 0.01, "the bonus is not the full one: " + truth.getMaxHealth() + " vs " + full);
            helper.assertTrue(AlphaRoar.roar(truth, 10) == null, "the Roar was refused");
            var strength = beta.getEffect(MobEffects.DAMAGE_BOOST);
            helper.assertTrue(strength != null && strength.getAmplifier() == PackRules.roarTier(cap, cap), "the Roar tier is not the top one: " + (strength == null ? "none" : strength.getAmplifier()));

            VampirismAPI.factionPlayerHandler(truth).leaveFaction(false); // cured: dormant
            RankEffects.sync(truth);
            helper.assertTrue(Math.abs(truth.getMaxHealth() - 20) < 0.01, "the dormant True Alpha kept the bonus: " + truth.getMaxHealth());
            helper.assertTrue(state(truth).rank() == Rank.TRUE_ALPHA, "the rank was lost (R91)");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(truth).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not rejoin");
            RankEffects.sync(truth);
            helper.assertTrue(Math.abs(truth.getMaxHealth() - full) < 0.01, "the bonus did not come back on the rejoin: " + truth.getMaxHealth());
        } finally {
            clean(helper, truth, beta);
        }
        helper.succeed();
    }

    // ---- rule 6: heal destroys the spark ----

    /** The spark is destroyed, the healed one stays a Beta (never the heir), the leader's pack goes to the strongest other Alpha. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void healingDestroysTheSparkAndTheHealedStaysABetaWhileTheLeadPassesToTheStrongestAlpha(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer co = alpha(helper, 3, 2.5);
        ServerPlayer patient = wolf(helper, Rank.BETA, leader.getUUID(), 12, 3.5); // the strongest Beta: the old rule made them the heir
        online(leader, co, patient);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, co), "the co-Alpha could not join");
            helper.assertTrue(PackCommand.heal(leader, patient) == null, "the heal was refused");
            helper.assertTrue(state(patient).rank() == Rank.BETA && PackRules.pathOpen(state(patient)), "the healed one is not a Beta with an open path: " + state(patient));
            helper.assertTrue(state(leader).rank() == Rank.BETA && co.getUUID().equals(state(leader).packAlpha()), "the healer is not a Beta of the co-Alpha: " + state(leader));
            helper.assertTrue(index.isLeader(co.getUUID()) && co.getUUID().equals(index.leaderOf(patient.getUUID())), "the lead did not pass to the other Alpha");
            helper.assertTrue(state(co).rank() == Rank.ALPHA && state(co).sparks() == 1, "somebody inherited the spark: " + state(co));
        } finally {
            clean(helper, leader, co, patient);
        }
        // no other Alpha: the pack dissolves, nobody inherits
        ServerPlayer lone = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, lone.getUUID(), 12, 2.5);
        online(lone, beta);
        try {
            helper.assertTrue(PackCommand.heal(lone, beta) == null, "the heal was refused");
            helper.assertTrue(state(beta).rank() != Rank.ALPHA && PackRules.pathOpen(state(beta)), "the healed one inherited: " + state(beta));
            helper.assertTrue(state(lone).rank() == Rank.OMEGA && state(lone).heldSpark(), "the healer is " + state(lone));
            helper.assertTrue(index.betasOf(lone.getUUID()).isEmpty() && index.alphaOf(beta.getUUID()).isEmpty(), "the pack was not dissolved");
        } finally {
            clean(helper, lone, beta);
        }
        helper.succeed();
    }

    /** The spark was given up: the healer held one, so their path is closed for good (they can never be a True Alpha). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void whoHeldASparkAndGaveItUpCanNeverBeATrueAlpha(GameTestHelper helper) {
        ServerPlayer healer = alpha(helper, 6, 1.5);
        ServerPlayer patient = wolf(helper, Rank.BETA, healer.getUUID(), 1, 2.5);
        online(healer, patient);
        try {
            helper.assertTrue(PackCommand.heal(healer, patient) == null, "the heal was refused");
            WolfState w = state(healer);
            helper.assertTrue(w.heldSpark() && !PackRules.pathOpen(w), "the history was lost: " + w);
            helper.assertTrue(PackRules.apply(w.toBuilder().betaSince(1).defenderKills(999).build(), PackEvent.TRUE_ALPHA_EARNED, new PackContext(null, 10, 99_999_999L, 10, 1, 1, true)).refused(),
                    "a former Alpha earned the True Alpha rank");
        } finally {
            clean(helper, healer, patient);
        }
        helper.succeed();
    }

    /** The tombstone of an ex-Alpha is the one record from before this version: at login it marks them as having held a spark; a dormant True Alpha's is not that. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anExAlphaWithATombstoneIsMarkedAsHavingHeldASparkAtLogin(GameTestHelper helper) {
        ServerPlayer ex = wolf(helper, Rank.OMEGA, null, 1, 1.5);
        ServerPlayer clean = wolf(helper, Rank.OMEGA, null, 1, 2.5);
        ServerPlayer dormant = wolf(helper, Rank.TRUE_ALPHA, null, 1, 3.5);
        PackIndex index = PackIndex.get(helper.getLevel());
        online(ex, clean, dormant);
        try {
            index.markFormerAlpha(ex.getUUID());
            index.markFormerAlpha(dormant.getUUID());
            helper.assertTrue(PackRules.pathOpen(state(ex)), "control: the path was closed before the login");
            PackIndex.login(ex);
            PackIndex.login(clean);
            PackIndex.login(dormant);
            helper.assertTrue(state(ex).heldSpark() && !PackRules.pathOpen(state(ex)), "the ex-Alpha kept an open path: " + state(ex));
            helper.assertTrue(!state(clean).heldSpark() && PackRules.pathOpen(state(clean)), "someone with no history was marked: " + state(clean));
            helper.assertTrue(!state(dormant).heldSpark() && state(dormant).rank() == Rank.TRUE_ALPHA, "a dormant True Alpha was marked: " + state(dormant));
        } finally {
            index.clearFormerAlpha(ex.getUUID());
            index.clearFormerAlpha(dormant.getUUID());
            clean(helper, ex, clean, dormant);
        }
        helper.succeed();
    }

    /** A True Alpha's own client is told the full count even with no pack row (it does not know the cap); a dormant one, and an Alpha, see their own. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphasOwnClientIsToldTheFullSparkCount(GameTestHelper helper) {
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, null, 1, 1.5);
        ServerPlayer alpha = alpha(helper, 2, 2.5);
        online(truth, alpha);
        try {
            int cap = SpeciesConfig.maxStackedSparks();
            helper.assertTrue(cap > 1, "control: the cap is one");
            helper.assertTrue(BloodlineAccess.ownView(truth, BloodlineAccess.get(truth)).wolf().sparks() == cap, "the True Alpha's own view has " + BloodlineAccess.ownView(truth, BloodlineAccess.get(truth)).wolf().sparks());
            helper.assertTrue(BloodlineAccess.ownView(alpha, BloodlineAccess.get(alpha)).wolf().sparks() == state(alpha).sparks(), "an Alpha's own view is not their own count");
            helper.assertTrue(BloodlineAccess.clientView(BloodlineAccess.get(truth), false).wolf().sparks() <= 1, "another player was told the True Alpha's full count");
            VampirismAPI.factionPlayerHandler(truth).leaveFaction(false);
            helper.assertTrue(BloodlineAccess.ownView(truth, BloodlineAccess.get(truth)).wolf().sparks() == 1, "a dormant True Alpha's view still has the full count");
        } finally {
            clean(helper, truth, alpha);
        }
        helper.succeed();
    }

    /** Turning the protection off asks twice in the hub, so the typed command must too: a bare "off" is no command. And it means nothing to a player with no lineage. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void protectOffNeedsConfirmAndMeansNothingWithoutALineage(GameTestHelper helper) {
        ServerPlayer wolf = wolf(helper, Rank.OMEGA, null, 1, 1.5);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        try {
            var source = wolf.createCommandSourceStack();
            helper.assertTrue(PackCommand.protect(wolf, true) == null, "control");
            var bare = dispatcher.parse("pack protect off", source);
            helper.assertTrue(bare.getContext().getCommand() != null && bare.getExceptions().isEmpty(), "a bare /pack protect off is a parse error, not a hint");
            try {
                dispatcher.execute(bare);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                helper.fail("a bare /pack protect off is a parse error: " + e.getMessage());
            }
            helper.assertTrue(state(wolf).protectPath(), "a bare /pack protect off changed the setting");
            helper.assertTrue(dispatcher.parse("pack protect off confirm", source).getContext().getCommand() != null, "/pack protect off confirm is no command");
            helper.assertTrue(dispatcher.parse("pack protect on", source).getContext().getCommand() != null, "/pack protect on is no command");
            helper.assertTrue(PackCommand.protect(human, false) != null && BloodlineAccess.get(human).wolf().protectPath(), "a human changed a setting of a path they do not have");
            helper.assertTrue(PackCommand.protect(wolf, false) == null && !state(wolf).protectPath(), "the werewolf could not turn it off");
        } finally {
            clean(helper, wolf);
        }
        helper.succeed();
    }

    /** A protected killer whose victim was not fighting them is told the truth: the kill is innocent, the eyes turn blue and the path is closed anyway. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aProtectedKillerIsToldWhenTheKillWasInnocentAnyway(GameTestHelper helper) {
        TestPlayers.TalkingDuelist fought = talkingWolf(helper, Rank.OMEGA, 1.5);
        TestPlayers.TalkingDuelist unprovoked = talkingWolf(helper, Rank.OMEGA, 2.5);
        ServerPlayer a = alpha(helper, 5, 3.5);
        ServerPlayer b = alpha(helper, 5, 4.5);
        online(fought, unprovoked, a, b);
        try {
            a.setLastHurtMob(fought);
            kill(fought, a);
            helper.assertTrue(fought.received("supernaturalbloodlines.pack.spark_destroyed") && !fought.received("supernaturalbloodlines.pack.spark_destroyed_innocent"),
                    "a kill in a fight was not told as a plain destroyed spark: " + fought.keys());
            helper.assertTrue(PackRules.pathOpen(state(fought)), "the path closed after a fair fight");
            kill(unprovoked, b);
            helper.assertTrue(unprovoked.received("supernaturalbloodlines.pack.spark_destroyed_innocent") && !unprovoked.received("supernaturalbloodlines.pack.spark_destroyed"),
                    "an unprovoked kill was told 'you keep your path': " + unprovoked.keys());
            helper.assertTrue(state(unprovoked).killedInnocent() && !PackRules.pathOpen(state(unprovoked)), "the blue eyes did not close the path: " + state(unprovoked));
        } finally {
            clean(helper, fought, unprovoked, a, b);
        }
        helper.succeed();
    }

    private static TestPlayers.TalkingDuelist talkingWolf(GameTestHelper helper, Rank rank, double x) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).build()).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    /** A cure of an Alpha keeps what they held: the spark history outlives the wolf layer. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCuredAlphaStillHasHeldASpark(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 6, 1.5);
        online(a);
        try {
            VampirismAPI.factionPlayerHandler(a).leaveFaction(false);
            helper.assertTrue(BloodlineAccess.get(a).wolf().heldSpark(), "the cure erased the spark history: " + BloodlineAccess.get(a).wolf());
        } finally {
            clean(helper, a);
        }
        helper.succeed();
    }
}
