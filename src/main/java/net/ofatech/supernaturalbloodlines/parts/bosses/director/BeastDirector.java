package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.boss.BeastPhase;
import net.ofatech.supernaturalbloodlines.core.boss.BeastPhaseRules;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.PikeRules;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanHostEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanSpiritEntity;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.Imprisonment;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/**
 * The Beast of Gevaudan's encounter (R184 to R187), one step at a time. {@link #tick} (the director's look at the world) makes the Host on a full moon night, once
 * per qualifying night, for an Alpha, a True Alpha or a Hunter of the Five, and makes the Host change into the Beast on a later full moon night; the Beast reports
 * itself through {@link #watch} (it collapses at a quarter of its health or in a closed ring of mountain ash), the Spirit through {@link #escape} and
 * {@link #killed}. Every step writes the phase ({@link BeastPhase}) in the index, tells the players near what just happened, and keeps the index's entity the
 * one that is alive: the next body is written down before the last one goes. The reward (the Mantle, once, to the player who did the most to the Beast) is {@link #pay}.
 */
public final class BeastDirector {
    /** Players this close to the place are told what just happened there. */
    public static final double NEAR = 64.0;
    public static final String HOST_NOTICE = "supernaturalbloodlines.boss.notice.beast";
    public static final String HOST_HINT = "supernaturalbloodlines.boss.notice.beast.hint";
    public static final String CHANGE = "supernaturalbloodlines.boss.beast.change";
    public static final String COLLAPSE = "supernaturalbloodlines.boss.beast.collapse";
    public static final String ESCAPED = "supernaturalbloodlines.boss.beast.escaped";
    public static final String GONE = "supernaturalbloodlines.boss.beast.gone";
    public static final String GONE_FINAL = "supernaturalbloodlines.boss.beast.gone_final";
    public static final String ENDED = "supernaturalbloodlines.boss.beast.ended";
    public static final String FAILED = "supernaturalbloodlines.boss.beast.failed";
    public static final String REWARD = "supernaturalbloodlines.boss.beast.reward";
    public static final String REWARD_DROPPED = "supernaturalbloodlines.boss.beast.reward_dropped";
    public static final String REWARD_TO = "supernaturalbloodlines.boss.beast.reward_to";
    public static final String REWARD_OWED = "supernaturalbloodlines.boss.beast.reward_owed";
    public static final String REWARD_LATE = "supernaturalbloodlines.boss.beast.reward_late";
    public static final String SOMEONE = "supernaturalbloodlines.boss.beast.someone";

    private BeastDirector() {
    }

    // ---- the Host ----

    /** Whether this player may be the one the Host appears for: an Alpha or True Alpha, or a Hunter of the Five, in the overworld, and no spectator. */
    public static boolean hostTarget(ServerPlayer p) {
        boolean alpha = PackAccess.isAlpha(PackAccess.wolf(p).rank());
        boolean hunter = BloodlineAccess.get(p).species() == Species.HUNTER_OF_THE_FIVE;
        return BeastRules.hostTarget(new BeastRules.Target(alpha, hunter, p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD, p.isSpectator()));
    }

    private static long dayOf(ServerLevel overworld) {
        return DesiccationRules.dayOf(overworld.getDayTime());
    }

    /** One look at the world, from {@link BossDirector#check}. */
    static void tick(MinecraftServer server, BossIndex index) {
        BossId boss = BossId.BEAST_OF_GEVAUDAN;
        if (!BossesConfig.enabled(boss) || !BossDirector.modsPresent(boss)) {
            return;
        }
        BossIndex.Entry entry = index.of(boss);
        ServerLevel overworld = server.overworld();
        if (BeastPhaseRules.bodyLost(entry.phase, index.state(boss) == BossState.LOST)) {
            bodyLost(server, index, entry);
            return;
        }
        if (BeastPhaseRules.spiritOverdue(entry.phase, overworld.getGameTime(), entry.spiritUntil, spiritAlive(server, entry))) {
            ServerLevel where = levelOf(server, entry.dimension);
            resolveEscape(where, index, entry, entry.pos);
            return;
        }
        boolean moon = BossDirector.fullMoonNightNow(overworld);
        long day = dayOf(overworld);
        switch (entry.phase) {
            case NONE, FAILED -> roll(server, index, entry, overworld, moon, day);
            case HOST, RETURN_PENDING -> {
                if (BeastPhaseRules.changeDue(entry.phase, false, moon, day, entry.phaseDay)) {
                    change(overworld);
                }
            }
            default -> {
            }
        }
    }

