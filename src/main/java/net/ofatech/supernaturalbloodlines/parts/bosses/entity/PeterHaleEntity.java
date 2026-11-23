package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.DamageTally;
import net.ofatech.supernaturalbloodlines.core.boss.PeterRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Peter Hale (plan 17g, R181): one entity with a synced form, like Klaus and Deucalion. He waits in his burned house as a man (the player model,
 * {@code peter_hale_burned.png} until he has met a full moon, {@code peter_hale.png} after), shifts at half health or on a full moon night and fights on as the
 * Alpha, a quadruped (GeckoLib), for good. As the wolf he is feral on a full moon night: he attacks anything alive that is not a boss. His pounce moves the root bone of
 * the model, so while it plays he stands still and ends where the animation does (nothing of his own is added to the model's height). This class names no client class.
 */
public final class PeterHaleEntity extends BossNpc implements GeoEntity, ShiftBody {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(PeterHaleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHIFT_TICKS = SynchedEntityData.defineId(PeterHaleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MET_MOON = SynchedEntityData.defineId(PeterHaleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation BEAST_DAMAGE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "peter_beast_damage");
    private static final ResourceLocation BEAST_SPEED = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "peter_beast_speed");
    private static final String CONTROLLER = "main";
    private static final String SWIPE = "swipe";
    private static final String BITE = "bite";
    private static final String POUNCE = "pounce";
    private static final int HOME_RADIUS = 32;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DamageTally tally = new DamageTally();
    private boolean swipe;
    private int pounceTicks;
    private int pauseLeft;
    private Vec3 pounceDirection = Vec3.ZERO;
    @Nullable
    private BlockPos home;

    public PeterHaleEntity(EntityType<? extends PeterHaleEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public BossId boss() {
        return BossId.PETER_HALE;
    }

    @Override
    public net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look barLook() {
        return net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.shifter(form());
    }

    @Override
    public String skin() {
        return PeterRules.skin(entityData.get(MET_MOON));
    }

    // ---- the form ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, BossForm.HUMAN.ordinal());
        builder.define(SHIFT_TICKS, 0);
        builder.define(MET_MOON, false);
    }

    @Override
    public BossForm form() {
        return BossForm.of(entityData.get(FORM));
    }

    @Override
    public int shiftTicks() {
        return entityData.get(SHIFT_TICKS);
    }

    @Override
    public void setShiftTicks(int ticks) {
        entityData.set(SHIFT_TICKS, ticks);
    }

    @Override
    public void setForm(BossForm form) {
        entityData.set(FORM, form.ordinal());
        if (!level().isClientSide) {
            beastBonuses(form == BossForm.BEAST);
        }
        refreshDimensions();
    }

    /** Only how it looks (the forms and the skin the renderer reads), for the dev screenshots: no bonuses, no shift. */
    public void lookForm(BossForm form, int ticks, boolean metMoon) {
        entityData.set(FORM, form.ordinal());
        entityData.set(SHIFT_TICKS, ticks);
        entityData.set(MET_MOON, metMoon);
        refreshDimensions();
    }

    public boolean metMoon() {
        return entityData.get(MET_MOON);
    }

    public void startShift() {
        if (form() == BossForm.HUMAN) {
            entityData.set(SHIFT_TICKS, 0);
            setForm(BossForm.SHIFTING);
        }
    }

    private void beastBonuses(boolean on) {
        ShiftCore.beastBonuses(this, BEAST_DAMAGE, BEAST_SPEED, on);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        ShiftRules.Size size = PeterRules.dimensionsFor(BossForm.of(entityData.get(FORM)), entityData.get(SHIFT_TICKS));
        return EntityDimensions.scalable(size.width(), size.height());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key) || SHIFT_TICKS.equals(key)) {
            refreshDimensions();
        }
    }

    /** The body is swapped: the wolf's hitbox from this tick, smoke over the change, and a howl. */
    @Override
    public void onSwap(ServerLevel server) {
        refreshDimensions();
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.9, getZ(), 40, 0.4, 0.6, 0.4, 0.04);
        server.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.WOLF_HOWL, net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.4F);
    }

    // ---- the tick, the pounce ----

    @Override
    public void tick() {
        ShiftCore.hold(this, this);
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ShiftCore.advance(this);
        if (pauseLeft > 0) {
            pauseLeft--;
        }
        if (pounceTicks > 0) {
            pounceTick();
        } else if (isAlive() && getTarget() != null && form() == BossForm.BEAST && onGround() && pauseLeft <= 0
                && PeterRules.pounces(form(), distanceToSqr(getTarget()), onGround(), pauseLeft, hasLineOfSight(getTarget()))) {
            startPounce(getTarget());
        }
    }

    /** While the pounce plays the model's root bone is what moves: nothing of his own travel is added, whatever the navigation wants. */
    @Override
    public void travel(Vec3 input) {
        if (!level().isClientSide && pounceTicks > 0) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    private void startPounce(LivingEntity target) {
        Vec3 to = target.position().subtract(position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        pounceDirection = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
        pounceTicks = 1;
        pauseLeft = PeterRules.POUNCE_PAUSE;
        setYRot((float) (Math.toDegrees(Math.atan2(-pounceDirection.x, pounceDirection.z))));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        triggerAnim(CONTROLLER, POUNCE);
    }

    private void pounceTick() {
        if (isDeadOrDying()) {
            pounceTicks = 0;
            return;
        }
        if (pounceTicks == PeterRules.POUNCE_LAND_TICK) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && PeterRules.pounceLands(target.position().distanceToSqr(position().add(pounceDirection.scale(PeterRules.POUNCE_DISTANCE))), hasLineOfSight(target))) {
                pounceHit = true;
                try {
                    doHurtTarget(target);
                } finally {
                    pounceHit = false;
                }
            }
        }
        if (pounceTicks >= PeterRules.POUNCE_TICKS) {
            land();
            pounceTicks = 0;
            return;
        }
        pounceTicks++;
    }

    /** The animation ends with the model 30 pixels ahead of where it began: the entity goes there too (as far as there is room), so nothing jumps back. */
    private void land() {
        for (double scale : new double[] {1.0, 0.5}) {
            Vec3 step = pounceDirection.scale(PeterRules.POUNCE_DISTANCE * scale);
            if (level().noCollision(this, getBoundingBox().move(step))) {
                setPos(getX() + step.x, getY(), getZ() + step.z);
                return;
            }
        }
    }

    private boolean pounceHit;

    public int pauseLeft() {
        return pauseLeft;
    }

    public boolean pouncing() {
        return pounceTicks > 0;
    }

    public int pounceTicks() {
        return pounceTicks;
    }

    // ---- the house, who he fights ----

    /** The house: he keeps within 32 blocks of it. */
    public void setHome(BlockPos pos) {
        home = pos;
        restrictTo(pos, HOME_RADIUS);
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    /** Who did the most to bring him down (R183). */
    public DamageTally tally() {
        return tally;
    }

    /** Feral: the wolf, on a full moon night, with the setting on. */
    public boolean feral() {
        return level() instanceof ServerLevel server && PeterRules.feral(form(), BossDirector.fullMoonNightNow(server), BossesConfig.peterFeral());
    }

    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        boolean moon = level() instanceof ServerLevel server && BossDirector.fullMoonNightNow(server);
        if (!metMoon() && PeterRules.metFullMoon(false, moon)) {
            entityData.set(MET_MOON, true);
        }
        if (PeterRules.shouldShift(form(), getHealth() / getMaxHealth(), moon, BossesConfig.peterFeral())) {
            startShift();
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new FeralGoal(this));
        targetSelector.addGoal(3, new GuardGoal(this));
    }

    /** On a full moon night the wolf takes anything alive within 24 blocks that is not a boss or somebody who cannot be hurt. */
    private static final class FeralGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private final PeterHaleEntity peter;

        FeralGoal(PeterHaleEntity peter) {
            super(peter, LivingEntity.class, 10, false, false, (Predicate<LivingEntity>) e -> PeterRules.mayTarget(e == peter, e instanceof BossNpc, e.isAlive(),
                    e instanceof Player p && (p.isCreative() || p.isSpectator())));
            this.peter = peter;
            // a feral beast smells what it cannot see: it takes anything alive within 24 blocks, through a wall too
            this.targetConditions.ignoreLineOfSight();
        }

        @Override
        public boolean canUse() {
            return peter.feral() && super.canUse();
        }

        @Override
        protected double getFollowDistance() {
            return PeterRules.FERAL_RANGE;
        }
    }

    /** The house is his: a player within 10 blocks is fought, wolf or man. */
    private static final class GuardGoal extends NearestAttackableTargetGoal<Player> {
        GuardGoal(PeterHaleEntity peter) {
            super(peter, Player.class, true);
        }

        @Override
        protected double getFollowDistance() {
            return PeterRules.GUARD_RANGE;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !pounceHit) {
            swipe = !swipe;
            triggerAnim(CONTROLLER, swipe ? SWIPE : BITE);
        }
        return hit;
    }

    // ---- save ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("form", form().ordinal());
        tag.putInt("shift_ticks", shiftTicks());
        tag.putBoolean("met_moon", metMoon());
        if (home != null) {
            tag.putIntArray("home", new int[] {home.getX(), home.getY(), home.getZ()});
        }
        net.minecraft.nbt.ListTag hits = new net.minecraft.nbt.ListTag();
        tally.entries().forEach((id, entry) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("who", id);
            t.putFloat("amount", entry.amount());
            t.putLong("seq", entry.seq());
            hits.add(t);
        });
        tag.put("damage", hits);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM, BossForm.of(tag.getInt("form")).ordinal());
        entityData.set(SHIFT_TICKS, tag.getInt("shift_ticks"));
        entityData.set(MET_MOON, tag.getBoolean("met_moon"));
        int[] h = tag.getIntArray("home");
        if (h.length == 3) {
            setHome(new BlockPos(h[0], h[1], h[2]));
        }
        for (Tag t : tag.getList("damage", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("who")) {
                tally.put(c.getUUID("who"), c.getFloat("amount"), c.getLong("seq"));
            }
        }
        refreshDimensions();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            beastBonuses(form() == BossForm.BEAST);
        }
    }

    public static PeterHaleEntity create(Level level) {
        return BossRegistries.PETER_HALE.get().create(level);
    }

    /** The Alpha that took the killing blow's credit is whoever the damage tally says; the entity itself only knows the players it was hurt by. */
    @Nullable
    public ServerPlayer topPlayer(net.minecraft.server.MinecraftServer server) {
        return tally.top().map(id -> net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.player(server, id)).orElse(null);
    }

    // ---- GeckoLib (the Alpha) ----

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state -> {
            PeterHaleEntity p = state.getAnimatable();
            String name = net.ofatech.supernaturalbloodlines.core.boss.KlausRules.animation(p.isDeadOrDying(), false, ShiftRules.howling(p.form(), p.shiftTicks()),
                    state.isMoving(), p.getDeltaMovement().horizontalDistance());
            RawAnimation animation = RawAnimation.begin();
            return state.setAndContinue(switch (name) {
                case "death" -> animation.thenPlayAndHold("animation.peter_hale_alpha.death");
                case "howl" -> animation.thenPlay("animation.peter_hale_alpha.roar");
                default -> animation.thenLoop("animation.peter_hale_alpha." + name);
            });
        }).triggerableAnim(SWIPE, RawAnimation.begin().thenPlay("animation.peter_hale_alpha.attack_swipe"))
                .triggerableAnim(BITE, RawAnimation.begin().thenPlay("animation.peter_hale_alpha.attack_bite"))
                .triggerableAnim(POUNCE, RawAnimation.begin().thenPlay("animation.peter_hale_alpha.pounce")));
    }
}
