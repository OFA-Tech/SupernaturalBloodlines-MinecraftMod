package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.DeucalionRules;
import net.ofatech.supernaturalbloodlines.core.boss.KlausRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.PreyGoal;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Deucalion (plan 17f): one entity with a synced form, like Klaus. He walks as a man (the player model, {@code deucalion_human.png}; the glasses are the
 * hat layer and go with the shift), drops to fours below three quarters of his health or when his target is within 8 blocks at night, and fights on as the
 * demon wolf (GeckoLib) for good. He is no Original and no Vampirism entity: he dies, and his death pays the sparks and the Roar ({@code DeucalionEvents}).
 * This class names no client class and no Werewolves class: the pack is only a list of UUIDs here.
 */
public final class DeucalionEntity extends BossNpc implements GeoEntity, ShiftBody {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(DeucalionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHIFT_TICKS = SynchedEntityData.defineId(DeucalionEntity.class, EntityDataSerializers.INT);
    private static final ResourceLocation BEAST_DAMAGE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "deucalion_beast_damage");
    private static final ResourceLocation BEAST_SPEED = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "deucalion_beast_speed");
    public static final String LOST_SCENT = "supernaturalbloodlines.boss.deucalion.lost_scent";
    private static final String CONTROLLER = "main";
    private static final String SWIPE = "swipe";
    private static final String BITE = "bite";
    private static final java.util.Map<String, RawAnimation> CLIPS = ShiftCore.clips("deucalion_demon_wolf", "roar");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HuntState hunt = new HuntState();
    private final net.ofatech.supernaturalbloodlines.core.boss.DamageTally tally = new net.ofatech.supernaturalbloodlines.core.boss.DamageTally();
    private boolean swipe;
    private final List<UUID> pack = new ArrayList<>();

    public DeucalionEntity(EntityType<? extends DeucalionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 280.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 80.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public BossId boss() {
        return BossId.DEUCALION;
    }

    @Override
    public net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look barLook() {
        return net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.shifter(form());
    }

    @Override
    public String skin() {
        return "deucalion_human";
    }

    @Override
    public boolean hatVisible() {
        return ShiftRules.hatVisible(form());
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

    public void setForm(BossForm form) {
        entityData.set(FORM, form.ordinal());
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
    public EntityDimensions getDefaultDimensions(Pose pose) {
        ShiftRules.Size size = DeucalionRules.dimensionsFor(BossForm.of(entityData.get(FORM)), entityData.get(SHIFT_TICKS));
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
        if (level().isClientSide) {
            return;
        }
        ShiftCore.advance(this);
    }

    /** The body is swapped: the hitbox is the wolf's from this tick, smoke hides the change and he roars; the wolf arrives with its own {@code shift_in} clip. */
    @Override
    public void onSwap(ServerLevel server) {
        refreshDimensions();
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.9, getZ(), 40, 0.4, 0.6, 0.4, 0.04);
        server.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.WOLF_HOWL, net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.5F);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            beastBonuses(form() == BossForm.BEAST);
        }
    }

    // ---- the hunt and the pack ----

    public void hunt(@Nullable UUID prey) {
        hunt.hunt(prey);
    }

    @Nullable
    public UUID prey() {
        return hunt.prey();
    }

    public int sinceHit() {
        return hunt.sinceHit();
    }

    /** Who did the most to bring him down (R179): the damage players dealt to him, a pet's for its owner. */
    public net.ofatech.supernaturalbloodlines.core.boss.DamageTally tally() {
        return tally;
    }

    /** The Alphas that came with him (the director fills it). */
    public List<UUID> pack() {
        return pack;
    }

    /** The pack goes when he does: at his death, at dawn, when he gives up. */
    public void dismissPack() {
        if (level() instanceof ServerLevel level) {
            for (UUID id : pack) {
                for (ServerLevel l : level.getServer().getAllLevels()) {
                    Entity member = l.getEntity(id);
                    if (member != null) {
                        member.discard();
                    }
                }
            }
        }
        pack.clear();
    }

    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        LivingEntity fighting = getTarget();
        boolean night = !KlausRules.dawn(level().getDayTime());
        if (DeucalionRules.shouldShift(form(), getHealth() / getMaxHealth(), fighting != null && DeucalionRules.targetClose(distanceToSqr(fighting)), night)) {
            startShift();
        }
        switch (hunt.period(this, true)) {
            case DAWN -> withdraw("supernaturalbloodlines.boss.deucalion.dawn");
            case LOST -> withdraw(LOST_SCENT);
            case NONE -> {
            }
        }
    }

    /** He goes, his pack with him: the index says lost (so the director may send him again), and the one he hunted is told why. */
    public void withdraw(@Nullable String say) {
        hunt.leave(this, say);
        dismissPack();
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

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            swipe = !swipe;
            triggerAnim(CONTROLLER, swipe ? SWIPE : BITE);
            hunt.landed(target);
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
        net.minecraft.nbt.ListTag hits = new net.minecraft.nbt.ListTag();
        tally.entries().forEach((id, entry) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("who", id);
            t.putFloat("amount", entry.amount());
            t.putLong("seq", entry.seq());
            hits.add(t);
        });
        tag.put("damage", hits);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        pack.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put("pack", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM, BossForm.of(tag.getInt("form")).ordinal());
        entityData.set(SHIFT_TICKS, tag.getInt("shift_ticks"));
        hunt.load(tag);
        for (Tag t : tag.getList("damage", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("who")) {
                tally.put(c.getUUID("who"), c.getFloat("amount"), c.getLong("seq"));
            }
        }
        pack.clear();
        for (Tag t : tag.getList("pack", Tag.TAG_INT_ARRAY)) {
            pack.add(NbtUtils.loadUUID(t));
        }
        refreshDimensions();
    }

    public static DeucalionEntity create(Level level) {
        return BossRegistries.DEUCALION.get().create(level);
    }

    // ---- GeckoLib (the demon wolf) ----

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state -> {
            DeucalionEntity d = state.getAnimatable();
            String name = KlausRules.animation(d.isDeadOrDying(), false, net.ofatech.supernaturalbloodlines.core.boss.MorphRules.arriving(d.form(), d.shiftTicks()),
                    ShiftRules.howling(d.form(), d.shiftTicks()), state.isMoving(), d.getDeltaMovement().horizontalDistance());
            return state.setAndContinue(CLIPS.get(name));
        }).triggerableAnim(SWIPE, RawAnimation.begin().thenPlay("animation.deucalion_demon_wolf.attack_swipe"))
                .triggerableAnim(BITE, RawAnimation.begin().thenPlay("animation.deucalion_demon_wolf.attack_bite")));
    }
}
