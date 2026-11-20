package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
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
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.RebekahRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.PreyGoal;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/** Rebekah (R169): comes for whoever made a Cure at the rite, fast, by day or night. A hand-to-hand fighter: no weapon, so nothing of hers is white oak (R158). */
public final class RebekahEntity extends NpcOriginalBoss {
    @Nullable
    private UUID prey;
    private int strandedTicks;

    public RebekahEntity(EntityType<? extends RebekahEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 170.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    @Override
    public BossId boss() {
        return BossId.REBEKAH;
    }

    @Override
    protected ItemStack reward() {
        return new ItemStack(BossItems.THE_TRUE_CURE.get());
    }

    public void hunt(@Nullable UUID player) {
        this.prey = player;
    }

    @Nullable
    public UUID prey() {
        return prey;
    }

    /** No prey here (logged off, another dimension, dead) and nobody to fight, for a minute: she withdraws and may come again. */
    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        if (prey == null || isDesiccated() || level().getServer() == null) {
            strandedTicks = 0;
            return;
        }
        boolean here = PreyGoal.find(this, prey) != null;
        strandedTicks = RebekahRules.stranded(here, getTarget() != null) ? strandedTicks + 10 : 0;
        if (RebekahRules.withdraws(strandedTicks)) {
            withdraw();
        }
    }

    public void withdraw() {
        if (level().getServer() != null) {
            if (prey != null && SireCall.player(level().getServer(), prey) instanceof ServerPlayer p) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.boss.rebekah.gone"), true);
            }
            BossIndex.get(level().getServer()).apply(boss(), BossRules.Event.LOST);
        }
        discard();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
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

    public static RebekahEntity create(Level level) {
        return BossRegistries.REBEKAH.get().create(level);
    }
}