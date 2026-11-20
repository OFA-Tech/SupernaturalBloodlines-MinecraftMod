package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.KolRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.BossLeapGoal;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.PreyGoal;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/** Kol (R173): fast, and a leap at his target; no other ability. He hunts the Hunter of the Five the director sent him for, and pays Kol's Grimoire. */
public final class KolEntity extends NpcOriginalBoss {
    @Nullable
    private UUID prey;
    private int strandedTicks;
    private int leapPause;

    public KolEntity(EntityType<? extends KolEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.40)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    @Override
    public BossId boss() {
        return BossId.KOL;
    }

    @Override
    protected ItemStack reward() {
        return new ItemStack(BossItems.KOL_GRIMOIRE.get());
    }

    public void hunt(@Nullable UUID player) {
        this.prey = player;
    }

    @Nullable
    public UUID prey() {
        return prey;
    }

    public int leapPause() {
        return leapPause;
    }

    /** The leap: up and toward {@code target}, and the pause begins. */
    public void leapAt(@Nullable LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 toward = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
        Vec3 flat = toward.lengthSqr() < 1.0E-6 ? Vec3.ZERO : toward.normalize().scale(0.9);
        setDeltaMovement(flat.x, 0.5, flat.z);
        hasImpulse = true;
        leapPause = KolRules.LEAP_PAUSE;
    }

    /** No prey here (logged off, another dimension, dead) and nobody to fight, for a minute: he withdraws and may come again. */
    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        leapPause = Math.max(0, leapPause - 10);
        if (prey == null || isDesiccated() || level().getServer() == null) {
            strandedTicks = 0;
            return;
        }
        boolean here = PreyGoal.find(this, prey) != null;
        strandedTicks = KolRules.stranded(here, getTarget() != null) ? strandedTicks + 10 : 0;
        if (KolRules.withdraws(strandedTicks)) {
            withdraw();
        }
    }

    public void withdraw() {
        if (level().getServer() != null) {
            if (prey != null && SireCall.player(level().getServer(), prey) instanceof ServerPlayer p) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.boss.kol.gone"), true);
            }
            BossIndex.get(level().getServer()).apply(boss(), BossRules.Event.LOST);
        }
        discard();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BossLeapGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new PreyGoal(this, () -> prey));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (prey != null) {
            tag.putUUID("prey", prey);
        }
        tag.putInt("stranded", strandedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        prey = tag.hasUUID("prey") ? tag.getUUID("prey") : null;
        strandedTicks = tag.getInt("stranded");
    }

    public static KolEntity create(Level level) {
        return BossRegistries.KOL.get().create(level);
    }
}
