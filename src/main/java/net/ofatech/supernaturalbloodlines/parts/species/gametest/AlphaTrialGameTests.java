package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfAlpha;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrial;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialBoss;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/** Plan 15c: the Alpha Trial (R147): the win, the rite and its warning, the boss. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class AlphaTrialGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final java.util.concurrent.atomic.AtomicLong NEXT = new java.util.concurrent.atomic.AtomicLong(5000);
    private static final SpeciesConfig.TrialTuning EASY = new SpeciesConfig.TrialTuning(true, 1, 3.0);

    private AlphaTrialGameTests() {
    }

    // ---- helpers ----

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, boolean heldSpark) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).heldSpark(heldSpark).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 12), "the test wolf could not join the werewolves");
        return p;
    }

    private static WolfState state(ServerPlayer p) {
        return PackAccess.wolf(p);
    }

    private static void online(ServerPlayer... players) {
        java.util.Map<java.util.UUID, ServerPlayer> map = new java.util.HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
    }

    /** A full-moon night nobody used (a multiple of 8 days is phase 0), at 13000. */
    private static long fullNight() {
        return 24000L * 8 * NEXT.incrementAndGet() + 13000;
    }

    private static void clean(GameTestHelper helper, List<? extends Entity> mobs, ServerPlayer... players) {
        mobs.forEach(Entity::discard);
        PackIndex index = PackIndex.get(helper.getLevel());
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            AlphaTrialRiteFactory.LEDGER.consume(p.getUUID());
        }
        AlphaTrialBoss.reset();
        AlphaTrialRiteFactory.overrideTransformed(null);
        SireCall.overridePlayerLookup(null);
        SpeciesConfig.overrideTrial(null);
    }

    private static BlockPos spot(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 1, 2));
    }

    private static List<Mob> bosses(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(spot(helper)).inflate(40), AlphaTrialBoss::isBoss);
    }

    private static List<Mob> beastsOf(GameTestHelper helper, long packId) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(spot(helper)).inflate(40),
                m -> m.hasData(SpeciesRegistries.NPC_MARK) && m.getData(SpeciesRegistries.NPC_MARK).packId() == packId);
    }

    /** Starts the trial for {@code p} at the circle and gives the boss back (null when it did not start). */
    private static Mob begin(GameTestHelper helper, ServerPlayer p) {
        String refusal = AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), p, true);
        helper.assertTrue(refusal == null, "the trial did not start: " + refusal);
        Mob boss = AlphaTrialBoss.bossOf(helper.getLevel(), p.getUUID());
        helper.assertTrue(boss != null, "the trial started but no boss stands (found " + bosses(helper).size() + " in the area, at " + spot(helper).toShortString() + ")");
        return boss;
    }

    private static void atNight(GameTestHelper helper, long dayTime, Runnable body) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        level.setDayTime(dayTime);
        try {
            body.run();
        } finally {
            level.setDayTime(saved);
        }
    }

    // ---- R147: what winning does ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOmegaWhoWinsLeadsAPackOfTheirOwn(GameTestHelper helper) {
        ServerPlayer omega = wolf(helper, Rank.OMEGA, false);
        ServerPlayer other = wolf(helper, Rank.OMEGA, false);
        online(omega, other);
        try {
            BloodlineAccess.set(omega, BloodlineAccess.get(omega).toBuilder().wolf(state(omega).toBuilder().weak(new Weak(2, 500, 3, 70000)).build()).build());
            helper.assertTrue(AlphaTrial.win(omega, true) == null, "the Omega could not win the trial");
            WolfState w = state(omega);
            helper.assertTrue(w.rank() == Rank.ALPHA && w.heldSpark() && w.sparks() == 1, "the Omega is " + w);
            helper.assertTrue(w.weakSparks() == 0, "the weaker sparks stayed");
            PackIndex index = PackIndex.get(helper.getLevel());
            helper.assertTrue(index.leaderOf(omega.getUUID()).equals(omega.getUUID()) && index.alphaOf(omega.getUUID()).isEmpty(), "the Omega is not the leader of their own pack");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand.invite(omega, other) == null, "the new Alpha could not invite");
        } finally {
            clean(helper, List.of(), omega, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlphaAHumanAndAFormerAlphaAreRefused(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, true);
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, false);
        ServerPlayer human = TestPlayers.createMortal(helper);
        online(alpha, truth, human);
        try {
            helper.assertTrue(AlphaTrial.win(alpha, true) != null, "an Alpha won the trial");
            helper.assertTrue(AlphaTrial.win(truth, true) != null && state(truth).rank() == Rank.TRUE_ALPHA, "a True Alpha won the trial");
            helper.assertTrue(AlphaTrial.win(human, true) != null && state(human).lineage() == Lineage.NONE, "a human won the trial");
        } finally {
            clean(helper, List.of(), alpha, truth);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOpenPathNeedsTheWarningAndAClosedOneDoesNot(GameTestHelper helper) {
        ServerPlayer open = wolf(helper, Rank.OMEGA, false);
        ServerPlayer closed = wolf(helper, Rank.OMEGA, true);
        online(open, closed);
        try {
            helper.assertTrue(AlphaTrial.win(open, false) != null && state(open).rank() == Rank.OMEGA, "the trial took a spark nobody was warned about");
            helper.assertTrue(AlphaTrial.win(closed, false) == null && state(closed).rank() == Rank.ALPHA, "a path already closed was asked again");
            helper.assertTrue(AlphaTrial.win(open, true) == null && state(open).rank() == Rank.ALPHA, "the warned Omega could not win");
        } finally {
            clean(helper, List.of(), open, closed);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whoeverHeldASparkCanNeverBeATrueAlpha(GameTestHelper helper) {
        ServerPlayer winner = wolf(helper, Rank.OMEGA, false);
        online(winner);
        try {
            helper.assertTrue(AlphaTrial.win(winner, true) == null, "the Omega could not win the trial");
            // the spark is given up (a Beta again) and every True Alpha condition is met: the held spark still refuses it for good
            PackContext ctx = new PackContext(null, 14, 30L * 24000 + 100, 14, 30L * 24000, 1, true);
            BloodlineAccess.set(winner, BloodlineAccess.get(winner).toBuilder().wolf(state(winner).toBuilder().rank(Rank.BETA).betaSince(100).defenderKills(5).build()).build());
            var out = net.ofatech.supernaturalbloodlines.core.bloodline.PackRules.apply(state(winner), PackEvent.TRUE_ALPHA_EARNED, ctx);
            helper.assertTrue(out.refused() && out.refusalKey().endsWith("true_alpha_held_spark"), "a held spark did not close the path: " + out.refusalKey());
        } finally {
            clean(helper, List.of(), winner);
        }
        helper.succeed();
    }

    // ---- the rite and its warning ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRiteIsInTheDataWithItsIngredients(GameTestHelper helper) {
        var type = RiteTestSupport.type(helper, "alpha_trial");
        helper.assertTrue(type != null && type.getPower() == 12000, "alpha_trial missing or wrong power");
        var ids = type.getItems().stream().map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString() + "x" + s.getCount()).sorted().toList();
        helper.assertTrue(ids.equals(List.of("minecraft:bonex8", "supernaturalbloodlines:moonstonex1", "werewolves:silver_ingotx1")), "the ingredients are " + ids);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBetaWithAnOpenPathIsRefusedOnceWarnedAndThenTheBossComes(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, false);
        online(beta);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            atNight(helper, fullNight(), () -> {
                String first = AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true);
                helper.assertTrue(first != null && first.endsWith("warn_sent"), "the first start was " + first);
                helper.assertTrue(bosses(helper).isEmpty(), "a boss came before the warning was answered");
                helper.assertTrue(state(beta).rank() == Rank.BETA, "the spark was taken before the player chose");
                String again = AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true);
                helper.assertTrue(again != null && again.endsWith("warn_sent") && bosses(helper).isEmpty(), "a second start without an answer went through: " + again);
                helper.assertTrue(!AlphaTrialRiteFactory.LEDGER.confirm(java.util.UUID.randomUUID(), helper.getLevel().getGameTime()), "a stranger was believed");
                helper.assertTrue(AlphaTrialRiteFactory.LEDGER.confirm(beta.getUUID(), helper.getLevel().getGameTime()), "the answer to a sent warning was not believed");
                Mob boss = begin(helper, beta);
                found.add(boss);
                helper.assertTrue(boss instanceof IWerewolfAlpha, "the boss is not an Alpha");
                helper.assertTrue(boss.getData(SpeciesRegistries.TRIAL_MARK).challenger().equals(beta.getUUID()), "the boss has the wrong challenger");
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true).endsWith("already"), "a second trial for the same player started");
            });
        } finally {
            clean(helper, found, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClosedPathStartsWithNoWarningAndTheBossHasTheConfiguredHealth(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, true);
        online(beta);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            atNight(helper, fullNight(), () -> {
                @SuppressWarnings("unchecked")
                Mob plain = net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner.place(helper.getLevel(),
                        (EntityType<? extends Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("werewolves:alpha_werewolf")), spot(helper).offset(8, 0, 0),
                        net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner.MAX_LEVEL);
                helper.assertTrue(plain != null, "an Alpha at its highest level did not get into the world");
                double base = plain.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
                plain.discard();
                Mob boss = begin(helper, beta);
                found.add(boss);
                double bossBase = boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
                helper.assertTrue(Math.abs(bossBase - base * 3.0) < 0.5 && boss.getHealth() == boss.getMaxHealth(), "health base " + bossBase + " from " + base + ", now " + boss.getHealth() + "/" + boss.getMaxHealth());
            });
        } finally {
            clean(helper, found, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyOtherRefusalHasItsOwnReasonAndStartsNothing(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, true);
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, true);
        ServerPlayer human = TestPlayers.createMortal(helper);
        online(beta, alpha, human);
        try {
            SpeciesConfig.overrideTrial(new SpeciesConfig.TrialTuning(true, 13, 3.0));
            atNight(helper, fullNight(), () -> {
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true).endsWith("level"), "level 12 under a minimum of 13 started");
                SpeciesConfig.overrideTrial(EASY);
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), alpha, true).endsWith("rank"), "an Alpha started");
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), human, true).endsWith("not_werewolf"), "a human started");
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, false).endsWith("not_transformed"), "a Beta in human shape started");
                SpeciesConfig.overrideTrial(new SpeciesConfig.TrialTuning(false, 1, 3.0));
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true).endsWith("off"), "alpha_trial_enabled = false started");
            });
            atNight(helper, fullNight() - 13000 + 6000, () -> {
                SpeciesConfig.overrideTrial(EASY);
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true).endsWith("not_night"), "the trial started in daylight");
            });
            atNight(helper, 24000L * 8 * NEXT.incrementAndGet() + 24000 + 13000, () -> {
                helper.assertTrue(AlphaTrialRiteFactory.tryStart(helper.getLevel(), spot(helper), beta, true).endsWith("not_night"), "the trial started on a night with no full moon");
            });
            helper.assertTrue(bosses(helper).isEmpty(), "a refused trial left a boss");
        } finally {
            clean(helper, bosses(helper), beta, alpha);
        }
        helper.succeed();
    }

    /** The real rite, started the way an altar does: the warning cancels it (the ingredients come back) and sends nothing a FakePlayer cannot take. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRealRiteCancelsOnTheWarningAndGivesTheIngredientsBack(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, false);
        online(beta);
        try {
            SpeciesConfig.overrideTrial(EASY);
            AlphaTrialRiteFactory.overrideTransformed(p -> true);
            RiteTestSupport.place(helper, beta);
            atNight(helper, fullNight(), () -> {
                RiteTestSupport.start(helper, "alpha_trial", beta, List.of(new ItemStack(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.MOONSTONE.get()),
                        new ItemStack(RiteTestSupport.item("werewolves:silver_ingot")), new ItemStack(Items.BONE, 8)));
            });
            helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:bone") == 8, "the bones did not come back: " + RiteTestSupport.dropped(helper, "minecraft:bone"));
            helper.assertTrue(state(beta).rank() == Rank.BETA, "the rite took the spark");
            helper.assertTrue(AlphaTrialRiteFactory.LEDGER.confirm(beta.getUUID(), helper.getLevel().getGameTime()), "the rite did not send the warning");
        } finally {
            RiteTestSupport.remove(helper, beta);
            clean(helper, List.of(), beta);
        }
        helper.succeed();
    }

    // ---- the boss ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheChallengerAndTheirPackCanHurtTheBoss(GameTestHelper helper) {
        ServerPlayer challenger = wolf(helper, Rank.BETA, true);
        ServerPlayer mate = wolf(helper, Rank.BETA, true);
        ServerPlayer stranger = wolf(helper, Rank.ALPHA, true);
        online(challenger, mate, stranger);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            atNight(helper, fullNight(), () -> {
                PackIndex.get(helper.getLevel()).join(mate.getUUID(), challenger.getUUID());
                Mob boss = begin(helper, challenger);
                found.add(boss);
                float full = boss.getHealth();
                boss.hurt(stranger.damageSources().playerAttack(stranger), 10f);
                helper.assertTrue(boss.getHealth() == full, "a stranger hurt the boss");
                boss.hurt(challenger.damageSources().playerAttack(challenger), 10f);
                helper.assertTrue(boss.getHealth() < full, "the challenger could not hurt the boss");
                float afterChallenger = boss.getHealth();
                boss.invulnerableTime = 0;
                boss.hurt(mate.damageSources().playerAttack(mate), 10f);
                helper.assertTrue(boss.getHealth() < afterChallenger, "the challenger's pack could not hurt the boss");
                float afterMate = boss.getHealth();
                boss.invulnerableTime = 0;
                boss.hurt(boss.damageSources().generic(), 10f);
                helper.assertTrue(boss.getHealth() < afterMate, "the world could not hurt the boss");
            });
        } finally {
            PackIndex.get(helper.getLevel()).leave(mate.getUUID());
            clean(helper, found, challenger, mate, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoBeastsJoinAtTwoThirdsAndTwoMoreAtOneThird(GameTestHelper helper) {
        ServerPlayer challenger = wolf(helper, Rank.BETA, true);
        online(challenger);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            atNight(helper, fullNight(), () -> {
                Mob boss = begin(helper, challenger);
                found.add(boss);
                long pack = boss.getData(SpeciesRegistries.TRIAL_MARK).packId();
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(beastsOf(helper, pack).isEmpty(), "beasts came at full health");
                boss.setHealth(boss.getMaxHealth() * 0.6f);
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(beastsOf(helper, pack).size() == 2, "two beasts were expected at 60%, found " + beastsOf(helper, pack).size());
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(beastsOf(helper, pack).size() == 2, "the same phase called more beasts");
                boss.setHealth(boss.getMaxHealth() * 0.3f);
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(beastsOf(helper, pack).size() == 4, "four beasts were expected at 30%, found " + beastsOf(helper, pack).size());
                helper.assertTrue(boss.getData(SpeciesRegistries.TRIAL_MARK).phase() == 2, "the phase was not remembered");
                found.addAll(beastsOf(helper, pack));
            });
        } finally {
            clean(helper, found, challenger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killingTheBossGivesTheSparkAndNoWeakerOneAndSendsTheBeastsAway(GameTestHelper helper) {
        ServerPlayer challenger = wolf(helper, Rank.OMEGA, true);
        online(challenger);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            atNight(helper, fullNight(), () -> {
                Mob boss = begin(helper, challenger);
                found.add(boss);
                long pack = boss.getData(SpeciesRegistries.TRIAL_MARK).packId();
                boss.setHealth(boss.getMaxHealth() * 0.5f);
                AlphaTrialBoss.tickNow(boss);
                List<Mob> beasts = beastsOf(helper, pack);
                helper.assertTrue(!beasts.isEmpty(), "no beasts to send away");
                boss.hurt(challenger.damageSources().playerAttack(challenger), 1_000_000f);
                helper.assertTrue(!boss.isAlive(), "the boss did not die");
                helper.assertTrue(state(challenger).rank() == Rank.ALPHA && state(challenger).heldSpark(), "the winner is " + state(challenger));
                helper.assertTrue(state(challenger).weakSparks() == 0, "the boss gave a weaker spark as well");
                helper.assertTrue(beasts.stream().allMatch(Entity::isRemoved), "the beasts stayed");
                helper.assertTrue(!AlphaTrialBoss.running(helper.getLevel(), challenger.getUUID()), "the trial still runs");
            });
        } finally {
            clean(helper, found, challenger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTrialIsLostWhenTheChallengerFallsLeavesOrTheNightEnds(GameTestHelper helper) {
        ServerPlayer challenger = wolf(helper, Rank.BETA, true);
        online(challenger);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            // dead
            long night = fullNight();
            atNight(helper, night, () -> {
                Mob boss = begin(helper, challenger);
                found.add(boss);
                challenger.setHealth(0f);
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(boss.isRemoved(), "the boss stayed after the challenger fell");
                helper.assertTrue(state(challenger).rank() == Rank.BETA, "a lost trial gave the spark");
                challenger.setHealth(challenger.getMaxHealth());
            });
            // far away: the leash is 30 seconds
            atNight(helper, night, () -> {
                Mob boss = begin(helper, challenger);
                found.add(boss);
                challenger.setPos(boss.position().add(60, 0, 0));
                for (int i = 0; i < 58; i++) {
                    AlphaTrialBoss.tickNow(boss);
                }
                helper.assertTrue(!boss.isRemoved(), "the boss left before the leash was up");
                for (int i = 0; i < 4; i++) {
                    AlphaTrialBoss.tickNow(boss);
                }
                helper.assertTrue(boss.isRemoved(), "the boss stayed after 30 seconds away");
                challenger.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
            });
            // coming back resets the leash
            atNight(helper, night, () -> {
                Mob boss = begin(helper, challenger);
                found.add(boss);
                challenger.setPos(boss.position().add(60, 0, 0));
                for (int i = 0; i < 50; i++) {
                    AlphaTrialBoss.tickNow(boss);
                }
                challenger.setPos(boss.position().add(3, 0, 0));
                AlphaTrialBoss.tickNow(boss);
                challenger.setPos(boss.position().add(60, 0, 0));
                for (int i = 0; i < 50; i++) {
                    AlphaTrialBoss.tickNow(boss);
                }
                helper.assertTrue(!boss.isRemoved(), "the leash did not start again when the challenger came back");
                // the night ends
                helper.getLevel().setDayTime(night - 13000 + 23500);
                challenger.setPos(boss.position().add(3, 0, 0));
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(boss.isRemoved(), "the boss stayed after the night");
            });
        } finally {
            challenger.setHealth(challenger.getMaxHealth());
            clean(helper, found, challenger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillOfAnotherAlphaStillGivesTheWeakerSpark(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, true);
        online(p);
        @SuppressWarnings("unchecked")
        Mob natural = net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner.place(helper.getLevel(), (EntityType<? extends Mob>) EntityType.byString("werewolves:alpha_werewolf").orElseThrow(), spot(helper),
                net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner.HALF_LEVEL);
        try {
            helper.assertTrue(!AlphaTrialBoss.isBoss(natural), "control: the natural Alpha is a trial boss");
            natural.hurt(p.damageSources().playerAttack(p), 1_000_000f);
            helper.assertTrue(state(p).weakSparks() == 1, "control: an ordinary Alpha gave no weaker spark");
        } finally {
            clean(helper, List.of(natural), p);
        }
        helper.succeed();
    }

    /** What a restart does to a running trial: the boss and its mark are saved with the entity, so its tick finds its challenger again. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBossMarkSurvivesAndTheTrialIsFoundAgain(GameTestHelper helper) {
        ServerPlayer challenger = wolf(helper, Rank.BETA, true);
        online(challenger);
        List<Mob> found = new ArrayList<>();
        try {
            SpeciesConfig.overrideTrial(EASY);
            atNight(helper, fullNight(), () -> {
                Mob boss = begin(helper, challenger);
                found.add(boss);
                TrialMark mark = boss.getData(SpeciesRegistries.TRIAL_MARK);
                var tag = boss.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                helper.assertTrue(tag.toString().contains("trial_mark"), "the mark is not saved with the entity");
                AlphaTrialBoss.reset(); // a restart forgets the maps
                helper.assertTrue(!AlphaTrialBoss.running(helper.getLevel(), challenger.getUUID()), "control: the map was not forgotten");
                AlphaTrialBoss.tickNow(boss);
                helper.assertTrue(AlphaTrialBoss.running(helper.getLevel(), challenger.getUUID()), "the boss did not find its trial again");
                helper.assertTrue(mark.challenger().equals(boss.getData(SpeciesRegistries.TRIAL_MARK).challenger()), "the challenger changed");
            });
        } finally {
            clean(helper, found, challenger);
        }
        helper.succeed();
    }
}
