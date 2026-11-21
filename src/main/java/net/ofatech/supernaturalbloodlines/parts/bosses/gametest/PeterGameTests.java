package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.PeterRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterEvents;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService;

/**
 * Peter Hale (plan 17g): the house that calls him (one Peter, whatever the number of houses), the shift and the skins, feral on a full moon night, the pounce, what his
 * death pays (one Alpha spark by the ladder of a stolen spark to the one who did the most, and the Memory Claws for anybody) and what the claws may and may not learn.
 * The moon, the mods and the houses are pinned. Werewolves only: without it every test passes without running.
 */
@PrefixGameTestTemplate(false)
public final class PeterGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long NIGHT = 24000L * 20 + 15000;

    private PeterGameTests() {
    }

    /** A house standing in the test: its floor layer is the test's floor, so Peter's spot is one block above it, inside the loaded chunk. */
    private static BoundingBox house(GameTestHelper helper, int dx) {
        BlockPos at = helper.absolutePos(new BlockPos(5, 1, 4)).offset(dx, 0, 0);
        return new BoundingBox(at.getX() - 5, at.getY() - PeterRules.FLOOR_LAYER, at.getZ() - 4, at.getX() + 5, at.getY() - PeterRules.FLOOR_LAYER + 12, at.getZ() + 4);
    }

    private static boolean werewolves() {
        return ModList.get().isLoaded("werewolves");
    }

    private static LineTestPlayer wolf(GameTestHelper helper, Rank rank, int sparks, double x) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 2.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 5), "the test wolf could not join the werewolves");
        PackAccess.setSparks(p, sparks);
        return p;
    }

    private static void clean(LineTestPlayer... ps) {
        SireCall.overridePlayerLookup(null);
        for (LineTestPlayer p : ps) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(java.util.Set.of()).build());
        }
    }

    /** The mods there, the moon as given, the dice passing, the ground fixed, and these players the only ones in the overworld. */
    private static void world(boolean moon, java.util.function.Supplier<List<BoundingBox>> houses, ServerPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMoon(l -> moon);
        BossDirector.useMods(b -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useWorld(l -> List.of(ps), null);
        BossDirector.useHouses((level, from) -> houses.get().stream().filter(b -> from.distSqr(b.getCenter()) <= 24.0 * 24.0).findFirst());
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
    }

    /** The test has no floor of its own: a stone one at y 0, so a wolf stands on the ground and a villager does not fall away. */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
    }

    private static PeterHaleEntity peterOf(BossTestRig rig) {
        return BossDirector.anyBossBody(rig.server, BossId.PETER_HALE) instanceof PeterHaleEntity p ? p : null;
    }

    private static PeterHaleEntity peter(GameTestHelper helper) {
        return BossDirector.spawnPeterAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
    }

    private static void run(PeterHaleEntity p, int ticks) {
        for (int i = 0; i < ticks && !p.isRemoved(); i++) {
            p.tickCount++;
            p.tick();
        }
    }

    private static void hit(PeterHaleEntity p, net.minecraft.world.damagesource.DamageSource by, float amount) {
        p.invulnerableTime = 0;
        p.hurt(by, amount);
    }

    private static void kill(PeterHaleEntity p, ServerPlayer killer) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(p, killer.damageSources().playerAttack(killer)));
    }

    private static int stack(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf().sparks();
    }

    private static long clawsNear(GameTestHelper helper, PeterHaleEntity p) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8.0), i -> i.getItem().is(BossItems.MEMORY_CLAWS.get())).size();
    }

    private static void sweep(GameTestHelper helper, PeterHaleEntity p) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8.0)).forEach(net.minecraft.world.entity.Entity::discard);
    }

    // ---- the house, and one Peter ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerNearAHouseCallsHimOnceWhateverTheNumberOfHousesAndHeStandsInIt(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer near = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BoundingBox HOUSE = house(helper, 0);
            BoundingBox OTHER_HOUSE = house(helper, 200);
            near.setPos(HOUSE.getCenter().getX(), HOUSE.minY() + 4, HOUSE.getCenter().getZ());
            world(false, () -> List.of(HOUSE, OTHER_HOUSE), near);
            BossDirector.check(rig.server);
            PeterHaleEntity p = peterOf(rig);
            helper.assertTrue(p != null && p.isAlive(), "he did not come for a player at a house");
            helper.assertTrue(p.form() == BossForm.HUMAN && !p.metMoon(), "he came as a wolf, or has met a moon already: " + p.form());
            helper.assertTrue(BlockPos.containing(p.position()).equals(BossDirector.peterSpot(HOUSE)) || p.blockPosition().equals(BossDirector.peterSpot(HOUSE)), "he is not at the spot of the house: " + p.blockPosition());
            helper.assertTrue(p.home() != null && p.home().equals(BossDirector.peterSpot(HOUSE)), "his home is not the house: " + p.home());
            helper.assertTrue(BossDirector.peterSpot(HOUSE).getY() == HOUSE.minY() + 4 && BossDirector.peterSpot(HOUSE).getX() == HOUSE.getCenter().getX(), "his spot is not one block above the floor layer, in the middle: " + BossDirector.peterSpot(HOUSE));
            helper.assertTrue(rig.index.state(BossId.PETER_HALE) == BossState.ACTIVE && rig.index.of(BossId.PETER_HALE).appearances == 1, "the index says " + rig.index.state(BossId.PETER_HALE));
            helper.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) == 200.0, "his health is not the base");
            // the player walks to the other house: there is one Peter, and the index says so
            near.setPos(OTHER_HOUSE.getCenter().getX(), OTHER_HOUSE.minY() + 4, OTHER_HOUSE.getCenter().getZ());
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.PETER_HALE).appearances == 1, "a second Peter came at the second house");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PeterHaleEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(400, 400, 400)).size() == 1, "there is more than one Peter in the world");
            p.discard();
        } finally {
            clean(near);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStaysAwayWithoutAHouseNearOrWithoutWerewolvesOrForASpectatorOrWhenSwitchedOffOrInPeace(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BoundingBox HOUSE = house(helper, 0);
            p.setPos(HOUSE.getCenter().getX(), HOUSE.minY() + 4, HOUSE.getCenter().getZ());
            world(false, () -> List.of(HOUSE), p);
            // 25 blocks from the house's middle is out of range
            p.setPos(HOUSE.getCenter().getX() + 25, HOUSE.minY() + 4, HOUSE.getCenter().getZ());
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null, "he came for a player 25 blocks from a house");
            p.setPos(HOUSE.getCenter().getX(), HOUSE.minY() + 4, HOUSE.getCenter().getZ());
            BossDirector.useMods(b -> false);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null, "he came without Werewolves");
            BossDirector.useMods(b -> true);
            BossesConfig.override("peter_hale_enabled", false);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null, "he came with his switch off");
            BossesConfig.override("peter_hale_enabled", null);
            BossDirector.useDifficulty(l -> net.minecraft.world.Difficulty.PEACEFUL);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null, "he came in peaceful");
            BossDirector.useDifficulty(null);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null, "he came for a spectator");
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) != null, "he did not come for the same player when nothing was in the way");
            peterOf(rig).discard();
        } finally {
            BossesConfig.override("peter_hale_enabled", null);
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLostPeterComesAgainAndADeadOneOnlyWithTheRespawnSwitchAndEnoughDays(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer killer = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            rig.day(12);
            BoundingBox HOUSE = house(helper, 0);
            killer.setPos(HOUSE.getCenter().getX(), HOUSE.minY() + 4, HOUSE.getCenter().getZ());
            world(false, () -> List.of(HOUSE), killer);
            PeterHaleEntity p = peter(helper);
            kill(p, killer);
            helper.assertTrue(rig.index.state(BossId.PETER_HALE) == BossState.DEAD && rig.index.of(BossId.PETER_HALE).returnDay == 12, "he is not written down as dead on day 12: " + rig.index.of(BossId.PETER_HALE).returnDay);
            p.discard();
            rig.day(40);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null || !peterOf(rig).isAlive(), "a dead Peter came back with the respawn switch off");
            BossesConfig.override("peter_respawn", true);
            rig.day(18);
            BossDirector.check(rig.server);
            helper.assertTrue(peterOf(rig) == null, "he came back after 6 days of 7");
            rig.day(19);
            BossDirector.check(rig.server);
            PeterHaleEntity again = peterOf(rig);
            helper.assertTrue(again != null && again.isAlive() && !rig.index.of(BossId.PETER_HALE).rewarded, "he did not come back after 7 days, or the reward was not reset");
            again.discard();
        } finally {
            BossesConfig.override("peter_respawn", null);
            clean(killer);
        }
        helper.succeed();
    }

    // ---- the shift, the skins ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heShiftsOnAFullMoonNightAndAtHalfHealthAndOnlyThenAndHisSkinIsHealedAfterTheFirstMoon(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p0 = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, List::of, p0);
            PeterHaleEntity p = peter(helper);
            helper.assertTrue(p.skin().equals("peter_hale_burned"), "the man's skin before a moon is " + p.skin());
            run(p, 40);
            helper.assertTrue(p.form() == BossForm.HUMAN && !p.metMoon(), "he shifted without a moon or a wound: " + p.form());
            BossDirector.useMoon(l -> true);
            run(p, 40);
            helper.assertTrue(p.metMoon() && p.skin().equals("peter_hale"), "he has met a moon and his skin is " + p.skin());
            helper.assertTrue(p.form() != BossForm.HUMAN, "he did not shift on a full moon night");
            run(p, ShiftRules.SHIFT_TICKS + 10);
            helper.assertTrue(p.form() == BossForm.BEAST, "he did not finish the shift: " + p.form());
            BossDirector.useMoon(l -> false);
            run(p, 60);
            helper.assertTrue(p.form() == BossForm.BEAST && p.skin().equals("peter_hale"), "he changed back or lost his skin when the moon went");
            p.discard();
            // the switch off: the moon does not shift him, half health does
            BossesConfig.override("peter_feral_full_moon", false);
            BossDirector.useMoon(l -> true);
            PeterHaleEntity q = BossDirector.spawnPeterAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
            run(q, 40);
            helper.assertTrue(q.form() == BossForm.HUMAN, "the moon shifted him with the feral setting off");
            q.setHealth(q.getMaxHealth() * 0.5F);
            run(q, 40);
            helper.assertTrue(q.form() != BossForm.HUMAN, "half health did not shift him");
            q.discard();
        } finally {
            BossesConfig.override("peter_feral_full_moon", null);
            clean(p0);
        }
        helper.succeed();
    }

    // ---- feral ----

    /** The first mob he takes within {@code ticks}, or null for nobody: looked at every tick, because a wolf kills a villager quickly. */
    private static net.minecraft.world.entity.LivingEntity takes(PeterHaleEntity p, int ticks) {
        for (int i = 0; i < ticks && !p.isRemoved(); i++) {
            run(p, 1);
            if (p.getTarget() != null) {
                return p.getTarget();
            }
        }
        return null;
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void asTheWolfOnAFullMoonNightHeTakesAVillagerButNotABossAndWithoutTheMoonOrTheSettingNot(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p0 = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
            world(true, List::of, p0);
            PeterHaleEntity p = peter(helper);
            p.setForm(BossForm.BEAST);
            Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(7, 1, 2));
            var kol = rig.spawnKol();
            kol.setPos(helper.absoluteVec(new Vec3(4.5, 1, 2.5)));
            helper.assertTrue(p.feral(), "the wolf is not feral on a full moon night");
            for (int y = 1; y <= 4; y++) {
                for (int z = 0; z < 8; z++) {
                    helper.setBlock(new BlockPos(5, y, z), net.minecraft.world.level.block.Blocks.STONE);
                }
            }
            helper.assertTrue(!p.hasLineOfSight(villager), "the wall does not hide the villager");
            helper.assertTrue(takes(p, 200) == villager, "a feral wolf did not take a villager behind a wall (or took the boss nearer): " + p.getTarget());
            p.setTarget(null);
            villager.setHealth(villager.getMaxHealth());
            BossDirector.useMoon(l -> false);
            helper.assertTrue(!p.feral(), "feral without the moon");
            helper.assertTrue(takes(p, 200) == null, "he took somebody without the moon");
            BossDirector.useMoon(l -> true);
            BossesConfig.override("peter_feral_full_moon", false);
            helper.assertTrue(!p.feral(), "feral with the setting off");
            helper.assertTrue(takes(p, 200) == null, "he took somebody with the setting off");
            villager.discard();
            p.discard();
        } finally {
            BossesConfig.override("peter_feral_full_moon", null);
            clean(p0);
        }
        helper.succeed();
    }

    // ---- the pounce ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theWolfPouncesAtATargetFromAFewBlocksStandsStillWhileItPlaysAndEndsWhereTheAnimationDoes(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer p0 = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
            world(false, List::of, p0);
            PeterHaleEntity p = peter(helper);
            p.setForm(BossForm.BEAST);
            p.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
            Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6, 1, 2));
            villager.setNoAi(true);
            p.setTarget(villager);
            p.setOnGround(true);
            for (int i = 0; i < 10 && !p.pouncing(); i++) {
                run(p, 1);
            }
            helper.assertTrue(p.pouncing(), "he did not pounce at a target 3.5 blocks away: form " + p.form() + ", ground " + p.onGround() + ", target " + p.getTarget() + ", pause " + p.pauseLeft() + ", distance " + p.distanceToSqr(villager));
            run(p, 1); // the tick that started it had already moved him; from the next one he is held
            Vec3 start = p.position();
            // whatever the navigation asks for, nothing of his own travel is added while the animation plays
            p.travel(new Vec3(0.0, 0.0, 1.0));
            helper.assertTrue(p.position().equals(start) && p.getDeltaMovement().lengthSqr() < 1.0E-6, "his own travel moved him during the pounce: " + p.position().subtract(start));
            helper.assertTrue(p.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6, "he moves while the pounce plays (the root bone is what moves)");
            run(p, 8);
            helper.assertTrue(p.position().distanceToSqr(start.x, p.getY(), start.z) < 0.05 && p.pouncing(), "he walked during the pounce: " + p.position());
            float before = villager.getHealth();
            // measured the tick it ends: after it he walks on to the target like any wolf
            for (int i = 0; i < PeterRules.POUNCE_TICKS && p.pouncing(); i++) {
                run(p, 1);
            }
            helper.assertTrue(!p.pouncing(), "the pounce did not end");
            double moved = Math.sqrt(p.position().distanceToSqr(start.x, p.getY(), start.z));
            helper.assertTrue(Math.abs(moved - PeterRules.POUNCE_DISTANCE) < 0.3, "he ended " + moved + " blocks on, the animation moves the model " + PeterRules.POUNCE_DISTANCE);
            helper.assertTrue(villager.getHealth() < before, "the pounce did not hurt the target in reach");
            helper.assertTrue(p.pauseLeft() > PeterRules.POUNCE_PAUSE - 40, "no pause after a pounce: " + p.pauseLeft());
            run(p, 5);
            helper.assertTrue(!p.pouncing(), "he pounced again while paused");
            p.discard();
            villager.discard();
        } finally {
            clean(p0);
        }
        helper.succeed();
    }

    // ---- who is paid ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisDeathPaysOnceTheClawsForAnybodyAndOneSparkToTheOneWhoDidTheMostNotTheLastBlow(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer a = wolf(helper, Rank.ALPHA, 2, 2.5);
        LineTestPlayer b = wolf(helper, Rank.ALPHA, 2, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            rig.day(9);
            world(false, List::of, a, b);
            PeterHaleEntity p = peter(helper);
            hit(p, a.damageSources().playerAttack(a), 20F);
            hit(p, b.damageSources().playerAttack(b), 30F);
            hit(p, helper.getLevel().damageSources().magic(), 100F);
            helper.assertTrue(b.getUUID().equals(p.tally().top().orElse(null)) && p.tally().entries().size() == 2, "the one who did the most is not b: " + p.tally().entries());
            kill(p, a);
            helper.assertTrue(stack(b) == 3 && stack(a) == 2 && b.received("supernaturalbloodlines.pack.spark_gained") && !a.received("supernaturalbloodlines.pack.spark_gained"), "the spark did not go to the one who did the most: b " + stack(b) + ", a " + stack(a));
            helper.assertTrue(clawsNear(helper, p) == 1, "the Memory Claws were not dropped once: " + clawsNear(helper, p));
            helper.assertTrue(rig.index.state(BossId.PETER_HALE) == BossState.DEAD && rig.index.of(BossId.PETER_HALE).rewarded && rig.index.of(BossId.PETER_HALE).returnDay == 9, "the index says " + rig.index.state(BossId.PETER_HALE));
            helper.assertTrue(!PeterEvents.pay(helper.getLevel(), p, b) && stack(b) == 3 && clawsNear(helper, p) == 1, "the reward was paid twice");
            ItemEntity claws = helper.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8.0), i -> i.getItem().is(BossItems.MEMORY_CLAWS.get())).get(0);
            helper.assertTrue(claws.isInvulnerable() && claws.getAge() < 0, "the claws can burn or despawn: age " + claws.getAge());
            sweep(helper, p);
        } finally {
            clean(a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillWithNoDamageTalliedBeforeItStillPaysTheKillerAndAPetsDamageIsItsOwnersAndAMobsIsNobodys(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer a = wolf(helper, Rank.ALPHA, 2, 2.5);
        LineTestPlayer b = wolf(helper, Rank.ALPHA, 2, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, List::of, a, b);
            // nothing tallied: a one-shot by a player is still paid to that player
            PeterHaleEntity p = peter(helper);
            kill(p, a);
            helper.assertTrue(stack(a) == 3 && stack(b) == 2, "a kill with an empty tally paid " + stack(a) + " and " + stack(b));
            sweep(helper, p);
            rig.index.forget(BossId.PETER_HALE);
            // a pet: its damage is its owner's, a zombie's and the environment's are nobody's
            PeterHaleEntity q = peter(helper);
            hit(q, a.damageSources().playerAttack(a), 10F);
            net.minecraft.world.entity.animal.Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(4, 1, 4));
            pet.tame(b);
            hit(q, helper.getLevel().damageSources().mobAttack(pet), 15F);
            hit(q, helper.getLevel().damageSources().mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5))), 100F);
            hit(q, helper.getLevel().damageSources().magic(), 100F);
            helper.assertTrue(b.getUUID().equals(q.tally().top().orElse(null)) && q.tally().entries().size() == 2, "the pet's damage is not its owner's, or a mob counted: " + q.tally().entries());
            sweep(helper, q);
            q.discard();
        } finally {
            clean(a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaTakesNoSparkANonWerewolfIsToldSoAndAMobsKillPaysOnlyTheClaws(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer trueAlpha = wolf(helper, Rank.TRUE_ALPHA, 1, 2.5);
        LineTestPlayer plain = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, List::of, trueAlpha, plain);
            PeterHaleEntity p = peter(helper);
            hit(p, trueAlpha.damageSources().playerAttack(trueAlpha), 10F);
            int before = stack(trueAlpha);
            int plainBefore = BloodlineAccess.get(plain).wolf().sparks();
            kill(p, trueAlpha);
            helper.assertTrue(stack(trueAlpha) == before && trueAlpha.received("supernaturalbloodlines.boss.peter.reward_true_alpha"), "a True Alpha took a spark, or was not told: " + trueAlpha.heard());
            helper.assertTrue(clawsNear(helper, p) == 1, "the claws were not paid for a True Alpha's kill");
            sweep(helper, p);
            rig.index.forget(BossId.PETER_HALE);
            PeterHaleEntity q = peter(helper);
            hit(q, plain.damageSources().playerAttack(plain), 10F);
            kill(q, plain);
            helper.assertTrue(plain.received("supernaturalbloodlines.boss.peter.reward_no_werewolf") && BloodlineAccess.get(plain).wolf().sparks() == plainBefore, "somebody who is no werewolf was not told, or got a spark: " + plain.heard());
            sweep(helper, q);
            rig.index.forget(BossId.PETER_HALE);
            PeterHaleEntity r = peter(helper);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(r, helper.getLevel().damageSources().mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5)))));
            helper.assertTrue(clawsNear(helper, r) == 1 && rig.index.of(BossId.PETER_HALE).rewarded, "a mob's kill did not pay the claws, or kept the reward for later");
            helper.assertTrue(stack(trueAlpha) == before && BloodlineAccess.get(plain).wolf().sparks() == plainBefore, "a mob's kill paid a spark");
            sweep(helper, r);
        } finally {
            clean(trueAlpha);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisKillNeverTouchesALineAndHisHitNeverCountsAsWhiteOak(GameTestHelper helper) {
        LineTestPlayer original = LineTestPlayer.create(helper);
        LineTestPlayer child = LineTestPlayer.create(helper);
        BloodlineAccess.set(original, BloodlineAccess.get(original).toBuilder().species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.ORIGINAL).build());
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder.record(original, child);
            BossDirector.useMods(b -> true);
            PeterHaleEntity p = peter(helper);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(BossItems.MIKAELS_STAKE.get()));
            BossesConfig.override("mikael_stakes_originals", true);
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules.isWhiteOakKill(p.damageSources().mobAttack(p)), "Peter's hit with a stake counts as a white oak kill");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(original, p.damageSources().mobAttack(p)));
            helper.assertTrue(!child.isDeadOrDying(), "Peter's hit killed an Original's line");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(p, rig.whiteOakBlow()));
            helper.assertTrue(!child.isDeadOrDying() && rig.index.state(BossId.PETER_HALE) == BossState.DEAD, "his white oak death touched a line, or the index says " + rig.index.state(BossId.PETER_HALE));
            sweep(helper, p);
        } finally {
            BossesConfig.override("mikael_stakes_originals", null);
            net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex.get(helper.getLevel().getServer()).remove(child.getUUID());
            net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex.get(helper.getLevel().getServer()).remove(original.getUUID());
            BloodlineAccess.set(original, BloodlineState.NONE);
            BloodlineAccess.set(child, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- save ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisFormMoonSkinHomeAndDamageSurviveASave(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer a = wolf(helper, Rank.OMEGA, 0, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true, List::of, a);
            PeterHaleEntity p = peter(helper);
            run(p, 40 + ShiftRules.SHIFT_TICKS + 5);
            hit(p, a.damageSources().playerAttack(a), 12F);
            CompoundTag tag = new CompoundTag();
            p.saveWithoutId(tag);
            PeterHaleEntity again = PeterHaleEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.form() == p.form() && again.shiftTicks() == p.shiftTicks() && again.form() == BossForm.BEAST, "the form was lost: " + again.form());
            helper.assertTrue(again.metMoon() && again.skin().equals("peter_hale"), "the moon he met was forgotten");
            helper.assertTrue(again.home() != null && again.home().equals(p.home()), "the house was forgotten: " + again.home());
            helper.assertTrue(a.getUUID().equals(again.tally().top().orElse(null)) && again.tally().entries().equals(p.tally().entries()), "the damage tally was lost: " + again.tally().entries());
            helper.assertTrue(again.getBbWidth() == 1.4F, "the wolf's hitbox was lost: " + again.getBbWidth());
            p.discard();
        } finally {
            clean(a);
        }
        helper.succeed();
    }

    // ---- the Memory Claws ----

    private static ItemStack claws() {
        return new ItemStack(BossItems.MEMORY_CLAWS.get());
    }

    /** The server knows these players by id (a fake player is in nobody's list), so a profile can be read about them. */
    private static void known(GameTestHelper helper, ServerPlayer... ps) {
        // the GameTest server has PvP off; the claws are a rule about PvP, so the test says which it is and puts it back
        pvpBefore = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(true);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
    }

    private static Boolean pvpBefore;

    private static void restore(GameTestHelper helper) {
        SireCall.overridePlayerLookup(null);
        if (pvpBefore != null) {
            helper.getLevel().getServer().setPvpAllowed(pvpBefore);
            pvpBefore = null;
        }
    }

    private static void agree(ServerPlayer target) {
        target.setShiftKeyDown(true);
        target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClawsReadASiresAndGenesOnlyOfAPlayerWhoAgreesAndTellThemAndNothingMoreIsDiscovered(GameTestHelper helper) {
        LineTestPlayer user = LineTestPlayer.create(helper);
        LineTestPlayer target = LineTestPlayer.create(helper);
        BloodlineAccess.set(target, BloodlineAccess.get(target).toBuilder().doppelgangerGene(true).doppelgangerRolled(true).build());
        known(helper, user, target);
        try {
            LineTestPlayer.useRealHarm(true);
            ItemStack stack = claws();
            var item = (net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem) stack.getItem();
            // no consent: refused, nothing read, nothing spent, the target is not told
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.received("item.supernaturalbloodlines.memory_claws.refused") && stack.getDamageValue() == 0, "a refusal was not said, or cost a use: " + user.heard());
            helper.assertTrue(!ProfileService.of(user, target.getUUID()).facts().containsKey(Fact.GENES), "the claws read a player who did not agree");
            helper.assertTrue(target.received() == 0, "the target was told of a refused use");
            // consent
            agree(target);
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.received(net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem.LEARNED) && stack.getDamageValue() == 1, "the user was not told, or no use was spent: damage " + stack.getDamageValue() + ", heard " + user.heard());
            helper.assertTrue(target.received(net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem.USED_ON), "the target was not told it was used on them");
            helper.assertTrue(user.getCooldowns().isOnCooldown(item), "no cooldown after a use");
            var facts = ProfileService.of(user, target.getUUID()).facts();
            helper.assertTrue(facts.containsKey(Fact.GENES) && facts.containsKey(Fact.SIRE), "the claws did not teach the sire and the genes: " + facts.keySet());
            helper.assertTrue(!facts.containsKey(Fact.LINE), "the claws named a line");
            // what the genes say is what the target has discovered themselves: nothing, so unknown, whatever they carry
            helper.assertTrue(facts.get(Fact.GENES).stream().allMatch(s -> s.endsWith(":unknown")), "an undiscovered gene was read: " + facts.get(Fact.GENES));
            helper.assertTrue(BloodlineAccess.get(target).revealedGenes().isEmpty() && BloodlineAccess.get(user).revealedGenes().isEmpty(), "a gene was discovered by the claws");
            // a second player in the user's pack learns nothing from it: it is the user's own record
            LineTestPlayer other = LineTestPlayer.create(helper);
            helper.assertTrue(!ProfileService.of(other, target.getUUID()).facts().containsKey(Fact.GENES), "somebody else learned from the user's claws");
        } finally {
            LineTestPlayer.useRealHarm(false);
            restore(helper);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClawsNeverNameAnNpcOriginalAsTheSireOrALine(GameTestHelper helper) {
        LineTestPlayer user = LineTestPlayer.create(helper);
        LineTestPlayer target = LineTestPlayer.create(helper);
        known(helper, user, target);
        try {
            LineTestPlayer.useRealHarm(true);
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder.record(net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.KLAUS.uuid(), "Klaus", target);
            agree(target);
            ItemStack stack = claws();
            ((net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem) stack.getItem()).interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            var facts = ProfileService.of(user, target.getUUID()).facts();
            helper.assertTrue(stack.getDamageValue() == 1 && facts.containsKey(Fact.GENES), "the claws did not work on a player sired by an NPC Original: damage " + stack.getDamageValue() + ", facts " + facts + ", user heard " + user.heard() + ", target ok " + target.isShiftKeyDown() + " " + target.getMainHandItem());
            helper.assertTrue(!facts.containsKey(Fact.SIRE) && !facts.containsKey(Fact.LINE), "the claws named an NPC Original as the sire or a line: " + facts);
            helper.assertTrue(facts.values().stream().flatMap(List::stream).noneMatch(s -> s.toLowerCase(java.util.Locale.ROOT).contains("klaus")), "the name of an NPC Original is in what the claws taught: " + facts);
        } finally {
            LineTestPlayer.useRealHarm(false);
            restore(helper);
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex.get(helper.getLevel()).remove(target.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClawsWaitHalfAMinuteBetweenUsesWhoeverIsTheTarget(GameTestHelper helper) {
        LineTestPlayer user = LineTestPlayer.create(helper);
        LineTestPlayer target = LineTestPlayer.create(helper);
        known(helper, user, target);
        try {
            LineTestPlayer.useRealHarm(true);
            agree(target);
            ItemStack stack = claws();
            var item = (net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem) stack.getItem();
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(stack.getDamageValue() == 1 && user.getCooldowns().isOnCooldown(item), "the first use did not work or started no cooldown");
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(stack.getDamageValue() == 1 && user.received(net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules.WAIT), "a second use at once was not refused with the wait message: " + user.heard());
            // the wait is the same for a target who would be refused anyway: it tells nothing about them
            LineTestPlayer other = LineTestPlayer.create(helper);
            item.interactLivingEntity(stack, user, other, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.count(net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules.WAIT) == 2 && !user.received(net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules.REFUSED), "the wait did not come first: " + user.heard());
        } finally {
            LineTestPlayer.useRealHarm(false);
            restore(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClawsWorkOnNothingButAnotherPlayerAndNeverOnTheUserAndCostNothingWhenTheyFail(GameTestHelper helper) {
        LineTestPlayer user = LineTestPlayer.create(helper);
        known(helper, user);
        try {
            LineTestPlayer.useRealHarm(true);
            ItemStack stack = claws();
            var item = (net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem) stack.getItem();
            Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
            item.interactLivingEntity(stack, user, villager, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.received(net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules.NOT_THIS) && stack.getDamageValue() == 0, "a villager was read, or it cost a use: " + user.heard());
            agree(user);
            item.interactLivingEntity(stack, user, user, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.received(net.ofatech.supernaturalbloodlines.core.boss.MemoryClawsRules.SELF) && stack.getDamageValue() == 0, "the user was read, or it cost a use");
            villager.discard();
        } finally {
            LineTestPlayer.useRealHarm(false);
            restore(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClawsFollowPvpAndTheRightToHarm(GameTestHelper helper) {
        LineTestPlayer user = LineTestPlayer.create(helper);
        LineTestPlayer target = LineTestPlayer.create(helper);
        known(helper, user, target);
        try {
            agree(target);
            ItemStack stack = claws();
            var item = (net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem) stack.getItem();
            // a fake player never harms a player (the test's own default): the right to harm is missing, so a player who agrees is still refused
            LineTestPlayer.useRealHarm(false);
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.received("item.supernaturalbloodlines.memory_claws.refused") && stack.getDamageValue() == 0 && !ProfileService.of(user, target.getUUID()).facts().containsKey(Fact.GENES),
                    "the claws read a player the user may not hurt");
            // PvP off: refused even with the right to harm and consent
            LineTestPlayer.useRealHarm(true);
            boolean pvp = helper.getLevel().getServer().isPvpAllowed();
            helper.getLevel().getServer().setPvpAllowed(false);
            try {
                item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
                helper.assertTrue(stack.getDamageValue() == 0 && !ProfileService.of(user, target.getUUID()).facts().containsKey(Fact.GENES), "the claws read a player where PvP is off");
            } finally {
                helper.getLevel().getServer().setPvpAllowed(pvp);
            }
            // sneaking without a lead, and a lead without sneaking, are not consent
            target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(stack.getDamageValue() == 0, "sneaking alone was consent");
            target.setShiftKeyDown(false);
            target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
            item.interactLivingEntity(stack, user, target, InteractionHand.MAIN_HAND);
            helper.assertTrue(stack.getDamageValue() == 0, "a lead alone was consent");
        } finally {
            LineTestPlayer.useRealHarm(false);
            restore(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyRefusalOfTheClawsSaysTheSameWordsSoNothingIsLearnedFromTheWayItFails(GameTestHelper helper) {
        LineTestPlayer user = LineTestPlayer.create(helper);
        LineTestPlayer plain = LineTestPlayer.create(helper);
        LineTestPlayer rich = LineTestPlayer.create(helper);
        BloodlineAccess.set(rich, BloodlineAccess.get(rich).toBuilder().species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.ORIGINAL).doppelgangerGene(true).doppelgangerRolled(true).build());
        try {
            LineTestPlayer.useRealHarm(true);
            ItemStack stack = claws();
            var item = (net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem) stack.getItem();
            item.interactLivingEntity(stack, user, plain, InteractionHand.MAIN_HAND);
            item.interactLivingEntity(stack, user, rich, InteractionHand.MAIN_HAND);
            helper.assertTrue(user.count("item.supernaturalbloodlines.memory_claws.refused") == 2 && user.received() == 2, "a refusal says different things for different players: " + user.heard());
        } finally {
            LineTestPlayer.useRealHarm(false);
            BloodlineAccess.set(rich, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- the structure ----

    private static boolean houseData(GameTestHelper helper) {
        return werewolves() && net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("bosses");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHouseIsInTheRegistriesWithItsPoolItsSetItsBiomesAndItsTemplate(GameTestHelper helper) {
        if (!houseData(helper)) {
            helper.succeed();
            return;
        }
        var access = helper.getLevel().registryAccess();
        var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "hale_house");
        var structure = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOptional(key);
        helper.assertTrue(structure.isPresent() && structure.get() instanceof net.ofatech.supernaturalbloodlines.parts.bosses.world.HaleHouse, "the structure is not a loaded Hale house: " + structure);
        helper.assertTrue(access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).containsKey(key), "its structure set is not loaded");
        helper.assertTrue(access.registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL).containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "hale_house/start")), "its pool is not loaded");
        var holder = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, key));
        helper.assertTrue(holder.is(BossDirector.HALE_HOUSE), "the structure is not in the hale_house tag the director looks for");
        var template = helper.getLevel().getStructureManager().get(key);
        helper.assertTrue(template.isPresent() && template.get().getSize().equals(new net.minecraft.core.Vec3i(11, 13, 9)), "the template is missing or the wrong size: " + template.map(t -> t.getSize()));
        var forests = access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrCreateTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "hale_house_biomes")));
        helper.assertTrue(forests.size() > 0 && forests.stream().anyMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.FOREST)) && forests.stream().anyMatch(b -> b.is(net.minecraft.world.level.biome.Biomes.TAIGA)),
                "the house's biomes are not forests and taigas: " + forests.size());
        var set = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).get(key);
        helper.assertTrue(set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement r && r.spacing() >= 32 && r.separation() < r.spacing(),
                "the houses are not far apart: " + set.placement());
        helper.succeed();
    }

    private static Optional<net.minecraft.world.level.levelgen.structure.Structure.GenerationStub> generate(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var structure = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "hale_house"));
        var chunkSource = level.getChunkSource();
        var context = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(access, chunkSource.getGenerator(), chunkSource.getGenerator().getBiomeSource(),
                chunkSource.randomState(), level.getStructureManager(), level.getSeed(), new net.minecraft.world.level.ChunkPos(helper.absolutePos(BlockPos.ZERO)), level, b -> true);
        return structure.findValidGenerationPoint(context);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHouseGeneratesOneTemplatePieceWhereTheSettingsLetItAndNothingWhereTheyDoNot(GameTestHelper helper) {
        if (!houseData(helper)) {
            helper.succeed();
            return;
        }
        try {
            BossesConfig.override("peter_house_enabled", true);
            BossesConfig.override("peter_house_chance", 1.0);
            var stub = generate(helper);
            helper.assertTrue(stub.isPresent(), "a chance of 1 did not generate a house");
            var pieces = stub.get().getPiecesBuilder().build().pieces();
            helper.assertTrue(pieces.size() == 1, "the house is not one piece: " + pieces.size());
            BoundingBox box = pieces.get(0).getBoundingBox();
            var chunkSource = helper.getLevel().getChunkSource();
            int ground = chunkSource.getGenerator().getFirstFreeHeight(box.getCenter().getX(), box.getCenter().getZ(), net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, helper.getLevel(), chunkSource.randomState());
            // the first free height is the air above the ground: the floor layer replaces the top block of the ground, so Peter stands on it at that height
            helper.assertTrue(box.minY() + PeterRules.FLOOR_LAYER == ground - 1, "the floor layer is not the top block of the ground: minY " + box.minY() + ", first free height " + ground);
            helper.assertTrue(box.getYSpan() == 13 && Math.min(box.getXSpan(), box.getZSpan()) == 9 && Math.max(box.getXSpan(), box.getZSpan()) == 11, "the piece is " + box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan());
            BossesConfig.override("peter_house_chance", 0.0);
            helper.assertTrue(generate(helper).isEmpty(), "a chance of 0 generated a house");
            BossesConfig.override("peter_house_chance", 1.0);
            BossesConfig.override("peter_house_enabled", false);
            helper.assertTrue(generate(helper).isEmpty(), "a house generated with the switch off");
        } finally {
            BossesConfig.override("peter_house_enabled", null);
            BossesConfig.override("peter_house_chance", null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlacedHouseIsFoundBySomebodyNearItAndNotBySomebodyFarFromIt(GameTestHelper helper) {
        if (!houseData(helper)) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "hale_house"));
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(key.location());
        var touched = new java.util.ArrayList<net.minecraft.world.level.chunk.ChunkAccess>();
        try {
            BossesConfig.override("peter_house_enabled", true);
            BossesConfig.override("peter_house_chance", 1.0);
            var stub = generate(helper).orElseThrow();
            var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, new net.minecraft.world.level.ChunkPos(helper.absolutePos(BlockPos.ZERO)), 0, stub.getPiecesBuilder().build());
            var chunk = level.getChunk(helper.absolutePos(BlockPos.ZERO));
            touched.add(chunk);
            chunk.setStartForStructure(structure, start);
            BoundingBox box = start.getBoundingBox();
            long startChunk = new net.minecraft.world.level.ChunkPos(helper.absolutePos(BlockPos.ZERO)).toLong();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    var c = level.getChunk(cx, cz);
                    touched.add(c);
                    c.addReferenceForStructure(structure, startChunk);
                }
            }
            BlockPos inside = new BlockPos(box.getCenter().getX(), box.getCenter().getY(), box.getCenter().getZ());
            var found = BossDirector.nearestHouse(level, inside.offset(10, 0, 0));
            helper.assertTrue(found.isPresent() && found.get().equals(box), "a player 10 blocks from a house did not find it: " + found);
            helper.assertTrue(BossDirector.nearestHouse(level, inside.offset(60, 0, 0)).isEmpty(), "a player 60 blocks from the house found it");
        } finally {
            // put the chunks back as they were: a null start left in the map crashes the next chunk that is generated around it
            for (var c : touched) {
                var starts = new java.util.HashMap<>(c.getAllStarts());
                starts.remove(structure);
                c.setAllStarts(starts);
                var refs = new java.util.HashMap<>(c.getAllReferences());
                refs.remove(structure);
                c.setAllReferences(refs);
            }
            BossesConfig.override("peter_house_enabled", null);
            BossesConfig.override("peter_house_chance", null);
        }
        helper.succeed();
    }
}
