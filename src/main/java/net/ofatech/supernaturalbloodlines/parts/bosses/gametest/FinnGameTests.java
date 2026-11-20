package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.FinnRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Finn (plan 17e, R175 and R176): when he comes, the ritual that hurts nobody, what breaks it off, and the grimoire he pays. */
@PrefixGameTestTemplate(false)
public final class FinnGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String UNKNOWN = "supernaturalbloodlines.boss.notice.finn.unknown";
    private static final String KNOWN = "supernaturalbloodlines.boss.notice.finn.known";
    private static final String DONE = "supernaturalbloodlines.boss.finn.done";
    private static final String BROKEN = "supernaturalbloodlines.boss.finn.broken";
    private static final long DAY = 24000L * 20;

    private FinnGameTests() {
    }

    private static LineTestPlayer witch(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5)));
        return p;
    }

    private static void world(GameTestHelper helper, boolean eclipse, LineTestPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMods(b -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useEclipse(s -> eclipse);
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
        helper.getLevel().setDayTime(DAY + 15000);
    }

    private static void forget(LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static FinnEntity finnOf(BossTestRig rig) {
        return BossDirector.bossEntity(rig.server, BossId.FINN) instanceof FinnEntity f ? f : null;
    }

    // ---- when he comes ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heComesUnderAnEclipseToAWitchTypeWithTheRitualRunningAndWithoutHisName(GameTestHelper helper) {
        LineTestPlayer p = witch(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, true, p);
            BossDirector.check(rig.server);
            FinnEntity f = finnOf(rig);
            helper.assertTrue(f != null && f.isAlive() && f.ritualing(), "he did not come and begin the ritual");
            helper.assertTrue(rig.index.state(BossId.FINN) == BossState.ACTIVE, "the index says " + rig.index.state(BossId.FINN));
            helper.assertTrue(p.received(UNKNOWN) && !p.received(KNOWN), "the notice named him: " + p.heard());
        } finally {
            forget(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStaysAwayWithoutAnEclipseWithoutAWitchTypeOrAVampireAndForAHumanSpectatorOrASwitchedOffBoss(GameTestHelper helper) {
        LineTestPlayer human = LineTestPlayer.create(helper);
        LineTestPlayer p = witch(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, false, p);
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he came with no eclipse");
            world(helper, true, human);
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he came for a human");
            world(helper, true, p);
            BossesConfig.override("finn_enabled", false);
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he came with his switch off");
            BossesConfig.override("finn_enabled", null);
            BossesConfig.override("finn_chance_per_eclipse", 0.0);
            BossDirector.useRoll(() -> 0.99);
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he came on a failed roll");
            helper.assertTrue(rig.index.of(BossId.FINN).lastRollDay == DesiccationRules.dayOf(DAY + 15000), "the roll of the night was not written down");
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he rolled twice in one night");
        } finally {
            BossesConfig.override("finn_enabled", null);
            BossesConfig.override("finn_chance_per_eclipse", null);
            forget(human, p);
        }
        helper.succeed();
    }

    // ---- the ritual ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRitualLeftAloneHurtsNobodyTellsThePeopleNearAndHeIsBackOnlyAfterAWeek(GameTestHelper helper) {
        LineTestPlayer p = witch(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, true, p);
            BossDirector.check(rig.server);
            FinnEntity f = finnOf(rig);
            helper.assertTrue(f != null, "he did not come");
            float health = p.getHealth();
            f.setNoAi(true);
            f.setInvulnerable(true);
            f.ritualTick(FinnRules.totalTicks(BossesConfig.finnRitualSeconds()) - 20);
            helper.assertTrue(f.ritualing() && !f.isRemoved(), "he finished early");
            f.ritualTick(40);
            helper.assertTrue(f.isRemoved() && rig.index.state(BossId.FINN) == BossState.LOST, "he did not withdraw: " + rig.index.state(BossId.FINN));
            helper.assertTrue(p.received(DONE) && p.getHealth() == health, "nobody was told, or somebody was hurt: " + p.heard());
            long today = DesiccationRules.dayOf(DAY + 15000);
            helper.assertTrue(rig.index.of(BossId.FINN).lastRollDay == today + FinnRules.COOLDOWN_DAYS - 1, "the next roll is not a week away: " + rig.index.of(BossId.FINN).lastRollDay);
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he came back the same night");
            helper.getLevel().setDayTime(DAY + 15000 + 24000L * (FinnRules.COOLDOWN_DAYS - 1));
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) == null, "he came back one day early");
            helper.getLevel().setDayTime(DAY + 15000 + 24000L * FinnRules.COOLDOWN_DAYS);
            BossDirector.check(rig.server);
            helper.assertTrue(finnOf(rig) != null, "he did not come back after the week");
        } finally {
            forget(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void harmingHimALotOrPullingHimFromTheRingBreaksTheRitualOffAndHeFights(GameTestHelper helper) {
        LineTestPlayer p = witch(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, true, p);
            BossDirector.check(rig.server);
            FinnEntity f = finnOf(rig);
            f.setNoAi(true);
            float max = f.getMaxHealth();
            f.invulnerableTime = 0;
            f.hurt(p.damageSources().playerAttack(p), max * 0.10F);
            helper.assertTrue(f.ritualing(), "a tenth of his health broke the ritual off");
            f.invulnerableTime = 0;
            f.hurt(p.damageSources().playerAttack(p), max * 0.15F);
            helper.assertTrue(!f.ritualing() && f.fighting() && !f.isRemoved(), "a quarter of his health did not break it off");
            helper.assertTrue(p.received(BROKEN), "the people near were not told: " + p.heard());
            helper.assertTrue(rig.index.state(BossId.FINN) == BossState.ACTIVE, "the fight is not an open boss: " + rig.index.state(BossId.FINN));
        } finally {
            forget(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void beingPulledMoreThanTwentyFourBlocksFromTheRingBreaksTheRitualOff(GameTestHelper helper) {
        LineTestPlayer p = witch(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, true, p);
            BossDirector.check(rig.server);
            FinnEntity f = finnOf(rig);
            f.setNoAi(true);
            f.setPos(f.getX() + FinnRules.LEASH - 2, f.getY(), f.getZ());
            f.ritualTick(10);
            helper.assertTrue(f.ritualing(), "he broke it off inside the leash");
            f.setPos(f.getX() + 4, f.getY(), f.getZ());
            f.ritualTick(10);
            helper.assertTrue(!f.ritualing() && f.fighting(), "he did not break it off beyond the leash");
        } finally {
            forget(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEclipseThatEndsBeforeHeIsDoneSendsHimAwayWithNothingDone(GameTestHelper helper) {
        LineTestPlayer p = witch(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, true, p);
            BossDirector.check(rig.server);
            FinnEntity f = finnOf(rig);
            f.setNoAi(true);
            BossDirector.useEclipse(s -> false);
            f.ritualTick(20);
            helper.assertTrue(f.isRemoved() && rig.index.state(BossId.FINN) == BossState.LOST, "he stayed after the eclipse ended");
            helper.assertTrue(!p.received(DONE), "he said the ritual was done when it was not");
            helper.assertTrue(rig.index.of(BossId.FINN).lastRollDay == DesiccationRules.dayOf(DAY + 15000), "an unfinished ritual earned a week's rest");
        } finally {
            forget(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRitualSurvivesASaveAndHeTargetsNobodyDuringIt(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            FinnEntity f = rig.spawnFinn();
            helper.assertTrue(f.ritualing() && f.centre() != null, "control: he is not in a ritual");
            f.ritualTick(100);
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            f.saveWithoutId(tag);
            FinnEntity again = FinnEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.ritualing() && again.ritualTicks() == f.ritualTicks() && again.centre().equals(f.centre()), "the ritual was lost through a save");
            helper.assertTrue(f.getTarget() == null, "he went after somebody during the ritual");
        }
        helper.succeed();
    }

    // ---- the reward and the line ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hePaysEstherGrimoireOnceWhenBroughtDown(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            FinnEntity f = rig.spawnFinn();
            f.desiccate();
            helper.assertTrue(rig.dropsNear(f, BossItems.ESTHER_GRIMOIRE.get()) == 1, "he did not pay the grimoire: " + rig.dropsNear(f, BossItems.ESTHER_GRIMOIRE.get()));
            f.recover();
            f.desiccate();
            helper.assertTrue(rig.dropsNear(f, BossItems.ESTHER_GRIMOIRE.get()) == 1, "he paid a second grimoire");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandsSpawnShowDesiccateReturnAndForgetFinn(GameTestHelper helper) {
        BossTestRig.fixedGround();
        LineTestPlayer op = LineTestPlayer.create(helper);
        op.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        try (BossTestRig rig = new BossTestRig(helper)) {
            var source = op.createCommandSourceStack().withPermission(2);
            var commands = rig.server.getCommands();
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss finn spawn");
            FinnEntity f = finnOf(rig);
            helper.assertTrue(f != null && f.isAlive(), "the spawn command made no Finn");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss finn desiccate");
            helper.assertTrue(f.isDesiccated(), "the desiccate command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss finn return");
            helper.assertTrue(!f.isDesiccated(), "the return command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss finn reset");
            helper.assertTrue(rig.index.state(BossId.FINN) == BossState.UNSEEN, "the reset command did not forget him");
            LineIndex.get(rig.server).unmarkDead(BossId.FINN.npcOriginal().orElseThrow().uuid());
        }
        helper.succeed();
    }
}
