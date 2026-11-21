package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.core.ModActions;
import de.teamlapen.werewolves.util.FormHelper;
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

    /** The skill catalogue belongs to the supernatural part: without it S-WOL-11 is saved but inert, so a skill that counts cannot be tested. */
    private static boolean skillsOn() {
        return net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural");
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

    /** Nothing was paid to this player: still no Alpha and no Roar. */
    private static boolean unpaid(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf().rank() == Rank.OMEGA && !SupernaturalAccess.get(p).skills().contains(DemonRoarRules.SKILL);
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
            helper.assertTrue(d.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > d.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.2
                    && d.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > d.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.4,
                    "the wolf is not faster and harder than the man");
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
            d.setNoAi(true); // he stays where he is: the distances are the test's, not his walk's
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHuntWhoseBlowsLandOnSomebodyElseEndsAfterFourMinutesAndOneOnTheirPreyStartsTheCountAgain(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            helper.getLevel().setDayTime(NIGHT);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            // inside the structure: the test world loads the chunks of the structures and no others, so a cow 30 blocks away can stand in a chunk that is not
            // loaded (the neighbour decides), and then the hunt finds no prey (it gives up early) or the cow cannot be placed (a NullPointerException)
            net.minecraft.world.entity.animal.Cow prey = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(6, 1, 5));
            net.minecraft.world.entity.animal.Cow other = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
            for (var cow : List.of(prey, other)) {
                cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10_000);
                cow.setHealth(10_000);
            }
            d.hunt(prey.getUUID());
            d.setNoAi(true); // he stays where he is: nothing is hit but what the test hits
            run(d, 100);
            helper.assertTrue(d.sinceHit() >= 100 && !d.isRemoved(), "the count did not run: " + d.sinceHit());
            d.doHurtTarget(other);
            helper.assertTrue(d.sinceHit() >= 100, "a blow on somebody else started the count again");
            d.doHurtTarget(prey);
            helper.assertTrue(d.sinceHit() == 0, "a blow on his prey did not start the count again: " + d.sinceHit());
            run(d, KlausRules.GIVE_UP_AFTER - 100);
            helper.assertTrue(!d.isRemoved(), "he gave up before four minutes");
            run(d, 200);
            helper.assertTrue(d.isRemoved() && rig.index.state(BossId.DEUCALION) == BossState.LOST, "he did not give up after four minutes without a hit on a prey he could see");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withNoGroundForHimTheNightIsSpentAndHeDoesNotTryAgainUntilTheNextFullMoon(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 4, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.useGround((level, from, min, max) -> java.util.Optional.empty());
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null && !p.received(NOTICE), "he came with no ground, or the Alpha was told he did");
            helper.assertTrue(rig.index.of(BossId.DEUCALION).lastRollDay != -1, "the night was not spent");
            BossTestRig.fixedGround();
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) == null, "he tried a second time on the same night");
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    // ---- his death ----

    private static BossTestRig dead(GameTestHelper helper, ServerPlayer killer, LineTestPlayer... others) {
        BossTestRig rig = new BossTestRig(helper);
        rig.day(12);
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
            helper.assertTrue(rig.index.of(BossId.DEUCALION).returnDay == 12 && BossClock.today(helper.getLevel()) == 12, "the day he died was not written down: " + rig.index.of(BossId.DEUCALION).returnDay);
            DeucalionEvents.giveRoar(killer);
            helper.assertTrue(killer.count("supernaturalbloodlines.boss.deucalion.roar_given") == 1, "the Roar was given twice");
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

    // ---- who is paid ----

    private static void hit(DeucalionEntity d, net.minecraft.world.damagesource.DamageSource by, float amount) {
        d.invulnerableTime = 0;
        d.hurt(by, amount);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRewardGoesToThePlayerWhoDidTheMostNotToTheLastBlowAndAPetsDamageIsItsOwnersAndAMobsIsNobodys(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer a = wolf(helper, Rank.OMEGA, 0, false, 2.5);
        LineTestPlayer b = wolf(helper, Rank.OMEGA, 0, false, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(a, b);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            hit(d, a.damageSources().playerAttack(a), 20F);
            hit(d, b.damageSources().playerAttack(b), 10F);
            net.minecraft.world.entity.animal.Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(4, 1, 4));
            pet.tame(b);
            hit(d, helper.getLevel().damageSources().mobAttack(pet), 15F);
            hit(d, helper.getLevel().damageSources().mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5))), 100F);
            hit(d, helper.getLevel().damageSources().magic(), 100F);
            helper.assertTrue(b.getUUID().equals(d.tally().top().orElse(null)), "the one who did the most is not b (10 and the pet's 15 against a's 20): " + d.tally().entries());
            helper.assertTrue(d.tally().entries().size() == 2, "a mob or the environment is in the tally: " + d.tally().entries());
            kill(d, a); // a lands the last blow: it is not what counts
            helper.assertTrue(stack(b) == 4 && SupernaturalAccess.get(b).skills().contains(DemonRoarRules.SKILL), "the one who did the most was not paid: " + stack(b));
            helper.assertTrue(unpaid(a), "the last blow was paid");
        } finally {
            clean(a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTieInTheDamageGoesToTheLastHitter(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        for (boolean aLast : new boolean[] {false, true}) {
            LineTestPlayer a = wolf(helper, Rank.OMEGA, 0, false, 2.5);
            LineTestPlayer b = wolf(helper, Rank.OMEGA, 0, false, 3.5);
            try (BossTestRig rig = new BossTestRig(helper)) {
                night(a, b);
                DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
                LineTestPlayer first = aLast ? b : a;
                LineTestPlayer last = aLast ? a : b;
                hit(d, first.damageSources().playerAttack(first), 10F);
                hit(d, last.damageSources().playerAttack(last), 10F);
                kill(d, first);
                helper.assertTrue(stack(last) == 4 && unpaid(first), "the tie did not go to the last hitter (aLast=" + aLast + "): " + stack(last) + " and " + stack(first));
            } finally {
                clean(a, b);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTopPlayerWhoIsNoWerewolfIsToldWhyAndNobodyElseIsPaid(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer plain = LineTestPlayer.create(helper);
        LineTestPlayer wolf = wolf(helper, Rank.OMEGA, 0, false, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(plain, wolf);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            hit(d, plain.damageSources().playerAttack(plain), 50F);
            hit(d, wolf.damageSources().playerAttack(wolf), 10F);
            kill(d, wolf);
            helper.assertTrue(plain.received("supernaturalbloodlines.boss.deucalion.reward_no_werewolf"), "the top player, no werewolf, was told nothing: " + plain.heard());
            helper.assertTrue(unpaid(wolf), "the werewolf who landed the last blow was paid");
            helper.assertTrue(rig.index.of(BossId.DEUCALION).rewarded, "the reward was kept for later");
        } finally {
            clean(plain, wolf);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaWhoDidTheMostIsToldTheyTakeNoSparkAndStillLearnsTheRoar(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer trueAlpha = wolf(helper, Rank.TRUE_ALPHA, 1, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(trueAlpha);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            hit(d, trueAlpha.damageSources().playerAttack(trueAlpha), 30F);
            kill(d, trueAlpha);
            helper.assertTrue(trueAlpha.received("supernaturalbloodlines.boss.deucalion.reward_true_alpha"), "the True Alpha was told nothing: " + trueAlpha.heard());
            helper.assertTrue(stack(trueAlpha) == 1 && SupernaturalAccess.get(trueAlpha).skills().contains(DemonRoarRules.SKILL), "the True Alpha took a spark or did not learn the Roar");
        } finally {
            clean(trueAlpha);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTallyIsSavedWithHimAndAnOfflineTopPlayerIsNobodyToPay(GameTestHelper helper) {
        LineTestPlayer a = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useMods(b -> true);
            DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            d.tally().add(a.getUUID(), 12F);
            CompoundTag tag = new CompoundTag();
            d.saveWithoutId(tag);
            DeucalionEntity again = DeucalionEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(a.getUUID().equals(again.tally().top().orElse(null)) && again.tally().entries().get(a.getUUID()).amount() == 12F, "the tally was forgotten through a save: " + again.tally().entries());
            d.dismissPack();
        }
        helper.succeed();
    }

    // ---- who is called ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaCountsAsHoldingTheFullStackSoHeComesForThemAndTheirRewardIsTheRoarOnly(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer trueAlpha = wolf(helper, Rank.TRUE_ALPHA, 1, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(trueAlpha);
            helper.assertTrue(PackAccess.effectiveSparks(trueAlpha) >= BossesConfig.deucalionMinSparks(), "control: a True Alpha does not count as holding enough: " + PackAccess.effectiveSparks(trueAlpha));
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) != null, "he did not come for a True Alpha with the full stack");
            helper.assertTrue(trueAlpha.received(NOTICE), "the True Alpha was not warned: " + trueAlpha.heard());
        } finally {
            clean(trueAlpha);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWarningNamesTheThresholdFromTheConfigNotAFixedNumber(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.ALPHA, 7, true, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossesConfig.override("deucalion_min_sparks", 7);
            BossDirector.check(rig.server);
            helper.assertTrue(deucalionOf(rig) != null && p.received(NOTICE), "he did not come for 7 sparks with the minimum at 7");
            helper.assertTrue(p.heard().contains("7"), "the warning does not give the minimum of the config: " + p.heard());
            deucalionOf(rig).dismissPack();
        } finally {
            BossesConfig.override("deucalion_min_sparks", null);
            clean(p);
        }
        helper.succeed();
    }

    // ---- the pack that was asleep ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackAlphaThatLoadsAfterHisNightIsOverGoesAndOneOfTheSameNightStaysAndAWildOneIsLeftAlone(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            var level = helper.getLevel();
            level.setDayTime(NIGHT);
            long today = level.getDayTime() / 24000L;
            Mob member = net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner.spawnBossAlphas(level, helper.absolutePos(new BlockPos(3, 1, 3)), 1, today).get(0);
            try {
                java.util.function.BiPredicate<Boolean, Mob> joins = (disk, m) -> {
                    var e = new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(m, level, disk);
                    NeoForge.EVENT_BUS.post(e);
                    return e.isCanceled();
                };
                helper.assertTrue(!joins.test(true, member), "a pack Alpha of this very night, in the dark, was sent away");
                member.setData(SpeciesRegistries.NPC_MARK, NpcMark.bossPack(1, today - 1));
                helper.assertTrue(joins.test(true, member), "a pack Alpha of an older night stayed");
                helper.assertTrue(!joins.test(false, member), "a pack Alpha that was just made (not loaded) was sent away");
                member.setData(SpeciesRegistries.NPC_MARK, NpcMark.bossPack(1, today));
                level.setDayTime(MORNING);
                helper.assertTrue(joins.test(true, member), "a pack Alpha loaded at dawn stayed");
                level.setDayTime(NIGHT);
                member.setData(SpeciesRegistries.NPC_MARK, NpcMark.wild(1, today - 5));
                helper.assertTrue(!joins.test(true, member), "a wild wolf of an older night was sent away");
            } finally {
                member.discard();
            }
        }
        helper.succeed();
    }

    // ---- the Roar and its limits ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarSparesPlayersWhereThereIsNoPvpSpectatorsAndTheSameTeam(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer roarer = wolf(helper, Rank.ALPHA, 4, false, 2.5);
        LineTestPlayer rival = wolf(helper, Rank.ALPHA, 1, false, 6.5);
        LineTestPlayer.useRealHarm(true);
        var server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        var board = server.getScoreboard();
        String teamName = "deu" + Integer.toHexString(helper.hashCode() & 0xfff);
        try {
            night(roarer, rival);
            server.setPvpAllowed(true);
            helper.assertTrue(DemonRoar.targets(roarer, List.of(roarer, rival)).equals(List.of(rival)), "control: the rival is not a target");
            server.setPvpAllowed(false);
            helper.assertTrue(DemonRoar.targets(roarer, List.of(roarer, rival)).isEmpty(), "the Roar forced a player where PvP is off");
            server.setPvpAllowed(true);
            rival.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            helper.assertTrue(DemonRoar.targets(roarer, List.of(roarer, rival)).isEmpty(), "the Roar forced a spectator");
            rival.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var team = board.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
            board.addPlayerToTeam(roarer.getScoreboardName(), team);
            board.addPlayerToTeam(rival.getScoreboardName(), team);
            helper.assertTrue(roarer.getTeam() != null && rival.getTeam() != null, "control: the test players are not in the team");
            helper.assertTrue(DemonRoar.targets(roarer, List.of(roarer, rival)).isEmpty(), "the Roar forced somebody of the roarer's own team");
            team.setAllowFriendlyFire(true);
            helper.assertTrue(DemonRoar.targets(roarer, List.of(roarer, rival)).equals(List.of(rival)), "a team that allows friendly fire is spared");
        } finally {
            LineTestPlayer.useRealHarm(false);
            server.setPvpAllowed(pvp);
            var t = board.getPlayerTeam(teamName);
            if (t != null) {
                board.removePlayerTeam(t);
            }
            clean(roarer, rival);
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
        LineTestPlayer.useRealHarm(true);
        helper.getLevel().getServer().setPvpAllowed(true);
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
            LineTestPlayer.useRealHarm(false);
            clean(roarer, rival, beta, far, mate);
        }
        helper.succeed();
    }

    /** The Demon Wolf Roar rides on the Alpha Roar itself, so a roar from the HUD slot (no key any more) shifts the rival Alpha too. Needs the hud part. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void theDemonWolfRoarWorksFromTheAlphaRoarSlot(GameTestHelper helper) {
        if (!werewolves() || !skillsOn() || !net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("hud")) {
            helper.succeed();
            return;
        }
        LineTestPlayer.useRealHarm(true);
        helper.getLevel().getServer().setPvpAllowed(true);
        LineTestPlayer roarer = wolf(helper, Rank.ALPHA, 4, false, 2.5);
        LineTestPlayer rival = wolf(helper, Rank.ALPHA, 1, false, 22.5);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(rival).setFactionAndLevel(WReference.WEREWOLF_FACTION, 14), "the rival could not be levelled");
            night(roarer, rival);
            unlockForms(rival);
            give(roarer);
            net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService.offerDefaults(roarer);
            int slot = net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService.get(roarer).slotOf(net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions.ALPHA_ROAR);
            helper.assertTrue(slot >= 0, "the Alpha Roar was not put in a slot: " + net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService.get(roarer));
            helper.assertTrue(!inForm(rival), "control: the rival is in a form before the roar");
            String refusal = net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService.trigger(roarer, slot, -1, false);
            helper.assertTrue(refusal == null, "the Alpha Roar from its slot was refused: " + refusal);
            helper.assertTrue(inForm(rival) && rival.received(SHIFTED), "the Demon Wolf Roar did not shift the rival from the slot: " + rival.heard());
        } finally {
            AlphaRoar.reset();
            LineTestPlayer.useRealHarm(false);
            clean(roarer, rival);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarWorksOnTheAlphaRoarsKeyOnlyForWhoHoldsItWithItsOwnCooldown(GameTestHelper helper) {
        if (!werewolves() || !skillsOn()) {
            helper.succeed();
            return;
        }
        LineTestPlayer.useRealHarm(true);
        helper.getLevel().getServer().setPvpAllowed(true);
        LineTestPlayer roarer = wolf(helper, Rank.ALPHA, 4, false, 2.5);
        // 20 blocks away: out of the ordinary roar's 16 (which forces other werewolves out of their form), inside the Demon Wolf Roar's 32
        LineTestPlayer rival = wolf(helper, Rank.ALPHA, 1, false, 22.5);
        LineTestPlayer beta = wolf(helper, Rank.BETA, 0, false, 21.5);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(rival).setFactionAndLevel(WReference.WEREWOLF_FACTION, 14), "the rival could not be levelled");
            night(roarer, rival, beta);
            unlockForms(rival);
            unlockForms(beta);
            helper.assertTrue(!LadderQuery.owns(roarer, DemonRoarRules.SKILL), "control: the roarer holds the Roar before it was given");
            long now = 100_000;
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the Alpha Roar was refused");
            helper.assertTrue(!inForm(rival) && !rival.received(SHIFTED), "the rival was shifted by a roar without the skill");
            give(roarer);
            helper.assertTrue(LadderQuery.owns(roarer, DemonRoarRules.SKILL), "the Roar is not active for a werewolf who was given it");
            now += 5000;
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the Alpha Roar was refused with the skill");
            helper.assertTrue(inForm(rival) && rival.received(SHIFTED), "the rival Alpha was not forced to shift, or not told: " + rival.heard());
            DemonRoar.reset(); // ready again at once: a rival who is in the form already must stay in it (a toggle would switch it off)
            now += AlphaRoar.cooldown(roarer) + 10;
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the Alpha Roar was refused again");
            helper.assertTrue(inForm(rival), "a second Demon Wolf Roar switched the rival's form off");
            helper.assertTrue(!inForm(beta) && !beta.received(SHIFTED), "a Beta was shifted by the Demon Wolf Roar");
            helper.assertTrue(roarer.count("supernaturalbloodlines.pack.demon_roar_cooldown") == 0, "the Roar's cooldown message came on its first use");
            FormHelper.deactivateWerewolfActions((IWerewolfPlayer) VampirismAPI.factionPlayerHandler(rival).getCurrentFactionPlayer().orElseThrow());
            helper.assertTrue(!inForm(rival), "control: the rival's form did not go");
            now += AlphaRoar.cooldown(roarer) + 10; // the Alpha Roar is ready again, the Demon Wolf Roar (2400) is not
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the second Alpha Roar was refused");
            helper.assertTrue(roarer.count("supernaturalbloodlines.pack.demon_roar_cooldown") == 1 && !inForm(rival), "the Roar's own cooldown did not hold: " + roarer.heard());
            now += DemonRoarRules.COOLDOWN;
            helper.assertTrue(AlphaRoar.roar(roarer, now) == null, "the third Alpha Roar was refused");
            // (the rival's own form action is on a cooldown of its own for a while after it was switched off, so what is checked is the Roar's: no new refusal)
            helper.assertTrue(roarer.count("supernaturalbloodlines.pack.demon_roar_cooldown") == 1, "the Roar was still on its cooldown after 2400 ticks: " + roarer.heard());
            DemonRoar.reset();
        } finally {
            LineTestPlayer.useRealHarm(false);
            clean(roarer, rival, beta);
        }
        helper.succeed();
    }

    private static final String SHIFTED = "supernaturalbloodlines.pack.demon_roar_shifted";

    private static boolean inForm(ServerPlayer p) {
        return FormHelper.isFormActionActive((IWerewolfPlayer) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void unlockForms(ServerPlayer p) {
        ((IActionHandler) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getActionHandler())
                .unlockActions(List.of(ModActions.BEAST_FORM.get(), ModActions.SURVIVAL_FORM.get(), ModActions.HUMAN_FORM.get()));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarIsInactiveForAHolderWhoStoppedBeingAWerewolf(GameTestHelper helper) {
        if (!werewolves() || !skillsOn()) {
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
