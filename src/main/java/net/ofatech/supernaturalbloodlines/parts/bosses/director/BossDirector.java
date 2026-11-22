package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossFacts;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.BossWeight;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.ElijahRules;
import net.ofatech.supernaturalbloodlines.core.boss.DeucalionRules;
import net.ofatech.supernaturalbloodlines.core.boss.PeterRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity;
import net.ofatech.supernaturalbloodlines.core.boss.KlausRules;
import net.ofatech.supernaturalbloodlines.core.boss.RebekahRules;
import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.core.boss.SpawnRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerVillagers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;
import net.ofatech.supernaturalbloodlines.parts.species.line.Imprisonment;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The one {@code LevelTickEvent.Post} hook of the bosses part (plan 17, decision 5): every {@code boss_check_seconds} on the overworld it looks for
 * a boss whose entity is gone, and asks {@link SpawnRules} (with the facts) whether a boss may come. No vanilla spawn rules, no biome modifier.
 */
public final class BossDirector {
    /** Mountain ash this far from a player counts as a ring around them. */
    private static final int ASH_RING_RADIUS = 12;
    /** How many checks in a row an entity must be missing from a loaded chunk before the boss is lost. */
    private static final int MISSES_TO_LOSE = 2;

    private static volatile DoubleSupplier roll = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile java.util.function.Function<ServerPlayer, MikaelRules.Facts> factsSource = BossDirector::realFacts;
    private static volatile java.util.function.Function<ServerLevel, List<ServerPlayer>> players = l -> new ArrayList<>(l.players());
    private static final java.util.Map<BossId, Integer> MISSES = new java.util.EnumMap<>(BossId.class);

    private BossDirector() {
    }

