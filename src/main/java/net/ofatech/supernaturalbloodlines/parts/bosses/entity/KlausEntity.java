package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
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
public final class KlausEntity extends NpcOriginalBoss implements GeoEntity {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(KlausEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHIFT_TICKS = SynchedEntityData.defineId(KlausEntity.class, EntityDataSerializers.INT);
    private static final ResourceLocation BEAST_DAMAGE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "klaus_beast_damage");
    private static final ResourceLocation BEAST_SPEED = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "klaus_beast_speed");
    private static final String CONTROLLER = "main";
    private static final String ATTACK = "attack";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID prey;
    private int strandedTicks;

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

    public BossForm form() {
        return BossForm.of(entityData.get(FORM));
    }

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
        bonus(Attributes.ATTACK_DAMAGE, BEAST_DAMAGE, ShiftRules.BEAST_DAMAGE_BONUS, on);
        bonus(Attributes.MOVEMENT_SPEED, BEAST_SPEED, ShiftRules.BEAST_SPEED_BONUS, on);
    }

    private void bonus(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation id, double amount, boolean on) {
        AttributeInstance a = getAttribute(attribute);
        if (a == null) {
            return;
        }
        a.removeModifier(id);
        if (on) {
            a.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        ShiftRules.Size size = ShiftRules.dimensionsFor(BossForm.of(entityData.get(FORM)));
        return EntityDimensions.scalable(size.width(), size.height());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        // he stands still while he shifts: before the tick moves him, not after
        if (!level().isClientSide && form() == BossForm.SHIFTING) {
            getNavigation().stop();
            setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
        }
        super.tick();
        if (level().isClientSide) {
            return;
        }
        BossForm form = form();
        if (form != BossForm.HUMAN && shiftTicks() < ShiftRules.HOWL_TICKS) {
            entityData.set(SHIFT_TICKS, shiftTicks() + 1);
        }
        BossForm after = ShiftRules.afterTick(form, shiftTicks());
        if (after != form) {
            setForm(after);
        }
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
        this.prey = prey;
    }

    @Nullable
    public UUID prey() {
        return prey;
    }

    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        LivingEntity fighting = getTarget();
        if (ShiftRules.shouldShift(form(), getHealth() / getMaxHealth(), fighting != null && VampirismSupernatural.isWerewolf(fighting))) {
            startShift();
        }
        if (prey == null || isDesiccated() || level().getServer() == null) {
            strandedTicks = 0;
            return;
        }
        strandedTicks = KlausRules.stranded(PreyGoal.find(this, prey) != null, fighting != null) ? strandedTicks + 10 : 0;
        if (KlausRules.withdraws(strandedTicks)) {
            withdraw();
        }
    }

    /** He goes: the index says lost (so the director may send him again), the entity is gone. */
    public void withdraw() {
        if (level().getServer() != null) {
            BossIndex.get(level().getServer()).apply(boss(), BossRules.Event.LOST);
        }
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
        targetSelector.addGoal(2, new PreyGoal(this, () -> prey));
    }

    /** R163: the bite of the wolf is werewolf venom (the effect itself, as Venom would give it: the faction rule is for players and Werewolves mobs). */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim(CONTROLLER, ATTACK);
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
        if (prey != null) {
            tag.putUUID("prey", prey);
        }
        tag.putInt("stranded", strandedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM, BossForm.of(tag.getInt("form")).ordinal());
        entityData.set(SHIFT_TICKS, tag.getInt("shift_ticks"));
        prey = tag.hasUUID("prey") ? tag.getUUID("prey") : null;
        strandedTicks = tag.getInt("stranded");
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
            if (k.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.klaus_hybrid.death"));
            }
            if (ShiftRules.howling(k.form(), k.shiftTicks())) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.klaus_hybrid.howl"));
            }
            if (state.isMoving()) {
                double speed = k.getDeltaMovement().horizontalDistance();
                return state.setAndContinue(RawAnimation.begin().thenLoop(speed > 0.2 ? "animation.klaus_hybrid.run" : "animation.klaus_hybrid.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.klaus_hybrid.idle"));
        }).triggerableAnim(ATTACK, RawAnimation.begin().thenPlay("animation.klaus_hybrid.attack_bite")));
    }
}
