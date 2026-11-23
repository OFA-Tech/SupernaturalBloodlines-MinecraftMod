package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.BeastPhaseRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals;

/**
 * The Host of the Beast of Gevaudan (R184): a soldier of the old kind, in the Valet's coat and tricorn, who stands where the director put him and does nothing but
 * wait for a full moon night. He is protected: nothing but the operator's reset, the void and {@code /kill} hurts him, because the Beast needs him. A death
 * from those fails the encounter until a later full moon night ({@code BeastEvents}). He is never a drop and never a reward. This class names no client class.
 */
public final class GevaudanHostEntity extends BossNpc {
    /** The blocks he keeps within of where he was put. */
    public static final int WANDER = 12;
    public static final String UNFELT = "supernaturalbloodlines.boss.beast.unfelt";

    public GevaudanHostEntity(EntityType<? extends GevaudanHostEntity> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(BossItems.SEBASTIEN_TRICORN.get()));
        setDropChance(EquipmentSlot.HEAD, 0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public BossId boss() {
        return BossId.BEAST_OF_GEVAUDAN;
    }

    @Override
    public net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look barLook() {
        return net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.host(0);
    }

    @Override
    public String skin() {
        return "sebastien_valet";
    }

    @Override
    protected boolean stale() {
        return BeastPhaseRules.staleHost(BossIndex.get(level().getServer()).of(BossId.BEAST_OF_GEVAUDAN).phase);
    }

    @Override
    protected boolean hasBar() {
        return false;
    }

    public void stayNear(BlockPos pos) {
        restrictTo(pos, WANDER);
    }

    /** Only what bypasses invulnerability (the void, {@code /kill}) hurts him: the encounter needs him standing. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** A blow that does nothing says so, to the player behind it (a tamed animal's owner too): nobody wonders whether the game is broken. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isInvulnerableTo(source) && Deals.playerBehind(level().getServer(), source.getEntity()) instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable(UNFELT), true);
        }
        return super.hurt(source, amount);
    }

    // The encounter needs him where he is: he cannot be led away, carried off, pushed or sent to another dimension.
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    /** Pistons, flowing water, explosions and shoves do not move him either. */
    @Override
    public net.minecraft.world.level.material.PushReaction getPistonPushReaction() {
        return net.minecraft.world.level.material.PushReaction.IGNORE;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
        return true;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static GevaudanHostEntity create(Level level) {
        return BossRegistries.GEVAUDAN_HOST.get().create(level);
    }
}
