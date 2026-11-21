package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.KlausRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.PreyGoal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.Venom;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Klaus: one entity with a synced form (plan 17, decision 4). He walks as a man (the player model, {@code klaus.png}), shifts at half health or against
 * a werewolf, and fights on as the hybrid wolf (GeckoLib, {@code klaus_hybrid}), whose bite is werewolf venom. The UUID, the health, the boss bar and the
 * index entry stay the same across the shift. This class names no client class: the renderer is in {@code client/}. GeckoLib's animation types are common code.
 */
public final class KlausEntity extends NpcOriginalBoss implements GeoEntity, ShiftBody {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(KlausEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHIFT_TICKS = SynchedEntityData.defineId(KlausEntity.class, EntityDataSerializers.INT);
    private static final ResourceLocation BEAST_DAMAGE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "klaus_beast_damage");
    private static final ResourceLocation BEAST_SPEED = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "klaus_beast_speed");
    /** The action-bar line for a hunted player when the wolf gives up or the hunt is called off. */
    public static final String LOST_SCENT = "supernaturalbloodlines.boss.klaus.lost_scent";
    private static final String CONTROLLER = "main";
    private static final String ATTACK = "attack";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HuntState hunt = new HuntState();

    public KlausEntity(EntityType<? extends KlausEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 240.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 80.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public BossId boss() {
        return BossId.KLAUS;
    }

    @Override
    protected ItemStack reward() {
        return new ItemStack(BossItems.KLAUS_BLOOD.get());
    }

    // ---- the form ----

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, BossForm.HUMAN.ordinal());
        builder.define(SHIFT_TICKS, 0);
    }

    @Override
    public BossForm form() {
        return BossForm.of(entityData.get(FORM));
    }

    @Override
    public int shiftTicks() {
        return entityData.get(SHIFT_TICKS);
    }

    /** Sets the form, the hitbox with it and the beast's bonuses; the ticks of the shift start again when it is the start of one. */
    public void setForm(BossForm form) {
        entityData.set(FORM, form.ordinal());
        if (form == BossForm.HUMAN) {
            entityData.set(SHIFT_TICKS, 0);
        }
        if (!level().isClientSide) {
            beastBonuses(form == BossForm.BEAST);
        }
        refreshDimensions();
    }

    private static volatile java.util.function.Consumer<KlausEntity> swapObserver;

    /** Tests: told every time the body is swapped (the hitbox is already the wolf's then); null stops. */
    public static void observeSwap(@Nullable java.util.function.Consumer<KlausEntity> observer) {
        swapObserver = observer;
    }

    /**
     * The body is swapped (tick {@link ShiftRules#SWAP_TICK}): the hitbox is the wolf's from this tick, a burst of smoke hides the change, he howls
     * (the howl animation starts on the same tick) and the model shakes (the hurt tilt, which the client plays on the entity event).
     */
    @Override
    public void onSwap(ServerLevel server) {
        refreshDimensions();
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.9, getZ(), 40, 0.4, 0.6, 0.4, 0.04);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 20, 0.5, 0.4, 0.5, 0.08);
        server.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.WOLF_HOWL, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        server.broadcastEntityEvent(this, (byte) 2);
        java.util.function.Consumer<KlausEntity> o = swapObserver;
        if (o != null) {
            o.accept(this);
        }
    }

    /** Only how it looks (the forms the renderer reads), for the dev screenshots: no bonuses, no shift. */
    public void lookForm(BossForm form, int ticks) {
        entityData.set(FORM, form.ordinal());
        entityData.set(SHIFT_TICKS, ticks);
        refreshDimensions();
    }

    /** Starts the shift: he stops where he stands, howls, and is the wolf when it is done. */
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
    public void setShiftTicks(int ticks) {
        entityData.set(SHIFT_TICKS, ticks);
    }

    @Override
    protected EntityDimensions standingDimensions(Pose pose) {
        ShiftRules.Size size = ShiftRules.dimensionsFor(BossForm.of(entityData.get(FORM)), entityData.get(SHIFT_TICKS));
        return EntityDimensions.scalable(size.width(), size.height());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key) || SHIFT_TICKS.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        ShiftCore.hold(this, this);
        super.tick();
        // a fallen Klaus is not in the middle of anything: the shift counter, the swap and the howl wait until he gets up
        if (level().isClientSide || isDesiccated()) {
            return;
        }
        ShiftCore.advance(this);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            beastBonuses(form() == BossForm.BEAST);
        }
    }

    // ---- the hunt ----

    public void hunt(@Nullable UUID prey) {
        hunt.hunt(prey);
    }

    @Nullable
    public UUID prey() {
        return hunt.prey();
    }

    /** Ticks since he last landed a blow on his prey (the tests read it). */
    public int sinceHit() {
        return hunt.sinceHit();
    }

    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        LivingEntity fighting = getTarget();
        if (!isDesiccated() && ShiftRules.shouldShift(form(), getHealth() / getMaxHealth(), fighting != null && VampirismSupernatural.isWerewolf(fighting))) {
            startShift();
        }
        switch (hunt.period(this, !isDesiccated())) {
            case DAWN -> withdraw("supernaturalbloodlines.boss.klaus.dawn");
            case LOST -> withdraw(LOST_SCENT);
            case NONE -> {
            }
        }
    }

    /** He goes: the index says lost (so the director may send him again), the entity is gone. */
    public void withdraw() {
        withdraw(null);
    }

    /** He goes, and the player he hunted (if they are here) is told why. */
    private void withdraw(@Nullable String say) {
        hunt.leave(this, say);
        discard();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new PreyGoal(this, () -> hunt.prey()));
    }

    /** R163: the bite of the wolf is werewolf venom (the effect itself, as Venom would give it: the faction rule is for players and Werewolves mobs). */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim(CONTROLLER, ATTACK);
            hunt.landed(target);
            if (target instanceof LivingEntity victim
                    && KlausRules.venomBite(form(), VampirismSupernatural.isVampire(victim), victim.hasEffect(SpeciesRegistries.HYBRID_VENOM))) {
                victim.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, Venom.DURATION_TICKS));
            }
        }
        return hit;
    }

    // ---- save ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("form", form().ordinal());
        tag.putInt("shift_ticks", shiftTicks());
        hunt.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM, BossForm.of(tag.getInt("form")).ordinal());
        entityData.set(SHIFT_TICKS, tag.getInt("shift_ticks"));
        hunt.load(tag);
        refreshDimensions();
    }

    public static KlausEntity create(Level level) {
        return BossRegistries.KLAUS.get().create(level);
    }

    // ---- GeckoLib (the wolf) ----

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state -> {
            KlausEntity k = state.getAnimatable();
            String name = KlausRules.animation(k.isDeadOrDying(), k.isDesiccated(), ShiftRules.howling(k.form(), k.shiftTicks()), state.isMoving(),
                    k.getDeltaMovement().horizontalDistance());
            RawAnimation animation = RawAnimation.begin();
            return state.setAndContinue(switch (name) {
                case "death" -> animation.thenPlayAndHold("animation.klaus_hybrid.death");
                case "howl" -> animation.thenPlay("animation.klaus_hybrid.howl");
                default -> animation.thenLoop("animation.klaus_hybrid." + name);
            });
        }).triggerableAnim(ATTACK, RawAnimation.begin().thenPlay("animation.klaus_hybrid.attack_bite")));
    }
}
