package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.huntNone;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.kill;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.marked;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.reset;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.state;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.HunterMarkGameTests.vampire;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterInstinct;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterRules;

/** Hunters of the Five: instinct (R84), the vampire sense and the compulsion immunity. */
@PrefixGameTestTemplate(false)
public final class HunterInstinctGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long IDLE = 72000;

    private HunterInstinctGameTests() {}

    private static ServerPlayer idleHunter(GameTestHelper helper, long lastKill) {
        ServerPlayer p = marked(helper, 0);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().lastVampireKill(lastKill).build());
        return p;
    }

    private static boolean weakened(ServerPlayer p) {
        return p.hasEffect(MobEffects.WEAKNESS);
    }

    private static Mob vampireMob(GameTestHelper helper, Vec3 at) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire"));
        Mob mob = (Mob) type.create(helper.getLevel());
        mob.setNoAi(true);
        mob.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void instinctWeakensIdleHunter(GameTestHelper helper) {
        ServerPlayer hunter = idleHunter(helper, 1000);
        ServerPlayer potential = human(helper);
        ServerPlayer plain = huntNone(helper);
        try {
            BloodlineAccess.set(potential, BloodlineState.NONE.toBuilder().species(Species.POTENTIAL).build());
            online(hunter, potential, plain);
            HunterInstinct.instinct(hunter, 1000 + IDLE);
            helper.assertTrue(!weakened(hunter), "weakened after exactly 3 days");
            HunterInstinct.instinct(hunter, 1000 + IDLE + 1);
            MobEffectInstance e = hunter.getEffect(MobEffects.WEAKNESS);
            helper.assertTrue(e != null && e.getAmplifier() == 0 && e.getDuration() == 1300, "effect is " + e);
            HunterInstinct.instinct(potential, 1000 + 10 * IDLE);
            HunterInstinct.instinct(plain, 1000 + 10 * IDLE);
            helper.assertTrue(!weakened(potential) && !weakened(plain), "a potential or an unmarked hunter was weakened");
        } finally {
            hunter.removeAllEffects();
            HunterMarkGameTests.clearHooks();
            reset(hunter, potential, plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void instinctResetByKill(GameTestHelper helper) {
        ServerPlayer hunter = idleHunter(helper, 0);
        ServerPlayer victim = vampire(helper);
        try {
            HunterInstinct.instinct(hunter, 80000);
            helper.assertTrue(weakened(hunter), "an idle hunter was not weakened");
            hunter.removeAllEffects();
            kill(hunter, victim);
            long killed = BloodlineAccess.get(hunter).lastVampireKill();
            HunterInstinct.instinct(hunter, killed + IDLE);
            helper.assertTrue(!weakened(hunter), "weakened within 3 days of a kill");
            HunterInstinct.instinct(hunter, killed + IDLE + 1);
            helper.assertTrue(weakened(hunter), "not weakened 3 days after the kill");
        } finally {
            hunter.removeAllEffects();
            reset(hunter, victim);
        }
        helper.succeed();
    }

    /** A newly marked hunter starts the clock: no weakness the moment the mark is gained. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void instinctDoesNotHitNewHunter(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        try {
            HunterInstinct.instinct(hunter, hunter.level().getGameTime() + 1200);
            helper.assertTrue(!weakened(hunter), "a hunter with a fresh mark was weakened");
        } finally {
            reset(hunter);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void instinctSkippedWithVampireTeammate(GameTestHelper helper) {
        ServerPlayer hunter = idleHunter(helper, 0);
        ServerPlayer friend = vampire(helper);
        ServerPlayer human = human(helper);
        Scoreboard board = helper.getLevel().getScoreboard();
        PlayerTeam team = board.addPlayerTeam("hunt_" + hunter.getUUID().toString().substring(0, 8));
        try {
            online(hunter, friend, human);
            board.addPlayerToTeam(hunter.getScoreboardName(), team);
            board.addPlayerToTeam(human.getScoreboardName(), team);
            HunterInstinct.instinct(hunter, 80000);
            helper.assertTrue(weakened(hunter), "a human teammate shielded the hunter");
            hunter.removeAllEffects();
            board.addPlayerToTeam(friend.getScoreboardName(), team);
            HunterInstinct.instinct(hunter, 80000);
            helper.assertTrue(!weakened(hunter), "weakened while on a team with a vampire");
            HunterMarkGameTests.clearHooks();
            online(hunter, human);
            HunterInstinct.instinct(hunter, 80000);
            helper.assertTrue(weakened(hunter), "an offline vampire teammate still shielded the hunter");
        } finally {
            hunter.removeAllEffects();
            board.removePlayerTeam(team);
            HunterMarkGameTests.clearHooks();
            reset(hunter, friend, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void instinctRunsEvery1200Ticks(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer hunter = idleHunter(helper, -IDLE * 2);
        try {
            online(hunter);
            HunterInstinct.tick(server, 1199);
            HunterInstinct.tick(server, 1201);
            helper.assertTrue(!weakened(hunter), "instinct ran off its cadence");
            HunterInstinct.tick(server, 1200);
            helper.assertTrue(weakened(hunter), "instinct did not run at tick 1200");
        } finally {
            hunter.removeAllEffects();
            HunterMarkGameTests.clearHooks();
            reset(hunter);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hunterImmuneToCompulsion(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 0);
        ServerPlayer potential = human(helper);
        ServerPlayer plain = huntNone(helper);
        ServerPlayer vampire = vampire(helper);
        try {
            BloodlineAccess.set(potential, state(Species.POTENTIAL, 0));
            helper.assertTrue(HunterRules.immuneToCompulsion(hunter), "a hunter of the Five is not immune");
            helper.assertTrue(HunterRules.immuneToCompulsion(potential), "a potential is not immune");
            helper.assertTrue(!HunterRules.immuneToCompulsion(plain), "an unmarked hunter is immune");
            helper.assertTrue(!HunterRules.immuneToCompulsion(vampire), "a vampire is immune");
        } finally {
            reset(hunter, potential, plain, vampire);
        }
        helper.succeed();
    }

    private static String describe(ServerPlayer hunter, Mob... mobs) {
        StringBuilder b = new StringBuilder("hunter at ").append(hunter.position());
        for (Mob m : mobs) {
            b.append("; ").append(m.getType().builtInRegistryHolder().key().location()).append('#').append(m.getId())
                    .append(" alive=").append(m.isAlive()).append(" at ").append(m.position())
                    .append(" distance=").append(m.distanceTo(hunter)).append(" prey=").append(HunterInstinct.senses(hunter, m));
        }
        return b.toString();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void senseSendsPreyInRange(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer hunter = marked(helper, 5);
        ServerPlayer young = marked(helper, 4);
        Vec3 home = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        hunter.setPos(home);
        young.setPos(home);
        // The range is shrunk to 8 so every mob stands inside the test structure: at the real 24 blocks the edge and far mobs
        // were outside it (unloaded chunks, no floor), which made this test flaky.
        Mob near = vampireMob(helper, home.add(3, 0, 0));
        Mob edge = vampireMob(helper, home.add(7.5, 0, 0));
        Mob far = vampireMob(helper, home.add(10, 0, 0));
        Mob cow = (Mob) EntityType.COW.create(helper.getLevel());
        cow.moveTo(home.x + 3, home.y, home.z);
        helper.getLevel().addFreshEntity(cow);
        List<String> sent = new ArrayList<>();
        try {
            online(hunter, young);
            HunterInstinct.overrideRange(8.0);
            GlowPayload.overrideSpy((p, ids) -> sent.add(p.getUUID() + ":" + ids.stream().sorted().toList()));
            HunterInstinct.tick(server, 41);
            helper.assertTrue(sent.isEmpty(), "sent off its 40 tick cadence");
            HunterInstinct.tick(server, 40);
            String expected = hunter.getUUID() + ":" + List.of(near.getId(), edge.getId()).stream().sorted().toList();
            helper.assertTrue(sent.size() == 1 && sent.get(0).equals(expected),
                    "sent " + sent + ", expected [" + expected + "] (the 4 kill hunter gets nothing); " + describe(hunter, near, edge, far, cow));
        } finally {
            HunterInstinct.overrideRange(null);
            GlowPayload.overrideSpy(null);
            HunterMarkGameTests.clearHooks();
            near.discard();
            edge.discard();
            far.discard();
            cow.discard();
            reset(hunter, young);
        }
        helper.succeed();
    }

    /** The sense filter is R81 (no witches or siphoners, whatever their faction) plus range, living and no spectators. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void senseSkipsWitchesSiphonersAndSpectators(GameTestHelper helper) {
        ServerPlayer hunter = marked(helper, 5);
        ServerPlayer plain = vampire(helper);
        ServerPlayer witch = vampire(helper);
        ServerPlayer siphoner = vampire(helper);
        ServerPlayer spectator = vampire(helper);
        ServerPlayer far = vampire(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
            BloodlineAccess.set(siphoner, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).build());
            spectator.setGameMode(GameType.SPECTATOR);
            far.setPos(hunter.getX() + 30, hunter.getY(), hunter.getZ());
            helper.assertTrue(HunterInstinct.senses(hunter, plain), "a plain vampire is not sensed");
            helper.assertTrue(!HunterInstinct.senses(hunter, witch), "a witch is sensed");
            helper.assertTrue(!HunterInstinct.senses(hunter, siphoner), "a siphoner is sensed");
            helper.assertTrue(!HunterInstinct.senses(hunter, spectator), "a spectator is sensed");
            helper.assertTrue(!HunterInstinct.senses(hunter, far), "a vampire out of range is sensed");
            helper.assertTrue(!HunterInstinct.senses(hunter, hunter), "the hunter senses themself");
            helper.assertTrue(!HunterInstinct.senses(hunter, human(helper)), "a human is sensed");
        } finally {
            spectator.setGameMode(GameType.SURVIVAL);
            reset(hunter, plain, witch, siphoner, spectator, far);
        }
        helper.succeed();
    }
}
