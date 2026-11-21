package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.boss.MantleRules;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactSlots;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Mantle's Shadow Wrap (R191): once a second, for a werewolf type who is shifted and wears the Mantle, a veil of shadow round them; and when the wait is over
 * the Wrap weakens and slows the enemies within 6 blocks for 20 seconds, then waits 120. Everyone else has a cloak with no effect. Enemies are the hostile mobs,
 * and the players who may be hurt (PvP on, no team).
 */
public final class MantleEvents {
    public static final String WRAPPED = "supernaturalbloodlines.boss.mantle.wrap";
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private MantleEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) {
                tick(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> READY_AT.clear());
    }

    /** Whether the Mantle does anything for this player now. */
    public static boolean active(ServerPlayer p) {
        boolean worn = ArtifactSlots.isWorn(p, BossItems.MANTLE_OF_THE_BEAST.get());
        return worn && MantleRules.active(shifted(p), VampirismSupernatural.isWerewolf(p), true);
    }

    private static volatile java.util.function.Predicate<ServerPlayer> shiftedHook;

    /** Tests: whether a player counts as shifted (the real form needs a real Werewolves player); null puts the real answer back. */
    public static void useShifted(@org.jetbrains.annotations.Nullable java.util.function.Predicate<ServerPlayer> pinned) {
        shiftedHook = pinned;
    }

    private static boolean shifted(ServerPlayer p) {
        java.util.function.Predicate<ServerPlayer> pinned = shiftedHook;
        if (pinned != null) {
            return pinned.test(p);
        }
        return ModList.get().isLoaded("werewolves") && Wolves.shifted(p);
    }

    /** The game time at which this player's Shadow Wrap is ready again (0 when it has never been used). */
    public static long readyAt(UUID player) {
        return READY_AT.getOrDefault(player, 0L);
    }

    /** One second of the Mantle for one player; the tests call it directly. Returns how many enemies it wrapped. */
    public static int tick(ServerPlayer p) {
        if (!active(p) || !(p.level() instanceof ServerLevel level)) {
            return 0;
        }
        level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.4, 0.7, 0.4, 0.01);
        long now = level.getGameTime();
        if (!MantleRules.wrapReady(now, READY_AT.getOrDefault(p.getUUID(), 0L))) {
            return 0;
        }
        int wrapped = 0;
        int ticks = MantleRules.WRAP_SECONDS * 20;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(MantleRules.RANGE), x -> x != p && x.isAlive())) {
            boolean enemy = e instanceof Enemy && e instanceof Mob || e instanceof ServerPlayer other && p.canHarmPlayer(other) && !other.isCreative() && !other.isSpectator();
            if (enemy && MantleRules.wraps(e.distanceToSqr(p), false, e.isAlliedTo(p), true)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), p);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0), p);
                wrapped++;
            }
        }
        READY_AT.put(p.getUUID(), MantleRules.nextWrapAt(now));
        p.displayClientMessage(Component.translatable(WRAPPED, wrapped), true);
        return wrapped;
    }

    /** Tests: the wait forgotten. */
    public static void reset() {
        READY_AT.clear();
    }

    /** Kept apart so the game that has no Werewolves never loads its classes. */
    private static final class Wolves {
        static boolean shifted(ServerPlayer p) {
            return de.teamlapen.werewolves.util.FormHelper.getForm(p).isTransformed();
        }
    }
}
