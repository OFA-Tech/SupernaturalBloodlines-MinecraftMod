package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodDrink;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.DownedPayload;
import net.ofatech.supernaturalbloodlines.parts.species.turning.PendingSire;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/** Dying with vampire blood in you (plan 18b, R209): the lethal blow stands you up where you fell, for the 30 s of the dying, with the turning running after. */
@PrefixGameTestTemplate(false)
public final class TurningDyingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final UUID MIKAEL = NpcOriginal.MIKAEL.uuid();

    private TurningDyingGameTests() {
    }

    private static ServerPlayer marked(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 100000).build());
        return p;
    }

    private static void reset(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Downed.clearForTest(p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static DamageSource zombieBlow(GameTestHelper helper) {
        return helper.getLevel().damageSources().mobAttack(EntityType.ZOMBIE.create(helper.getLevel()));
    }

    private static void lethal(GameTestHelper helper, ServerPlayer p) {
        p.invulnerableTime = 0; // the second blow of a test would otherwise be swallowed by the hurt cooldown
        p.hurt(zombieBlow(helper), 1000f);
    }

    private static void kill(GameTestHelper helper, ServerPlayer p) {
        p.invulnerableTime = 0;
        p.hurt(helper.getLevel().damageSources().genericKill(), 1000f);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static boolean click(ServerPlayer p) {
        var click = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(click);
        return click.isCanceled() && click.getCancellationResult().consumesAction();
    }

    private static void wake(ServerPlayer p) {
        for (int i = 0; i < Downed.TICKS; i++) {
            Downed.tick(p);
        }
    }

    private static void assertDying(GameTestHelper helper, ServerPlayer p, String what) {
        helper.assertTrue(!p.isDeadOrDying() && Downed.isDowned(p) && Downed.isDyingForTurning(p), what + ": not dying, health " + p.getHealth() + " " + BloodlineAccess.get(p));
        helper.assertTrue(BloodlineAccess.get(p).inTurning(), what + ": the turning did not start: " + BloodlineAccess.get(p));
    }

    // ---- the stand-up ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lethalBlowWithVampireBloodStandsYouUpWhereYouFell(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            Vec3 where = p.position();
            lethal(helper, p);
            assertDying(helper, p, "a lethal blow");
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(p.position().equals(where) && p.getHealth() == 1.0f, "they were moved or healed: " + p.position() + " " + p.getHealth());
            helper.assertTrue(s.vampireBloodUntil() == 0 && s.turningTicksLeft() == SpeciesConfig.transitionTicks(), "state " + s);
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == null, "joined a faction before drinking");
            helper.assertTrue(Downed.mode(p) == Downed.MODE_TURNING && p.getData(SpeciesRegistries.DOWNED) == Downed.TICKS && Downed.TICKS == 600, "the dying is not 30 s of the turning");
            helper.assertTrue(((TestPlayers.TalkingDuelist) p).received("supernaturalbloodlines.turning.dying"), "not told");
            helper.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "they can still move");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moreDamageWhileDyingChangesNothing(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            // other kinds of damage, with the hurt cooldown out of the way: none gets through, and health stays at 1
            for (DamageSource other : new DamageSource[] {helper.getLevel().damageSources().magic(), helper.getLevel().damageSources().explosion(null, null),
                    helper.getLevel().damageSources().lava(), zombieBlow(helper), helper.getLevel().damageSources().drown()}) {
                p.invulnerableTime = 0;
                p.hurt(other, 1000f);
                helper.assertTrue(!p.isDeadOrDying() && Downed.isDowned(p) && p.getHealth() == 1.0f, other.type().msgId() + " got through to a dying player: health " + p.getHealth());
            }
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theScreenIsToldTheModeAndTheCodecKeepsIt(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            DownedPayload payload = Downed.payload(p, true);
            helper.assertTrue(payload.downed() && payload.mode() == Downed.MODE_TURNING, "the payload is " + payload);
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            DownedPayload.STREAM_CODEC.encode(buf, payload);
            helper.assertTrue(DownedPayload.STREAM_CODEC.decode(buf).equals(payload), "the codec lost the mode");
            Downed.clearForTest(p);
            helper.assertTrue(Downed.payload(p, false).mode() == Downed.MODE_DYING, "the mode outlived the dying");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireOrAHumanWithoutBloodIsNotStoodUp(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer vampire = TestPlayers.createMortal(helper);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
            BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 1000).build());
            lethal(helper, human);
            helper.assertTrue(human.isDeadOrDying() && !Downed.isDowned(human), "a human without vampire blood was stood up");
        } finally {
            VampirismAPI.factionPlayerHandler(vampire).leaveFaction(false);
            reset(human, vampire);
        }
        helper.succeed();
    }

    // ---- killing it, for real ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void voidAndKillCommandStillKillAndTheyRespawnInTheTurning(GameTestHelper helper) {
        for (boolean viaVoid : new boolean[] {true, false}) {
            ServerPlayer p = marked(helper);
            try {
                DamageSource source = viaVoid ? helper.getLevel().damageSources().fellOutOfWorld() : helper.getLevel().damageSources().genericKill();
                p.invulnerableTime = 0;
                p.hurt(source, 1000f);
                helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), (viaVoid ? "void" : "/kill") + " did not kill, or left a dying state: " + Downed.isDowned(p));
                helper.assertTrue(BloodlineAccess.get(p).inTurning(), "the turning is gone after a real death: " + BloodlineAccess.get(p));
            } finally {
                reset(p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killingADyingPlayerForRealEndsTheDyingState(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            assertDying(helper, p, "setup");
            kill(helper, p);
            helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), "a /kill of a dying player left the state, or did not kill: " + Downed.isDowned(p));
            helper.assertTrue(BloodlineAccess.get(p).inTurning(), "a real death of the dying ended the turning");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void giveUpEndsTheTurningAndKillsWithTheOriginalBlow(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        DamageSource blow = zombieBlow(helper);
        DamageSource[] died = new DamageSource[1];
        Consumer<LivingDeathEvent> spy = e -> {
            if (e.getEntity() == p) {
                died[0] = e.getSource();
            }
        };
        try {
            BloodDrink.drank((ServerPlayer) p, MIKAEL, "ORIGINAL", false);
            p.hurt(blow, 1000f);
            assertDying(helper, p, "setup");
            helper.assertTrue(BloodlineAccess.get(p).pendingSire() != null || MIKAEL.equals(p.getData(SpeciesRegistries.BLOOD_SOURCE)), "setup: no sire in the blood");
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingDeathEvent.class, spy);
            Downed.giveUp(p);
            helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), "Give up did not kill, or left the state");
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.inTurning() && s.vampireBloodUntil() == 0 && s.pendingSire() == null && s.turningTicksLeft() == 0, "the turning is not over: " + s);
            helper.assertTrue(died[0] != null && died[0].type() == blow.type() && died[0].getEntity() == blow.getEntity(), "it was not the original blow: " + died[0]);
            ServerPlayer reborn = TestPlayers.createMortal(helper);
            reborn.copyAttachmentsFrom(p, true);
            helper.assertTrue(!BloodlineAccess.get(reborn).inTurning() && !Downed.isDowned(reborn) && BloodlineAccess.get(reborn).sire() == null, "a respawn is not a plain human: " + BloodlineAccess.get(reborn));
        } finally {
            NeoForge.EVENT_BUS.unregister(spy);
            reset(p);
        }
        helper.succeed();
    }

    // ---- waiting ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void waitWakesAtHalfHealthInTheSamePlaceWithTheTurningRunning(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            Vec3 where = p.position();
            for (int i = 0; i < Downed.TICKS - 1; i++) {
                Downed.tick(p);
            }
            helper.assertTrue(Downed.isDowned(p), "woke early");
            Downed.tick(p);
            helper.assertTrue(!Downed.isDowned(p) && !Downed.isDyingForTurning(p) && Downed.mode(p) == Downed.MODE_DYING, "still dying after the 30 s");
            helper.assertTrue(p.getHealth() == p.getMaxHealth() / 2.0f && p.position().equals(where), "health " + p.getHealth() + " at " + p.position());
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.inTurning() && s.turningTicksLeft() == SpeciesConfig.transitionTicks(), "the turning or its clock changed: " + s);
            helper.assertTrue(((TestPlayers.TalkingDuelist) p).receivedOnActionBar("supernaturalbloodlines.turning.woke"), "no wake line");
            helper.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "they cannot move after waking");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClockStandsStillWhileDyingAndRunsAfter(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            int before = BloodlineAccess.get(p).turningTicksLeft();
            for (int i = 0; i < 40; i++) {
                TurningHandler.secondPassed(p);
            }
            helper.assertTrue(BloodlineAccess.get(p).turningTicksLeft() == before, "the clock ran while dying: " + BloodlineAccess.get(p).turningTicksLeft());
            wake(p);
            TurningHandler.secondPassed(p);
            helper.assertTrue(BloodlineAccess.get(p).turningTicksLeft() == before - 20, "the clock did not run after waking: " + BloodlineAccess.get(p).turningTicksLeft());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void relogWhileDyingResumes(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        ServerPlayer reloaded = TestPlayers.createMortal(helper);
        try {
            lethal(helper, p);
            for (int i = 0; i < 200; i++) {
                Downed.tick(p);
            }
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            reloaded.load(saved);
            helper.assertTrue(Downed.isDyingForTurning(reloaded) && reloaded.getData(SpeciesRegistries.DOWNED) == Downed.TICKS - 200, "the dying was lost on relog: " + reloaded.getData(SpeciesRegistries.DOWNED) + " mode " + Downed.mode(reloaded));
            helper.assertTrue(BloodlineAccess.get(reloaded).inTurning(), "the turning was lost on relog");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(reloaded));
            helper.assertTrue(reloaded.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "the relogged player can move");
            wake(reloaded);
            helper.assertTrue(!Downed.isDowned(reloaded) && BloodlineAccess.get(reloaded).inTurning(), "the relogged player did not wake into the turning");
        } finally {
            reset(p, reloaded);
        }
        helper.succeed();
    }

    // ---- what they can do while dying, and after ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void drinkingBloodWhileDyingIsRefusedAndTheBottleKept(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            for (String bottle : new String[] {"vampirism:blood_bottle", "vampirism:vampire_blood_bottle"}) {
                ItemStack held = new ItemStack(item(bottle));
                p.setItemInHand(InteractionHand.MAIN_HAND, held);
                helper.assertTrue(!click(p) && p.getMainHandItem() == held && held.getCount() == 1, bottle + " was drunk, or the click was taken over, while dying");
            }
            helper.assertTrue(BloodlineAccess.get(p).inTurning() && VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == null, "the turning completed while dying: " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void afterWakingHumanBloodCompletesTheTurningAndTheSireIsTheOneDrunk(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = (TestPlayers.TalkingDuelist) marked(helper);
        try {
            BloodDrink.drank(p, MIKAEL, "ORIGINAL", false);
            lethal(helper, p);
            helper.assertTrue(MIKAEL.equals(BloodlineAccess.get(p).pendingSire()), "the dying lost the pending sire: " + BloodlineAccess.get(p));
            wake(p);
            helper.assertTrue(MIKAEL.equals(BloodlineAccess.get(p).pendingSire()), "waking lost the pending sire: " + BloodlineAccess.get(p));
            ItemStack human = new ItemStack(item("vampirism:blood_bottle"));
            p.setItemInHand(InteractionHand.MAIN_HAND, human);
            helper.assertTrue(click(p), "the human blood was not drunk after waking");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION, "they did not join the vampires");
            helper.assertTrue(MIKAEL.equals(BloodlineAccess.get(p).sire()) && !BloodlineAccess.get(p).inTurning(), "the sire is " + BloodlineAccess.get(p).sire());
            helper.assertTrue(p.received("supernaturalbloodlines.turning.completed") && p.received(PendingSire.HIDDEN_LINE), "not told");
        } finally {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondRealDeathAfterWakingKeepsTheTurning(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            wake(p);
            p.removeAllEffects(); // the 5 s of Resistance V of the wake-up are over
            lethal(helper, p);
            helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), "a second lethal blow after waking stood them up again, or left a state");
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.inTurning() && s.turningTicksLeft() == SpeciesConfig.transitionTicks(), "the turning did not keep running: " + s);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void failingTheTurningStillKills(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            wake(p);
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().turningTicksLeft(20).build());
            TurningHandler.secondPassed(p);
            helper.assertTrue(p.isDeadOrDying() && !BloodlineAccess.get(p).inTurning(), "running out of time did not kill: " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    // ---- a Ghoul has Vampirism's own dying state ----

    /** These two are on Vampirism's implementation only, not in its API jar. */
    private static void vampirePlayerCall(ServerPlayer p, String method) {
        try {
            var vampire = VampirismAPI.vampirePlayer(p);
            vampire.getClass().getMethod(method).invoke(vampire);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ServerPlayer ghoul(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createTalkingDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).vampireBloodUntil(helper.getLevel().getGameTime() + 100000).build());
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulDbnoStartsTheTurningAndItsClockStandsStill(GameTestHelper helper) {
        ServerPlayer p = ghoul(helper);
        try {
            lethal(helper, p);
            helper.assertTrue(VampirismAPI.vampirePlayer(p).isDBNO() && BloodlineAccess.get(p).inTurning(), "a Ghoul's lethal blow did not start Vampirism's dying and the turning: dbno " + VampirismAPI.vampirePlayer(p).isDBNO() + " " + BloodlineAccess.get(p));
            int before = BloodlineAccess.get(p).turningTicksLeft();
            TurningHandler.secondPassed(p);
            helper.assertTrue(BloodlineAccess.get(p).turningTicksLeft() == before, "the clock ran during a Ghoul's dying");
        } finally {
            vampirePlayerCall(p, "tryResurrect");
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulGiveUpEndsTheTurning(GameTestHelper helper) {
        ServerPlayer p = ghoul(helper);
        try {
            lethal(helper, p);
            helper.assertTrue(VampirismAPI.vampirePlayer(p).isDBNO() && BloodlineAccess.get(p).inTurning(), "setup: no dying");
            vampirePlayerCall(p, "giveUpDBNO");
            helper.assertTrue(p.isDeadOrDying() && !BloodlineAccess.get(p).inTurning() && BloodlineAccess.get(p).vampireBloodUntil() == 0, "a Ghoul's give up did not end the turning: " + BloodlineAccess.get(p));
        } finally {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            reset(p);
        }
        helper.succeed();
    }

    // ---- review fixes (plan 18b, round 2) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void drinkingVampireBloodAgainInTheTurningBuysNoSecondLife(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = (TestPlayers.TalkingDuelist) marked(helper);
        try {
            lethal(helper, p);
            wake(p);
            p.removeAllEffects(); // the 5 s of Resistance V of the wake-up are over
            ItemStack bottle = new ItemStack(item("vampirism:vampire_blood_bottle"));
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle);
            helper.assertTrue(!click(p) && p.getMainHandItem() == bottle && bottle.getCount() == 1, "the bottle was drunk in the turning, or the click was taken over");
            helper.assertTrue(p.receivedOnActionBar("supernaturalbloodlines.bottle.turning_has_you"), "the player was not told the turning has them");
            helper.assertTrue(!TurningHandler.drankVampireBlood(p) && BloodlineAccess.get(p).vampireBloodUntil() == 0, "the rule marked a player already in the turning: " + BloodlineAccess.get(p));
            // and a bottle drunk by a route that does not go through the click (the finish event, a bite) does nothing either
            p.invulnerableTime = 0;
            lethal(helper, p);
            helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), "a lethal blow after drinking again stood them up a second time");
            helper.assertTrue(BloodlineAccess.get(p).inTurning(), "the real death ended the turning");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    /** Belt and braces for the same hole: a state that has vampire blood in it while the turning already runs (a world from before, an admin) is not caught again either. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerInTheTurningWithVampireBloodStillInThemIsNotCaughtTwice(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        try {
            lethal(helper, p);
            wake(p);
            p.removeAllEffects();
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 100000).build());
            lethal(helper, p);
            helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), "they were stood up a second time");
            helper.assertTrue(BloodlineAccess.get(p).inTurning(), "the real death ended the turning");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeathThatWasCaughtIsNotSeenByTheOnesThatCountDeaths(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        int[] seen = new int[1];
        Consumer<LivingDeathEvent> counter = e -> {
            if (e.getEntity() == p) {
                seen[0]++;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, counter);
        try {
            lethal(helper, p);
            helper.assertTrue(seen[0] == 0, "a listener that counts deaths saw a death that was caught");
            Downed.giveUp(p);
            helper.assertTrue(seen[0] == 1 && p.isDeadOrDying(), "Give up was not a death for the others: " + seen[0]);
        } finally {
            NeoForge.EVENT_BUS.unregister(counter);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wakingGivesResistanceTellsTheTurningAndPutsOutTheFire(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = (TestPlayers.TalkingDuelist) marked(helper);
        try {
            lethal(helper, p);
            p.setRemainingFireTicks(200);
            p.setAirSupply(0);
            wake(p);
            var resistance = p.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
            helper.assertTrue(resistance != null && resistance.getAmplifier() == 4 && resistance.getDuration() > 0 && resistance.getDuration() <= Downed.WAKE_RESISTANCE_TICKS, "Resistance V for 5 s: " + resistance);
            helper.assertTrue(p.getRemainingFireTicks() <= 0 && p.getAirSupply() == p.getMaxAirSupply(), "fire " + p.getRemainingFireTicks() + ", air " + p.getAirSupply());
            var chat = p.message("supernaturalbloodlines.turning.woke_chat");
            helper.assertTrue(chat != null && ((net.minecraft.network.chat.contents.TranslatableContents) chat.getContents()).getArgs()[0].equals((int) Math.ceil(SpeciesConfig.transitionTicks() / 1200.0)),
                    "no chat line with the minutes left: " + chat);
            helper.assertTrue(p.receivedOnActionBar("supernaturalbloodlines.turning.woke"), "no action bar line");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wakingInLavaMovesThemOutAndWakingInWaterDoesNot(GameTestHelper helper) {
        // the test box has a barrier ceiling two blocks up, and the eyes of a standing player are above it: open it over the floor so the line of sight is real
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new net.minecraft.core.BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.AIR);
            }
        }
        net.minecraft.core.BlockPos lava = new net.minecraft.core.BlockPos(2, 1, 2);
        net.minecraft.core.BlockPos water = new net.minecraft.core.BlockPos(7, 1, 2);
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new net.minecraft.core.BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        helper.setBlock(lava, net.minecraft.world.level.block.Blocks.LAVA);
        helper.setBlock(water, net.minecraft.world.level.block.Blocks.WATER);
        ServerPlayer burning = marked(helper);
        ServerPlayer swimming = marked(helper);
        try {
            burning.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
            swimming.setPos(helper.absoluteVec(new Vec3(7.5, 1, 2.5)));
            lethal(helper, burning);
            lethal(helper, swimming);
            burning.setRemainingFireTicks(300);
            swimming.setAirSupply(0);
            Vec3 waterAt = swimming.position();
            wake(burning);
            wake(swimming);
            net.minecraft.core.BlockPos now = burning.blockPosition();
            helper.assertTrue(burning.level().getFluidState(now).isEmpty() && burning.level().getFluidState(now.above()).isEmpty(), "they woke in the lava, at " + now + " (spot "
                    + net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel.safeSpot(helper.getLevel(), now, burning, 8) + ", pos " + burning.position() + ", feet-block " + helper.getLevel().getBlockState(now.west()) + " below " + helper.getLevel().getBlockState(now.west().below()) + ")");
            helper.assertTrue(burning.position().distanceTo(helper.absoluteVec(new Vec3(2.5, 1, 2.5))) <= 8, "they were moved further than 8 blocks");
            helper.assertTrue(burning.getRemainingFireTicks() <= 0, "still burning");
            helper.assertTrue(swimming.position().equals(waterAt) && swimming.getAirSupply() == swimming.getMaxAirSupply(), "water moved them, or no air came back: " + swimming.position() + " " + swimming.getAirSupply());
        } finally {
            helper.setBlock(lava, net.minecraft.world.level.block.Blocks.AIR);
            helper.setBlock(water, net.minecraft.world.level.block.Blocks.AIR);
            for (int x = 0; x <= 9; x++) {
                for (int z = 0; z <= 4; z++) {
                    helper.setBlock(new net.minecraft.core.BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.BARRIER);
                }
            }
            reset(burning, swimming);
        }
        helper.succeed();
    }

    /** The harness's fake players are refused by every vehicle (a mount event is cancelled for them), so the mount is put on by hand: the field and the vehicle's passenger list, as startRiding does. */
    private static void mountByForce(ServerPlayer rider, net.minecraft.world.entity.Entity vehicle) {
        try {
            var field = net.minecraft.world.entity.Entity.class.getDeclaredField("vehicle");
            field.setAccessible(true);
            field.set(rider, vehicle);
            var add = net.minecraft.world.entity.Entity.class.getDeclaredMethod("addPassenger", net.minecraft.world.entity.Entity.class);
            add.setAccessible(true);
            add.invoke(vehicle, rider);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aMountIsLeftWhenTheBlowIsCaught(GameTestHelper helper) {
        ServerPlayer rider = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(rider, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 100000).build());
            var pig = helper.spawn(EntityType.PIG, new net.minecraft.core.BlockPos(4, 1, 1));
            rider.setPos(pig.position());
            mountByForce(rider, pig);
            helper.assertTrue(rider.isPassenger() && rider.getVehicle() == pig, "setup: could not mount");
            lethal(helper, rider);
            helper.assertTrue(Downed.isDowned(rider) && !rider.isPassenger(), "they stayed on the mount");
        } finally {
            reset(rider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theVoidStillKillsADyingPlayer(GameTestHelper helper) {
        ServerPlayer dropped = marked(helper);
        try {
            // the void takes a dying player with the small blow it deals (4), not only with a huge one
            lethal(helper, dropped);
            dropped.invulnerableTime = 0;
            dropped.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 4f);
            helper.assertTrue(dropped.isDeadOrDying() && !Downed.isDowned(dropped), "the void did not kill a dying player: health " + dropped.getHealth());
        } finally {
            reset(dropped);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theScreenIsSentAgainWhileTheyWaitWithTheTimeThatIsLeft(GameTestHelper helper) {
        ServerPlayer p = marked(helper);
        java.util.List<DownedPayload> sent = new java.util.ArrayList<>();
        Downed.overrideSender((to, payload) -> {
            if (to == p) {
                sent.add(payload);
            }
        });
        try {
            lethal(helper, p);
            for (int i = 0; i < 250; i++) {
                Downed.tick(p);
            }
            java.util.List<Integer> left = sent.stream().filter(DownedPayload::downed).map(DownedPayload::ticksLeft).toList();
            helper.assertTrue(left.equals(java.util.List.of(600, 500, 400)), "the screen was sent with " + left + " (expected 600, then every 100 ticks)");
            helper.assertTrue(sent.stream().allMatch(s -> s.mode() == Downed.MODE_TURNING), "a payload had the wrong mode");
            for (int i = 0; i < 350; i++) {
                Downed.tick(p);
            }
            DownedPayload last = sent.get(sent.size() - 1);
            helper.assertTrue(!last.downed() && last.ticksLeft() == 0, "the screen was not closed when they woke: " + last);
        } finally {
            Downed.overrideSender(null);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulWhomVampirismDidNotCatchIsTakenByOurDying(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = (TestPlayers.TalkingDuelist) ghoul(helper);
        try {
            var neonatal = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("vampirism:neonatal"));
            helper.assertTrue(neonatal.isPresent(), "setup: no neonatal effect");
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(neonatal.get(), 1000)); // Vampirism does not put a neonatal vampire in its dying state
            lethal(helper, p);
            helper.assertTrue(!VampirismAPI.vampirePlayer(p).isDBNO(), "setup: Vampirism caught the blow anyway");
            assertDying(helper, p, "an uncaught Ghoul");
            helper.assertTrue(p.received("supernaturalbloodlines.turning.dying"), "no word of the 30 s");
        } finally {
            p.removeAllEffects();
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulIsToldWhenVampirismsDyingStateEnds(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = (TestPlayers.TalkingDuelist) ghoul(helper);
        try {
            lethal(helper, p);
            helper.assertTrue(VampirismAPI.vampirePlayer(p).isDBNO(), "setup: no Vampirism dying");
            helper.assertTrue(p.received("supernaturalbloodlines.turning.dying_ghoul") && !p.received("supernaturalbloodlines.turning.dying"), "the Ghoul was told a time that is Vampirism's: ");
            TurningHandler.watchGhoulDying(p);
            helper.assertTrue(!p.received("supernaturalbloodlines.turning.woke_chat"), "woke while dying");
            // its timer runs out, and then it stands them up (the way its own tick does)
            try {
                var vampire = VampirismAPI.vampirePlayer(p);
                var setTimer = vampire.getClass().getDeclaredMethod("setDBNOTimer", int.class);
                setTimer.setAccessible(true);
                setTimer.invoke(vampire, 0);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
            vampirePlayerCall(p, "tryResurrect");
            helper.assertTrue(!VampirismAPI.vampirePlayer(p).isDBNO(), "setup: the dying did not end");
            TurningHandler.watchGhoulDying(p);
            helper.assertTrue(p.received("supernaturalbloodlines.turning.woke_chat") && p.receivedOnActionBar("supernaturalbloodlines.turning.woke"), "no wake lines when the dying ended");
            TurningHandler.watchGhoulDying(p);
            helper.assertTrue(p.count("supernaturalbloodlines.turning.woke_chat") == 1, "the wake line came twice");
        } finally {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            reset(p);
        }
        helper.succeed();
    }}
