package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfAlpha;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackSkillRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.TargetPriority.Tier;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackBrain;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackNights;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import net.ofatech.supernaturalbloodlines.parts.species.npc.PackSkills;
import net.ofatech.supernaturalbloodlines.parts.species.npc.WeakSpark;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 15b: the NPC werewolf packs (R143, R144), the weaker spark (R145) and the two pack abilities (R146). Werewolves only. */
@PrefixGameTestTemplate(false)
public final class NpcPackGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    /** Day 24 is a full-moon day (24 % 8 = 0); 13000 is night. */
    private static final long FULL_NIGHT = 24000L * 24 + 13000;
    private static final long PLAIN_NIGHT = 24000L * 25 + 13000;
    /** Every test that rolls a night gets its own: the ledger of a night lives in the world, so two tests would share its count of packs. */
    private static final java.util.concurrent.atomic.AtomicLong NEXT_NIGHT = new java.util.concurrent.atomic.AtomicLong(1000);
    private static final SpeciesConfig.NpcTuning T = SpeciesConfig.NpcTuning.DEFAULT;

    private NpcPackGameTests() {
    }

    // ---- helpers ----

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, boolean protect) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).protectPath(protect).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "the test wolf could not join the werewolves");
        return p;
    }

    private static void clean(GameTestHelper helper, List<? extends Entity> mobs, ServerPlayer... players) {
        mobs.forEach(Entity::discard);
        release(helper);
        PackIndex index = PackIndex.get(helper.getLevel());
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            p.removeAllEffects();
        }
        PackSkills.reset();
        NpcPackBrain.reset();
        net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(null);
        SpeciesConfig.overrideNpc(null);
    }

    /** A full-moon night nobody used yet (a multiple of 8 days is phase 0). */
    private static long freshFullNight() {
        return 24000L * 8 * NEXT_NIGHT.incrementAndGet() + 13000;
    }

    private static java.util.function.DoubleSupplier dice(double... v) {
        int[] i = {0};
        return () -> v[Math.min(i[0]++, v.length - 1)];
    }

    private static void online(ServerPlayer... players) {
        java.util.Map<java.util.UUID, ServerPlayer> map = new java.util.HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(map::get);
    }

    private static BlockPos spot(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 1, 2));
    }

    private static List<Mob> marked(ServerLevel level, BlockPos around) {
        return level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(around).inflate(24), m -> m.hasData(SpeciesRegistries.NPC_MARK)
                && m.getData(SpeciesRegistries.NPC_MARK).kind() == NpcMark.Kind.WILD);
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new IllegalStateException("no entity " + id));
    }

    /**
     * The chunks this test holds loaded, by test. The test world loads the chunks of the structures and no others, so which chunks a test 25 blocks from its
     * own structure has depends on what stands beside it (the order and the mix of the tests of a run): an entity added to a chunk that is not loaded is
     * "added" but hidden, and nothing finds it.
     */
    private static final java.util.Map<GameTestHelper, java.util.List<net.minecraft.world.level.ChunkPos>> HELD = new java.util.HashMap<>();
    private static final java.util.Map<net.minecraft.world.level.ChunkPos, Integer> HOLDS = new java.util.HashMap<>();
    /**
     * Our own ticket, never the runner's force: the runner forces the chunks of the structures it is running and lets them go when each one ends, so a chunk that "is
     * already forced" when this test asks may be gone a tick later (the flake under load). A region ticket of this type is ours alone; radius 2 is the level of a
     * forced chunk (entities found and ticking).
     */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKET =
            net.minecraft.server.level.TicketType.create("supernaturalbloodlines_npc_pack_test", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
    private static final int TICKET_RADIUS = 2;

    private static void release(GameTestHelper helper) {
        for (net.minecraft.world.level.ChunkPos cp : HELD.getOrDefault(helper, java.util.List.of())) {
            if (HOLDS.merge(cp, -1, Integer::sum) <= 0) {
                HOLDS.remove(cp);
                helper.getLevel().getChunkSource().removeRegionTicket(TICKET, cp, TICKET_RADIUS, cp);
            }
        }
        HELD.remove(helper);
    }

    /** Holds the chunks at these x offsets, waits until something placed in each is found by a lookup (a tick or two), then runs {@code body}; {@link #clean} lets them go. */
    private static void whenLoaded(GameTestHelper helper, double[] xs, Runnable body) {
        List<BlockPos> spots = new java.util.ArrayList<>();
        for (double x : xs) {
            BlockPos at = helper.absolutePos(new BlockPos((int) x, 1, 2));
            net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(at);
            if (HOLDS.merge(cp, 1, Integer::sum) == 1) {
                helper.getLevel().getChunkSource().addRegionTicket(TICKET, cp, TICKET_RADIUS, cp); // always our own hold, whatever else holds the chunk
            }
            // the chunk's loading runs on other threads in real time while the test's own ticks come as fast as the server can make them, so waiting a number of ticks for it is a race under load: load it now
            helper.getLevel().getChunk(cp.x, cp.z);
            HELD.computeIfAbsent(helper, k -> new java.util.ArrayList<>()).add(cp);
            spots.add(at);
        }
        // the question that matters is whether something placed there is found: ask it with a marker, every tick, until the answer is yes
        helper.startSequence().thenWaitUntil(() -> {
            boolean all = true;
            for (BlockPos s : spots) {
                net.minecraft.world.entity.Entity probe = EntityType.MARKER.create(helper.getLevel());
                probe.moveTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5);
                helper.getLevel().addFreshEntity(probe);
                all &= helper.getLevel().getEntity(probe.getUUID()) == probe;
                probe.discard();
            }
            helper.assertTrue(all, "the chunks are not loaded yet");
        }).thenExecute(guarded(helper, body));
        // a test that times out never reaches its body or its clean-up: let the chunks go a few ticks before the runner gives up
        helper.runAfterDelay(95, () -> release(helper));
    }

    /** The body, and whatever way it ends (a failed assertion included) the chunks it held are let go: a failed test must not leave them forced for the next batch. */
    static Runnable guarded(GameTestHelper helper, Runnable body) {
        return () -> {
            try {
                body.run();
            } catch (RuntimeException | Error e) {
                release(helper);
                throw e;
            }
        };
    }

    private static Mob mob(GameTestHelper helper, String id, double x) {
        // placed, not just created: an Alpha beside a weak player is refused entry by the Werewolves mod unless it has a level (see NpcPackSpawner.place)
        Mob m = NpcPackSpawner.place(helper.getLevel(), NpcPackSpawner.asMob(type(id)), helper.absolutePos(new BlockPos((int) x, 1, 2)), NpcPackSpawner.HALF_LEVEL);
        helper.assertTrue(m != null, "could not place " + id + " in the world");
        // a mob added to a chunk that is not loaded is "there" and found by nothing: say so, instead of a wolf that "did not see" it
        helper.assertTrue(helper.getLevel().getEntity(m.getUUID()) == m, id + " was placed at x " + x + " in a chunk that is not loaded: use whenLoaded");
        return m;
    }

    private static Mob wildWolf(GameTestHelper helper, double x, long pack) {
        SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, T.spawn(), false, 1.2, 1.1, true, 3, 2, 3, true, true));
        Mob m = mob(helper, "werewolves:werewolf_beast", x);
        m.setData(SpeciesRegistries.NPC_MARK, NpcMark.wild(pack, NpcSpawnRules.nightOf(helper.getLevel().getDayTime())));
        return m;
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof ServerPlayer) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(victim, killer.damageSources().playerAttack(killer)));
        } else {
            victim.hurt(killer.damageSources().playerAttack(killer), 10_000f);
        }
    }

    private static WolfState state(ServerPlayer p) {
        return PackAccess.wolf(p);
    }

    /** A source whose mood is a Blood Moon: stands in for Enhanced Celestials. */
    private static void bloodMoon(boolean on) {
        LunarSource src = new LunarSource() {
            @Override
            public LunarMood now(Level level) {
                return new LunarMood(LunarMood.Kind.BLOOD, false);
            }

            @Override
            public List<Upcoming> ahead(Level level, int days) {
                return List.of();
            }
        };
        LunarMoods.use(on ? src : LunarSource.NONE, () -> new LunarMoods.Tuning(1, 1, 1, false, 0.5));
    }

    // ---- R143: the spawn ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFullMoonNightSpawnsAPackWithAnAlphaAndALoneWolf(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        List<Mob> found = new ArrayList<>();
        try {
            long night = freshFullNight();
            level.setDayTime(night);
            List<Mob> spawned = NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper));
            found.addAll(spawned);
            helper.assertTrue(spawned.size() >= 3, "a pack and a lone wolf were expected, got " + spawned.size());
            helper.assertTrue(found.stream().filter(m -> m instanceof IWerewolfAlpha).count() == 1, "no Alpha leads the pack: " + found.stream().map(m -> m.getType().toString()).toList());
            helper.assertTrue(found.stream().allMatch(m -> m.isAddedToLevel() && level.getEntity(m.getUUID()) == m), "a wolf of the pack is not in the world (the Alpha is refused entry without a level)");
            // the Alpha leads only as many wolves as the Werewolves mod lets it (read at run time), so the pack is the Alpha, that many (three at most) and the lone wolf
            Mob lead = found.stream().filter(m -> m instanceof IWerewolfAlpha).findFirst().orElseThrow();
            int wolves = Math.min(3, ((de.teamlapen.vampirism.api.entity.IEntityLeader) lead).getMaxFollowerCount());
            helper.assertTrue(found.size() == 1 + Math.max(1, wolves) + 1, "the Alpha, " + Math.max(1, wolves) + " wolves and a lone wolf were expected, found " + found.size());
            helper.assertTrue(found.stream().allMatch(m -> m.getData(SpeciesRegistries.NPC_MARK).night() == NpcSpawnRules.nightOf(night)), "a wolf has the wrong night");
        } finally {
            level.setDayTime(saved);
            clean(helper, found);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAlphaLeadsOnlyAsManyAsItCan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        List<Mob> found = new ArrayList<>();
        try {
            level.setDayTime(freshFullNight());
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, new NpcSpawnRules.Tuning(1, 0, 1, 8, 8, 3), true, 1.2, 1.1, true, 3, 2, 3, true, true));
            found.addAll(NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper)));
            Mob alpha = found.stream().filter(m -> m instanceof IWerewolfAlpha).findFirst().orElse(null);
            helper.assertTrue(alpha != null, "no Alpha");
            int limit = ((de.teamlapen.vampirism.api.entity.IEntityLeader) alpha).getMaxFollowerCount();
            helper.assertTrue(found.size() - 1 <= limit && found.size() - 1 >= 1, "an Alpha that leads " + limit + " got " + (found.size() - 1) + " wolves");
        } finally {
            level.setDayTime(saved);
            clean(helper, found);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOrdinaryNightAndTheDaySpawnNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            level.setDayTime(PLAIN_NIGHT);
            helper.assertTrue(NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper)).isEmpty(), "an ordinary night spawned wolves");
            level.setDayTime(FULL_NIGHT - 13000 + 6000); // the full-moon day at noon
            helper.assertTrue(NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper)).isEmpty(), "the day of the full moon spawned wolves");
            helper.assertTrue(marked(level, spot(helper)).isEmpty(), "wolves are about");
        } finally {
            level.setDayTime(saved);
            clean(helper, marked(level, spot(helper)));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerIsRolledOncePerNightEvenAfterARestart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        List<Mob> found = new ArrayList<>();
        try {
            long night = freshFullNight();
            level.setDayTime(night);
            List<Mob> first = NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper));
            found.addAll(first);
            helper.assertTrue(first.size() >= 3, "the first roll");
            helper.assertTrue(NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper)).isEmpty(), "the same night rolled again");
            // a restart: the ledger is read back from its saved form
            NpcPackNights back = NpcPackNights.load(NpcPackNights.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess()), level.registryAccess());
            long id = NpcSpawnRules.nightOf(night);
            helper.assertTrue(back.lastNight(p.getUUID()) == id && back.packsIn(id) == 1, "the ledger lost the night: " + back.lastNight(p.getUUID()) + "/" + back.packsIn(id));
            // the next night is a new roll (an ordinary one: nothing spawns, but the player is rolled for)
            level.setDayTime(night + 24000L * 8);
            List<Mob> next = NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper));
            found.addAll(next);
            helper.assertTrue(next.size() >= 3, "the next full moon did not roll");
        } finally {
            level.setDayTime(saved);
            clean(helper, found);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLevelHasAMostOfPacksPerNight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer a = TestPlayers.createMortal(helper);
        ServerPlayer b = TestPlayers.createMortal(helper);
        List<Mob> found = new ArrayList<>();
        try {
            level.setDayTime(freshFullNight());
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, new NpcSpawnRules.Tuning(1, 0, 0, 3, 3, 1), true, 1.2, 1.1, true, 3, 2, 3, true, true));
            found.addAll(NpcPackSpawner.roll(level, List.of(a, b), () -> 0.0, near -> spot(helper)));
            helper.assertTrue(found.size() == 3, "one pack of three wolves was expected under a most of 1, found " + found.size());
        } finally {
            level.setDayTime(saved);
            clean(helper, found);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSwitchTurnsTheSpawnOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            level.setDayTime(FULL_NIGHT + 24000L * 24);
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(false, T.spawn(), true, 1.2, 1.1, true, 3, 2, 3, true, true));
            helper.assertTrue(NpcPackSpawner.roll(level, List.of(p), () -> 0.0, near -> spot(helper)).isEmpty(), "npc_packs_enabled = false still spawned");
        } finally {
            level.setDayTime(saved);
            clean(helper, marked(level, spot(helper)));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBloodMoonNightRollsAtTheCelestialsFactor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        ServerPlayer q = TestPlayers.createMortal(helper);
        List<Mob> found = new ArrayList<>();
        try {
            bloodMoon(true);
            level.setDayTime(freshFullNight() + 24000L); // the night after a full moon: phase 1, not a full moon
            helper.assertTrue(LunarMoods.packChance(level) == 0.5, "the Blood Moon factor did not come through LunarMoods: " + LunarMoods.packChance(level));
            // 0.2 is under the full-moon chance (0.35) but not under half of it (0.175)
            helper.assertTrue(NpcPackSpawner.roll(level, List.of(p), dice(0.2, 0.0, 0.0, 0.99), near -> spot(helper)).isEmpty(), "a Blood Moon rolled at the full chance");
            List<Mob> half = NpcPackSpawner.roll(level, List.of(q), dice(0.1, 0.0, 0.0, 0.99), near -> spot(helper));
            found.addAll(half);
            helper.assertTrue(!half.isEmpty(), "a Blood Moon never spawned a pack at half the chance");
        } finally {
            bloodMoon(false);
            level.setDayTime(saved);
            clean(helper, found);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRunningEclipseIsAFullMoonNight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        ServerPlayer p = TestPlayers.createMortal(helper);
        List<Mob> found = new ArrayList<>();
        try {
            level.setDayTime(PLAIN_NIGHT + 24000L * 40);
            helper.assertTrue(NpcPackSpawner.tonight(level) == net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night.NONE, "control: an ordinary night");
            helper.assertTrue(LunarEclipse.start(level.getServer()), "the eclipse did not start");
            helper.assertTrue(NpcPackSpawner.tonight(level) == net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night.ECLIPSE, "the eclipse is not tonight");
        } finally {
            LunarEclipse.stop(level.getServer());
            level.setDayTime(saved);
            clean(helper, found);
        }
        helper.succeed();
    }

    // ---- R144: the hunt ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWolfHuntsAVampireBeforeAHunterBeforePrey(GameTestHelper helper) {
        Mob wolf = wildWolf(helper, 2, 501);
        Mob vampire = mob(helper, "vampirism:vampire", 6);
        Mob hunter = mob(helper, "vampirism:hunter", 4);
        Cow cow = (Cow) mob(helper, "minecraft:cow", 3);
        List<Mob> all = List.of(wolf, vampire, hunter, cow);
        try {
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(wolf.getTarget() == vampire, "the wolf did not pick the vampire: " + wolf.getTarget());
            vampire.discard();
            wolf.setTarget(null);
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(wolf.getTarget() == hunter, "the wolf did not pick the hunter: " + wolf.getTarget());
            hunter.discard();
            wolf.setTarget(null);
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(wolf.getTarget() == cow, "the wolf did not pick the prey: " + wolf.getTarget());
        } finally {
            clean(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayersTierFollowsTheirFaction(GameTestHelper helper) {
        ServerPlayer plain = TestPlayers.createMortal(helper);
        ServerPlayer vamp = TestPlayers.createMortal(helper);
        ServerPlayer hunter = TestPlayers.createMortal(helper);
        ServerPlayer creative = TestPlayers.createMortal(helper);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(vamp).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "no vampire");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "no hunter");
            creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(NpcPackBrain.tierOf(plain) == Tier.PLAYER, "a plain player is " + NpcPackBrain.tierOf(plain));
            helper.assertTrue(NpcPackBrain.tierOf(vamp) == Tier.VAMPIRE, "a vampire is " + NpcPackBrain.tierOf(vamp));
            helper.assertTrue(NpcPackBrain.tierOf(hunter) == Tier.HUNTER, "a hunter is " + NpcPackBrain.tierOf(hunter));
            helper.assertTrue(NpcPackBrain.tierOf(creative) == null, "a creative player is hunted");
            helper.assertTrue(NpcPackBrain.tierOf(mobOf(helper, "minecraft:sheep")) == Tier.PREY && NpcPackBrain.tierOf(mobOf(helper, "minecraft:zombie")) == null, "the prey tag is wrong");
        } finally {
            for (ServerPlayer p : List.of(vamp, hunter)) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            clean(helper, List.of());
        }
        helper.succeed();
    }

    private static Mob mobOf(GameTestHelper helper, String id) {
        Mob m = mob(helper, id, 5);
        m.discard();
        return m;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTestThatFailsInsideItsBodyStillLetsItsChunksGo(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(96, 1, 2));
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(at);
        HOLDS.merge(cp, 1, Integer::sum);
        HELD.computeIfAbsent(helper, k -> new java.util.ArrayList<>()).add(cp);
        boolean threw = false;
        try {
            guarded(helper, () -> {
                throw new IllegalStateException("the body failed");
            }).run();
        } catch (IllegalStateException expected) {
            threw = true;
        }
        helper.assertTrue(threw, "the failure of the body was swallowed");
        // this test's own bookkeeping only: a neighbouring test may hold the same chunk in the same run
        helper.assertTrue(!HELD.containsKey(helper), "a failed body left its chunk held");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobFarFromTheStructureIsFoundOnceTheTestHoldsItsChunk(GameTestHelper helper) {
        // 96 blocks along: no structure of this test is near, so the test world has nothing loaded there until the test holds it
        whenLoaded(helper, new double[] {96}, () -> {
            Mob far = mob(helper, "werewolves:werewolf_beast", 96);
            try {
                helper.assertTrue(helper.getLevel().getEntity(far.getUUID()) == far, "a mob placed far away is not in the level");
                helper.assertTrue(!helper.getLevel().getEntitiesOfClass(Mob.class, far.getBoundingBox().inflate(4), m -> m == far).isEmpty(), "a mob placed far away is not found by a search");
                clean(helper, List.of(far));
                // a neighbouring test's structure may be there, held by the runner or by another test: what is ours is only this test's own bookkeeping
                helper.assertTrue(!HELD.containsKey(helper), "the chunk was kept held after the test");
            } finally {
                clean(helper, List.of(far));
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePackHuntsTogether(GameTestHelper helper) {
        // the wolves are up to 45 blocks apart, which can be a chunk the test world did not load
        whenLoaded(helper, new double[] {-20, 0, 25}, () -> {
        Mob a = wildWolf(helper, 0, 777);
        Mob vampire = mob(helper, "vampirism:vampire", 25);
        Mob b = wildWolf(helper, -20, 777);   // 45 blocks from the vampire: beyond its own sight, within the pack's
        Mob stranger = wildWolf(helper, -20, 778); // another pack
        List<Mob> all = List.of(a, vampire, b, stranger);
        try {
            NpcPackBrain.tickNow(a);
            helper.assertTrue(a.getTarget() == vampire, "the first wolf did not see the vampire: " + a.getTarget());
            NpcPackBrain.tickNow(b);
            helper.assertTrue(b.getTarget() == vampire, "the pack did not share its target: " + b.getTarget());
            NpcPackBrain.tickNow(stranger);
            helper.assertTrue(stranger.getTarget() == null, "another pack joined the hunt");
        } finally {
            clean(helper, all);
        }
        helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWorseTargetNeverReplacesABetterOne(GameTestHelper helper) {
        Mob wolf = wildWolf(helper, 2, 502);
        Mob vampire = mob(helper, "vampirism:vampire", 6);
        Cow cow = (Cow) mob(helper, "minecraft:cow", 4);
        try {
            wolf.setTarget(vampire);
            wolf.setTarget(cow);
            helper.assertTrue(wolf.getTarget() == vampire, "prey replaced a vampire: " + wolf.getTarget());
            wolf.setTarget(null);
            wolf.setTarget(cow);
            helper.assertTrue(wolf.getTarget() == cow, "control: nothing in the way of a first target");
            wolf.setTarget(vampire);
            helper.assertTrue(wolf.getTarget() == vampire, "a vampire did not take over from prey");
        } finally {
            clean(helper, List.of(wolf, vampire, cow));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFeralNightMakesAWildWolfStrongerAndOnlyThen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        Mob wolf = wildWolf(helper, 2, 503);
        try {
            level.setDayTime(FULL_NIGHT + 5000); // midnight: at dusk the sky is not yet dark
            level.updateSkyBrightness(); // the sky is darkened at the next tick, and a test reads it at once
            // the light where it stands: a test platform under the night sky is dark enough, but say so if it is not
            helper.assertTrue(level.getMaxLocalRawBrightness(wolf.blockPosition()) <= 7, "the test spot is not dark: " + level.getMaxLocalRawBrightness(wolf.blockPosition()) + " block=" + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, wolf.blockPosition()) + " sky=" + level.getBrightness(net.minecraft.world.level.LightLayer.SKY, wolf.blockPosition()) + " darken=" + level.getSkyDarken() + " time=" + level.getDayTime() + " pos=" + wolf.blockPosition().toShortString());
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(NpcPackBrain.isFeral(wolf), "not feral on a dark full-moon night");
            level.setDayTime(PLAIN_NIGHT + 5000);
            level.updateSkyBrightness();
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(!NpcPackBrain.isFeral(wolf), "still feral on an ordinary night");
            level.setDayTime(FULL_NIGHT + 5000);
            level.updateSkyBrightness();
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, T.spawn(), true, 1.0, 1.0, true, 3, 2, 3, true, true));
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(!NpcPackBrain.isFeral(wolf), "a factor of 1 still gave a bonus");
        } finally {
            level.setDayTime(saved);
            clean(helper, List.of(wolf));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWildWolfWithNoPlayerNearLeavesAtDawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        long night = freshFullNight();
        level.setDayTime(night);
        Mob wolf = wildWolf(helper, 2, 504);
        Mob kept = wildWolf(helper, 3, 505);
        try {
            SpeciesConfig.overrideNpc(null);
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(!wolf.isRemoved(), "the wolf left in the middle of its night");
            level.setDayTime(night - 13000 + 23300);
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, T.spawn(), false, 1.2, 1.1, true, 3, 2, 3, true, true));
            NpcPackBrain.tickNow(kept);
            helper.assertTrue(!kept.isRemoved(), "npc_pack_dawn_despawn = false still sent it away");
            SpeciesConfig.overrideNpc(null);
            NpcPackBrain.tickNow(wolf);
            helper.assertTrue(wolf.isRemoved(), "the wolf did not leave at dawn");
        } finally {
            level.setDayTime(saved);
            clean(helper, List.of(wolf, kept));
        }
        helper.succeed();
    }

    // ---- R145: the weaker spark ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killingAnAlphaGivesAnUnprotectedWolfAWeakSparkThatCountsForNothing(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, false);
        Mob alpha = NpcPackSpawner.spawnPackAt(helper.getLevel(), spot(helper), new NpcSpawnRules.Plan(true, true, 1, false), 24).get(0);
        try {
            helper.assertTrue(alpha instanceof IWerewolfAlpha, "the pack has no Alpha");
            kill(p, alpha);
            WolfState w = state(p);
            helper.assertTrue(w.weakSparks() == 1, "no weak spark: " + w.weak());
            helper.assertTrue(w.rank() == Rank.BETA && w.sparks() == 1 && !w.stoleSpark(), "the weak spark counted as a spark: " + w);
            helper.assertTrue(PackAccess.effectiveSparks(p) == 1, "it counted toward the stack");
        } finally {
            clean(helper, marked(helper.getLevel(), spot(helper)), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNaturalForestAlphaGivesOneToo(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.OMEGA, false);
        Mob alpha = mob(helper, "werewolves:alpha_werewolf", 2); // no mark: a natural one
        try {
            helper.assertTrue(!alpha.hasData(SpeciesRegistries.NPC_MARK), "control: this Alpha is not ours");
            kill(p, alpha);
            helper.assertTrue(state(p).weakSparks() == 1, "a natural Alpha gave nothing: " + state(p).weak());
        } finally {
            clean(helper, List.of(alpha), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOpenProtectedPathDestroysTheSparkAndAKeepsThePath(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, true);
        Mob alpha = mob(helper, "werewolves:alpha_werewolf", 2);
        try {
            kill(p, alpha);
            WolfState w = state(p);
            helper.assertTrue(w.weakSparks() == 0 && w.rank() == Rank.BETA && !w.heldSpark(), "the path was not kept: " + w);
        } finally {
            clean(helper, List.of(alpha), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaAndAHumanTakeNothing(GameTestHelper helper) {
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, false);
        ServerPlayer human = TestPlayers.createMortal(helper);
        Mob a = mob(helper, "werewolves:alpha_werewolf", 2);
        Mob b = mob(helper, "werewolves:alpha_werewolf", 3);
        try {
            kill(truth, a);
            kill(human, b);
            helper.assertTrue(state(truth).weakSparks() == 0, "a True Alpha took a weak spark");
            helper.assertTrue(state(human).weakSparks() == 0 && state(human).lineage() == Lineage.NONE, "a human took one");
        } finally {
            clean(helper, List.of(a, b), truth);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownAndTheMostHeldBoundTheFarming(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, false);
        Mob a = mob(helper, "werewolves:alpha_werewolf", 2);
        Mob b = mob(helper, "werewolves:alpha_werewolf", 3);
        Mob c = mob(helper, "werewolves:alpha_werewolf", 4);
        try {
            kill(p, a);
            kill(p, b);
            helper.assertTrue(state(p).weakSparks() == 1, "a second Alpha on the same day gave another: " + state(p).weak());
            // three days on, with one held: another may come; a third would be over the most of 2
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, T.spawn(), true, 1.2, 1.1, true, 3, 2, 0, true, true));
            kill(p, c);
            helper.assertTrue(state(p).weakSparks() == 2, "no cooldown, one held: a second was expected: " + state(p).weak());
            Mob d = mob(helper, "werewolves:alpha_werewolf", 5);
            kill(p, d);
            helper.assertTrue(state(p).weakSparks() == 2, "a third was given over the most held");
            d.discard();
        } finally {
            clean(helper, List.of(a, b, c), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSwitchStopsTheSparkFromAnAlpha(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, false);
        Mob a = mob(helper, "werewolves:alpha_werewolf", 2);
        try {
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, T.spawn(), true, 1.2, 1.1, false, 3, 2, 3, true, true));
            kill(p, a);
            helper.assertTrue(state(p).weakSparks() == 0, "npc_alpha_spark_enabled = false still gave one");
        } finally {
            clean(helper, List.of(a), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itGrowsOnlyWithOnlineTimeAndBecomesARealSpark(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, false);
        try {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(state(p).toBuilder().weak(new Weak(1, 0, 1)).build()).build());
            WeakSpark.grow(p, 3 * 24000 - 200);
            helper.assertTrue(state(p).weakSparks() == 1 && state(p).rank() == Rank.BETA, "it grew early: " + state(p));
            WeakSpark.grow(p, 200);
            WolfState w = state(p);
            helper.assertTrue(w.weakSparks() == 0 && w.rank() == Rank.ALPHA && w.sparks() == 1 && w.heldSpark(), "it did not become a spark: " + w);
        } finally {
            clean(helper, List.of(), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGrownSparkWithersToKeepAnOpenProtectedPath(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.BETA, false);
        try {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(state(p).toBuilder().weak(new Weak(1, 0, 1)).protectPath(true).build()).build());
            WeakSpark.grow(p, 3 * 24000);
            WolfState w = state(p);
            helper.assertTrue(w.weakSparks() == 0 && w.rank() == Rank.BETA && !w.heldSpark(), "protection was not honoured: " + w);
        } finally {
            clean(helper, List.of(), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWeakSparkCannotBeStolenWithTheAlphasRank(GameTestHelper helper) {
        ServerPlayer holder = wolf(helper, Rank.ALPHA, false);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, false);
        online(holder, thief);
        try {
            BloodlineAccess.set(holder, BloodlineAccess.get(holder).toBuilder().wolf(state(holder).toBuilder().weak(new Weak(2, 100, 5)).build()).build());
            kill(thief, holder);
            helper.assertTrue(state(thief).rank() == Rank.ALPHA && state(thief).sparks() == 1, "the thief took more than the one real spark: " + state(thief));
            helper.assertTrue(state(thief).weakSparks() == 0, "the weak sparks were passed on");
            helper.assertTrue(state(holder).weakSparks() == 0 && state(holder).rank() == Rank.OMEGA, "the victim kept them: " + state(holder));
        } finally {
            clean(helper, List.of(), holder, thief);
        }
        helper.succeed();
    }

    // ---- R146: Moonblood Surge and Wolf Spirits ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSurgeBuffsThePackWithinRangeAndTheCallerPaysForIt(GameTestHelper helper) {
        ServerPlayer caller = wolf(helper, Rank.ALPHA, false);
        ServerPlayer near = wolf(helper, Rank.BETA, false);
        ServerPlayer far = wolf(helper, Rank.BETA, false);
        PackIndex index = PackIndex.get(helper.getLevel());
        online(caller, near, far);
        try {
            index.join(caller.getUUID(), near.getUUID());
            index.join(caller.getUUID(), far.getUUID());
            far.setPos(caller.position().add(40, 0, 0));
            helper.assertTrue(PackSkills.surge(caller, 100) == null, "the surge was refused");
            helper.assertTrue(near.hasEffect(MobEffects.REGENERATION) && near.hasEffect(MobEffects.DAMAGE_BOOST) && near.hasEffect(MobEffects.ABSORPTION), "the pack mate got no buff");
            helper.assertTrue(!near.hasEffect(MobEffects.WEAKNESS), "the pack mate paid the price");
            helper.assertTrue(!far.hasEffect(MobEffects.REGENERATION), "a member 40 blocks away was buffed");
            helper.assertTrue(caller.hasEffect(MobEffects.REGENERATION) && caller.hasEffect(MobEffects.WEAKNESS) && caller.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the caller did not pay");
        } finally {
            clean(helper, List.of(), caller, near, far);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSurgeIsRefusedWithoutAPackAWerewolfOrDuringTheCooldownOrAnEclipse(GameTestHelper helper) {
        ServerPlayer lone = wolf(helper, Rank.OMEGA, false);
        ServerPlayer caller = wolf(helper, Rank.ALPHA, false);
        ServerPlayer mate = wolf(helper, Rank.BETA, false);
        ServerPlayer human = TestPlayers.createMortal(helper);
        online(lone, caller, mate, human);
        try {
            helper.assertTrue(PackSkills.surge(lone, 100) != null, "a lone wolf surged");
            helper.assertTrue(PackSkills.surge(human, 100) != null, "a human surged");
            PackIndex.get(helper.getLevel()).join(caller.getUUID(), mate.getUUID());
            helper.assertTrue(PackSkills.surge(caller, 100) == null, "the first surge was refused");
            long cd = PackSkillRules.SURGE_COOLDOWN_TICKS;
            helper.assertTrue(PackSkills.surge(caller, 100 + cd - 1) != null, "the cooldown ended a tick early");
            helper.assertTrue(PackSkills.surge(caller, 100 + cd) == null, "the cooldown did not end");
            caller.removeAllEffects();
            helper.assertTrue(LunarEclipse.start(helper.getLevel().getServer()), "no eclipse");
            helper.assertTrue(PackSkills.surge(caller, 100 + 3 * cd) != null && !caller.hasEffect(MobEffects.REGENERATION), "a surge in an eclipse");
            LunarEclipse.stop(helper.getLevel().getServer());
            helper.assertTrue(PackSkills.surge(caller, 100 + 3 * cd) == null, "a refused surge spent the cooldown");
            SpeciesConfig.overrideNpc(new SpeciesConfig.NpcTuning(true, T.spawn(), true, 1.2, 1.1, true, 3, 2, 3, false, true));
            helper.assertTrue(PackSkills.surge(caller, 100 + 9 * cd) != null, "moonblood_surge_enabled = false still surged");
        } finally {
            LunarEclipse.stop(helper.getLevel().getServer());
            clean(helper, List.of(), lone, caller, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlphaCallsThreeSpiritsAndOnlyAnAlphaDoes(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, false);
        ServerPlayer beta = wolf(helper, Rank.BETA, false);
        List<Mob> spirits = new ArrayList<>();
        try {
            helper.assertTrue(PackSkills.spirits(beta, 100) != null, "a Beta called spirits");
            helper.assertTrue(PackSkills.spirits(alpha, 100) == null, "the Alpha was refused");
            spirits.addAll(spiritsOf(helper, alpha));
            helper.assertTrue(spirits.size() == PackSkillRules.SPIRITS_COUNT, "three spirits were expected, found " + spirits.size());
            helper.assertTrue(spirits.stream().allMatch(s -> s.isCurrentlyGlowing() || s.hasGlowingTag()), "a spirit does not glow");
            helper.assertTrue(PackSkills.spirits(alpha, 100 + PackSkillRules.SPIRITS_COOLDOWN_TICKS - 1) != null, "the cooldown ended a tick early");
            // they leave when their time is up
            Mob first = spirits.get(0);
            first.setData(SpeciesRegistries.NPC_MARK, NpcMark.spirit(alpha.getUUID(), helper.getLevel().getGameTime()));
            NpcPackBrain.tickNow(first);
            helper.assertTrue(first.isRemoved(), "a spirit stayed past its time");
        } finally {
            spirits.forEach(Entity::discard);
            clean(helper, spiritsOf(helper, alpha), alpha, beta);
        }
        helper.succeed();
    }

    private static List<Mob> spiritsOf(GameTestHelper helper, ServerPlayer owner) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(owner.blockPosition()).inflate(16),
                m -> m.hasData(SpeciesRegistries.NPC_MARK) && m.getData(SpeciesRegistries.NPC_MARK).kind() == NpcMark.Kind.SPIRIT
                        && owner.getUUID().equals(m.getData(SpeciesRegistries.NPC_MARK).owner()));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritNeverTurnsOnOrHurtsItsOwnersPackOrAllies(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.ALPHA, false);
        ServerPlayer mate = wolf(helper, Rank.BETA, false);
        ServerPlayer ally = wolf(helper, Rank.ALPHA, false);
        ServerPlayer stranger = wolf(helper, Rank.ALPHA, false);
        PackIndex index = PackIndex.get(helper.getLevel());
        List<Mob> spirits = new ArrayList<>();
        try {
            index.join(owner.getUUID(), mate.getUUID());
            index.ally(owner.getUUID(), ally.getUUID());
            helper.assertTrue(PackSkills.spirits(owner, 100) == null, "no spirits");
            spirits.addAll(spiritsOf(helper, owner));
            Mob spirit = spirits.get(0);
            for (ServerPlayer friend : List.of(owner, mate, ally)) {
                spirit.setTarget(friend);
                helper.assertTrue(spirit.getTarget() == null, "the spirit turned on " + friend.getName().getString());
                float before = friend.getHealth();
                friend.hurt(spirit.damageSources().mobAttack(spirit), 4f);
                helper.assertTrue(friend.getHealth() == before, "the spirit hurt " + friend.getName().getString());
            }
            spirit.setTarget(stranger);
            helper.assertTrue(spirit.getTarget() == stranger, "the spirit would not target a stranger");
            float before = stranger.getHealth();
            stranger.hurt(spirit.damageSources().mobAttack(spirit), 4f);
            helper.assertTrue(stranger.getHealth() < before, "control: the spirit cannot hurt a stranger");
        } finally {
            index.unally(owner.getUUID(), ally.getUUID());
            clean(helper, spirits, owner, mate, ally, stranger);
        }
        helper.succeed();
    }

    /** A Wolf Spirit gives nothing when it dies. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritDropsNothing(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.ALPHA, false);
        List<Mob> spirits = new ArrayList<>();
        try {
            helper.assertTrue(PackSkills.spirits(owner, 100) == null, "no spirits");
            spirits.addAll(spiritsOf(helper, owner));
            Mob s = spirits.get(0);
            s.spawnAtLocation(net.minecraft.world.item.Items.BONE); // a stand-in drop on the ground does not count; the event is what is cancelled
            var event = new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(s, s.damageSources().generic(), new ArrayList<>(), false);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "a spirit's drops were not cancelled");
            var xp = new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(s, owner, 5);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(xp);
            helper.assertTrue(xp.isCanceled(), "a spirit gave experience");
        } finally {
            clean(helper, spirits, owner);
        }
        helper.succeed();
    }


    /** A Wolf Spirit does not breed (the puppy would stay for ever) and does not wear wolf armour (it would vanish with the spirit). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritDoesNotBreedOrWearWolfArmour(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.ALPHA, false);
        List<Mob> spirits = new ArrayList<>();
        Mob plain = mob(helper, "minecraft:wolf", 4);
        Mob plain2 = mob(helper, "minecraft:wolf", 5);
        try {
            helper.assertTrue(PackSkills.spirits(owner, 100) == null, "no spirits");
            spirits.addAll(spiritsOf(helper, owner));
            Mob spirit = spirits.get(0);
            var withSpirit = new net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent(spirit, plain, null);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(withSpirit);
            helper.assertTrue(withSpirit.isCanceled(), "a spirit bred");
            var otherWay = new net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent(plain, spirit, null);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(otherWay);
            helper.assertTrue(otherWay.isCanceled(), "a spirit bred as the second parent");
            var control = new net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent(plain, plain2, null);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(control);
            helper.assertTrue(!control.isCanceled(), "control: ordinary wolves can no longer breed");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WOLF_ARMOR));
            var armour = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND, spirit);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(armour);
            helper.assertTrue(armour.isCanceled(), "wolf armour was put on a spirit");
            var armourOnPlain = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND, plain);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(armourOnPlain);
            helper.assertTrue(!armourOnPlain.isCanceled(), "control: wolf armour on an ordinary wolf was stopped");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        } finally {
            plain.discard();
            plain2.discard();
            clean(helper, spirits, owner);
        }
        helper.succeed();
    }

    /** A spirit is never put inside a wall: with the caster in a stone pocket every one of many tries finds a free spot. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritIsNotPutInsideBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = TestPlayers.createMortal(helper);
        BlockPos centre = owner.blockPosition();
        List<BlockPos> stone = new ArrayList<>();
        try {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    for (int y = -1; y <= 3; y++) {
                        BlockPos p = centre.offset(x, y, z);
                        boolean pocket = x == 0 && z == 0 && (y == 0 || y == 1);
                        if (!pocket && level.getBlockState(p).isAir()) {
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                            stone.add(p);
                        }
                    }
                }
            }
            for (int i = 0; i < 40; i++) {
                BlockPos at = PackSkills.spiritSpot(level, owner);
                helper.assertTrue(level.noCollision(EntityType.WOLF.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)), "a spirit was put inside a block at " + at.toShortString());
            }
        } finally {
            stone.forEach(p -> level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3));
            clean(helper, List.of(), owner);
        }
        helper.succeed();
    }

    /** The debug pack stands on the ground at the operator's height, not on top of a roof the way a heightmap would put it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theDebugPackStandsOnGroundNotOnTheRoof(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos start = helper.absolutePos(new BlockPos(2, 1, 2));
        List<BlockPos> roof = new ArrayList<>();
        try {
            BlockPos open = NpcPackSpawner.groundNear(level, start);
            helper.assertTrue(open != null, "no ground near the start under the open sky");
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    BlockPos p = start.offset(x - 2, 6, z - 2);
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                    roof.add(p);
                }
            }
            BlockPos roofed = NpcPackSpawner.groundNear(level, start);
            helper.assertTrue(open.equals(roofed), "a roof moved the ground from " + open + " to " + roofed);
            int heightmapY = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start).getY();
            helper.assertTrue(heightmapY > roofed.getY() + 3, "control: a heightmap would have put the pack on the roof (" + heightmapY + " vs " + roofed.getY() + ")");
            helper.assertTrue(NpcPackSpawner.groundNear(level, start.above(60)) == null, "ground found 60 blocks up in the air");
        } finally {
            roof.forEach(p -> level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3));
        }
        helper.succeed();
    }
}
