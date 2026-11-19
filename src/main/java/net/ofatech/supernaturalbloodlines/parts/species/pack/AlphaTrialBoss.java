package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.werewolves.core.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialRules;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.npc.Friends;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackNights;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The boss of the Alpha Trial (R147): an Alpha of the Werewolves mod with {@code alpha_trial_hp_factor} times the health, a boss bar, two beasts at
 * two thirds and again at one third of its health, and these ends. <b>Won</b> when it dies while its challenger is alive within 64 blocks: {@link AlphaTrial#win}.
 * <b>Lost</b> when the challenger dies, stays beyond 48 blocks (or logs out) for 30 seconds, leaves the dimension, or the night ends: the boss and its beasts
 * are sent away. Only the challenger and their pack can hurt it, so no stranger takes the win. It never gives the weaker spark ({@code WeakSpark}).
 * Links Werewolves: registered only when it is loaded.
 */
public final class AlphaTrialBoss {
    static final int PERIOD = 10;
    private static final double BAR_RANGE = 64;
    private static final String KEY = "supernaturalbloodlines.trial.";

    private static final Map<UUID, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    /** Challenger to boss: one trial each. Rebuilt by the boss'' own tick after a restart. */
    private static final Map<UUID, UUID> RUNNING = new ConcurrentHashMap<>();

    private AlphaTrialBoss() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.tickCount % PERIOD == m.getId() % PERIOD && isBoss(m)) {
                tick(m);
            }
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, LivingIncomingDamageEvent.class, AlphaTrialBoss::onDamage);
        // LOWEST, and never a death something else already cancelled: a boss that did not die is no win
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (LivingDeathEvent e) -> {
            if (!e.isCanceled() && e.getEntity() instanceof Mob m && !m.level().isClientSide && isBoss(m)) {
                died(m);
            }
        });
        // a boss that leaves the level any other way (its chunk unloads, it changes dimension) must not leave its bar and its trial behind
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && isBoss(m)) {
                Entity.RemovalReason why = m.getRemovalReason();
                if (why != Entity.RemovalReason.KILLED && why != Entity.RemovalReason.DISCARDED) {
                    end(m, m.getData(SpeciesRegistries.TRIAL_MARK));
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> reset());
    }

    /** For tests: one check now, without waiting for the boss' turn of the ten. */
    @VisibleForTesting
    public static void tickNow(Mob boss) {
        if (isBoss(boss)) {
            tick(boss);
        }
    }

    /** For tests: whether a boss bar is still kept for this boss. */
    @VisibleForTesting
    public static boolean hasBar(UUID boss) {
        return BARS.containsKey(boss);
    }

    @VisibleForTesting
    public static void reset() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        RUNNING.clear();
    }

    public static boolean isBoss(Entity e) {
        return e instanceof Mob && e.hasData(SpeciesRegistries.TRIAL_MARK) && e.getData(SpeciesRegistries.TRIAL_MARK).challenger() != null;
    }

    /**
     * Whether {@code challenger} has a trial running: a live boss in the map, or any <b>loaded</b> boss that carries their mark (the map is forgotten
     * on a restart and when a boss leaves the level). Best effort: a boss sitting in a chunk that is not loaded is not seen, so a second trial can
     * start; when the old one loads again its own tick finds the newer one and sends itself away ({@link #tick}).
     */
    public static boolean running(ServerLevel level, UUID challenger) {
        UUID boss = RUNNING.get(challenger);
        if (boss != null) {
            if (level.getEntity(boss) instanceof Mob m && m.isAlive() && isBoss(m)) {
                return true;
            }
            RUNNING.remove(challenger);
        }
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob m && m.isAlive() && isBoss(m) && challenger.equals(m.getData(SpeciesRegistries.TRIAL_MARK).challenger())) {
                return true;
            }
        }
        return false;
    }

    /** The boss of {@code challenger}'s running trial, or null. */
    @Nullable
    public static Mob bossOf(ServerLevel level, UUID challenger) {
        UUID boss = RUNNING.get(challenger);
        return boss != null && level.getEntity(boss) instanceof Mob m && m.isAlive() && isBoss(m) ? m : null;
    }

    /** Spawns the boss at {@code spot} for {@code challenger}; null when it could not be placed. */
    @Nullable
    public static Mob spawn(ServerLevel level, BlockPos spot, ServerPlayer challenger) {
        Mob boss = NpcPackSpawner.place(level, NpcPackSpawner.asMob(ModEntities.ALPHA_WEREWOLF.get()), spot, NpcPackSpawner.MAX_LEVEL);
        if (boss == null) {
            return null;
        }
        long packId = NpcPackNights.get(level).newPackId();
        boss.setData(SpeciesRegistries.TRIAL_MARK, new TrialMark(challenger.getUUID(), packId, NpcSpawnRules.nightOf(level.getDayTime()), 0, 0));
        // a trial is the same fight every time: the boss is at its highest level (set as it joins) and has the configured times the health
        var health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * SpeciesConfig.trial().hpFactor());
            boss.setHealth(boss.getMaxHealth());
        }
        boss.setPersistenceRequired();
        boss.setCustomName(Component.translatable(KEY + "boss"));
        boss.setCustomNameVisible(true);
        RUNNING.put(challenger.getUUID(), boss.getUUID());
        bar(boss).addPlayer(challenger);
        return boss;
    }

    private static ServerBossEvent bar(Mob boss) {
        return BARS.computeIfAbsent(boss.getUUID(), id -> new ServerBossEvent(Component.translatable(KEY + "boss"), BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_6));
    }

    static void tick(Mob boss) {
        if (!(boss.level() instanceof ServerLevel level) || !boss.isAlive()) {
            return;
        }
        TrialMark mark = boss.getData(SpeciesRegistries.TRIAL_MARK);
        UUID id = mark.challenger();
        UUID known = RUNNING.putIfAbsent(id, boss.getUUID());
        // an older boss that was in an unloaded chunk while a newer trial began: the newer one is the trial, this one just goes
        if (known != null && !known.equals(boss.getUUID()) && level.getEntity(known) instanceof Mob other && other.isAlive() && isBoss(other)) {
            beastsOf(level, boss, mark).forEach(Entity::discard);
            ServerBossEvent stale = BARS.remove(boss.getUUID());
            if (stale != null) {
                stale.removeAllPlayers();
            }
            boss.discard();
            return;
        }
        ServerPlayer challenger = SireCall.player(level.getServer(), id);
        // dead: lost at once; in another dimension: lost at once; far away or logged out: the leash runs for 30 seconds
        boolean dead = challenger != null && (!challenger.isAlive() || challenger.isSpectator());
        boolean near = challenger != null && !dead && challenger.level() == level
                && boss.distanceToSqr(challenger) <= TrialRules.LEASH_BLOCKS * TrialRules.LEASH_BLOCKS;
        int away = TrialRules.away(mark.away(), near, PERIOD);
        TrialRules.Fail fail = TrialRules.failure(!dead, challenger == null || challenger.level() == level, away, nightOver(level, mark));
        if (fail != TrialRules.Fail.NONE) {
            lose(boss, mark, challenger, fail);
            return;
        }
        // the beasts of the next phase
        int adds = TrialRules.addsFor(mark.phase(), boss.getHealth(), boss.getMaxHealth());
        int phase = TrialRules.phase(boss.getHealth(), boss.getMaxHealth());
        if (adds > 0) {
            callBeasts(level, boss, mark, adds);
        }
        boss.setData(SpeciesRegistries.TRIAL_MARK, mark.withPhase(Math.max(mark.phase(), phase)).withAway(away));
        ServerBossEvent bar = bar(boss);
        bar.setProgress(Math.max(0f, Math.min(1f, boss.getHealth() / boss.getMaxHealth())));
        for (ServerPlayer p : level.players()) {
            boolean in = p.distanceToSqr(boss) <= BAR_RANGE * BAR_RANGE;
            if (in) {
                bar.addPlayer(p);
            } else {
                bar.removePlayer(p);
            }
        }
    }

    private static boolean nightOver(ServerLevel level, TrialMark mark) {
        return Math.floorMod(level.getDayTime(), 24000L) >= MoonRule.NIGHT_END || NpcSpawnRules.nightOf(level.getDayTime()) != mark.night();
    }

    private static void callBeasts(ServerLevel level, Mob boss, TrialMark mark, int count) {
        NpcMark wild = NpcMark.wild(mark.packId(), mark.night());
        for (int i = 0; i < count; i++) {
            BlockPos at = boss.blockPosition().offset(level.random.nextInt(7) - 3, 0, level.random.nextInt(7) - 3);
            Mob beast = NpcPackSpawner.place(level, NpcPackSpawner.asMob(ModEntities.WEREWOLF_BEAST.get()), at, NpcPackSpawner.NO_LEVEL);
            if (beast != null) {
                beast.setData(SpeciesRegistries.NPC_MARK, wild);
            }
        }
    }

    /** The beasts of this trial, within reach of the boss. */
    private static List<Mob> beastsOf(ServerLevel level, Mob boss, TrialMark mark) {
        return new ArrayList<>(level.getEntitiesOfClass(Mob.class, new AABB(boss.blockPosition()).inflate(80),
                m -> m.hasData(SpeciesRegistries.NPC_MARK) && m.getData(SpeciesRegistries.NPC_MARK).packId() == mark.packId()));
    }

    private static void lose(Mob boss, TrialMark mark, @Nullable ServerPlayer challenger, TrialRules.Fail fail) {
        ServerLevel level = (ServerLevel) boss.level();
        if (challenger != null) {
            challenger.sendSystemMessage(Component.translatable(KEY + "lost." + fail.name().toLowerCase(java.util.Locale.ROOT)));
        }
        beastsOf(level, boss, mark).forEach(Entity::discard);
        end(boss, mark);
        boss.discard();
    }

    private static void died(Mob boss) {
        ServerLevel level = (ServerLevel) boss.level();
        TrialMark mark = boss.getData(SpeciesRegistries.TRIAL_MARK);
        ServerPlayer challenger = SireCall.player(level.getServer(), mark.challenger());
        boolean here = challenger != null && challenger.isAlive() && challenger.level() == level && challenger.distanceToSqr(boss) <= BAR_RANGE * BAR_RANGE;
        beastsOf(level, boss, mark).forEach(Entity::discard);
        end(boss, mark);
        if (here) {
            Component error = AlphaTrial.win(challenger, true);
            if (error != null) {
                challenger.sendSystemMessage(error);
            }
        } else if (challenger != null) {
            // the real reason: dead, in another dimension, or just too far (the leash is the stand-in for any distance)
            boolean alive = challenger.isAlive() && !challenger.isSpectator();
            TrialRules.Fail why = TrialRules.failure(alive, challenger.level() == level, TrialRules.LEASH_TICKS, false);
            challenger.sendSystemMessage(Component.translatable(KEY + "lost." + why.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private static void end(Mob boss, TrialMark mark) {
        ServerBossEvent bar = BARS.remove(boss.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
        if (mark.challenger() != null) {
            RUNNING.remove(mark.challenger(), boss.getUUID());
        }
    }

    /** Only the challenger and their pack (and allies) can hurt the boss. */
    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof Mob boss) || boss.level().isClientSide || !isBoss(boss)) {
            return;
        }
        if (ownerlessProjectile(e.getSource())) {
            e.setCanceled(true);
            return;
        }
        UUID attacker = attackerPlayer(e.getSource());
        if (attacker == null) {
            return;
        }
        UUID challenger = boss.getData(SpeciesRegistries.TRIAL_MARK).challenger();
        PackIndex index = PackIndex.get((ServerLevel) boss.level());
        boolean friend = challenger != null && !attacker.equals(challenger) && (index.samePackNow(attacker, challenger) || index.alliedPlayers(attacker, challenger));
        if (!TrialRules.mayHurt(attacker, challenger, friend)) {
            e.setCanceled(true);
        }
    }

    /**
     * The player behind a hit: the attacker, the shooter of an arrow, the owner of a Wolf Spirit or of any tamed pet; null when no player is behind it.
     * A hit by a projectile nobody shot (a dispenser, a trap) is no player's and is {@link #ownerlessProjectile refused} too; a fall, fire, magic and
     * the like (no entity at all) are the world's and do count.
     */
    @Nullable
    static UUID attackerPlayer(DamageSource source) {
        Entity direct = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (!(direct instanceof LivingEntity living)) {
            return null;
        }
        UUID player = Friends.playerOf(living);
        return player != null ? player : living instanceof OwnableEntity pet ? pet.getOwnerUUID() : null;
    }

    /** A projectile with nobody behind it: a dispenser's arrow can be fired by anyone, so it cannot hurt a trial boss. */
    static boolean ownerlessProjectile(DamageSource source) {
        return source.getEntity() == null && source.getDirectEntity() instanceof Projectile;
    }
}
