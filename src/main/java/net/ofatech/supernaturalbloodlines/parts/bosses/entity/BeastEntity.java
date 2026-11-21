package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Beast of Gevaudan (R184 to R187): a shadow with the shape of a wolf too big for a wolf, 2.4 blocks wide and 3 high. It hunts the players near it, rears up and
 * Roars (the fear lands after the wind-up; Alphas and True Alphas are immune), heals 2 health a second unless a wolfsbane strain is on it, takes 0.6 of what shots
 * do, and is stunned for a second at most by a Hearing Device. It cannot die of damage: at a quarter of its health, or inside a closed ring of mountain ash, it
 * collapses ({@link BeastDirector#collapse}): the Host stands up where it fell and the Spirit runs. This class names no client class.
 */
public final class BeastEntity extends BossNpc implements GeoEntity {
    private static final EntityDataAccessor<Boolean> COLLAPSING = SynchedEntityData.defineId(BeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ROAR_TICKS = SynchedEntityData.defineId(BeastEntity.class, EntityDataSerializers.INT);
    /** How long the collapse animation (3.4 s) plays before the body is gone. */
    public static final int COLLAPSE_TICKS = 70;
    private static final String CONTROLLER = "main";
    private static final String BITE = "bite";
    private static final String SWIPE = "swipe";
    private static final String HOWL = "howl";
    private static final String REAR_UP = "rear_up";
    private static final ResourceLocation[] WOLFSBANE_EFFECTS = {
            ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "wolfsbane_yellow"),
            ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "wolfsbane_northern_blue"),
            ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "wolf_lichen")};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean swipe;
    private long roarReadyAt;
    private int collapseTicks;

    public BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 420.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override
    public BossId boss() {
        return BossId.BEAST_OF_GEVAUDAN;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLLAPSING, false);
        builder.define(ROAR_TICKS, 0);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(2.4F, 3.0F);
    }

    public boolean collapsing() {
        return entityData.get(COLLAPSING);
    }

    /** The Roar's wind-up so far (0 when it is not rearing up). */
    public int roarTicks() {
        return entityData.get(ROAR_TICKS);
    }

    /** The body falls: no more thinking, no more blows, and the collapse plays before it goes. The director has made the Host and the Spirit already. */
    public void collapse() {
        if (collapsing()) {
            return;
        }
        entityData.set(COLLAPSING, true);
        entityData.set(ROAR_TICKS, 0);
        collapseTicks = 0;
        setNoAi(true);
        getNavigation().stop();
    }

    @Override
    protected boolean hasBar() {
        return !collapsing();
    }

    /** Only how it looks (for the dev screenshots): the fall, as the model plays it, and the Roar's wind-up. No AI, no hitbox change. */
    public void lookState(boolean collapsing, int roarTicks) {
        entityData.set(COLLAPSING, collapsing);
        entityData.set(ROAR_TICKS, roarTicks);
    }

    /** The index follows a Beast that is on its feet; one that is falling has handed its place to the Host. */
    @Override
    protected boolean tracked() {
        return !collapsing();
    }

    public void roarNow(long gameTime) {
        if (!collapsing() && roarTicks() == 0) {
            entityData.set(ROAR_TICKS, 1);
            triggerAnim(CONTROLLER, REAR_UP);
            roarReadyAt = BeastRules.nextRoarAt(gameTime, BossesConfig.beastRoarCooldownSeconds());
        }
    }

    public boolean roarReady(long gameTime) {
        return BeastRules.roarReady(gameTime, roarReadyAt);
    }

    /** The howl of the change: the model's roar plays once. */
    public void howl() {
        triggerAnim(CONTROLLER, HOWL);
    }

    /** Whether any wolfsbane strain is on it: the three of ours, or the common one's Weakness with Nausea. */
    public boolean underWolfsbane() {
        for (ResourceLocation id : WOLFSBANE_EFFECTS) {
            if (BuiltInRegistries.MOB_EFFECT.getHolder(id).map(this::hasEffect).orElse(false)) {
                return true;
            }
        }
        return hasEffect(MobEffects.WEAKNESS) && hasEffect(MobEffects.CONFUSION);
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return super.isInvulnerableTo(source) || collapsing() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (collapsing()) {
            if (++collapseTicks >= COLLAPSE_TICKS) {
                discard();
            }
            return;
        }
        int rearing = roarTicks();
        if (rearing > 0) {
            getNavigation().stop();
            setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
            if (rearing == BeastRules.ROAR_WINDUP_TICKS && level() instanceof ServerLevel server) {
                BeastRoar.let(server, this);
            }
            entityData.set(ROAR_TICKS, rearing >= BeastRules.ROAR_WINDUP_TICKS + 24 ? 0 : rearing + 1);
        }
    }

    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        if (collapsing() || !(level() instanceof ServerLevel server)) {
            return;
        }
        // 2 health a second, in the ten ticks that pass: one
        float heal = BeastRules.regen(underWolfsbane(), getHealth(), getMaxHealth()) / 2.0F;
        if (heal > 0F) {
            setHealth(getHealth() + heal);
        }
        MobEffectInstance slow = getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 4) {
            // the Hearing Device's stun (Slowness V and Nausea): a second at most on this one
            capStun(slow);
            capStun(getEffect(MobEffects.CONFUSION));
        }
        BeastDirector.watch(server, this);
    }

    private void capStun(@javax.annotation.Nullable MobEffectInstance on) {
        if (on != null && on.getDuration() > BeastRules.STUN_CAP_TICKS) {
            removeEffect(on.getEffect());
            addEffect(new MobEffectInstance(on.getEffect(), BeastRules.stunTicks(on.getDuration()), on.getAmplifier()));
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RoarGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    /** It rears up when it has somebody to frighten and the cooldown is over; while it does the other goals wait. */
    private static final class RoarGoal extends Goal {
        private final BeastEntity beast;

        RoarGoal(BeastEntity beast) {
            this.beast = beast;
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return beast.getTarget() != null && !beast.collapsing() && beast.roarTicks() == 0 && beast.roarReady(beast.level().getGameTime())
                    && BeastRules.roarInRange(beast.distanceToSqr(beast.getTarget()), BossesConfig.beastRoarRange());
        }

        @Override
        public boolean canContinueToUse() {
            return beast.roarTicks() > 0;
        }

        @Override
        public void start() {
            beast.roarNow(beast.level().getGameTime());
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            swipe = !swipe;
            triggerAnim(CONTROLLER, swipe ? SWIPE : BITE);
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("collapsing", collapsing());
        tag.putLong("roar_ready_at", roarReadyAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        roarReadyAt = tag.getLong("roar_ready_at");
        if (tag.getBoolean("collapsing")) {
            // a body that was falling when the world closed is gone: the Host and the Spirit were made when it fell
            entityData.set(COLLAPSING, true);
            collapseTicks = COLLAPSE_TICKS;
            setNoAi(true);
        }
    }

    public static BeastEntity create(Level level) {
        return BossRegistries.BEAST.get().create(level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state -> {
            BeastEntity b = state.getAnimatable();
            RawAnimation animation = RawAnimation.begin();
            if (b.collapsing()) {
                return state.setAndContinue(animation.thenPlayAndHold("animation.beast_of_gevaudan.collapse"));
            }
            if (b.isDeadOrDying()) {
                return state.setAndContinue(animation.thenPlayAndHold("animation.beast_of_gevaudan.death"));
            }
            if (b.roarTicks() > 0) {
                return state.setAndContinue(animation.thenPlay("animation.beast_of_gevaudan.rear_up"));
            }
            double speed = b.getDeltaMovement().horizontalDistance();
            return state.setAndContinue(animation.thenLoop("animation.beast_of_gevaudan." + (!state.isMoving() ? "idle" : speed > 0.16 ? "run" : "walk")));
        }).triggerableAnim(BITE, RawAnimation.begin().thenPlay("animation.beast_of_gevaudan.attack_bite"))
                .triggerableAnim(SWIPE, RawAnimation.begin().thenPlay("animation.beast_of_gevaudan.attack_swipe"))
                .triggerableAnim(HOWL, RawAnimation.begin().thenPlay("animation.beast_of_gevaudan.roar"))
                .triggerableAnim(REAR_UP, RawAnimation.begin().thenPlay("animation.beast_of_gevaudan.rear_up")));
    }
}
