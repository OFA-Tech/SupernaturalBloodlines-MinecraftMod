package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.KolRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.BossLeapGoal;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Kol (plan 17e, R173 and R174): when he comes, his leap, his withdrawal, and Kol's Grimoire that pays its skill points once. */
@PrefixGameTestTemplate(false)
public final class KolGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String UNKNOWN = "supernaturalbloodlines.boss.notice.kol.unknown";
    private static final String KNOWN = "supernaturalbloodlines.boss.notice.kol.known";
    private static final String HINT = "supernaturalbloodlines.boss.notice.kol.hint";
    private static final long DAY = 24000L * 20;
    private static final long NIGHT = DAY + 15000;

    private KolGameTests() {
    }

    /** A hunter with {@code kills} on the mark near the middle of the test (or another species with them). */
    private static LineTestPlayer hunter(GameTestHelper helper, Species species, int kills) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(species).markKills(kills).build());
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5)));
        return p;
    }

    private static void world(GameTestHelper helper, long dayTime, LineTestPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMods(b -> true);
        BossDirector.useOnGround(p -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
        helper.getLevel().setDayTime(dayTime);
    }

    private static void forget(GameTestHelper helper, LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static KolEntity kolOf(BossTestRig rig) {
        return BossDirector.bossEntity(rig.server, BossId.KOL) instanceof KolEntity k ? k : null;
    }

    private static int minMark() {
        return BossesConfig.kolMinMark();
    }

    // ---- when he comes ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heComesByNightForAHunterWithEnoughKillsAndTellsThemWithoutHisName(GameTestHelper helper) {
        LineTestPlayer p = hunter(helper, Species.HUNTER_OF_THE_FIVE, minMark());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, NIGHT, p);
            BossDirector.check(rig.server);
            KolEntity k = kolOf(rig);
            helper.assertTrue(k != null && k.isAlive(), "he did not come for a hunter with the kills");
            helper.assertTrue(p.getUUID().equals(k.prey()), "he does not hunt them");
            helper.assertTrue(rig.index.state(BossId.KOL) == BossState.ACTIVE, "the index says " + rig.index.state(BossId.KOL));
            helper.assertTrue(p.received(UNKNOWN) && !p.received(KNOWN) && p.received(HINT), "the notice named him, or said nothing: " + p.heard());
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStaysAwayFromTheWrongPeopleTheWrongTimeAndAGoneNight(GameTestHelper helper) {
        LineTestPlayer few = hunter(helper, Species.HUNTER_OF_THE_FIVE, minMark() - 1);
        LineTestPlayer notHunter = hunter(helper, Species.WITCH, 20);
        LineTestPlayer enough = hunter(helper, Species.HUNTER_OF_THE_FIVE, minMark());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, NIGHT, few, notHunter);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) == null, "he came for a hunter one kill short, or for somebody who is not a hunter");
            world(helper, DAY + 6000, enough);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) == null, "he came by day");
            world(helper, NIGHT, enough);
            BossDirector.useOnGround(p -> false);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) == null, "he came for a hunter who was in the air");
            BossDirector.useOnGround(p -> true);
            BossesConfig.override("kol_enabled", false);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) == null, "he came with his switch off");
            BossesConfig.override("kol_enabled", null);
            BossDirector.check(rig.server);
            KolEntity k = kolOf(rig);
            helper.assertTrue(k != null, "he did not come for the hunter with the kills");
            k.withdraw();
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) == null, "he came back the same night he withdrew");
            helper.getLevel().setDayTime(NIGHT + 24000);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) != null, "he did not come back the next night");
        } finally {
            BossesConfig.override("kol_enabled", null);
            forget(helper, few, notHunter, enough);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMinimumMarkIsTheSettingNotANumberInTheCode(GameTestHelper helper) {
        LineTestPlayer p = hunter(helper, Species.HUNTER_OF_THE_FIVE, 3);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, NIGHT, p);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) == null, "control: he came for 3 kills with the default setting");
            BossesConfig.override("kol_min_mark", 3);
            BossDirector.check(rig.server);
            helper.assertTrue(kolOf(rig) != null, "he did not come for 3 kills when the setting says 3");
        } finally {
            BossesConfig.override("kol_min_mark", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- his leap and his hunt ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heLeapsAtATargetFromAFewBlocksAndThenPauses(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KolEntity k = rig.spawnKol();
            k.setNoAi(true);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(8, 1, 2));
            k.setTarget(cow);
            k.setOnGround(true);
            BossLeapGoal goal = new BossLeapGoal(k);
            helper.assertTrue(goal.canUse(), "he does not leap at a target six blocks away: dist2=" + k.distanceToSqr(cow) + " ground=" + k.onGround() + " pause=" + k.leapPause() + " sight=" + k.hasLineOfSight(cow) + " alive=" + cow.isAlive());
            goal.start();
            helper.assertTrue(k.getDeltaMovement().y > 0.3 && k.getDeltaMovement().x > 0.3, "the leap goes nowhere: " + k.getDeltaMovement());
            helper.assertTrue(k.leapPause() == KolRules.LEAP_PAUSE && !goal.canUse(), "he leaps again at once");
            cow.setPos(k.getX() + 1.5, k.getY(), k.getZ());
            for (int i = 0; i < 20; i++) {
                k.tickCount += 10;
            }
            helper.assertTrue(k.getTarget() == cow, "control: the target is gone");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heWithdrawsWhenNobodyIsLeftToHuntAndStaysWhileThePreyIsHere(GameTestHelper helper) {
        LineTestPlayer prey = hunter(helper, Species.HUNTER_OF_THE_FIVE, minMark());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, NIGHT, prey);
            KolEntity k = rig.spawnKol();
            k.setNoAi(true);
            k.setInvulnerable(true); // thousands of ticks in a test structure: he must not suffocate in its floor
            k.hunt(prey.getUUID());
            for (int i = 0; i < KolRules.WITHDRAW_AFTER + 40; i++) {
                k.tickCount++;
                k.tick();
            }
            helper.assertTrue(!k.isRemoved(), "he gave up on a prey who is right there");
            k.hunt(UUID.randomUUID());
            for (int i = 0; i < KolRules.WITHDRAW_AFTER + 40 && !k.isRemoved(); i++) {
                k.tickCount++;
                k.tick();
            }
            helper.assertTrue(k.isRemoved() && rig.index.state(BossId.KOL) == BossState.LOST, "he did not withdraw: " + rig.index.state(BossId.KOL));
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whenHeLosesTheTrailTheOneHeHuntedIsToldAndHeMayComeAgain(GameTestHelper helper) {
        LineTestPlayer prey = hunter(helper, Species.HUNTER_OF_THE_FIVE, minMark());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, NIGHT, prey);
            KolEntity k = rig.spawnKol();
            k.hunt(prey.getUUID());
            k.withdraw();
            helper.assertTrue(prey.received("supernaturalbloodlines.boss.kol.gone"), "the hunted one was not told: " + prey.heard());
            helper.assertTrue(rig.index.state(BossId.KOL) == BossState.LOST && k.isRemoved(), "he did not leave");
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisPreyAndHisPauseSurviveASave(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KolEntity k = rig.spawnKol();
            UUID prey = UUID.randomUUID();
            k.hunt(prey);
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            k.saveWithoutId(tag);
            KolEntity again = KolEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(prey.equals(again.prey()), "his prey was forgotten");
        }
        helper.succeed();
    }

    // ---- the grimoire ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hePaysHisGrimoireOnceWhenBroughtDown(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KolEntity k = rig.spawnKol();
            k.desiccate();
            helper.assertTrue(rig.dropsNear(k, BossItems.KOL_GRIMOIRE.get()) == 1, "he did not pay his grimoire: " + rig.dropsNear(k, BossItems.KOL_GRIMOIRE.get()));
            k.recover();
            k.desiccate();
            helper.assertTrue(rig.dropsNear(k, BossItems.KOL_GRIMOIRE.get()) == 1, "he paid a second grimoire");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void readingTheGrimoirePaysItsSkillPointsOncePerGrimoireIntoTheSharedPool(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        boolean tracked = SupernaturalAccess.tracksFakePlayers();
        SupernaturalAccess.trackFakePlayers(true);
        try {
            int before = SupernaturalAccess.get(p).sp();
            ItemStack grimoire = new ItemStack(BossItems.KOL_GRIMOIRE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, grimoire);
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.bosses.item.KolGrimoireItem.isRead(grimoire), "control: a new grimoire is read");
            BossesConfig.override("kol_grimoire_sp", 7);
            grimoire.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(SupernaturalAccess.get(p).sp() == before + 7, "the points are not the setting: " + (SupernaturalAccess.get(p).sp() - before));
            helper.assertTrue(SupernaturalAccess.get(p).spEarned() >= 7, "the points are not earned points");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.bosses.item.KolGrimoireItem.isRead(p.getItemInHand(InteractionHand.MAIN_HAND)), "it is not marked read");
            helper.assertTrue(p.received("item.supernaturalbloodlines.kol_grimoire.read"), "the reader was not told: " + p.heard());
            grimoire.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(SupernaturalAccess.get(p).sp() == before + 7, "a second reading paid again");
            helper.assertTrue(p.received("item.supernaturalbloodlines.kol_grimoire.already"), "the second reading was not answered: " + p.heard());
            ItemStack other = new ItemStack(BossItems.KOL_GRIMOIRE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, other);
            other.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(SupernaturalAccess.get(p).sp() == before + 14, "another grimoire did not pay for itself");
        } finally {
            BossesConfig.override("kol_grimoire_sp", null);
            SupernaturalAccess.trackFakePlayers(tracked);
        }
        helper.succeed();
    }

    // ---- the line and the operator ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobHitWithWhiteOakIsNeverAWhiteOakKillForKolOrFinn(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KolEntity kol = rig.spawnKol();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity finn = rig.spawnFinn();
            for (net.minecraft.world.entity.LivingEntity boss : new net.minecraft.world.entity.LivingEntity[] {kol, finn}) {
                boss.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak.STAKE.get()));
                helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules.isWhiteOakKill(boss.damageSources().mobAttack(boss)),
                        boss.getType() + " with a stake in hand made a white oak kill");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandsSpawnShowDesiccateReturnAndForgetKol(GameTestHelper helper) {
        BossTestRig.fixedGround();
        LineTestPlayer op = LineTestPlayer.create(helper);
        op.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        try (BossTestRig rig = new BossTestRig(helper)) {
            var source = op.createCommandSourceStack().withPermission(2);
            var commands = rig.server.getCommands();
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss kol spawn");
            KolEntity k = kolOf(rig);
            helper.assertTrue(k != null && k.isAlive(), "the spawn command made no Kol");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss kol desiccate");
            helper.assertTrue(k.isDesiccated(), "the desiccate command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss kol return");
            helper.assertTrue(!k.isDesiccated() && rig.index.state(BossId.KOL) == BossState.ACTIVE, "the return command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss kol reset");
            helper.assertTrue(rig.index.state(BossId.KOL) == BossState.UNSEEN, "the reset command did not forget him: " + rig.index.state(BossId.KOL));
        }
        helper.succeed();
    }
}
