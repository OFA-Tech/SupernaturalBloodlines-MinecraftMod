package net.ofatech.supernaturalbloodlines.parts.species.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.LeapMath;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * Hybrid Leap (R212, skill S-HYB-3 of the Hybrid and the Original hybrid): a slot action that throws the player at the creature their crosshair is on, as far
 * as {@code hybrid_leap_range}. It is a push (a velocity), never a teleport, so walls and drops on the way count; when the flight brings them within
 * {@link LeapMath#HIT_RADIUS} of the target and they can see it (the same line of sight the leap needed at the start, asked again at the landing, as Peter's
 * pounce does) the blow lands once, as an ordinary attack (PvP, claims, duels and totems apply) with Weakness after it. An Original is hurt like anyone: there
 * is no kill in it. It needs no werewolf form and links no Werewolves class.
 * <p>
 * Refusals, none of which names a species: no target (nothing under the crosshair, or not in front), too far, cooldown, cannot act (downed, snapped, riding,
 * in a fluid, gliding, already in a flight) and one {@code refused} for everything else (creative, spawn protection, already down, claims, PvP, a duel, a
 * pack-mate). The same target checks as Rip Heart and Neck Snap ({@link TargetChecks}), the same damage probe ({@link net.ofatech.supernaturalbloodlines.core.death.DamageProbe}).
 */
public final class HybridLeapAction implements ExtraActions.Action {
    public static final HybridLeapAction INSTANCE = new HybridLeapAction();
    public static final String SKILL = "S-HYB-3";
    private static final String KEY = "supernaturalbloodlines.hud.action.hybrid_leap.";
    public static final String NO_TARGET = KEY + "no_target";
    public static final String TOO_FAR = KEY + "too_far";
    public static final String COOLDOWN = KEY + "cooldown";
    public static final String CANNOT_ACT = KEY + "cannot_act";
    /** The leaper's own reasons: they say nothing about the target, so they may be named. Downed or snapped keeps the vague {@link #CANNOT_ACT}. */
    public static final String CANNOT_RIDING = KEY + "cannot_act.riding";
    public static final String CANNOT_SWIMMING = KEY + "cannot_act.swimming";
    public static final String CANNOT_AIR = KEY + "cannot_act.air";
    public static final String CANNOT_BUSY = KEY + "cannot_act.busy";
    public static final String REFUSED = KEY + "refused";
    public static final String DONE = KEY + "done";
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/hybrid_leap.png");
    /** The farthest a client may name a target from (the config range tops out here); the action says "too far" itself. */
    public static final double MAX_REACH = LeapMath.MAX_RANGE;

    /** A leap in the air. */
    private static final class Flight {
        final LivingEntity target;
        int ticks;
        boolean leftGround;
        boolean landedBlow;

        Flight(LivingEntity target) {
            this.target = target;
        }
    }

    private static final Map<UUID, Flight> FLIGHTS = new ConcurrentHashMap<>();
    /** Game time until which a landing is eased, by player. */
    private static final Map<UUID, Long> EASED_UNTIL = new ConcurrentHashMap<>();

    private HybridLeapAction() {
    }

    public static void register() {
        ExtraActions.install(INSTANCE);
        // a player who is put out of action (downed, snapped) is not carried on to a blow they can no longer deliver
        ExtraActions.addStopper(HybridLeapAction::end);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                tick(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingFallEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && eased(p)) {
                e.setDistance((float) LeapMath.fallAfterLeap(e.getDistance()));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            FLIGHTS.remove(e.getEntity().getUUID());
            EASED_UNTIL.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> FLIGHTS.remove(e.getEntity().getUUID()));
    }

    /** For tests: nothing is in flight and nothing is eased. */
    public static void reset() {
        FLIGHTS.clear();
        EASED_UNTIL.clear();
    }

    public static boolean inFlight(Player p) {
        return FLIGHTS.containsKey(p.getUUID());
    }

    /** A flight is over (or was never): the landing is eased for a moment longer. */
    private static void end(ServerPlayer p) {
        if (FLIGHTS.remove(p.getUUID()) != null) {
            EASED_UNTIL.put(p.getUUID(), p.level().getGameTime() + LeapMath.FALL_GRACE_TICKS);
        }
    }

    private static boolean eased(ServerPlayer p) {
        if (FLIGHTS.containsKey(p.getUUID())) {
            return true;
        }
        Long until = EASED_UNTIL.get(p.getUUID());
        return until != null && until > p.level().getGameTime();
    }

    @Override
    public ResourceLocation id() {
        return ExtraActions.HYBRID_LEAP;
    }

    @Override
    public Component name() {
        return Component.translatable("supernaturalbloodlines.hud.action.hybrid_leap");
    }

