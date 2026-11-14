package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/** Turning the show's way (R20c, R20d): vampire blood, death, human blood. Needs no Werewolves. */
@PrefixGameTestTemplate(false)
public final class TurningGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TurningGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    /** A Ghoul as Vampirism's own route leaves one: in the vampire faction at level 1, with the flag. */
    private static ServerPlayer ghoul(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        return p;
    }

    private static void reset(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static boolean click(ServerPlayer p) {
        var click = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(click);
        return click.isCanceled() && click.getCancellationResult().consumesAction();
    }

    private static void killByAnything(GameTestHelper helper, ServerPlayer p) {
        p.hurt(helper.getLevel().damageSources().generic(), 1000f);
    }

    private static BloodlineState turning(long left) {
        return BloodlineState.NONE.toBuilder().turningTicksLeft((int) left).build();
    }

    /** Vampirism's own bite entry point: it posts the PlayerDrinkBloodEvent on the NeoForge bus. */
    private static void drinkFrom(ServerPlayer drinker, LivingEntity source) {
        VampirismAPI.vampirePlayer(drinker).drinkBlood(1, 0.1f, new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.of(source);
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        });
    }

    private static LivingEntity villager(GameTestHelper helper) {
        return EntityType.VILLAGER.create(helper.getLevel());
    }

    private static void assertVampireLevelOne(GameTestHelper helper, ServerPlayer p) {
        helper.assertTrue(handler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION && handler(p).getCurrentLevel() == 1,
                "not a level 1 vampire: " + handler(p).getCurrentFaction() + " " + handler(p).getCurrentLevel());
    }

    // ---- vampire blood ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireBloodItemMarksPlayer(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ServerPlayer viaFinish = TestPlayers.createMortal(helper);
        try {
            ItemStack bottle = new ItemStack(item("vampirism:vampire_blood_bottle"));
            helper.assertTrue(bottle.is(TurningHandler.VAMPIRE_BLOOD), "Vampirism's vampire blood bottle is not tagged vampire_blood");
            long now = helper.getLevel().getGameTime();
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle.copy());
            helper.assertTrue(click(p), "the click was not taken over");
            helper.assertTrue(BloodlineAccess.get(p).vampireBloodUntil() == now + SpeciesConfig.vampireBloodTicks(),
                    "vampire blood runs until " + BloodlineAccess.get(p).vampireBloodUntil());
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
            // an item that is a real drink goes through the finish event
            NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(viaFinish, bottle, 0, bottle));
            helper.assertTrue(BloodlineAccess.get(viaFinish).vampireBloodUntil() == now + SpeciesConfig.vampireBloodTicks(),
                    "the finish event did not mark");
        } finally {
            reset(p, viaFinish);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireBloodDoesNothingForAVampire(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("vampirism:vampire_blood_bottle")));
        helper.assertTrue(!click(p), "a vampire's click was taken over");
        helper.assertTrue(p.getMainHandItem().is(item("vampirism:vampire_blood_bottle")), "the bottle was used up for nothing");
        helper.assertTrue(BloodlineAccess.get(p).vampireBloodUntil() == 0, "a vampire was marked");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void drinkingFromVampireMarksPlayer(GameTestHelper helper) {
        ServerPlayer ghoul = ghoul(helper);
        ServerPlayer plain = vampire(helper);
        ServerPlayer source = vampire(helper);
        try {
            long now = helper.getLevel().getGameTime();
            drinkFrom(ghoul, source);
            helper.assertTrue(BloodlineAccess.get(ghoul).vampireBloodUntil() == now + SpeciesConfig.vampireBloodTicks(),
                    "a Ghoul who drank from a vampire is not marked: " + BloodlineAccess.get(ghoul));
            drinkFrom(plain, source);
            helper.assertTrue(BloodlineAccess.get(plain).vampireBloodUntil() == 0, "an existing vampire was marked");
        } finally {
            reset(ghoul, plain, source);
        }
        helper.succeed();
    }

    // ---- death ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deathWithVampireBloodStartsTurning(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 1000).build());
            killByAnything(helper, p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.inTurning() && s.turningTicksLeft() == SpeciesConfig.transitionTicks(), "not in the turning: " + s);
            helper.assertTrue(s.vampireBloodUntil() == 0 && s.species() == Species.NONE && !s.inTransition(), "state is " + s);
            helper.assertTrue(handler(p).getCurrentFaction() == null, "joined a faction before drinking");
            TurningHandler.secondPassed(p);
            helper.assertTrue(BloodlineAccess.get(p).turningTicksLeft() == SpeciesConfig.transitionTicks() - 20,
                    "the clock reads " + BloodlineAccess.get(p).turningTicksLeft());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void expiredVampireBloodNoTransition(GameTestHelper helper) {
        ServerPlayer expired = TestPlayers.createMortal(helper);
        ServerPlayer never = TestPlayers.createMortal(helper);
        ServerPlayer failed = TestPlayers.createMortal(helper);
        try {
            long now = helper.getLevel().getGameTime();
            BloodlineAccess.set(expired, BloodlineState.NONE.toBuilder().vampireBloodUntil(Math.max(1, now)).build());
            killByAnything(helper, expired);
            helper.assertTrue(!BloodlineAccess.get(expired).inTurning(), "expired vampire blood started a turning");
            killByAnything(helper, never);
            helper.assertTrue(!BloodlineAccess.get(never).inTurning(), "a death without vampire blood started a turning");
            BloodlineAccess.set(failed, BloodlineState.NONE.toBuilder().vampireBloodUntil(now + 1000).build());
            failed.hurt(failed.damageSources().source(OutcomeExecutor.FAILED_TRANSITION), 1000f);
            helper.assertTrue(!BloodlineAccess.get(failed).inTurning(), "a failed transition started a turning");
        } finally {
            reset(expired, never, failed);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireBloodDuringHybridTransitionIgnored(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            BloodlineState inTransition = BloodlineState.NONE.toBuilder().inTransition(true).transitionTicksLeft(5000)
                    .vampireBloodUntil(helper.getLevel().getGameTime() + 1000).build();
            BloodlineAccess.set(p, inTransition);
            killByAnything(helper, p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.inTurning() && s.inTransition() && s.transitionTicksLeft() == 5000, "the transition changed: " + s);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    // ---- human blood ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningFedHumanBloodMakesVampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(p, turning(9000));
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("vampirism:blood_bottle")));
            helper.assertTrue(click(p), "the blood bottle was not taken over");
            assertVampireLevelOne(helper, p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.inTurning() && !s.ghoul() && s.species() == Species.NONE, "state is " + s);
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodBottleOutsideTheTurningIsLeftAlone(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("vampirism:blood_bottle")));
        helper.assertTrue(!click(p), "a blood bottle was taken over outside the turning");
        helper.assertTrue(handler(p).getCurrentFaction() == null, "joined a faction");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulCompletesTurning(GameTestHelper helper) {
        ServerPlayer p = ghoul(helper);
        ServerPlayer source = vampire(helper);
        try {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().turningTicksLeft(9000).build());
            drinkFrom(p, source);
            helper.assertTrue(BloodlineAccess.get(p).inTurning() && BloodlineAccess.get(p).ghoul(), "vampire blood fed the turning");
            drinkFrom(p, EntityType.COW.create(helper.getLevel()));
            helper.assertTrue(BloodlineAccess.get(p).inTurning(), "a cow's blood fed the turning");
            drinkFrom(p, villager(helper));
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.ghoul() && !s.inTurning(), "the turning did not complete: " + s);
            assertVampireLevelOne(helper, p);
        } finally {
            reset(p, source);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanLikeBloodIsPlayersAndVillagers(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer vampire = vampire(helper);
        helper.assertTrue(TurningHandler.isHumanLikeBlood(human), "a human player is not human-like blood");
        helper.assertTrue(!TurningHandler.isHumanLikeBlood(vampire), "a vampire is human-like blood");
        helper.assertTrue(TurningHandler.isHumanLikeBlood(villager(helper)), "a villager is not human-like blood");
        helper.assertTrue(TurningHandler.isHumanLikeBlood(EntityType.WANDERING_TRADER.create(helper.getLevel())), "a trader is not");
        helper.assertTrue(!TurningHandler.isHumanLikeBlood(EntityType.COW.create(helper.getLevel())), "a cow is human-like blood");
        helper.assertTrue(!TurningHandler.isHumanLikeBlood(EntityType.ZOMBIE.create(helper.getLevel())), "a zombie is human-like blood");
        helper.succeed();
    }

    // ---- failure ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningNotFedDiesAndStaysHuman(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(p, turning(40));
            TurningHandler.secondPassed(p);
            helper.assertTrue(!p.isDeadOrDying() && BloodlineAccess.get(p).turningTicksLeft() == 20, "ended early: " + BloodlineAccess.get(p));
            TurningHandler.secondPassed(p);
            helper.assertTrue(p.isDeadOrDying(), "survived the failed turning");
            helper.assertTrue(p.getLastDamageSource() != null && p.getLastDamageSource().is(OutcomeExecutor.FAILED_TRANSITION),
                    "died of " + p.getLastDamageSource());
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.inTurning() && !s.ghoul() && s.species() == Species.NONE && s.vampireBloodUntil() == 0, "state is " + s);
            helper.assertTrue(handler(p).getCurrentFaction() == null, "became a vampire anyway");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulStaysGhoulWhenTurningFails(GameTestHelper helper) {
        ServerPlayer p = ghoul(helper);
        try {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().turningTicksLeft(20).build());
            TurningHandler.secondPassed(p);
            helper.assertTrue(p.isDeadOrDying(), "the Ghoul survived the failed turning");
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.ghoul() && !s.inTurning(), "state is " + s);
            assertVampireLevelOne(helper, p);
        } finally {
            reset(p);
        }
        helper.succeed();
    }
}
