package net.ofatech.supernaturalbloodlines.parts.species.npc;

import de.teamlapen.vampirism.api.entity.IEntityLeader;
import de.teamlapen.werewolves.core.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import org.jetbrains.annotations.Nullable;

/**
 * The spawner of the NPC werewolf packs (R143): on a full-moon night (a running eclipse is one; a Blood Moon on another night rolls at
 * {@code blood_moon_pack_chance_factor}), once per player per night, an Alpha of the Werewolves mod and its wolves, and a lone wolf, appear 24 to 48
 * blocks away in the dark and away from villages. They are marked ({@link NpcMark}) so {@link NpcPackBrain} can hunt with them and send them away at
 * dawn. The Werewolves mod's own taiga spawns and natural Alphas are not touched. Links Werewolves: registered only when it is loaded.
 */
public final class NpcPackSpawner {
    static final int PERIOD = 200;
    /** The wolves of a pack stand within this many blocks of where it spawned. */
    private static final int SPREAD = 4;
    private static final double HUMAN_SHARE = 0.15;

    private NpcPackSpawner() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && level.getGameTime() % PERIOD == 0) {
                roll(level, level.players(), level.random::nextDouble, near -> findSpot(level, near));
            }
        });
    }

    /** What tonight is on this level (the moon, the clock, Enhanced Celestials and a running eclipse); NONE by day. */
    public static MoonCalendar.Night tonight(ServerLevel level) {
        long timeOfDay = Math.floorMod(level.getDayTime(), 24000L);
        if (timeOfDay < MoonRule.NIGHT_START || timeOfDay >= MoonRule.NIGHT_END) {
            return MoonCalendar.Night.NONE;
        }
        return MoonCalendar.tonight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime(), LunarMoods.now(level),
                LunarEclipse.active(level.getServer()));
    }

    /**
     * One roll for every player of {@code players} who has not been rolled for tonight. {@code spots} finds the place a pack appears near a player (null:
     * none); the tests give it a fixed one, the game a dark one away from villages. @return every wolf it spawned (an Alpha, a pack's wolves and lone wolves)
     */
    public static List<Mob> roll(ServerLevel level, java.util.Collection<ServerPlayer> players, DoubleSupplier dice, UnaryOperator<BlockPos> spots) {
        SpeciesConfig.NpcTuning t = SpeciesConfig.npc();
        MoonCalendar.Night night = tonight(level);
        if (!t.packs() || night == MoonCalendar.Night.NONE) {
            return List.of();
        }
        long id = NpcSpawnRules.nightOf(level.getDayTime());
        NpcPackNights ledger = NpcPackNights.get(level);
        List<Mob> spawned = new ArrayList<>();
        for (ServerPlayer p : new ArrayList<>(players)) {
            if (p.isSpectator() || !NpcSpawnRules.mayRoll(ledger.lastNight(p.getUUID()), id, ledger.packsIn(id), t.spawn().maxPacksPerNight())) {
                continue;
            }
            ledger.rolled(p.getUUID(), id);
            NpcSpawnRules.Plan plan = NpcSpawnRules.plan(night, LunarMoods.packChance(level), t.spawn(), Integer.MAX_VALUE, dice);
            BlockPos spot = plan.any() ? spots.apply(p.blockPosition()) : null;
            if (spot == null) {
                continue;
            }
            if (plan.pack()) {
                List<Mob> pack = spawnPackAt(level, spot, plan, id);
                if (!pack.isEmpty()) {
                    ledger.packSpawned(id);
                    spawned.addAll(pack);
                }
            }
            Mob lone = plan.lone() ? spawnLoneAt(level, spot, id) : null;
            if (lone != null) {
                spawned.add(lone);
            }
        }
        return spawned;
    }

    /** The pack at exactly {@code spot} (the debug command and the tests); the Alpha first, then the wolves. */
    public static List<Mob> spawnPackAt(ServerLevel level, BlockPos spot, NpcSpawnRules.Plan plan, long night) {
        long packId = NpcPackNights.get(level).newPackId();
        NpcMark mark = NpcMark.wild(packId, night);
        List<Mob> out = new ArrayList<>();
        int wolves = plan.wolves();
        if (plan.alpha()) {
            Mob alpha = spawn(level, asMob(ModEntities.ALPHA_WEREWOLF.get()), spot, mark);
            if (alpha != null) {
                out.add(alpha);
                // the Alpha can lead only so many (the Werewolves mod's own limit, read at run time)
                if (alpha instanceof IEntityLeader leader) {
                    wolves = Math.max(1, Math.min(wolves, leader.getMaxFollowerCount()));
                }
            }
        }
        RandomSource rnd = level.random;
        for (int i = 0; i < wolves; i++) {
            BlockPos at = scatter(level, spot, rnd);
            Mob m = spawn(level, wolfType(rnd.nextDouble(), rnd.nextBoolean()), at, mark);
            if (m != null) {
                out.add(m);
            }
        }
        return out;
    }

    @Nullable
    public static Mob spawnLoneAt(ServerLevel level, BlockPos spot, long night) {
        long packId = NpcPackNights.get(level).newPackId();
        return spawn(level, wolfType(level.random.nextDouble(), level.random.nextBoolean()), spot, NpcMark.wild(packId, night));
    }

    private static EntityType<? extends Mob> wolfType(double humanRoll, boolean beast) {
        if (humanRoll < HUMAN_SHARE) {
            return asMob(ModEntities.HUMAN_WEREWOLF.get());
        }
        return asMob(beast ? ModEntities.WEREWOLF_BEAST.get() : ModEntities.WEREWOLF_SURVIVALIST.get());
    }

    /** The Werewolves mod's entity classes extend a Vampirism class that is not on this build's compile path, so their types are handled as plain mobs. */
    @SuppressWarnings("unchecked")
    public static EntityType<? extends Mob> asMob(EntityType<?> type) {
        return (EntityType<? extends Mob>) type;
    }

    @Nullable
    private static Mob spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos, NpcMark mark) {
        // an Alpha is made at half its highest level: the Werewolves mod would work a level out from the players near it and refuse to let an Alpha in
        // beside a weak one (see place)
        Mob m = place(level, type, pos, ModEntities.ALPHA_WEREWOLF.getId().equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type)) ? HALF_LEVEL : NO_LEVEL);
        if (m != null) {
            m.setData(SpeciesRegistries.NPC_MARK, mark);
        }
        return m;
    }

    /** {@link #place}: leave the level to the Werewolves mod. */
    public static final int NO_LEVEL = -1;
    /** {@link #place}: half the highest level of the entity. */
    public static final int HALF_LEVEL = -2;
    /** {@link #place}: the highest level of the entity. */
    public static final int MAX_LEVEL = -3;

    /**
     * Makes {@code type} at {@code pos} and puts it in the world, or returns null when it did not get in. The Werewolves mod (through Vampirism) cancels
     * the join of a leveled mob that has no level yet when the players near it are too weak for it (an Alpha beside a low-level werewolf): it would be
     * returned by {@code EntityType.spawn} but never exist. Giving it a level first, as that rule asks, is how it gets in.
     */
    @Nullable
    public static Mob place(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos, int preset) {
        Mob m = type.create(level);
        if (m == null) {
            return null;
        }
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        if (preset != NO_LEVEL && m instanceof de.teamlapen.vampirism.api.difficulty.IAdjustableLevel leveled) {
            int max = leveled.getMaxEntityLevel();
            leveled.setEntityLevel(preset == MAX_LEVEL ? max : Math.max(1, max / 2));
        }
        level.addFreshEntityWithPassengers(m);
        if (!m.isAddedToLevel()) {
            m.discard();
            return null;
        }
        return m;
    }

    private static BlockPos scatter(ServerLevel level, BlockPos around, RandomSource rnd) {
        int x = around.getX() + rnd.nextInt(2 * SPREAD + 1) - SPREAD;
        int z = around.getZ() + rnd.nextInt(2 * SPREAD + 1) - SPREAD;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        return Math.abs(top.getY() - around.getY()) <= 6 ? top : around;
    }

    /** A dark, solid, dry spot 24 to 48 blocks from {@code near}, at least not in a village; null after ten tries. */
    @Nullable
    static BlockPos findSpot(ServerLevel level, BlockPos near) {
        RandomSource rnd = level.random;
        for (int i = 0; i < 10; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double dist = 24 + rnd.nextDouble() * 24;
            int x = near.getX() + (int) (Math.cos(angle) * dist);
            int z = near.getZ() + (int) (Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (isGoodSpot(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Ground for a debug pack near {@code pos}: the nearest free spot (two blocks of air over a solid floor) to its height, looking 8 blocks down and
     * up (down first on a tie: the floor of the room, not the roof of it), so a roof or a cave does not decide it the way a heightmap would; null when there is none.
     */
    @Nullable
    public static BlockPos groundNear(ServerLevel level, BlockPos pos) {
        for (int d = 0; d <= 8; d++) {
            for (int sign : d == 0 ? new int[] {1} : new int[] {-1, 1}) {
                BlockPos p = pos.offset(0, d * sign, 0);
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                        && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty() && level.getFluidState(p).isEmpty()) {
                    return p;
                }
            }
        }
        return null;
    }

    static boolean isGoodSpot(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir() && level.getFluidState(pos.below()).isEmpty() && level.getFluidState(pos).isEmpty()
                && level.getMaxLocalRawBrightness(pos) <= 7 && !level.isVillage(pos) && !level.isCloseToVillage(pos, 2);
    }

}
