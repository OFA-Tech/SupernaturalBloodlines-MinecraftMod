package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public static final String ENDED = "supernaturalbloodlines.boss.beast.ended";
    public static final String FAILED = "supernaturalbloodlines.boss.beast.failed";
    public static final String REWARD = "supernaturalbloodlines.boss.beast.reward";
    public static final String REWARD_DROPPED = "supernaturalbloodlines.boss.beast.reward_dropped";

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
        entry.returned = false;
        entry.spirit = null;
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
        index.setDirty();
        level.sendParticles(ParticleTypes.SOUL, beast.getX(), beast.getY() + 1.5, beast.getZ(), 40, 0.8, 1.0, 0.8, 0.03);
        level.playSound(null, beast.getX(), beast.getY(), beast.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0F, 0.6F);
        tellNear(level, beast.blockPosition(), Component.translatable(COLLAPSE, BossesConfig.beastSpiritSeconds()));
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
        entry.spirit = null;
        BeastPhase next = BeastPhaseRules.next(BeastPhase.SPIRIT, 0.1F, false, 0, entry.returned, BossesConfig.beastReturnsOnce());
        entry.phase = next;
        if (next == BeastPhase.RETURN_PENDING) {
            entry.returned = true;
            entry.phaseDay = dayOf(level.getServer().overworld());
            tellNear(level, spirit.blockPosition(), Component.translatable(ESCAPED));
        } else {
            endHost(level, index, entry);
            index.apply(BossId.BEAST_OF_GEVAUDAN, BossRules.Event.KILLED);
            tellNear(level, spirit.blockPosition(), Component.translatable(GONE));
        }
        index.setDirty();
        BossIndex.flush(level.getServer());
    }

    /** A blow of the Valet Pike ended the Spirit: the Beast is dead for good, the Host is let go, and the Mantle is paid. */
    public static void killed(ServerLevel level, GevaudanSpiritEntity spirit, @Nullable ServerPlayer killer) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (entry.phase != BeastPhase.SPIRIT || !spirit.getUUID().equals(entry.spirit)) {
            return;
        }
        entry.spirit = null;
        entry.phase = BeastPhaseRules.after(entry.phase, BeastPhaseRules.Event.PIKE_HIT);
        endHost(level, index, entry);
        index.apply(BossId.BEAST_OF_GEVAUDAN, BossRules.Event.KILLED);
        entry.returnDay = BossClock.today(level);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spirit.getX(), spirit.getY(), spirit.getZ(), 50, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, spirit.getX(), spirit.getY(), spirit.getZ(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 1.2F);
        tellNear(level, spirit.blockPosition(), Component.translatable(ENDED));
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
     * The Mantle of the Beast, once: to the player who did the most to the Beast over the whole encounter (a tie goes to the last hitter, a pet's damage is its owner's),
     * or else to the one whose pike ended it, or else where the Spirit fell. Everybody near is told where it went. Whether it was paid.
     */
    public static boolean pay(ServerLevel level, @Nullable ServerPlayer striker, BlockPos where) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.BEAST_OF_GEVAUDAN);
        if (!RewardRules.pays(BossesConfig.rewardOn(), RewardRules.Defeat.DIED, entry.rewarded)) {
            return false;
        }
        entry.rewarded = true;
        ServerPlayer top = entry.tally.top().map(id -> SireCall.player(level.getServer(), id)).orElse(null);
        ServerPlayer owner = top != null ? top : striker;
        ItemStack mantle = new ItemStack(BossItems.MANTLE_OF_THE_BEAST.get());
        if (owner != null) {
            owner.getInventory().placeItemBackInInventory(mantle);
            owner.sendSystemMessage(Component.translatable(REWARD));
        } else {
            ItemEntity drop = new ItemEntity(level, where.getX() + 0.5, where.getY() + 0.5, where.getZ() + 0.5, mantle);
            drop.setUnlimitedLifetime();
            drop.setInvulnerable(true);
            level.addFreshEntity(drop);
            tellNear(level, where, Component.translatable(REWARD_DROPPED));
        }
        index.setDirty();
        return true;
    }

    static void tellNear(ServerLevel level, BlockPos at, Component text) {
        for (ServerPlayer p : BossDirector.playersIn(level)) {
            if (p.level() == level && p.blockPosition().distSqr(at) <= NEAR * NEAR) {
                p.sendSystemMessage(text);
            }
        }
    }
}
