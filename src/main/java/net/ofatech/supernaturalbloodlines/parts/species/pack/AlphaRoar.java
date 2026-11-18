package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.util.FormHelper;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The Alpha Roar (R94): the pack's Betas within 32 blocks get Strength I and Speed I for 30 s and may shift at will for
 * that time; other werewolves within 16 blocks are forced out of their form and slowed (Slowness II, 5 s). Only an Alpha
 * or True Alpha, once per 1200 ticks. The key press arrives as a {@code RoarPayload}; everything is decided here from
 * server state. The cooldown is kept in memory per player and forgotten when the server stops (tick counts start again). A dead or spectating Alpha cannot roar, and nobody can during a lunar eclipse (refused with a message, no cooldown spent). Links Werewolves.
 */
public final class AlphaRoar {
    public static final int COOLDOWN = 1200;
    public static final double PACK_RANGE = 32;
    public static final double SUBMIT_RANGE = 16;
    public static final int BUFF_TICKS = 600;
    public static final int SUBMIT_TICKS = 100;
    private static final String KEY = "supernaturalbloodlines.pack.";

    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PERMIT_UNTIL = new ConcurrentHashMap<>();
    /** Betas carrying the Primeval Roar's attack bonus (a transient modifier, never saved) and the tick it ends. */
    private static final Map<UUID, Long> PACK_ATTACK_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerPlayer> PACK_ATTACK_HOLDERS = new ConcurrentHashMap<>();
    public static final ResourceLocation PACK_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "primeval_pack_attack");
    private static volatile Function<MinecraftServer, Collection<ServerPlayer>> players = s -> s.getPlayerList().getPlayers();

    private AlphaRoar() {
    }

    public static void register() {
        ForcedShift.register();
        // R96: Alphas and True Alphas have moon control (FULL from the full-form level)
        MoonControl.addSource(p -> PackAccess.isAlpha(PackAccess.wolf(p).rank()));
        ForcedShift.addFormPermit(p -> PERMIT_UNTIL.getOrDefault(p.getUUID(), 0L) > p.server.getTickCount());
        // the maps hold server tick counts, which restart at 0 with an integrated server: a stale one would block a roar
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> onServerStopped());
        // a map lookup per player per second, and nothing at all while nobody carries the bonus
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (!PACK_ATTACK_UNTIL.isEmpty() && e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) {
                endPackAttack(p, false);
            }
        });
    }

    /** Takes the Primeval Roar's attack bonus off {@code p} when its time is up (or at once with {@code force}). */
    @VisibleForTesting
    public static void endPackAttack(ServerPlayer p, boolean force) {
        Long until = PACK_ATTACK_UNTIL.get(p.getUUID());
        if (until != null && (force || p.server.getTickCount() >= until)) {
            PACK_ATTACK_UNTIL.remove(p.getUUID());
            PACK_ATTACK_HOLDERS.remove(p.getUUID());
            p.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(PACK_ATTACK_ID);
        }
    }

    /** Forgets every cooldown and permit (the server's tick count is about to start again). */
    public static void onServerStopped() {
        reset();
    }

    /** For tests: who counts as online for the non-pack werewolves (mock players are not in the player list); null resets. */
    @VisibleForTesting
    public static void overridePlayers(@Nullable Function<MinecraftServer, Collection<ServerPlayer>> lookup) {
        players = lookup != null ? lookup : s -> s.getPlayerList().getPlayers();
    }

    @VisibleForTesting
    public static void reset() {
        LAST.clear();
        PERMIT_UNTIL.clear();
        // the bonus is a transient modifier on a player who may still be online: take it off, do not just forget when it ends
        for (ServerPlayer holder : PACK_ATTACK_HOLDERS.values()) {
            holder.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(PACK_ATTACK_ID);
        }
        PACK_ATTACK_HOLDERS.clear();
        PACK_ATTACK_UNTIL.clear();
    }

    /** Ticks between two roars: 1200, or 800 with Roar's Echo (S-WOL-6) or the Primeval Roar (S-WOL-8, which is Roar's Echo and more). */
    public static int cooldown(ServerPlayer p) {
        return (int) Math.min(LadderQuery.value(p, "S-WOL-6", "cooldown_ticks", COOLDOWN), LadderQuery.value(p, "S-WOL-8", "cooldown_ticks", COOLDOWN));
    }

    /** The Primeval Roar (S-WOL-8) doubles both ranges. */
    private static double rangeFactor(ServerPlayer p) {
        return LadderQuery.value(p, "S-WOL-8", "range_factor", 1);
    }

    /** The error to show, or null when the roar happened. */
    @Nullable
    public static Component roar(ServerPlayer p) {
        return roar(p, p.server.getTickCount());
    }

    /** {@code now} is the server tick count. */
    @Nullable
    public static Component roar(ServerPlayer p, long now) {
        if (!PackAccess.isAlpha(PackAccess.wolf(p).rank()) || !VampirismSupernatural.isWerewolf(p)) {
            return Component.translatable(KEY + "not_alpha");
        }
        if (LunarEclipse.active(p.server)) {
            return Component.translatable(KEY + "roar_eclipse");
        }
        if (!p.isAlive() || p.isSpectator()) {
            return Component.translatable(KEY + "roar_unavailable");
        }
        Long last = LAST.get(p.getUUID());
        long cooldown = cooldown(p);
        if (last != null && now - last < cooldown) {
            return Component.translatable(KEY + "roar_cooldown", (cooldown - (now - last) + 19) / 20);
        }
        LAST.put(p.getUUID(), now);

        double range = rangeFactor(p);
        double packAttack = LadderQuery.value(p, "S-WOL-8", "pack_attack", 0);
        int packTicks = (int) LadderQuery.value(p, "S-WOL-8", "pack_ticks", BUFF_TICKS);
        PackIndex index = PackIndex.get(p.serverLevel());
        // the whole pack: Betas, Packmates and the other Alphas (the leader when a co-Alpha roars)
        Set<UUID> pack = new HashSet<>(index.pack(p.getUUID()));
        pack.remove(p.getUUID());
        for (UUID id : pack) {
            ServerPlayer beta = SireCall.player(p.server, id);
            if (inRange(p, beta, PACK_RANGE * range)) {
                beta.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, PackRules.roarTier(PackRules.effectiveSparks(PackAccess.wolf(p).rank(), PackAccess.wolf(p).sparks(), SpeciesConfig.maxStackedSparks(), VampirismSupernatural.isWerewolf(p)), SpeciesConfig.maxStackedSparks())));
                beta.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_TICKS, 0));
                PERMIT_UNTIL.put(id, now + BUFF_TICKS);
                if (packAttack > 0) {
                    beta.getAttribute(Attributes.ATTACK_DAMAGE).addOrUpdateTransientModifier(new AttributeModifier(PACK_ATTACK_ID, packAttack,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    PACK_ATTACK_UNTIL.put(id, now + packTicks);
                    PACK_ATTACK_HOLDERS.put(id, beta);
                }
            }
        }
        for (ServerPlayer other : players.apply(p.server)) {
            if (other != p && !pack.contains(other.getUUID()) && !index.alliedPlayers(p.getUUID(), other.getUUID()) && VampirismSupernatural.isWerewolf(other) && inRange(p, other, SUBMIT_RANGE * range)) {
                VampirismAPI.factionPlayerHandler(other).getCurrentFactionPlayer()
                        .filter(f -> f instanceof IWerewolfPlayer).ifPresent(f -> FormHelper.deactivateWerewolfActions((IWerewolfPlayer) f));
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SUBMIT_TICKS, 1));
                other.sendSystemMessage(Component.translatable(KEY + "roar_submit", p.getGameProfile().getName()));
            }
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 3.0f, 0.5f);
        RoarEffectPayload.send(p, new RoarEffectPayload(p.getId(), BUFF_TICKS));
        return null;
    }

    private static boolean inRange(ServerPlayer from, @Nullable ServerPlayer other, double range) {
        return other != null && other.isAlive() && other.level() == from.level() && from.distanceToSqr(other) <= range * range;
    }
}
