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
            helper.assertTrue(!p.hurt(zombieBlow(helper), 1000f) && !p.isDeadOrDying() && Downed.isDowned(p), "a second blow killed a dying player");
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
        ServerPlayer p = TestPlayers.createMortal(helper);
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
}
