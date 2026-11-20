package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;

/** Mikael's encounter (plan 17b): who the director sends him for, once a night, and what the commands and the bar do. */
@PrefixGameTestTemplate(false)
public final class MikaelGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String HIDDEN_NOTICE = "supernaturalbloodlines.boss.notice.mikael.unknown";
    private static final MikaelRules.Facts HUNTABLE = new MikaelRules.Facts(true, 4, 3, true, true, true, false);

    private MikaelGameTests() {
    }

    /** The director looks at this one player, whose facts are fixed; it is night on day 20. */
    private static void world(GameTestHelper helper, ServerPlayer p, MikaelRules.Facts facts) {
        BossTestRig.fixedGround();
        BossDirector.useWorld(l -> List.of(p), who -> facts);
        helper.getLevel().setDayTime(24000L * 20 + 15000);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theDirectorSendsHimForAHighVampireAtNightAndTellsThemWithoutAName(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            world(helper, p, HUNTABLE);
            BossDirector.check(rig.server);
            MikaelEntity m = (MikaelEntity) BossDirector.bossEntity(rig.server, BossId.MIKAEL);
            helper.assertTrue(m != null && m.isAlive(), "the director did not send him");
            helper.assertTrue(p.getUUID().equals(m.prey()), "he does not hunt the player the director picked");
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.ACTIVE && rig.index.of(BossId.MIKAEL).appearances == 1, "the index says " + rig.index.state(BossId.MIKAEL));
            double d = Math.sqrt(m.distanceToSqr(p));
            helper.assertTrue(d >= 4 && d <= 45, "he came from " + d + " blocks away");
            helper.assertTrue(m.getMainHandItem().is(net.ofatech.supernaturalbloodlines.parts.bosses.BossItems.MIKAELS_STAKE.get()), "he does not hold his stake");
            helper.assertTrue(p.received(HIDDEN_NOTICE), "the target was not warned, or was told his name: " + p.heard());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFailedRollSpendsTheNightAndNobodyComes(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.99);
            world(helper, p, HUNTABLE);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "he came on a roll above the chance");
            BossDirector.useRoll(() -> 0.0);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "a second roll was taken the same night");
            helper.getLevel().setDayTime(24000L * 21 + 15000);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) != null, "he did not come the next night");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nobodyIsHuntedByDayOrWhenNobodyQualifies(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            world(helper, p, HUNTABLE);
            helper.getLevel().setDayTime(24000L * 20 + 6000);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "he came by day");
            world(helper, p, new MikaelRules.Facts(true, 1, 3, true, true, true, false));
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "he came for a low rung");
            world(helper, p, new MikaelRules.Facts(true, 4, 3, true, true, true, true));
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "he came for someone inside a mountain ash ring");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSwitchedOffMikaelNeverComesAndAPresentOneIsNotMadeTwice(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            BossesConfig.override("mikael_enabled", false);
            world(helper, p, HUNTABLE);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "mikael_enabled = false did not stop him");
            BossesConfig.override("mikael_enabled", null);
            BossDirector.check(rig.server);
            MikaelEntity first = (MikaelEntity) BossDirector.bossEntity(rig.server, BossId.MIKAEL);
            helper.getLevel().setDayTime(24000L * 21 + 15000);
            BossDirector.check(rig.server);
            helper.assertTrue(first != null && rig.index.of(BossId.MIKAEL).appearances == 1, "he was made a second time while he is out there");
        } finally {
            BossesConfig.override("mikael_enabled", null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void peacefulMeansNoMikael(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            world(helper, p, HUNTABLE);
            BossDirector.useDifficulty(l -> Difficulty.PEACEFUL);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "he came on peaceful");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBossWhoseEntityIsGoneIsLostAndMayComeBackWithTheRewardStillPaid(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            MikaelEntity m = rig.spawn();
            m.desiccate(); // pays the reward, once
            m.recover();
            helper.assertTrue(rig.index.of(BossId.MIKAEL).rewarded, "control: the reward is not marked paid");
            m.discard();
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "one missing look is not enough to lose him");
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.LOST, "the index says " + rig.index.state(BossId.MIKAEL));
            world(helper, p, HUNTABLE);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) != null && rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "a lost boss was not brought back");
            helper.assertTrue(rig.index.of(BossId.MIKAEL).rewarded, "the reward was forgotten when he came back");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeadMikaelIsNeverMadeAgain(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            MikaelEntity m = rig.spawn();
            m.kill();
            world(helper, p, HUNTABLE);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null || !BossDirector.bossEntity(rig.server, BossId.MIKAEL).isAlive(), "a dead Mikael was made again");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisBarFollowsHimAndGoesWithHim(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            BossBars.update(m);
            helper.assertTrue(BossBars.has(m.getUUID()), "no bar for a boss");
            BossesConfig.override("boss_bars_enabled", false);
            BossBars.update(m);
            helper.assertTrue(!BossBars.has(m.getUUID()), "boss_bars_enabled = false left the bar");
            BossesConfig.override("boss_bars_enabled", null);
            BossBars.update(m);
            m.discard();
            helper.assertTrue(!BossBars.has(m.getUUID()), "the bar outlived the boss");
        } finally {
            BossesConfig.override("boss_bars_enabled", null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandsSpawnReadDesiccateReturnAndForgetHim(GameTestHelper helper) {
        BossTestRig.fixedGround();
        LineTestPlayer op = LineTestPlayer.create(helper);
        op.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        try (BossTestRig rig = new BossTestRig(helper)) {
            var source = op.createCommandSourceStack().withPermission(2);
            var commands = rig.server.getCommands();
            commands.performPrefixedCommand(op.createCommandSourceStack().withPermission(0), "supernaturalbloodlines boss mikael spawn");
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "a player without permission 2 spawned him");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss mikael spawn");
            MikaelEntity m = (MikaelEntity) BossDirector.bossEntity(rig.server, BossId.MIKAEL);
            helper.assertTrue(m != null && m.isAlive(), "the spawn command made no Mikael");
            try {
                helper.assertTrue(commands.getDispatcher().execute("supernaturalbloodlines boss mikael spawn", source) == 0, "a second spawn was not refused");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException bad) {
                throw new AssertionError(bad);
            }
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(MikaelEntity.class, m.getBoundingBox().inflate(100)).size() == 1, "a second Mikael was made while the first is out");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss mikael desiccate");
            helper.assertTrue(m.isDesiccated(), "the desiccate command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss mikael return");
            helper.assertTrue(!m.isDesiccated() && rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "the return command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss mikael state");
            helper.assertTrue(op.received("supernaturalbloodlines.boss.cmd.state"), "the state command said nothing: " + op.heard());
            try {
                helper.assertTrue(commands.getDispatcher().execute("supernaturalbloodlines boss nobody spawn", source) == 0, "an unknown boss was not refused");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException bad) {
                throw new AssertionError(bad);
            }
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss mikael reset");
            helper.assertTrue(!m.isAlive() && rig.index.state(BossId.MIKAEL) == BossState.UNSEEN, "the reset left him or his record");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSpawnSpotIsOnTheGroundBetweenTwentyFourAndFortyBlocksAway(GameTestHelper helper) {
        BlockPos from = helper.absolutePos(new BlockPos(2, 1, 2));
        var spot = BossDirector.groundNear(helper.getLevel(), from, 24, 40);
        helper.assertTrue(spot.isEmpty() || (Math.sqrt(spot.get().distSqr(from)) >= 20 && Math.sqrt(spot.get().distSqr(from)) <= 45), "the spot is out of range: " + spot);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBossInAnUnloadedChunkIsNotLost(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.discard();
            rig.index.of(BossId.MIKAEL).pos = new BlockPos(2_000_000, 64, 2_000_000);
            for (int i = 0; i < 4; i++) {
                BossDirector.check(rig.server);
            }
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "a boss whose chunk is not loaded was written off: " + rig.index.state(BossId.MIKAEL));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBarIsShownWithinSixtyFourBlocksAndTakenAwayBeyond(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            p.setPos(m.getX() + 10, m.getY(), m.getZ());
            BossBars.update(m, List.of(p));
            helper.assertTrue(BossBars.viewers(m) == 1, "a player 10 blocks away does not see the bar: " + BossBars.viewers(m));
            p.setPos(m.getX() + 100, m.getY(), m.getZ());
            BossBars.update(m, List.of(p));
            helper.assertTrue(BossBars.viewers(m) == 0, "a player 100 blocks away still sees the bar: " + BossBars.viewers(m));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heIsTheStrongOneTheAttributesSay(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            helper.assertTrue(m.getMaxHealth() == 160.0F && m.getHealth() == 160.0F, "his health is " + m.getHealth() + " of " + m.getMaxHealth());
            helper.assertTrue(m.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 8.0, "his attack is " + m.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heFeedsOnAVampireHeHitsAndNotOnAMortal(GameTestHelper helper) {
        ServerPlayer vampire = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.createVulnerable(helper);
        ServerPlayer mortal = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.createVulnerable(helper);
        net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(vampire, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE.toBuilder()
                .species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.setHealth(100.0F);
            float before = vampire.getHealth();
            m.doHurtTarget(vampire);
            float lost = before - vampire.getHealth();
            helper.assertTrue(lost > 0, "control: the blow did no damage");
            helper.assertTrue(Math.abs(m.getHealth() - (100.0F + lost * 0.25F)) < 0.01F, "he healed " + (m.getHealth() - 100.0F) + " for " + lost + " damage");
            m.setHealth(100.0F);
            m.doHurtTarget(mortal);
            helper.assertTrue(m.getHealth() == 100.0F, "he fed on a mortal: " + m.getHealth());
        } finally {
            net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(vampire, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE);
        }
        helper.succeed();
    }
}