    private static boolean spiritAlive(MinecraftServer server, BossIndex.Entry entry) {
        return entry.spirit != null && BossDirector.find(server, entry.spirit) instanceof GevaudanSpiritEntity s && s.isAlive();
    }

    private static ServerLevel levelOf(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        return server.overworld();
    }

    /** Every player of every dimension, once. */
    static List<ServerPlayer> allPlayers(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer p : BossDirector.playersIn(level)) {
                if (seen.add(p.getUUID())) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    /** The index says the body of the Host or of the Beast is gone: the night is lost, and those who were near where it stood are told. */
    private static void bodyLost(MinecraftServer server, BossIndex index, BossIndex.Entry entry) {
        entry.phase = BeastPhaseRules.after(entry.phase, BeastPhaseRules.Event.BODY_LOST);
        entry.spirit = null;
        index.setDirty();
        tellNear(levelOf(server, entry.dimension), entry.pos, Component.translatable(FAILED));
        BossIndex.flush(server);
    }

    private static void roll(MinecraftServer server, BossIndex index, BossIndex.Entry entry, ServerLevel overworld, boolean moon, long day) {
        if (!moon || entry.lastRollDay == day || !BeastPhaseRules.hostMayBeMade(entry.phase, entry.hostsMade)
                || BossDirector.difficultyIn(overworld) == Difficulty.PEACEFUL) {
            return;
        }
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer p : BossDirector.playersIn(overworld)) {
            if (hostTarget(p)) {
                targets.add(p);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        // one try a night: when there is no ground for him he tries again the next full moon night
        entry.lastRollDay = day;
        index.setDirty();
        double roll = BossDirector.nextRoll();
        if (!BeastRules.hostAppears(entry.phase, entry.hostsMade, moon, false, true, BossesConfig.beastHostChance(), roll)) {
            return;
        }
        ServerPlayer target = targets.get(Math.min(targets.size() - 1, (int) (BossDirector.nextRoll() * targets.size())));
        if (spawnHost(overworld, target, null).isPresent()) {
            noticeHost(target);
        }
    }

    /** Makes the Host 24 to 40 blocks from {@code target} (6 to 24 with no ground that far, or at {@code at}) and writes it down. Empty when there is no ground, or none may be made. */
    public static Optional<GevaudanHostEntity> spawnHost(ServerLevel level, @Nullable LivingEntity target, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : target == null ? null
                : BossDirector.findGround(level, target.blockPosition(), 24, 40).or(() -> BossDirector.findGround(level, target.blockPosition(), 6, 24)).orElse(null);
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (pos == null || !BeastPhaseRules.hostMayBeMade(entry.phase, entry.hostsMade)) {
            return Optional.empty();
        }
        GevaudanHostEntity host = GevaudanHostEntity.create(level);
        host.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        host.stayNear(pos);
        entry.phase = BeastPhaseRules.after(entry.phase, BeastPhaseRules.Event.HOST_MADE);
        entry.hostsMade++;
        entry.phaseDay = dayOf(level.getServer().overworld());
        entry.appearances++;
        entry.target = target == null ? null : target.getUUID();
        entry.spirit = null;
        entry.witnesses.clear();
        index.apply(BossId.BEAST_OF_GEVAUDAN, BossRules.Event.SPAWNED);
        index.adopt(BossId.BEAST_OF_GEVAUDAN, host.getUUID(), level.dimension().location().toString(), pos);
        index.setDirty();
        level.addFreshEntity(host);
        return Optional.of(host);
    }

    /** "A man in an old soldier's coat stands out there": the action bar and chat, a hint, and a distant sound, for the player alone. Never a name. */
    static void noticeHost(ServerPlayer target) {
        Component text = Component.translatable(HOST_NOTICE);
        target.displayClientMessage(text, true);
        target.sendSystemMessage(text);
        target.sendSystemMessage(Component.translatable(HOST_HINT));
        target.playNotifySound(SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 1.0F, 0.4F);
    }

    // ---- the change ----

    /** The Host becomes the Beast, where it stands: at full health, or at half the second time. Whether it did (the Host may be unloaded or gone). */
    public static boolean change(ServerLevel level) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (entry.phase != BeastPhase.HOST && entry.phase != BeastPhase.RETURN_PENDING || entry.entity == null) {
            return false;
        }
        Entity found = BossDirector.find(level.getServer(), entry.entity);
        if (!(found instanceof GevaudanHostEntity host) || !host.isAlive() || !(host.level() instanceof ServerLevel where)) {
            return false;
        }
        BeastEntity beast = BeastEntity.create(where);
        beast.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), 0F);
        beast.applyFactors();
        beast.setHealth(beast.getMaxHealth() * BeastPhaseRules.startHealth(entry.returned));
        entry.phase = BeastPhaseRules.after(entry.phase, BeastPhaseRules.Event.CHANGE);
        entry.phaseDay = dayOf(level.getServer().overworld());
        index.adopt(BossId.BEAST_OF_GEVAUDAN, beast.getUUID(), where.dimension().location().toString(), host.blockPosition());
        index.setDirty();
        where.addFreshEntity(beast);
        host.discard();
        where.sendParticles(ParticleTypes.LARGE_SMOKE, beast.getX(), beast.getY() + 1.2, beast.getZ(), 60, 0.8, 1.0, 0.8, 0.05);
        where.playSound(null, beast.getX(), beast.getY(), beast.getZ(), SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 4.0F, 0.3F);
        beast.howl();
        tellNear(where, beast.blockPosition(), Component.translatable(CHANGE));
        return true;
    }

    // ---- the collapse ----

    /** Called by the Beast every second: it collapses at a quarter of its health, or when it stands inside a closed ring of mountain ash. */
    public static void watch(ServerLevel level, BeastEntity beast) {
        BossIndex.Entry entry = BossIndex.get(level.getServer()).of(BossId.BEAST_OF_GEVAUDAN);
        if (entry.phase != BeastPhase.BEAST || !beast.getUUID().equals(entry.entity)) {
            return;
        }
        // the ring walk is the costly part: look for one every fourth second, not every one
        boolean trapped = BossesConfig.beastRespectsAsh() && beast.tickCount % 80 < 10 && ringAround(level, beast.blockPosition());
        if (BeastPhaseRules.next(BeastPhase.BEAST, beast.getHealth() / beast.getMaxHealth(), trapped, 0, entry.returned, BossesConfig.beastReturnsOnce()) == BeastPhase.SPIRIT) {
            collapse(level, beast);
        }
    }

    private static boolean ringAround(ServerLevel level, BlockPos at) {
        return Imprisonment.ringClosed(level, at, SpeciesConfig.ringRadius());
    }

    /** The Beast falls: the Host stands up where it fell (protected, and the index's now), the Spirit rises and runs for {@code beast_spirit_seconds}. */
    public static void collapse(ServerLevel level, BeastEntity beast) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (entry.phase != BeastPhase.BEAST || beast.collapsing()) {
            return;
        }
        beast.collapse();
        entry.phase = BeastPhase.SPIRIT;
        GevaudanHostEntity host = GevaudanHostEntity.create(level);
        host.moveTo(beast.getX(), beast.getY(), beast.getZ(), beast.getYRot(), 0F);
        host.stayNear(beast.blockPosition());
        index.adopt(BossId.BEAST_OF_GEVAUDAN, host.getUUID(), level.dimension().location().toString(), beast.blockPosition());
        level.addFreshEntity(host);
        GevaudanSpiritEntity spirit = GevaudanSpiritEntity.create(level);
        spirit.moveTo(beast.getX(), beast.getY() + 1.2, beast.getZ(), beast.getYRot(), 0F);
        entry.spirit = spirit.getUUID();
        entry.spiritUntil = level.getGameTime() + BossesConfig.beastSpiritSeconds() * 20L;
        level.addFreshEntity(spirit);
        entry.witnesses.clear();
        for (ServerPlayer p : allPlayers(level.getServer())) {
            if (p.level() == level && p.blockPosition().distSqr(beast.blockPosition()) <= NEAR * NEAR) {
                entry.witnesses.add(p.getUUID());
            }
        }
        index.setDirty();
        level.sendParticles(ParticleTypes.SOUL, beast.getX(), beast.getY() + 1.5, beast.getZ(), 40, 0.8, 1.0, 0.8, 0.03);
        level.playSound(null, beast.getX(), beast.getY(), beast.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0F, 0.6F);
        tellNear(level, beast.blockPosition(), Component.translatable(COLLAPSE, BossesConfig.beastSpiritSeconds()));
        BossIndex.flush(level.getServer());
    }

    // ---- the Spirit ----

    /** Whether the Spirit's time is up, or it is not the Spirit of this encounter any more (a stray one goes at once). */
    public static boolean spiritTimeIsUp(ServerLevel level, GevaudanSpiritEntity spirit) {
        BossIndex.Entry entry = BossIndex.get(level.getServer()).of(BossId.BEAST_OF_GEVAUDAN);
        return entry.phase != BeastPhase.SPIRIT || !spirit.getUUID().equals(entry.spirit) || level.getGameTime() >= entry.spiritUntil;
    }

    /** The Spirit gets away: it comes back once, at half health, on a later full moon night; the second time it is gone for good and so is the Host. */
    public static void escape(ServerLevel level, GevaudanSpiritEntity spirit) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        boolean current = entry.phase == BeastPhase.SPIRIT && spirit.getUUID().equals(entry.spirit);
        spirit.discard();
        if (!current) {
            return;
        }
        level.sendParticles(ParticleTypes.POOF, spirit.getX(), spirit.getY(), spirit.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
        resolveEscape(level, index, entry, spirit.blockPosition());
    }

    /**
     * The Spirit is away (its body got there, or nobody can find it after its time and the grace): it comes back once, at half health, on a later full moon night; the
     * second time (or the first, when {@code beast_returns_once} is off) it is gone for good and so is the Host. Everybody who stood near when the Beast fell is told.
     */
    private static void resolveEscape(ServerLevel level, BossIndex index, BossIndex.Entry entry, BlockPos at) {
        entry.spirit = null;
        BeastPhase next = BeastPhaseRules.next(BeastPhase.SPIRIT, 0.1F, false, 0, entry.returned, BossesConfig.beastReturnsOnce());
        entry.phase = next;
        if (next == BeastPhase.RETURN_PENDING) {
            entry.returned = true;
            entry.phaseDay = dayOf(level.getServer().overworld());
            tellWitnesses(level, entry, at, Component.translatable(ESCAPED));
        } else {
            endHost(level, index, entry);
            index.apply(BossId.BEAST_OF_GEVAUDAN, BossRules.Event.KILLED);
            tellWitnesses(level, entry, at, Component.translatable(entry.returned ? GONE : GONE_FINAL));
        }
        index.setDirty();
        BossIndex.flush(level.getServer());
    }

    /** A blow of the Valet Pike ended the Spirit: the Beast is dead for good, the Host is let go, and the Mantle is paid. */
    public static void killed(ServerLevel level, GevaudanSpiritEntity spirit, @Nullable ServerPlayer killer) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (!PikeRules.endsBeast(entry.phase, spirit.pikeHit()) || !spirit.getUUID().equals(entry.spirit)) {
            return;
        }
        entry.spirit = null;
        entry.phase = BeastPhaseRules.after(entry.phase, BeastPhaseRules.Event.PIKE_HIT);
        endHost(level, index, entry);
        index.apply(BossId.BEAST_OF_GEVAUDAN, BossRules.Event.KILLED);
        entry.returnDay = BossClock.today(level);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spirit.getX(), spirit.getY(), spirit.getZ(), 50, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, spirit.getX(), spirit.getY(), spirit.getZ(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 1.2F);
        tellWitnesses(level, entry, spirit.blockPosition(), Component.translatable(ENDED));
        pay(level, killer, spirit.blockPosition());
        index.setDirty();
        BossIndex.flush(level.getServer());
    }

    /** The Host is let go: gone with the Beast, whether it got away or died. */
    private static void endHost(ServerLevel level, BossIndex index, BossIndex.Entry entry) {
        if (entry.entity != null && BossDirector.find(level.getServer(), entry.entity) instanceof GevaudanHostEntity host) {
            host.discard();
        }
    }

    /** The Host died (the void, {@code /kill}): the encounter is off until a later full moon night, and a Host is made again once. */
    public static void hostLost(ServerLevel level, GevaudanHostEntity host) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (!host.getUUID().equals(entry.entity)) {
            return;
        }
        BeastPhase next = BeastPhaseRules.after(entry.phase, BeastPhaseRules.Event.HOST_LOST);
        if (next == entry.phase) {
            return;
        }
        if (entry.spirit != null && BossDirector.find(level.getServer(), entry.spirit) instanceof GevaudanSpiritEntity spirit) {
            spirit.discard();
        }
        entry.spirit = null;
        entry.phase = next;
        index.apply(BossId.BEAST_OF_GEVAUDAN, BossRules.Event.LOST);
        index.setDirty();
        tellNear(level, host.blockPosition(), Component.translatable(FAILED));
    }

    /** The operator's reset: the Spirit, if there is one, goes with the rest (the index forgets the encounter itself). */
    public static void clear(MinecraftServer server) {
        BossIndex.Entry entry = BossIndex.get(server).of(BossId.BEAST_OF_GEVAUDAN);
        if (entry.spirit != null && BossDirector.find(server, entry.spirit) instanceof GevaudanSpiritEntity spirit) {
            spirit.discard();
        }
    }

    // ---- the reward ----

    /**
     * The Mantle of the Beast, once: to the player who did the most to the Beast over the whole encounter if they are here (a tie goes to the last hitter, a pet's damage is its
     * owner's); kept for them in the index, and handed over when they log in, if they are not (it is never given to somebody else); else to the one whose pike ended
     * it; else on the ground where the Spirit fell. Everybody near (and the one who struck, wherever they are) is told who got it. Whether it was paid.
     */
    public static boolean pay(ServerLevel level, @Nullable ServerPlayer striker, BlockPos where) {
        MinecraftServer server = level.getServer();
        BossIndex index = BossIndex.get(server);
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (!RewardRules.pays(BossesConfig.rewardOn(), RewardRules.Defeat.DIED, entry.rewarded)) {
            return false;
        }
        entry.rewarded = true;
        UUID topId = entry.tally.top().orElse(null);
        ServerPlayer top = topId == null ? null : SireCall.player(server, topId);
        switch (BeastRules.payee(topId != null, top != null, striker != null)) {
            case TOP_ONLINE -> {
                give(top);
                top.sendSystemMessage(Component.translatable(REWARD));
                announce(level, where, striker, top, REWARD_TO, nameOf(server, topId, top));
            }
            case TOP_OWED -> {
                entry.owed.add(topId);
                announce(level, where, striker, null, REWARD_OWED, nameOf(server, topId, null));
            }
            case STRIKER -> {
                give(striker);
                striker.sendSystemMessage(Component.translatable(REWARD));
            }
            case GROUND -> {
                ItemEntity drop = new ItemEntity(level, where.getX() + 0.5, where.getY() + 0.5, where.getZ() + 0.5, new ItemStack(BossItems.MANTLE_OF_THE_BEAST.get()));
                drop.setUnlimitedLifetime();
                drop.setInvulnerable(true);
                level.addFreshEntity(drop);
                tellNear(level, where, Component.translatable(REWARD_DROPPED));
            }
        }
        index.setDirty();
        BossIndex.flush(server);
        return true;
    }

    private static void give(ServerPlayer p) {
        p.getInventory().placeItemBackInInventory(new ItemStack(BossItems.MANTLE_OF_THE_BEAST.get()));
    }

    /** The one the Mantle was kept for is here: it is handed over, with a word. Whether there was one. */
    public static boolean deliverOwed(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        BossIndex index = BossIndex.get(server);
        if (!index.of(BossId.BEAST_OF_GEVAUDAN).owed.remove(p.getUUID())) {
            return false;
        }
        give(p);
        p.sendSystemMessage(Component.translatable(REWARD_LATE));
        index.setDirty();
        BossIndex.flush(server);
        return true;
    }

    private static Component nameOf(MinecraftServer server, UUID id, @Nullable ServerPlayer online) {
        if (online != null) {
            return online.getDisplayName();
        }
        var cache = server.getProfileCache();
        if (cache != null) {
            var profile = cache.get(id);
            if (profile.isPresent()) {
                return Component.literal(profile.get().getName());
            }
        }
        return Component.translatable(SOMEONE);
    }

    /** Tells everyone near {@code where}, and the one who struck wherever they are, but not {@code except}. */
    private static void announce(ServerLevel level, BlockPos where, @Nullable ServerPlayer striker, @Nullable ServerPlayer except, String key, Component name) {
        Set<UUID> told = new HashSet<>();
        if (except != null) {
            told.add(except.getUUID());
        }
        Component text = Component.translatable(key, name);
        if (striker != null && told.add(striker.getUUID())) {
            striker.sendSystemMessage(text);
        }
        for (ServerPlayer p : BossDirector.playersIn(level)) {
            if (p.level() == level && p.blockPosition().distSqr(where) <= NEAR * NEAR && told.add(p.getUUID())) {
                p.sendSystemMessage(text);
            }
        }
    }

    /** Everyone near {@code at} now, and everyone who stood near when the Beast fell and is still here, once. */
    private static void tellWitnesses(ServerLevel level, BossIndex.Entry entry, BlockPos at, Component text) {
        Set<UUID> told = new HashSet<>();
        for (ServerPlayer p : allPlayers(level.getServer())) {
            boolean near = p.level() == level && p.blockPosition().distSqr(at) <= NEAR * NEAR;
            if ((near || entry.witnesses.contains(p.getUUID())) && told.add(p.getUUID())) {
                p.sendSystemMessage(text);
            }
        }
    }
    static void tellNear(ServerLevel level, BlockPos at, Component text) {
        for (ServerPlayer p : BossDirector.playersIn(level)) {
            if (p.level() == level && p.blockPosition().distSqr(at) <= NEAR * NEAR) {
                p.sendSystemMessage(text);
            }
        }
    }
}