    @Override
    public Component note() {
        return Component.translatable("supernaturalbloodlines.hud.action.hybrid_leap.note", RipHeartAction.hearts(SpeciesConfig.hybridLeapDamage()), SpeciesConfig.hybridLeapWeaknessTicks() / 20,
                (int) SpeciesConfig.hybridLeapRange(), SpeciesConfig.hybridLeapCooldownSeconds());
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public boolean available(Player p) {
        return LadderQuery.owns(p, SKILL);
    }

    @Override
    public double targetReach() {
        return MAX_REACH;
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p) {
        return trigger(p, null);
    }

    /**
     * Why the player cannot throw themselves, as the lang key to show, or null when they can: downed, snapped or asleep (the vague line), riding, in a fluid,
     * already in a flight, or not standing on the ground (in the air, gliding, flying, spinning with a trident). The leap starts from the ground: a leap in
     * mid-air would be a free way to stop a fall.
     */
    static String cannotLeapBecause(ServerPlayer p) {
        return because(p, p.isPassenger(), p.isInWater() || p.isInLava() || p.isEyeInFluid(FluidTags.WATER) || p.isEyeInFluid(FluidTags.LAVA));
    }

    /** For tests: the same, with riding and being in a fluid given (a fake player is never moved by the game's physics, so it cannot board a vehicle or get wet). */
    public static String cannotLeapBecauseForTest(ServerPlayer p, boolean riding, boolean inFluid) {
        return because(p, riding, inFluid);
    }

    private static String because(ServerPlayer p, boolean riding, boolean inFluid) {
        if (Downed.cannotAct(p) || p.isSleeping()) {
            return CANNOT_ACT;
        }
        if (riding) {
            return CANNOT_RIDING;
        }
        if (inFluid) {
            return CANNOT_SWIMMING;
        }
        if (inFlight(p)) {
            return CANNOT_BUSY;
        }
        if (!p.onGround() || p.isFallFlying() || p.getAbilities().flying || p.isAutoSpinAttack()) {
            return CANNOT_AIR;
        }
        return null;
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p, @Nullable LivingEntity target) {
        if (target == null || !TargetChecks.inView(p, target)) {
            return Component.translatable(NO_TARGET);
        }
        String because = cannotLeapBecause(p);
        if (because != null) {
            return Component.translatable(because);
        }
        if (!ActionClock.ready(p, id())) {
            return Component.translatable(COOLDOWN);
        }
        Vec3 from = p.getBoundingBox().getCenter();
        Vec3 to = target.getBoundingBox().getCenter();
        if (!LeapMath.inReach(from.distanceTo(to), SpeciesConfig.hybridLeapRange())) {
            return Component.translatable(TOO_FAR);
        }
        // one refusal for everything that is not the leaper's to ask about: nothing here depends on what the target is
        if (TargetChecks.untouchable(target) || TargetChecks.lyingDown(target) || !TargetChecks.mayAttack(p, target)
                || !TargetChecks.damageWouldLand(target, attack(p), TargetChecks.PROBE)) {
            return Component.translatable(REFUSED);
        }
        Vec3 d = to.subtract(from);
        double[] v = LeapMath.impulse(d.x, d.y, d.z, LeapMath.speedFor(from.distanceTo(to)), LeapMath.liftFor(d.y));
        p.setDeltaMovement(v[0], v[1], v[2]);
        p.hurtMarked = true;
        p.hasImpulse = true;
        FLIGHTS.put(p.getUUID(), new Flight(target));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
        p.displayClientMessage(Component.translatable(DONE, target.getDisplayName()), true);
        ActionClock.start(p, id(), cooldownTicks(p));
        return null;
    }

    private static DamageSource attack(ServerPlayer p) {
        return p.damageSources().playerAttack(p);
    }

    /** Once per server tick for every player: the flight ends on landing or when its time runs out, and the blow lands when the leap has brought them to the target. */
    public static void tick(ServerPlayer p) {
        Flight f = FLIGHTS.get(p.getUUID());
        if (f == null) {
            return;
        }
        if (!p.isAlive() || Downed.cannotAct(p)) {
            end(p);
            return;
        }
        f.ticks++;
        f.leftGround |= !p.onGround();
        LivingEntity target = f.target;
        if (!f.landedBlow && target.isAlive() && target.level() == p.level()
                && LeapMath.hits(p.getBoundingBox().getCenter().distanceTo(target.getBoundingBox().getCenter()), LeapMath.HIT_RADIUS)
                // the line of sight the leap needed at the start is asked again at the landing: no blow through a wall or a floor
                && p.hasLineOfSight(target)) {
            f.landedBlow = true;
            land(p, target);
        }
        if (LeapMath.flightOver(f.ticks, f.leftGround, p.onGround())) {
            end(p);
        }
    }

    /** The blow: an ordinary attack, so PvP, claims, armour, shields and totems answer it; the Weakness only when it landed. */
    private static void land(ServerPlayer p, LivingEntity target) {
        // a claim or a duel that began while they were in the air answers here, as it would to a swing
        if (TargetChecks.untouchable(target) || TargetChecks.lyingDown(target) || !TargetChecks.mayAttack(p, target)) {
            return;
        }
        float damage = (float) SpeciesConfig.hybridLeapDamage();
        if (damage > 0.0F) {
            net.ofatech.supernaturalbloodlines.parts.species.pack.FightLedger.swing(p, target);
        }
        boolean landed = damage > 0.0F ? target.hurt(attack(p), damage) : TargetChecks.damageWouldLand(target, attack(p), TargetChecks.PROBE);
        if (landed && SpeciesConfig.hybridLeapWeaknessTicks() > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SpeciesConfig.hybridLeapWeaknessTicks(), 0));
        }
    }

    @Override
    public int cooldownTicks(ServerPlayer p) {
        return SpeciesConfig.hybridLeapCooldownSeconds() * 20;
    }

    @Override
    public int cooldownLeft(ServerPlayer p) {
        return ActionClock.left(p, id());
    }
}