    /** The world is closing: what the director counted is forgotten (a count must not carry into the next world of the same process). */
    @VisibleForTesting
    public static void serverStopped() {
        MISSES.clear();
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> serverStopped());
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                    && level.getGameTime() % Math.max(20L, BossesConfig.checkSeconds() * 20L) == 0) {
                check(level.getServer());
            }
        });
    }

    /** Tests: who counts as a player in the level (fake players are not in its list) and what is true of each; null puts the real ones back. */
    @VisibleForTesting
    public static void useWorld(@Nullable java.util.function.Function<ServerLevel, List<ServerPlayer>> who, @Nullable java.util.function.Function<ServerPlayer, MikaelRules.Facts> what) {
        players = who == null ? l -> new ArrayList<>(l.players()) : who;
        factsSource = what == null ? BossDirector::realFacts : what;
    }

    /** Where the ground is: the real search by default. */
    @FunctionalInterface
    public interface GroundFinder {
        Optional<BlockPos> find(ServerLevel level, BlockPos from, int min, int max);
    }

    private static volatile GroundFinder ground = BossDirector::groundNear;

    /** The spot the commands and the director use: {@link #groundNear} unless a test fixed one. */
    public static Optional<BlockPos> findGround(ServerLevel level, BlockPos from, int min, int max) {
        return ground.find(level, from, min, max);
    }

    /** Tests: a fixed spot, because the search looks at whatever the neighbouring test structures left standing. {@code null} puts the real one back. */
    @VisibleForTesting
    public static void useGround(@Nullable GroundFinder finder) {
        ground = finder == null ? BossDirector::groundNear : finder;
    }

    private static volatile java.util.function.Function<ServerLevel, Difficulty> difficulty = ServerLevel::getDifficulty;

    /** Tests: the difficulty the director sees. The GameTests run side by side, so one cannot switch the whole server to peaceful. {@code null} puts the real one back. */
    @VisibleForTesting
    public static void useDifficulty(@Nullable java.util.function.Function<ServerLevel, Difficulty> fixed) {
        difficulty = fixed == null ? ServerLevel::getDifficulty : fixed;
    }

    /** Tests: the dice. {@code null} puts the real ones back. */
    @VisibleForTesting
    public static void useRoll(@Nullable DoubleSupplier fixed) {
        roll = fixed == null ? () -> ThreadLocalRandom.current().nextDouble() : fixed;
        MISSES.clear();
    }

    /** The dice, the players of a level and its difficulty as the director sees them (the tests fix all three). */
    static double nextRoll() {
        return roll.getAsDouble();
    }

    static Difficulty difficultyIn(ServerLevel level) {
        return difficulty.apply(level);
    }

    /** One look at the world, as the tick does it; the tests call it directly. */
    public static void check(MinecraftServer server) {
        BossIndex index = BossIndex.get(server);
        for (BossId boss : BossId.values()) {
            if (boss.landed()) {
                lostCheck(server, index, boss);
            }
        }
        mikael(server, index);
        klaus(server, index);
        deucalion(server, index);
        peter(server, index);
        BeastDirector.tick(server, index);
        elijah(server, index);
        rebekah(server, index);
        kol(server, index);
        finn(server, index);
    }

    // ---- lost bosses ----

    @Nullable
    static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    private static void lostCheck(MinecraftServer server, BossIndex index, BossId boss) {
        BossIndex.Entry entry = index.of(boss);
        if ((entry.state != BossState.ACTIVE && entry.state != BossState.OPEN) || entry.entity == null) {
            MISSES.remove(boss);
            return;
        }
        Entity found = find(server, entry.entity);
        if (found != null && found.isAlive()) {
            MISSES.remove(boss);
            return;
        }
        ServerLevel where = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(entry.dimension)) {
                where = level;
            }
        }
        // an unloaded chunk says nothing: the boss may be right there, asleep with it
        if (where == null || !where.hasChunkAt(entry.pos)) {
            MISSES.remove(boss);
            return;
        }
        int misses = MISSES.merge(boss, 1, Integer::sum);
        if (misses >= MISSES_TO_LOSE) {
            MISSES.remove(boss);
            index.apply(boss, BossRules.Event.LOST);
            boss.npcOriginal().ifPresent(o -> LineWarning.closed(o.uuid()));
        }
    }

    // ---- Mikael ----

    private static void mikael(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.MIKAEL;
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), true, index.state(boss), false);
        ServerLevel overworld = server.overworld();
        if (SpawnRules.refusal(facts).isPresent() || !MikaelRules.isNight(overworld.getDayTime())
                || difficulty.apply(overworld) == Difficulty.PEACEFUL) {
            return;
        }
        long today = DesiccationRules.dayOf(overworld.getDayTime());
        BossIndex.Entry entry = index.of(boss);
        if (entry.lastRollDay == today) {
            return;
        }
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer p : players.apply(overworld)) {
            if (MikaelRules.eligible(factsSource.apply(p))) {
                eligible.add(p);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }
        // one roll a night, the first time somebody could be hunted
        entry.lastRollDay = today;
        index.setDirty();
        if (roll.getAsDouble() < BossWeight.chance(BossesConfig.mikaelNightChance(), 1.0)) {
            ServerPlayer target = eligible.get(Math.min(eligible.size() - 1, (int) (roll.getAsDouble() * eligible.size())));
            spawnMikael(overworld, target, null);
        }
    }

    public static MikaelRules.Facts factsOf(ServerPlayer p) {
        return factsSource.apply(p);
    }

    private static volatile java.util.function.BiPredicate<ServerLevel, BlockPos> ringCheck = (l, at) -> Imprisonment.ringClosed(l, at, ASH_RING_RADIUS);

    /** Tests: the mountain ash ring check, to count its calls; null puts the real one back. */
    @VisibleForTesting
    public static void useRingCheck(@Nullable java.util.function.BiPredicate<ServerLevel, BlockPos> check) {
        ringCheck = check == null ? (l, at) -> Imprisonment.ringClosed(l, at, ASH_RING_RADIUS) : check;
    }

    /** Whether the player is an Original or an Original hybrid: the two kinds Mikael's white oak would kill outright. */
    public static boolean isOriginalPlayer(ServerPlayer p) {
        return OriginalRules.isOriginal(BloodlineAccess.get(p).species());
    }

    /** The ring is looked for last, and only for a player who passes everything else: it walks every block around them. */
    private static MikaelRules.Facts realFacts(ServerPlayer p) {
        boolean stakes = BossesConfig.mikaelStakesOriginals();
        MikaelRules.Facts base = new MikaelRules.Facts(Supernatural.isVampiric(p), SupernaturalAccess.get(p).vampireRung(), BossesConfig.mikaelMinRung(),
                MikaelRules.isNight(p.level().getDayTime()), p.level().dimension() == Level.OVERWORLD, p.onGround(), false, isOriginalPlayer(p), stakes);
        if (!MikaelRules.eligible(base) || !(p.level() instanceof ServerLevel l)) {
            return base;
        }
        return new MikaelRules.Facts(base.vampire(), base.rung(), base.minRung(), base.night(), base.overworld(), base.onGround(),
                ringCheck.test(l, p.blockPosition()), base.original(), base.stakesOriginals());
    }

    /**
     * Makes Mikael, 24 to 40 blocks from {@code target} (or at {@code at} when it is given), and writes it down. Empty when there is no ground for him.
     * The caller has already asked {@link SpawnRules}.
     */
    public static Optional<MikaelEntity> spawnMikael(ServerLevel level, @Nullable ServerPlayer target, @Nullable BlockPos at) {
        // 24 to 40 blocks away; when the terrain or the loaded chunks leave no ground there, closer (6 to 24) rather than not at all
        BlockPos pos = at != null ? at : target == null ? null
                : ground.find(level, target.blockPosition(), 24, 40).or(() -> ground.find(level, target.blockPosition(), 6, 24)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        MikaelEntity m = MikaelEntity.create(level);
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        m.applyFactors();
        m.hunt(target == null ? null : target.getUUID());
        level.addFreshEntity(m);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.MIKAEL);
        index.apply(BossId.MIKAEL, BossRules.Event.SPAWNED);
        entry.appearances++;
        entry.target = target == null ? null : target.getUUID();
        index.adopt(BossId.MIKAEL, m.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (target != null) {
            notice(target);
        }
        return Optional.of(m);
    }

    /**
     * "A hunter in black is watching you" (the name once the player has discovered the Original, R123): on the action bar and in chat, with a sound,
     * and the hint that mountain ash will hold him. A target who is an Original (only when {@code mikael_stakes_originals} is on) is told that his
     * stake can end them and their line.
     */
    static void notice(ServerPlayer target) {
        boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.MIKAEL.npcOriginal().orElseThrow().uuid(),
                SpeciesConfig.originalVisibilityAlways());
        Component text = Component.translatable(known ? "supernaturalbloodlines.boss.notice.mikael.known" : "supernaturalbloodlines.boss.notice.mikael.unknown");
        target.displayClientMessage(text, true);
        target.sendSystemMessage(text);
        target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.mikael.hint"));
        if (isOriginalPlayer(target)) {
            target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.mikael.stake"));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.WITHER_AMBIENT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    /** A spot on the ground between {@code min} and {@code max} blocks from {@code from}: empty air above solid, no fluid. */
    public static Optional<BlockPos> groundNear(ServerLevel level, BlockPos from, int min, int max) {
        for (int i = 0; i < 32; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = min + level.random.nextDouble() * (max - min);
            int x = from.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = from.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (level.getBlockState(top.below()).isSolid() && level.getFluidState(top).isEmpty() && level.getBlockState(top).getCollisionShape(level, top).isEmpty()
                    && level.getBlockState(top.above()).getCollisionShape(level, top.above()).isEmpty()) {
                return Optional.of(top);
            }
        }
        return Optional.empty();
    }

    /** How far a boss may be for the Home page to mention it (blocks). */
    public static final int NEAR_BLOCKS = 128;

    /**
     * The Home page's "world now" line: the nearest boss that is out there (standing) or down (fallen) within {@link #NEAR_BLOCKS} blocks, and its
     * id when this player may know who it is (an NPC Original is named only to someone who has discovered them, R123); none when nothing is near.
     */
    public static WorldNowHook.BossNear near(ServerPlayer p) {
        BossIndex index = BossIndex.get(p.server);
        String here = p.level().dimension().location().toString();
        BossId best = null;
        double bestDist = Double.MAX_VALUE;
        for (BossId boss : BossId.values()) {
            BossIndex.Entry e = index.of(boss);
            double d = e.pos.distSqr(p.blockPosition());
            if ((e.state == BossState.ACTIVE || e.state == BossState.OPEN) && e.dimension.equals(here) && d <= (double) NEAR_BLOCKS * NEAR_BLOCKS && d < bestDist) {
                best = boss;
                bestDist = d;
            }
        }
        if (best == null) {
            return WorldNowHook.BossNear.NONE;
        }
        boolean known = best.npcOriginal().map(o -> LineRules.showsRoot(BloodlineAccess.get(p).knownLine(), o.uuid(), SpeciesConfig.originalVisibilityAlways())).orElse(true);
        return new WorldNowHook.BossNear(index.state(best) == BossState.OPEN ? WorldNowHook.BossNear.FALLEN : WorldNowHook.BossNear.STANDING, known ? best.id() : "");
    }

    private static volatile java.util.function.Predicate<BossId> modsHook;
    private static volatile java.util.function.Predicate<ServerLevel> moonHook;
    private static volatile java.util.function.Function<ServerLevel, java.util.Collection<Villager>> villagers = DoppelgangerVillagers::loaded;

    /** Tests: which villagers count as doppelgangers (the real list has every test villager in the world); null puts the real list back. */
    @VisibleForTesting
    public static void useVillagers(@Nullable java.util.function.Function<ServerLevel, java.util.Collection<Villager>> loaded) {
        villagers = loaded == null ? DoppelgangerVillagers::loaded : loaded;
    }

    /** Tests: whether the mods of a boss are there (the matrix runs without Werewolves, but its rules are still tested); null puts the real answer back. */
    @VisibleForTesting
    public static void useMods(@Nullable java.util.function.Predicate<BossId> present) {
        modsHook = present;
    }

    /** Tests: whether it is a full moon night; the moon phase follows the time of day, which the tests must not move. Null puts the real answer back. */
    @VisibleForTesting
    public static void useMoon(@Nullable java.util.function.Predicate<ServerLevel> fullMoonNight) {
        moonHook = fullMoonNight;
    }

    /** Whether this boss's mods are there; Mikael needs nothing beyond the part. */
    public static boolean modsPresent(BossId boss) {
        java.util.function.Predicate<BossId> hook = modsHook;
        if (hook != null) {
            return hook.test(boss);
        }
        return switch (boss) {
            case KLAUS, DEUCALION, PETER_HALE, BEAST_OF_GEVAUDAN -> ModList.get().isLoaded("werewolves");
            default -> true;
        };
    }

    // ---- Elijah ----

    /** What is true of this player for Elijah (R170): they know an Original (their own line's, once discovered), or carry the blood of an NPC Original. */
    public static ElijahRules.Facts elijahFacts(ServerPlayer p) {
        boolean knows = BloodlineAccess.get(p).knownLine() != null;
        boolean holds = false;
        for (net.minecraft.world.item.ItemStack stack : p.getInventory().items) {
            UUID source = stack.getItem() instanceof net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem
                    ? net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem.sourceOf(stack) : null;
            holds |= source != null && net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.byUuid(source).isPresent()
                    || stack.getItem() == net.ofatech.supernaturalbloodlines.parts.bosses.BossItems.KLAUS_BLOOD.get();
        }
        return new ElijahRules.Facts(knows, holds, p.level().dimension() == Level.OVERWORLD, grounded.test(p), p.isSpectator());
    }

    private static volatile java.util.function.Predicate<ServerPlayer> grounded = ServerPlayer::onGround;

    /** Tests: whether a player stands on the ground (a fake player never does); null puts the real answer back. */
    @VisibleForTesting
    public static void useOnGround(@Nullable java.util.function.Predicate<ServerPlayer> onGround) {
        grounded = onGround == null ? ServerPlayer::onGround : onGround;
    }

    private static void elijah(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.ELIJAH;
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        ServerLevel overworld = server.overworld();
        if (SpawnRules.refusal(facts).isPresent()) {
            return;
        }
        long today = DesiccationRules.dayOf(overworld.getDayTime());
        BossIndex.Entry entry = index.of(boss);
        if (entry.lastRollDay == today) {
            return;
        }
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer p : players.apply(overworld)) {
            if (ElijahRules.eligible(elijahFacts(p))) {
                eligible.add(p);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }
        // one roll a day, the first time somebody could be visited; he is calm, so the day and a peaceful world are no reason to stay away
        entry.lastRollDay = today;
        index.setDirty();
        if (roll.getAsDouble() < BossWeight.chance(BossesConfig.elijahChance(), 1.0)) {
            ServerPlayer target = eligible.get(Math.min(eligible.size() - 1, (int) (roll.getAsDouble() * eligible.size())));
            spawnElijah(overworld, target, null);
        }
    }

    /** Makes Elijah 8 to 20 blocks from {@code target} (4 to 8 when there is no ground that far, or at {@code at}), and writes it down. */
    public static Optional<ElijahEntity> spawnElijah(ServerLevel level, @Nullable ServerPlayer target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null
                : ground.find(level, target.blockPosition(), 8, 20).or(() -> ground.find(level, target.blockPosition(), 4, 8)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        ElijahEntity e = ElijahEntity.create(level);
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        e.applyFactors();
        level.addFreshEntity(e);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.ELIJAH);
        index.apply(BossId.ELIJAH, BossRules.Event.SPAWNED);
        entry.appearances++;
        index.adopt(BossId.ELIJAH, e.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (target != null) {
            boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.ELIJAH.npcOriginal().orElseThrow().uuid(),
                    SpeciesConfig.originalVisibilityAlways());
            Component text = Component.translatable("supernaturalbloodlines.boss.notice.elijah." + (known ? "known" : "unknown"));
            target.displayClientMessage(text, true);
            target.sendSystemMessage(text);
        }
        return Optional.of(e);
    }

    // ---- Rebekah ----

    /**
     * The Rite of the Cure made a Cure (U2): she comes {@code rebekah_delay_seconds} later for whoever cast it, unless she lies down, is dead, is out there
     * already or is switched off. A second Cure while she is already on her way does not move her.
     */
    public static void onCureMade(net.ofatech.supernaturalbloodlines.core.boss.CureMadeEvent e) {
        MinecraftServer server = e.level().getServer();
        BossIndex index = BossIndex.get(server);
        BossId boss = BossId.REBEKAH;
        BossIndex.Entry entry = index.of(boss);
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        if (e.caster() == null || SpawnRules.refusal(facts).isPresent() || !RebekahRules.answers(index.state(boss)) || entry.arriveAt >= 0) {
            return;
        }
        entry.arriveAt = RebekahRules.arrivesAt(BossClock.nowTicks(server.overworld()), BossesConfig.rebekahDelaySeconds());
        entry.target = e.caster();
        entry.dimension = e.level().dimension().location().toString();
        entry.pos = e.pos();
        index.setDirty();
        ServerPlayer caster = SireCall.player(server, e.caster());
        if (caster != null) {
            caster.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.rebekah.rite"));
        }
    }

    private static void rebekah(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.REBEKAH;
        BossIndex.Entry entry = index.of(boss);
        ServerLevel overworld = server.overworld();
        if (entry.arriveAt < 0 || !RebekahRules.due(BossClock.nowTicks(overworld), entry.arriveAt)) {
            return;
        }
        UUID who = entry.target;
        BlockPos circle = entry.pos;
        ServerLevel where = overworld;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(entry.dimension)) {
                where = level;
            }
        }
        entry.arriveAt = -1;
        entry.target = null;
        index.setDirty();
        boolean allowed = SpawnRules.refusal(new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false)).isEmpty()
                && RebekahRules.answers(index.state(boss)) && difficulty.apply(where) != Difficulty.PEACEFUL;
        if (allowed && who != null) {
            spawnRebekah(where, circle, SireCall.player(server, who), who);
        }
    }

    /** Makes Rebekah 12 to 48 blocks from the circle (the nearest ground when there is none that far), hunting {@code who}, and writes it down. */
    public static Optional<RebekahEntity> spawnRebekah(ServerLevel level, BlockPos circle, @Nullable ServerPlayer target, @Nullable UUID who) {
        BlockPos pos = ground.find(level, circle, RebekahRules.NEAREST, RebekahRules.RADIUS).or(() -> ground.find(level, circle, 4, RebekahRules.NEAREST)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        RebekahEntity r = RebekahEntity.create(level);
        r.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        r.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        r.applyFactors();
        r.hunt(who);
        level.addFreshEntity(r);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.REBEKAH);
        index.apply(BossId.REBEKAH, BossRules.Event.SPAWNED);
        entry.appearances++;
        entry.target = who;
        index.adopt(BossId.REBEKAH, r.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (target != null) {
            boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.REBEKAH.npcOriginal().orElseThrow().uuid(),
                    SpeciesConfig.originalVisibilityAlways());
            Component text = Component.translatable("supernaturalbloodlines.boss.notice.rebekah." + (known ? "known" : "unknown"));
            target.displayClientMessage(text, true);
            target.sendSystemMessage(text);
            target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.rebekah.hint"));
        }
        return Optional.of(r);
    }

    // ---- Kol ----

    /** What is true of this player for Kol (R173). */
    public static net.ofatech.supernaturalbloodlines.core.boss.KolRules.Facts kolFacts(ServerPlayer p) {
        var state = BloodlineAccess.get(p);
        return new net.ofatech.supernaturalbloodlines.core.boss.KolRules.Facts(state.species() == net.ofatech.supernaturalbloodlines.core.bloodline.Species.HUNTER_OF_THE_FIVE,
                state.markKills(), p.level().dimension() == Level.OVERWORLD, grounded.test(p), p.isSpectator());
    }

    private static void kol(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.KOL;
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        ServerLevel overworld = server.overworld();
        if (SpawnRules.refusal(facts).isPresent() || !MikaelRules.isNight(overworld.getDayTime())) {
            return;
        }
        long today = DesiccationRules.dayOf(overworld.getDayTime());
        BossIndex.Entry entry = index.of(boss);
        if (entry.lastRollDay == today) {
            return;
        }
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer p : players.apply(overworld)) {
            if (net.ofatech.supernaturalbloodlines.core.boss.KolRules.eligible(kolFacts(p), BossesConfig.kolMinMark())) {
                eligible.add(p);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }
        // once a night: if he cannot be placed, or withdraws, he tries again the next one
        entry.lastRollDay = today;
        index.setDirty();
        ServerPlayer target = eligible.get(Math.min(eligible.size() - 1, (int) (roll.getAsDouble() * eligible.size())));
        spawnKol(overworld, target, null);
    }

    /** Makes Kol 16 to 32 blocks from {@code target} (8 to 16 when there is no ground that far, or at {@code at}), hunting them, and writes it down. */
    public static Optional<net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity> spawnKol(ServerLevel level, @Nullable ServerPlayer target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null
                : ground.find(level, target.blockPosition(), 16, 32).or(() -> ground.find(level, target.blockPosition(), 8, 16)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        var e = net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity.create(level);
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        e.applyFactors();
        if (target != null) {
            e.hunt(target.getUUID());
        }
        level.addFreshEntity(e);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.KOL);
        index.apply(BossId.KOL, BossRules.Event.SPAWNED);
        entry.appearances++;
        index.adopt(BossId.KOL, e.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (target != null) {
            boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.KOL.npcOriginal().orElseThrow().uuid(),
                    SpeciesConfig.originalVisibilityAlways());
            Component text = Component.translatable("supernaturalbloodlines.boss.notice.kol." + (known ? "known" : "unknown"));
            target.displayClientMessage(text, true);
            target.sendSystemMessage(text);
            target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.kol.hint"));
        }
        return Optional.of(e);
    }

    // ---- Finn ----

    private static volatile java.util.function.Predicate<MinecraftServer> eclipse = net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse::active;

    /** Tests: whether a lunar eclipse is running; null puts the real answer back. */
    @VisibleForTesting
    public static void useEclipse(@Nullable java.util.function.Predicate<MinecraftServer> running) {
        eclipse = running == null ? net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse::active : running;
    }

    public static boolean eclipseNow(MinecraftServer server) {
        return eclipse.test(server);
    }

    /** The players of this level, as the director sees them (a test may have fixed them: its fake players are not in the level's list). */
    public static List<ServerPlayer> playersIn(ServerLevel level) {
        return players.apply(level);
    }

    private static void finn(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.FINN;
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        ServerLevel overworld = server.overworld();
        if (SpawnRules.refusal(facts).isPresent() || !eclipseNow(server)) {
            return;
        }
        long today = DesiccationRules.dayOf(overworld.getDayTime());
        BossIndex.Entry entry = index.of(boss);
        if (!net.ofatech.supernaturalbloodlines.core.boss.FinnRules.mayRoll(entry.lastRollDay, today)) {
            return;
        }
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer p : players.apply(overworld)) {
            if (net.ofatech.supernaturalbloodlines.core.boss.FinnRules.eligible(new net.ofatech.supernaturalbloodlines.core.boss.FinnRules.Facts(true,
                    p.level().dimension() == Level.OVERWORLD,
                    net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory.isWitchType(BloodlineAccess.get(p).species()) || net.ofatech.supernaturalbloodlines.core.Supernatural.isVampiric(p),
                    p.isSpectator()))) {
                eligible.add(p);
            }
        }
        if (eligible.isEmpty()) {
            return;
        }
        // one roll a night, the first time somebody could be visited
        entry.lastRollDay = today;
        index.setDirty();
        if (roll.getAsDouble() < BossWeight.chance(BossesConfig.finnChance(), 1.0)) {
            ServerPlayer target = eligible.get(Math.min(eligible.size() - 1, (int) (roll.getAsDouble() * eligible.size())));
            spawnFinn(overworld, target, null);
        }
    }

    /** Makes Finn 32 to 64 blocks from {@code target} (16 to 32 when there is no ground that far, or at {@code at}), at the start of his ritual, and writes it down. */
    public static Optional<net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity> spawnFinn(ServerLevel level, @Nullable ServerPlayer target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null
                : ground.find(level, target.blockPosition(), 32, 64).or(() -> ground.find(level, target.blockPosition(), 16, 32)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        var e = net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity.create(level);
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        e.applyFactors();
        e.beginRitual(pos, BossesConfig.finnRitualSeconds());
        level.addFreshEntity(e);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.FINN);
        index.apply(BossId.FINN, BossRules.Event.SPAWNED);
        entry.appearances++;
        index.adopt(BossId.FINN, e.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (target != null) {
            boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.FINN.npcOriginal().orElseThrow().uuid(),
                    SpeciesConfig.originalVisibilityAlways());
            Component text = Component.translatable("supernaturalbloodlines.boss.notice.finn." + (known ? "known" : "unknown"));
            target.displayClientMessage(text, true);
            target.sendSystemMessage(text);
            target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.finn.hint"));
        }
        return Optional.of(e);
    }

    // ---- Klaus ----
    /** Whether it is a full moon night in this level now (the director's own answer, which the tests may pin). */
    public static boolean fullMoonNightNow(ServerLevel level) {
        return fullMoonNight(level);
    }

    private static boolean fullMoonNight(ServerLevel level) {
        java.util.function.Predicate<ServerLevel> hook = moonHook;
        return hook != null ? hook.test(level) : MoonRule.isFullMoonNight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime());
    }

    /** Whether the player has the doppelganger gene and may be hunted (R162). */
    private static boolean huntable(ServerPlayer p) {
        return BloodlineAccess.get(p).doppelgangerGene()
                && KlausRules.eligible(new KlausRules.Target(true, true, p.level().dimension() == Level.OVERWORLD, p.isSpectator(), true), BossesConfig.klausHuntsPlayers());
    }

    private static void klaus(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.KLAUS;
        BossIndex.Entry entry = index.of(boss);
        ServerLevel overworld = server.overworld();
        if (entry.arriveAt >= 0) {
            klausArrives(server, index, entry, overworld);
            return;
        }
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        if (SpawnRules.refusal(facts).isPresent() || !fullMoonNight(overworld) || difficulty.apply(overworld) == Difficulty.PEACEFUL) {
            return;
        }
        long today = DesiccationRules.dayOf(overworld.getDayTime());
        if (entry.lastRollDay == today) {
            return;
        }
        List<ServerPlayer> here = players.apply(overworld);
        List<LivingEntity> targets = new ArrayList<>();
        for (ServerPlayer p : here) {
            if (huntable(p)) {
                targets.add(p);
            }
        }
        for (Villager v : villagers.apply(overworld)) {
            boolean near = here.stream().anyMatch(p -> p.level() == overworld && p.distanceToSqr(v) <= (double) KlausRules.VILLAGER_RANGE * KlausRules.VILLAGER_RANGE);
            if (v.isAlive() && KlausRules.eligible(new KlausRules.Target(false, true, true, false, near), BossesConfig.klausHuntsPlayers())) {
                targets.add(v);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        // one roll a night, the first time somebody could be hunted
        entry.lastRollDay = today;
        index.setDirty();
        if (!KlausRules.picks(true, true, BossWeight.chance(BossesConfig.klausChance(), 1.0), roll.getAsDouble())) {
            return;
        }
        LivingEntity target = targets.get(Math.min(targets.size() - 1, (int) (roll.getAsDouble() * targets.size())));
        if (target instanceof ServerPlayer p) {
            long warning = KlausRules.warningTicks(true, BossesConfig.klausWarningSeconds());
            if (warning > 0) {
                entry.target = p.getUUID();
                entry.arriveAt = BossClock.nowTicks(overworld) + warning;
                entry.reminded = 0;
                index.setDirty();
                noticeKlaus(p, BossesConfig.klausWarningSeconds());
                return;
            }
            noticeKlaus(p, 0);
        }
        spawnKlaus(overworld, target, null);
    }

    /** The warning has run out: he comes if the one he was coming for is still here, and goes nowhere if they are not. */
    private static void klausArrives(MinecraftServer server, BossIndex index, BossIndex.Entry entry, ServerLevel overworld) {
        UUID id = entry.target;
        ServerPlayer p = id == null ? null : SireCall.player(server, id);
        long now = BossClock.nowTicks(overworld);
        // the same questions as when he was announced, asked again at every look, not only when the time is up: the player may have died, left, changed
        // dimension or gone to spectator, the switch may have been turned off, or the world made peaceful
        boolean still = p != null && p.isAlive() && huntable(p) && difficulty.apply(overworld) != Difficulty.PEACEFUL && fullMoonNight(overworld);
        boolean allowed = SpawnRules.refusal(new BossFacts(BossesConfig.enabled(BossId.KLAUS), modsPresent(BossId.KLAUS), index.state(BossId.KLAUS), false)).isEmpty();
        if (!still || !allowed) {
            entry.arriveAt = -1;
            entry.target = null;
            entry.reminded = 0;
            index.setDirty();
            if (p != null) {
                p.displayClientMessage(Component.translatable(KlausEntity.LOST_SCENT), true);
            }
            return;
        }
        if (!KlausRules.arrivalDue(now, entry.arriveAt)) {
            int seconds = KlausRules.reminder(entry.arriveAt - now, KlausRules.warningTicks(true, BossesConfig.klausWarningSeconds()), entry.reminded);
            if (seconds > 0) {
                entry.reminded = seconds;
                index.setDirty();
                p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.klaus.countdown", seconds), true);
            }
            return;
        }
        entry.arriveAt = -1;
        entry.target = null;
        entry.reminded = 0;
        index.setDirty();
        spawnKlaus(overworld, p, null);
    }

    /**
     * Makes Klaus, 24 to 40 blocks from {@code target} (6 to 24 when there is no ground that far, or at {@code at} when it is given), and writes it down. Empty
     * when there is no ground for him. The caller has already asked {@link SpawnRules}.
     */
    public static Optional<KlausEntity> spawnKlaus(ServerLevel level, @Nullable LivingEntity target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null
                : ground.find(level, target.blockPosition(), 24, 40).or(() -> ground.find(level, target.blockPosition(), 6, 24)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        KlausEntity k = KlausEntity.create(level);
        k.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        k.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        k.applyFactors();
        k.hunt(target == null ? null : target.getUUID());
        level.addFreshEntity(k);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.KLAUS);
        index.apply(BossId.KLAUS, BossRules.Event.SPAWNED);
        entry.appearances++;
        entry.target = target == null ? null : target.getUUID();
        entry.arriveAt = -1;
        index.adopt(BossId.KLAUS, k.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        return Optional.of(k);
    }

    // ---- Deucalion ----

    private static boolean deucalionTarget(ServerPlayer p) {
        boolean werewolf = net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural.isWerewolf(p);
        var wolf = net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess.wolf(p);
        // a True Alpha holds the full stack, so he does come for them (their reward is the Roar only)
        return DeucalionRules.eligible(new DeucalionRules.Target(werewolf, net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess.isAlpha(wolf.rank()),
                net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess.effectiveSparks(p),
                p.level().dimension() == Level.OVERWORLD, p.isSpectator()), BossesConfig.deucalionMinSparks());
    }

    private static void deucalion(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.DEUCALION;
        ServerLevel overworld = server.overworld();
        long today = DesiccationRules.dayOf(overworld.getDayTime());
        BossIndex.Entry entry = index.of(boss);
        // the respawn, when it is switched on: his state and his reward start again (the day he died is written down in returnDay)
        if (index.state(boss) == BossState.DEAD && DeucalionRules.mayRespawn(BossesConfig.deucalionRespawn(), BossClock.today(overworld), entry.returnDay, BossesConfig.deucalionRespawnDays())) {
            index.forget(boss);
            entry = index.of(boss);
        }
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        if (SpawnRules.refusal(facts).isPresent() || !fullMoonNight(overworld) || difficulty.apply(overworld) == Difficulty.PEACEFUL || entry.lastRollDay == today) {
            return;
        }
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer p : players.apply(overworld)) {
            if (deucalionTarget(p)) {
                targets.add(p);
            }
        }
        if (!DeucalionRules.picks(true, !targets.isEmpty())) {
            return;
        }
        // one night, one try: when there is no ground for him he tries again the next full moon
        entry.lastRollDay = today;
        index.setDirty();
        ServerPlayer target = targets.get(Math.min(targets.size() - 1, (int) (roll.getAsDouble() * targets.size())));
        if (spawnDeucalion(overworld, target, null).isPresent()) {
            noticeDeucalion(target);
        }
    }

    /** Makes Deucalion 24 to 40 blocks from {@code target} (6 to 24 with no ground that far, or at {@code at}) with his pack, and writes it down. Empty when there is no ground for him. */
    public static Optional<DeucalionEntity> spawnDeucalion(ServerLevel level, @Nullable LivingEntity target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null
                : ground.find(level, target.blockPosition(), 24, 40).or(() -> ground.find(level, target.blockPosition(), 6, 24)).orElse(null);
        if (pos == null) {
            return Optional.empty();
        }
        DeucalionEntity d = DeucalionEntity.create(level);
        d.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        d.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        d.applyFactors();
        d.hunt(target == null ? null : target.getUUID());
        level.addFreshEntity(d);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.DEUCALION);
        index.apply(BossId.DEUCALION, BossRules.Event.SPAWNED);
        entry.appearances++;
        entry.target = target == null ? null : target.getUUID();
        entry.arriveAt = -1;
        index.adopt(BossId.DEUCALION, d.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (ModList.get().isLoaded("werewolves")) {
            DeucalionPack.summon(level, pos, BossesConfig.deucalionPackAlphas(), target, d.pack());
        }
        return Optional.of(d);
    }

    /** "He wants your sparks": on the action bar and in chat, with a howl for the hunted one alone. */
    static void noticeDeucalion(ServerPlayer target) {
        Component text = Component.translatable("supernaturalbloodlines.boss.notice.deucalion", BossesConfig.deucalionMinSparks());
        target.displayClientMessage(text, true);
        target.sendSystemMessage(text);
        target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.deucalion.hint"));
        target.playNotifySound(net.minecraft.sounds.SoundEvents.WOLF_HOWL, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    /** "A wolf is howling for you" (his name once the player knows the Original, R123), on the action bar and in chat, with a howl, the time left and the hint. */
    static void noticeKlaus(ServerPlayer target, int seconds) {
        boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.KLAUS.npcOriginal().orElseThrow().uuid(),
                SpeciesConfig.originalVisibilityAlways());
        String key = "supernaturalbloodlines.boss.notice.klaus." + (known ? "known" : "unknown") + (seconds > 0 ? "" : "_now");
        Component text = Component.translatable(key, seconds);
        // being hunted is how a doppelganger finds out what they are (the show's way): the gene is discovered here, and the message says why
        BloodlineAccess.reveal(target, Genes.DOPPELGANGER);
        target.displayClientMessage(text, true);
        target.sendSystemMessage(text);
        target.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.notice.klaus.hint"));
        // the howl is for the one he hunts, not for everybody near: nobody else is told who is hunted
        target.playNotifySound(net.minecraft.sounds.SoundEvents.WOLF_HOWL, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    // ---- Peter Hale ----

    /** Where the nearest piece of a burned Hale house is to a point: its bounding box, or nothing. The real search looks at the structure references of the loaded chunks. */
    @FunctionalInterface
    public interface HouseFinder {
        Optional<net.minecraft.world.level.levelgen.structure.BoundingBox> nearest(ServerLevel level, BlockPos from);
    }

    /** The tag of the structures that are a Hale house ({@code #supernaturalbloodlines:hale_house}). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> HALE_HOUSE = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.ofatech.supernaturalbloodlines.SupernaturalBloodlines.MODID, "hale_house"));

    private static volatile HouseFinder houses = BossDirector::realHouse;

    /** Tests: where the houses are, because the test level generates none. {@code null} puts the real search back. */
    @VisibleForTesting
    public static void useHouses(@Nullable HouseFinder finder) {
        houses = finder == null ? BossDirector::realHouse : finder;
    }

    /** The real search, whatever the test hook says: the nearest piece of a house to a point, or nothing. */
    public static Optional<net.minecraft.world.level.levelgen.structure.BoundingBox> nearestHouse(ServerLevel level, BlockPos from) {
        return realHouse(level, from);
    }

    /**
     * A house with a piece within {@link PeterRules#HOUSE_RADIUS} of a point, or nothing: the chunks that cover the square round the point are asked for the starts of
     * a Hale house (one call each, 25 at most), and a piece counts by its distance to the point.
     */
    private static Optional<net.minecraft.world.level.levelgen.structure.BoundingBox> realHouse(ServerLevel level, BlockPos from) {
        var manager = level.structureManager();
        int r = PeterRules.HOUSE_RADIUS;
        for (int cx = (from.getX() - r) >> 4; cx <= (from.getX() + r) >> 4; cx++) {
            for (int cz = (from.getZ() - r) >> 4; cz <= (from.getZ() + r) >> 4; cz++) {
                for (var start : manager.startsForStructure(new net.minecraft.world.level.ChunkPos(cx, cz), s -> s.type() == net.ofatech.supernaturalbloodlines.parts.bosses.world.HaleHouse.TYPE.get())) {
                    for (var piece : start.getPieces()) {
                        var b = piece.getBoundingBox();
                        if (PeterRules.near(PeterRules.boxDistanceSquared(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()))) {
                            return Optional.of(start.getBoundingBox());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static void peter(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.PETER_HALE;
        ServerLevel overworld = server.overworld();
        BossIndex.Entry entry = index.of(boss);
        // the respawn, when it is switched on: his state and his reward start again (the day he died is written down in returnDay)
        if (index.state(boss) == BossState.DEAD && PeterRules.mayRespawn(BossesConfig.peterRespawn(), BossClock.today(overworld), entry.returnDay, BossesConfig.peterRespawnDays())) {
            index.forget(boss);
        }
        BossFacts facts = new BossFacts(BossesConfig.enabled(boss), modsPresent(boss), index.state(boss), false);
        if (SpawnRules.refusal(facts).isPresent() || difficulty.apply(overworld) == Difficulty.PEACEFUL) {
            return;
        }
        for (ServerPlayer p : players.apply(overworld)) {
            Optional<net.minecraft.world.level.levelgen.structure.BoundingBox> house = p.level().dimension() == Level.OVERWORLD && !p.isSpectator()
                    ? houses.nearest(overworld, p.blockPosition()) : Optional.empty();
            if (PeterRules.calledBy(p.level().dimension() == Level.OVERWORLD, p.isSpectator(), house.isPresent())) {
                spawnPeter(overworld, house.get());
                // once, to the one who called him, and never his name: the house is not empty
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.boss.notice.peter"), true);
                p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.boss.notice.peter.hint"));
                return;
            }
        }
    }

    /** Where he stands in a house: the middle of its footprint, one block above the floor (the cellar is below it). */
    public static BlockPos peterSpot(net.minecraft.world.level.levelgen.structure.BoundingBox house) {
        return new BlockPos(house.getCenter().getX(), house.minY() + PeterRules.SPAWN_OFFSET, house.getCenter().getZ());
    }

    /** Makes Peter in the house and writes it down. */
    public static Optional<PeterHaleEntity> spawnPeter(ServerLevel level, net.minecraft.world.level.levelgen.structure.BoundingBox house) {
        return spawnPeterAt(level, peterSpot(house));
    }

    /** Makes Peter at a spot (the house, or where a command says) and writes it down. His home is the spot. */
    public static Optional<PeterHaleEntity> spawnPeterAt(ServerLevel level, BlockPos pos) {
        PeterHaleEntity p = PeterHaleEntity.create(level);
        p.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        p.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        p.applyFactors();
        p.setHome(pos);
        level.addFreshEntity(p);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.PETER_HALE);
        index.apply(BossId.PETER_HALE, BossRules.Event.SPAWNED);
        entry.appearances++;
        entry.arriveAt = -1;
        index.adopt(BossId.PETER_HALE, p.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        return Optional.of(p);
    }

    /** The loaded body of a boss of any kind (Deucalion is no Original). */
    @Nullable
    public static net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc anyBossBody(MinecraftServer server, BossId boss) {
        UUID id = BossIndex.get(server).of(boss).entity;
        return id != null && find(server, id) instanceof net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc b ? b : null;
    }

    @Nullable
    public static NpcOriginalBoss bossEntity(MinecraftServer server, BossId boss) {
        UUID id = BossIndex.get(server).of(boss).entity;
        return id != null && find(server, id) instanceof NpcOriginalBoss b ? b : null;
    }
}
