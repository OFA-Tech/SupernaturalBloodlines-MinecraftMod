package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.DemonRoarRules;
import net.ofatech.supernaturalbloodlines.core.boss.KlausRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEvents;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.DemonRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * Deucalion (plan 17f): who calls him, his pack, the shift and the glasses, what his death pays (the sparks by the ladder of a stolen spark, the Roar), and the
 * Roar itself. The moon, the mods and the ground are pinned. Werewolves only: without it every test passes without running.
 */
@PrefixGameTestTemplate(false)
public final class DeucalionGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NOTICE = "supernaturalbloodlines.boss.notice.deucalion";
    private static final String DAWN = "supernaturalbloodlines.boss.deucalion.dawn";
    private static final long NIGHT = 24000L * 20 + 15000;
    private static final long MORNING = 24000L * 20 + 6000;

    private DeucalionGameTests() {
    }

    private static boolean werewolves() {
        return ModList.get().isLoaded("werewolves");
    }

    /** A werewolf (recording what it is told) of this rank with this many stacked sparks, standing in the test. {@code protect} is the True Alpha path protection. */
    private static LineTestPlayer wolf(GameTestHelper helper, Rank rank, int sparks, boolean protect, double x) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 2.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).protectPath(protect).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 5), "the test wolf could not join the werewolves");
        PackAccess.setSparks(p, sparks);
        return p;
    }

    private static void clean(LineTestPlayer... ps) {
        SireCall.overridePlayerLookup(null);
        AlphaRoar.overridePlayers(null);
        AlphaRoar.reset();
        for (LineTestPlayer p : ps) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(java.util.Set.of()).build());
        }
    }

    /** A full moon night, the mods there, the dice passing, the ground fixed, and these players the only ones in the overworld. */
    private static void night(ServerPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMoon(l -> true);
        BossDirector.useMods(b -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useVillagers(l -> List.of());
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        AlphaRoar.overridePlayers(s -> List.of(ps));
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
    }

    private static DeucalionEntity deucalionOf(BossTestRig rig) {
        return BossDirector.anyBossBody(rig.server, BossId.DEUCALION) instanceof DeucalionEntity d ? d : null;
    }

    /** Game ticks for an entity the level is not ticking: its own count goes up one a tick, as the level does. */
    private static void run(DeucalionEntity d, int ticks) {
        for (int i = 0; i < ticks && !d.isRemoved(); i++) {
            d.tickCount++;
            d.tick();
        }
    }

    private static void kill(DeucalionEntity d, ServerPlayer killer) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(d, killer.damageSources().playerAttack(killer)));
    }

    private static int stack(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf().sparks();
    }

    // ---- the director ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heComesForAnAlphaWithEnoughStackedSparksAndNotForOneWithFewer(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 2, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came for an Alpha with 2 sparks");
            helper.assertTrue(rig.index.of(BossId.DEUCALION).lastRollDay == -1, "the night was spent with nobody to come for");
            PackAccess.setSparks(p, 3);
            BossDirector.check(rig.server);
            DeucalionEntity d = deucalionOf(rig);
            helper.assertTrue(d != null && d.isAlive(), "he did not come for an Alpha with 3 sparks");
            helper.assertTrue(p.getUUID().equals(d.prey()) && d.form() == BossForm.HUMAN, "he does not hunt the Alpha, or came as a wolf");
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.ACTIVE && rig.index.of(BossId.DEUCALION).appearances == 1, "the index says " + rig.index.state(BossId.DEUCALION));
            helper.assertTrue(p.count(NOTICE) == 2 && p.received(NOTICE + ".hint"), "the hunted Alpha was not told, on the action bar and in chat, with the hint: " + p.heard());
            helper.assertTrue(p.notified().stream().anyMatch(s -> s.contains("wolf.howl") || s.contains("howl")), "the howl was not for the hunted one alone: " + p.notified());
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.DEUCALION).appearances == 1, "he came twice");
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMinimumIsASettingAndOnlyAWerewolfAlphaCounts(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer beta = wolf(helper, Rank.BETA, 5, true, 2.5);
        LineTestPlayer alpha = wolf(helper, Rank.ALPHA, 4, true, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(beta);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came for a Beta with 5 sparks");
            night(alpha);
            BossesConfig.override("deucalion_min_sparks", 5);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came for 4 sparks with the minimum at 5");
            BossesConfig.override("deucalion_min_sparks", 4);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) != null, "he did not come for 4 sparks with the minimum at 4");
        } finally {
            BossesConfig.override("deucalion_min_sparks", null);
            clean(beta, alpha);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStaysAwayWithoutAFullMoonWithoutWerewolvesOnceSwitchedOffAndInPeace(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 4, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.useMoon(l -> false);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came without a full moon");
            BossDirector.useMoon(l -> true);
            BossDirector.useMods(b -> b != BossId.DEUCALION);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came without Werewolves");
            BossDirector.useMods(b -> true);
            BossesConfig.override("deucalion_enabled", false);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came with deucalion_enabled off");
            BossesConfig.override("deucalion_enabled", null);
            BossDirector.useDifficulty(l -> net.minecraft.world.Difficulty.PEACEFUL);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he came in a peaceful world");
            BossDirector.useDifficulty(null);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) != null, "control: nothing was wrong and he did not come");
        } finally {
            BossesConfig.override("deucalion_enabled", null);
            clean(p);
        }
        helper.succeed();
    }

    // ---- his pack ----

    private static List<Mob> packNear(GameTestHelper helper, DeucalionEntity d) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, d.getBoundingBox().inflate(40), DeucalionEvents::isBossPack);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisPackIsTheSettingsNumberOfMarkedAlphasAndTheyGoWithHim(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 3, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossesConfig.override("deucalion_pack_alphas", 2);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), p, null).orElseThrow();
            helper.assertTrue(d.pack().size() == 2 && packNear(helper, d).size() == 2, "his pack is " + d.pack().size() + " and " + packNear(helper, d).size() + " are in the world, not 2");
            for (Mob m : packNear(helper, d)) {
                helper.assertTrue(m.getData(SpeciesRegistries.NPC_MARK).kind() == NpcMark.Kind.BOSS_PACK && d.pack().contains(m.getUUID()), "a pack Alpha is not marked as his");
                helper.assertTrue(m.getTarget() == p, "a pack Alpha does not hunt the one he hunts");
            }
            d.dismissPack();
            helper.assertTrue(packNear(helper, d).isEmpty() && d.pack().isEmpty(), "his pack did not go when he sent it away");
            BossesConfig.override("deucalion_pack_alphas", 0);
            rig.index.forget(BossId.DEUCALION);
            d.discard();
            DeucalionEntity none = BossDirector.spawnDeucalion(helper.getLevel(), p, null).orElseThrow();
            helper.assertTrue(none.pack().isEmpty(), "a pack of 0 came with him");
        } finally {
            BossesConfig.override("deucalion_pack_alphas", null);
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackAlphaGivesNoWeakSparkAndNoDropsAndAWildOneDoes(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer killer = wolf(helper, Rank.OMEGA, 0, false, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(killer);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), killer, null).orElseThrow();
            Mob member = packNear(helper, d).get(0);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(member, killer.damageSources().playerAttack(killer)));
            helper.assertTrue(PackAccess.wolf(killer).weakSparks() == 0, "the killer of a pack Alpha got a weak spark");
            member.setData(SpeciesRegistries.NPC_MARK, NpcMark.wild(1, 1));
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(member, killer.damageSources().playerAttack(killer)));
            helper.assertTrue(PackAccess.wolf(killer).weakSparks() == 1, "control: the kill of a wild Alpha gave no weak spark (the setting or the faction is off): " + PackAccess.wolf(killer));
            Mob other = packNear(helper, d).get(0);
            ItemEntity drop = new ItemEntity(helper.getLevel(), other.getX(), other.getY(), other.getZ(), new ItemStack(net.minecraft.world.item.Items.BONE));
            var event = new LivingDropsEvent(other, killer.damageSources().playerAttack(killer), List.of(drop), false);
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "a pack Alpha dropped something");
            other.setData(SpeciesRegistries.NPC_MARK, NpcMark.wild(1, 1));
            var wild = new LivingDropsEvent(other, killer.damageSources().playerAttack(killer), List.of(drop), false);
            NeoForge.EVENT_BUS.post(wild);
            helper.assertTrue(!wild.isCanceled(), "control: a wild Alpha's drops were cancelled");
        } finally {
            clean(killer);
        }
        helper.succeed();
    }

    // ---- the shift and the glasses ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heDropsToFoursBelowThreeQuartersHidesTheGlassesAtTheFirstTickAndStaysAWolf(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            helper.assertTrue(d.form() == BossForm.HUMAN && d.hatVisible() && d.skin().equals("deucalion_human"), "the man has no glasses, or the wrong skin");
            d.setHealth(d.getMaxHealth() * 0.76F);
            run(d, 20);
            helper.assertTrue(d.form() == BossForm.HUMAN, "he shifted above three quarters");
            d.setHealth(d.getMaxHealth() * 0.74F);
            run(d, 10);
            helper.assertTrue(d.form() == BossForm.SHIFTING && !d.hatVisible(), "he did not start to shift below three quarters, or kept his glasses: " + d.form());
            helper.assertTrue(d.getBbWidth() < 0.7F, "the hitbox changed before the swap");
            run(d, ShiftRules.SWAP_TICK);
            helper.assertTrue(d.getBbWidth() > 1.0F, "the hitbox is not the wolf's at the swap: " + d.getBbWidth());
            run(d, ShiftRules.END_TICK + 5);
            helper.assertTrue(d.form() == BossForm.BEAST && !d.hatVisible(), "he is not the wolf after the shift, or the glasses are back: " + d.form());
            d.setHealth(d.getMaxHealth());
            run(d, 40);
            helper.assertTrue(d.form() == BossForm.BEAST, "the wolf changed back");
            d.dismissPack();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heShiftsWhenHisTargetIsWithinEightBlocksAtNightAndNotByDayOrFarAway(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            p.setPos(d.position().add(5.0, 0.0, 0.0));
            d.setTarget(p);
            helper.getLevel().setDayTime(MORNING);
            run(d, 20);
            helper.assertTrue(d.form() == BossForm.HUMAN, "he shifted by day with the target close");
            helper.getLevel().setDayTime(NIGHT);
            p.setPos(d.position().add(9.0, 0.0, 0.0));
            run(d, 20);
            helper.assertTrue(d.form() == BossForm.HUMAN, "he shifted with the target 9 blocks away");
            p.setPos(d.position().add(7.0, 0.0, 0.0));
            run(d, 20);
            helper.assertTrue(d.form() == BossForm.SHIFTING, "he did not shift with the target 7 blocks away at night: " + d.form());
            d.dismissPack();
        } finally {
            p.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisFormShiftAndPackSurviveASave(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            d.pack().add(UUID.randomUUID());
            List<UUID> expected = new java.util.ArrayList<>(d.pack());
            d.startShift();
            run(d, ShiftRules.SWAP_TICK + 3);
            int ticks = d.shiftTicks();
            CompoundTag tag = new CompoundTag();
            d.saveWithoutId(tag);
            DeucalionEntity again = DeucalionEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.form() == BossForm.SHIFTING && again.shiftTicks() == ticks, "the shift was forgotten: " + again.form() + " " + again.shiftTicks());
            helper.assertTrue(again.pack().equals(expected) && !expected.isEmpty(), "the pack was forgotten: " + again.pack());
            d.dismissPack();
            helper.assertTrue(again.getBbWidth() > 1.0F, "the hitbox was not the wolf's after a load");
        }
        helper.succeed();
    }

    // ---- he withdraws ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void atDawnHeGoesWithHisPackAndTheOneHeHuntedIsToldAndHeMayComeAgain(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 3, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            helper.getLevel().setDayTime(NIGHT);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), p, null).orElseThrow();
            run(d, 40);
            helper.assertTrue(d.isAlive() && packNear(helper, d).size() == 3, "control: he left in the night, or the pack is " + packNear(helper, d).size());
            helper.getLevel().setDayTime(MORNING);
            run(d, 20);
            helper.assertTrue(d.isRemoved(), "he did not leave at dawn");
            helper.assertTrue(p.received(DAWN), "the hunted one was not told: " + p.heard());
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.LOST, "the index says " + rig.index.state(BossId.DEUCALION));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(2, 1, 2))).inflate(60), DeucalionEvents::isBossPack).isEmpty(),
                    "his pack stayed when he left");
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHuntWithoutAHitEndsAfterFourMinutesAndOneWithNobodyToHuntAfterOne(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            helper.getLevel().setDayTime(NIGHT);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            d.hunt(UUID.randomUUID()); // somebody who is not here
            run(d, KlausRules.WITHDRAW_AFTER + 100);
            helper.assertTrue(d.isRemoved() && rig.index.state(BossId.DEUCALION) == BossState.LOST, "he did not give up a hunt with nobody to hunt after a minute");
        }
        helper.succeed();
    }

    // ---- his death ----

    private static BossTestRig dead(GameTestHelper helper, ServerPlayer killer, LineTestPlayer... others) {
        BossTestRig rig = new BossTestRig(helper);
        night(killer);
        DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
        kill(d, killer);
        return rig;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anUnprotectedWerewolfWhoKillsHimTakesFourSparksAndLearnsTheRoarAndHeIsDeadForGood(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer killer = wolf(helper, Rank.OMEGA, 0, false, 2.5);
        try (BossTestRig rig = dead(helper, killer)) {
            helper.assertTrue(stack(killer) == 4 && BloodlineAccess.get(killer).wolf().rank() == Rank.ALPHA, "the killer has " + stack(killer) + " sparks: " + BloodlineAccess.get(killer).wolf());
            helper.assertTrue(SupernaturalAccess.get(killer).skills().contains(DemonRoarRules.SKILL), "the killer did not learn the Roar");
            helper.assertTrue(killer.received("supernaturalbloodlines.pack.spark_gained") && killer.received("supernaturalbloodlines.boss.deucalion.roar_given"), "the killer was not told: " + killer.heard());
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.DEAD && rig.index.of(BossId.DEUCALION).rewarded, "the index says " + rig.index.state(BossId.DEUCALION));
            helper.assertTrue(rig.index.of(BossId.DEUCALION).returnDay == BossClock.today(helper.getLevel()), "the day he died was not written down");
            helper.assertTrue(DeucalionEvents.payReward(helper.getLevel(), killer) == false && stack(killer) == 4, "the reward was paid twice");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(2, 1, 2))).inflate(60), DeucalionEvents::isBossPack).isEmpty(),
                    "his pack stayed after his death");
        } finally {
            clean(killer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlphaAddsHisSparksToTheirOwnAndTheCapHolds(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        int cap = SpeciesConfig.maxStackedSparks();
        LineTestPlayer one = wolf(helper, Rank.ALPHA, 1, false, 2.5);
        try (BossTestRig rig = dead(helper, one)) {
            helper.assertTrue(stack(one) == Math.min(cap, 5), "an Alpha with 1 spark has " + stack(one) + " after 4 more, with the cap at " + cap);
        } finally {
            clean(one);
        }
        LineTestPlayer high = wolf(helper, Rank.ALPHA, cap - 1, false, 2.5);
        try (BossTestRig rig = dead(helper, high)) {
            helper.assertTrue(stack(high) == cap, "the stack is " + stack(high) + ", not the cap " + cap);
            helper.assertTrue(high.received("supernaturalbloodlines.pack.spark_capped"), "the killer was not told the cap stopped some: " + high.heard());
        } finally {
            clean(high);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaTakesNothingAndAnOpenProtectedPathDestroysTheSparks(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer trueAlpha = wolf(helper, Rank.TRUE_ALPHA, 1, true, 2.5);
        try (BossTestRig rig = dead(helper, trueAlpha)) {
            helper.assertTrue(stack(trueAlpha) == 1 && BloodlineAccess.get(trueAlpha).wolf().rank() == Rank.TRUE_ALPHA, "a True Alpha took sparks: " + BloodlineAccess.get(trueAlpha).wolf());
            helper.assertTrue(SupernaturalAccess.get(trueAlpha).skills().contains(DemonRoarRules.SKILL), "a True Alpha is a werewolf and learns the Roar");
        } finally {
            clean(trueAlpha);
        }
        LineTestPlayer protectedOne = wolf(helper, Rank.OMEGA, 0, true, 2.5);
        int before = stack(protectedOne);
        try (BossTestRig rig = dead(helper, protectedOne)) {
            helper.assertTrue(stack(protectedOne) == before && BloodlineAccess.get(protectedOne).wolf().rank() == Rank.OMEGA, "an open protected path took the sparks: " + BloodlineAccess.get(protectedOne).wolf());
            helper.assertTrue(protectedOne.received("supernaturalbloodlines.pack.spark_destroyed"), "the killer was not told the sparks were destroyed: " + protectedOne.heard());
        } finally {
            clean(protectedOne);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillerWhoIsNoWerewolfGetsNeitherTheSparksNorTheRoarAndTheRewardIsSpent(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer plain = LineTestPlayer.create(helper);
        int before = stack(plain);
        try (BossTestRig rig = dead(helper, plain)) {
            helper.assertTrue(stack(plain) == before && !SupernaturalAccess.get(plain).skills().contains(DemonRoarRules.SKILL), "somebody who is no werewolf got something");
            helper.assertTrue(rig.index.of(BossId.DEUCALION).rewarded && rig.index.state(BossId.DEUCALION) == BossState.DEAD, "the reward was kept for later");
        } finally {
            clean(plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobsKillTakesNothingToNobodyAndTheLineOfKlausIsNotTouchedByAWhiteOakDeath(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer wolf = wolf(helper, Rank.ALPHA, 1, false, 2.5);
        LineTestPlayer child = LineTestPlayer.create(helper);
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(child.getUUID(), BossTestRig.KLAUS, true);
            night(wolf, child);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(d, helper.getLevel().damageSources().mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5)))));
            helper.assertTrue(stack(wolf) == 1, "a zombie's kill gave sparks to somebody");
            helper.assertTrue(rig.index.of(BossId.DEUCALION).rewarded && rig.index.state(BossId.DEUCALION) == BossState.DEAD, "the reward was kept for a mob's kill");
            helper.assertTrue(!child.isDeadOrDying(), "a mob's kill of Deucalion killed a line");
        } finally {
            LineIndex.get(helper.getLevel().getServer()).remove(child.getUUID());
            BloodlineAccess.set(child, BloodlineState.NONE);
            clean(wolf);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWhiteOakDeathOfDeucalionKillsNoLineAndHisHitNeverCountsAsWhiteOak(GameTestHelper helper) {
        LineTestPlayer original = LineTestPlayer.create(helper);
        LineTestPlayer child = LineTestPlayer.create(helper);
        BloodlineAccess.set(original, BloodlineAccess.get(original).toBuilder().species(Species.ORIGINAL).build());
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder.record(original, child);
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            d.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.ofatech.supernaturalbloodlines.parts.bosses.BossItems.MIKAELS_STAKE.get()));
            BossesConfig.override("mikael_stakes_originals", true);
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules.isWhiteOakKill(d.damageSources().mobAttack(d)), "Deucalion's hit with a stake counts as a white oak kill");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(original, d.damageSources().mobAttack(d)));
            helper.assertTrue(!child.isDeadOrDying(), "Deucalion's hit killed an Original's line");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(d, rig.whiteOakBlow()));
            helper.assertTrue(!child.isDeadOrDying() && rig.index.state(BossId.DEUCALION) == BossState.DEAD, "his white oak death touched a line, or the index says " + rig.index.state(BossId.DEUCALION));
        } finally {
            BossesConfig.override("mikael_stakes_originals", null);
            LineIndex.get(helper.getLevel().getServer()).remove(child.getUUID());
            LineIndex.get(helper.getLevel().getServer()).remove(original.getUUID());
            BloodlineAccess.set(original, BloodlineState.NONE);
            BloodlineAccess.set(child, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- the operator and the hint ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyAPackEmissarysMountainAshLineHoldsDeucalionAPlainOneDoesNot(GameTestHelper helper) {
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "mountain_ash_line"))) {
            helper.succeed(); // the artifacts part is off: there is no line to test
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            BlockPos at = new BlockPos(6, 1, 6);
            helper.setBlock(at, net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh.LINE.get().defaultBlockState());
            BlockPos abs = helper.absolutePos(at);
            var level = helper.getLevel();
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(d, level, abs), "a plain line holds Deucalion: the hint says an Emissary's line does");
            ((net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlockEntity) level.getBlockEntity(abs)).setEmissary(true);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(d, level, abs), "an Emissary's line does not hold Deucalion, but his hint says it does");
            d.dismissPack();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deucalionIsExemptFromLinesLikeTheOtherBossesAndHisBiteStartsNone(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            helper.assertTrue(d.getType().is(net.ofatech.supernaturalbloodlines.parts.species.line.NpcLines.EXEMPT), "Deucalion is not in #line_exempt");
            d.dismissPack();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandSpawnsDeucalionWithHisPackAndResetsHimAndItRefusesWithoutWerewolves(GameTestHelper helper) {
        BossTestRig.fixedGround();
        LineTestPlayer op = LineTestPlayer.create(helper);
        op.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        try (BossTestRig rig = new BossTestRig(helper)) {
            var source = op.createCommandSourceStack().withPermission(2);
            var commands = rig.server.getCommands();
            BossDirector.useMods(b -> false);
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss deucalion spawn");
            helper.assertTrue(deucalionOf(rig) == null, "the command spawned him without Werewolves");
            BossDirector.useMods(b -> true);
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss deucalion spawn");
            DeucalionEntity d = deucalionOf(rig);
            helper.assertTrue(d != null && rig.index.state(BossId.DEUCALION) == BossState.ACTIVE, "the command did not spawn him");
            helper.assertTrue(!werewolves() || d.pack().size() == BossesConfig.deucalionPackAlphas(), "the command's Deucalion came with " + d.pack().size() + " Alphas");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss deucalion reset");
            helper.assertTrue(deucalionOf(rig) == null || deucalionOf(rig).isRemoved(), "the reset left him in the world");
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.UNSEEN, "the reset left him in the index: " + rig.index.state(BossId.DEUCALION));
            helper.assertTrue(d.pack().isEmpty() && packNear(helper, d).isEmpty(), "the reset left his pack in the world");
        } finally {
            op.discard();
        }
        helper.succeed();
    }

    // ---- the respawn ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heComesAgainOnlyWithTheSwitchOnAndEnoughDaysAndPaysAgain(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            BossDirector.useWorld(l -> List.of(), null);
            rig.day(10);
            rig.index.apply(BossId.DEUCALION, net.ofatech.supernaturalbloodlines.core.boss.BossRules.Event.SPAWNED);
            rig.index.apply(BossId.DEUCALION, net.ofatech.supernaturalbloodlines.core.boss.BossRules.Event.KILLED);
            rig.index.of(BossId.DEUCALION).returnDay = 10;
            rig.index.of(BossId.DEUCALION).rewarded = true;
            rig.day(30);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.DEAD, "he came back with the switch off");
            BossesConfig.override("deucalion_respawn", true);
            BossesConfig.override("deucalion_respawn_days", 21);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.DEAD, "he came back after 20 days with 21 asked");
            BossesConfig.override("deucalion_respawn_days", 20);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.DEUCALION) == BossState.UNSEEN && !rig.index.of(BossId.DEUCALION).rewarded, "he did not come back after 20 days: " + rig.index.state(BossId.DEUCALION));
        } finally {
            BossesConfig.override("deucalion_respawn", null);
            BossesConfig.override("deucalion_respawn_days", null);
        }
        helper.succeed();
    }

    // ---- the Demon Wolf Roar ----

    private static void give(LineTestPlayer p) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(java.util.Set.of(DemonRoarRules.SKILL)).build());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarPicksAnotherAlphaNearAndNotABetaTheRoarersPackAFarOneOrOneWhoIsNoWerewolf(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer roarer = wolf(helper, Rank.ALPHA, 4, false, 2.5);
        LineTestPlayer rival = wolf(helper, Rank.ALPHA, 1, false, 6.5);
        LineTestPlayer beta = wolf(helper, Rank.BETA, 0, false, 5.5);
        LineTestPlayer far = wolf(helper, Rank.ALPHA, 1, false, 2.5);
        LineTestPlayer mate = wolf(helper, Rank.ALPHA, 1, false, 4.5);
        try {
            far.setPos(roarer.position().add(DemonRoarRules.RANGE + 1, 0, 0));
            night(roarer, rival, beta, far, mate);
            helper.assertTrue(PackCommand.invite(roarer, mate) == null
                    && PackCommand.accept(mate, PackCommand.requests().tokenOf(roarer.getUUID(), mate.getUUID()).orElseThrow()) == null, "the other Alpha could not join the roarer's pack");
            helper.assertTrue(PackIndex.get(helper.getLevel()).pack(roarer.getUUID()).contains(mate.getUUID()), "control: the other Alpha is not in the pack");
            var picked = DemonRoar.targets(roarer, List.of(roarer, rival, beta, far, mate));
            helper.assertTrue(picked.equals(List.of(rival)), "the Roar picks " + picked.size() + " players, not the one rival Alpha");
        } finally {
            PackIndex.get(helper.getLevel()).leave(mate.getUUID());
            PackIndex.get(helper.getLevel()).leave(roarer.getUUID());
            clean(roarer, rival, beta, far, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarWorksOnTheAlphaRoarsKeyOnlyForWhoHoldsItWithItsOwnCooldown(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer roarer = wolf(helper, Rank.ALPHA, 4, false, 2.5);
        LineTestPlayer rival = wolf(helper, Rank.ALPHA, 1, false, 6.5);
        try {
            night(roarer, rival);
            helper.assertTrue(!LadderQuery.owns(roarer, DemonRoarRules.SKILL), "control: the roarer holds the Roar before it was given");
            long now = 100_000;
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the Alpha Roar was refused");
            helper.assertTrue(!rival.received("supernaturalbloodlines.pack.demon_roar_shifted"), "the rival was shifted by a roar without the skill");
            give(roarer);
            helper.assertTrue(LadderQuery.owns(roarer, DemonRoarRules.SKILL), "the Roar is not active for a werewolf who was given it");
            now += 5000;
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the Alpha Roar was refused with the skill");
            helper.assertTrue(DemonRoar.targets(roarer, List.of(rival)).size() == 1 && roarer.count("supernaturalbloodlines.pack.demon_roar_cooldown") == 0,
                    "the Roar's cooldown message came on its first use");
            now += AlphaRoar.cooldown(roarer) + 10; // the Alpha Roar is ready again, the Demon Wolf Roar (2400) is not
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the second Alpha Roar was refused");
            helper.assertTrue(roarer.count("supernaturalbloodlines.pack.demon_roar_cooldown") == 1, "the Roar's own cooldown was not told: " + roarer.heard());
            DemonRoar.reset();
        } finally {
            clean(roarer, rival);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarIsInactiveForAHolderWhoStoppedBeingAWerewolf(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 4, false, 2.5);
        try {
            give(p);
            helper.assertTrue(LadderQuery.owns(p, DemonRoarRules.SKILL), "control: the werewolf does not hold the Roar");
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            helper.assertTrue(!LadderQuery.owns(p, DemonRoarRules.SKILL), "the Roar counts for a player who is no werewolf");
        } finally {
            clean(p);
        }
        helper.succeed();
    }
}
