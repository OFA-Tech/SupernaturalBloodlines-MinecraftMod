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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossFacts;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.BossWeight;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules;
import net.ofatech.supernaturalbloodlines.core.boss.SpawnRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
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

    public static void register() {
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

    /** Tests: the dice. {@code null} puts the real ones back. */
    @VisibleForTesting
    public static void useRoll(@Nullable DoubleSupplier fixed) {
        roll = fixed == null ? () -> ThreadLocalRandom.current().nextDouble() : fixed;
        MISSES.clear();
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
    }

    // ---- lost bosses ----

    @Nullable
    private static Entity find(MinecraftServer server, UUID id) {
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
                || overworld.getDifficulty() == Difficulty.PEACEFUL) {
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

    private static MikaelRules.Facts realFacts(ServerPlayer p) {
        boolean ring = p.level() instanceof ServerLevel l && Imprisonment.ringClosed(l, p.blockPosition(), ASH_RING_RADIUS);
        return new MikaelRules.Facts(Supernatural.isVampiric(p), SupernaturalAccess.get(p).rung(SupernaturalRace.VAMPIRE), BossesConfig.mikaelMinRung(),
                MikaelRules.isNight(p.level().getDayTime()), p.level().dimension() == Level.OVERWORLD, p.onGround(), ring);
    }

    /**
     * Makes Mikael, 24 to 40 blocks from {@code target} (or at {@code at} when it is given), and writes it down. Empty when there is no ground for him.
     * The caller has already asked {@link SpawnRules}.
     */
    public static Optional<MikaelEntity> spawnMikael(ServerLevel level, @Nullable ServerPlayer target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null : groundNear(level, target.blockPosition(), 24, 40).orElse(null);
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
        index.seen(BossId.MIKAEL, m.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        if (target != null) {
            notice(target);
        }
        return Optional.of(m);
    }

    /** "A hunter in black is watching you", and the name once the player has discovered the Original (R123). */
    private static void notice(ServerPlayer target) {
        boolean known = LineRules.showsRoot(BloodlineAccess.get(target).knownLine(), BossId.MIKAEL.npcOriginal().orElseThrow().uuid(),
                SpeciesConfig.originalVisibilityAlways());
        target.sendSystemMessage(Component.translatable(known ? "supernaturalbloodlines.boss.notice.mikael.known" : "supernaturalbloodlines.boss.notice.mikael.unknown"));
    }

    /** A spot on the ground between {@code min} and {@code max} blocks from {@code from}: empty air above solid, no fluid. */
    public static Optional<BlockPos> groundNear(ServerLevel level, BlockPos from, int min, int max) {
        for (int i = 0; i < 16; i++) {
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

    /** Whether a boss that is out there (or down) is within 128 blocks of the player: the Home page's "world now" line. */
    public static boolean bossNear(ServerPlayer p) {
        BossIndex index = BossIndex.get(p.server);
        ResourceLocation here = p.level().dimension().location();
        for (BossId boss : BossId.values()) {
            BossIndex.Entry e = index.of(boss);
            if ((e.state == BossState.ACTIVE || e.state == BossState.OPEN) && e.dimension.equals(here.toString())
                    && e.pos.distSqr(p.blockPosition()) <= 128.0 * 128.0) {
                return true;
            }
        }
        return false;
    }

    /** Whether this boss's mods are there; Mikael needs nothing beyond the part. */
    public static boolean modsPresent(BossId boss) {
        return switch (boss) {
            case KLAUS, DEUCALION, PETER_HALE, BEAST_OF_GEVAUDAN -> ModList.get().isLoaded("werewolves");
            default -> true;
        };
    }

    @Nullable
    public static NpcOriginalBoss bossEntity(MinecraftServer server, BossId boss) {
        UUID id = BossIndex.get(server).of(boss).entity;
        return id != null && find(server, id) instanceof NpcOriginalBoss b ? b : null;
    }
}
