package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * What a Hunter of the Five feels (R84): instinct weakens one who has not killed prey for 3 days, and from 5 kills
 * the hunter senses prey within 24 blocks (the client outlines them, see {@link GlowPayload}).
 * Both read the saved {@code lastVampireKill}, which the rules set when the mark is gained and at every prey kill.
 */
public final class HunterInstinct {
    static final int INSTINCT_INTERVAL = 1200;
    static final long IDLE_TICKS = 72000;
    static final int WEAKNESS_TICKS = 1300;
    static final int SENSE_INTERVAL = 40;
    static final int SENSE_KILLS = 5;
    static final double SENSE_RANGE = 24;

    private static volatile Double rangeOverride;

    private HunterInstinct() {}

    /** For tests: shrinks the sense range so the test mobs stay inside the test structure; null clears it. */
    @VisibleForTesting
    public static void overrideRange(Double range) {
        rangeOverride = range;
    }

    private static double range() {
        Double pinned = rangeOverride;
        return pinned != null ? pinned : SENSE_RANGE;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer(), e.getServer().getTickCount()));
    }

    /** @param serverTick the server tick count */
    public static void tick(MinecraftServer server, long serverTick) {
        boolean instinct = serverTick % INSTINCT_INTERVAL == 0;
        if (!instinct && serverTick % SENSE_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer p : HunterEvents.online(server)) {
            if (BloodlineAccess.get(p).species() != Species.HUNTER_OF_THE_FIVE) {
                continue;
            }
            if (instinct) {
                instinct(p, p.level().getGameTime());
            }
            if (serverTick % SENSE_INTERVAL == 0) {
                sense(p);
            }
        }
    }

    /** Weakness I when more than 3 days have passed since the last prey kill, unless a prey teammate is online. @param now the game time */
    public static void instinct(ServerPlayer p, long now) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.species() != Species.HUNTER_OF_THE_FIVE || now - s.lastVampireKill() <= IDLE_TICKS || hasPreyTeammate(p)) {
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0, false, true, true));
    }

    private static boolean hasPreyTeammate(ServerPlayer p) {
        if (p.getTeam() == null) {
            return false;
        }
        for (ServerPlayer o : HunterEvents.online(p.server)) {
            if (o != p && p.isAlliedTo(o) && MarkTracker.isPrey(o)) {
                return true;
            }
        }
        return false;
    }

    /** Prey the hunter senses: alive, within range, not themself, no spectator, and prey by R81 (so no witches or siphoners). */
    @VisibleForTesting
    public static boolean senses(ServerPlayer hunter, LivingEntity e) {
        return e != hunter && e.isAlive() && !e.isSpectator() && e.distanceToSqr(hunter) <= range() * range() && MarkTracker.isPrey(e);
    }

    /** Sends the prey within range, if the hunter has the kills for it. Nothing is sent when there is none. */
    static void sense(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.species() != Species.HUNTER_OF_THE_FIVE || s.markKills() < SENSE_KILLS) {
            return;
        }
        List<Integer> ids = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range()), e -> senses(p, e))
                .stream().map(LivingEntity::getId).toList();
        if (!ids.isEmpty()) {
            GlowPayload.send(p, ids);
        }
    }
}
