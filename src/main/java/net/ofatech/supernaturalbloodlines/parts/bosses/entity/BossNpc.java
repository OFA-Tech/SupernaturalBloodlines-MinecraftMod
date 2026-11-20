package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;

/**
 * A plain {@code PathfinderMob} (not a Vampirism entity, plan 17 decision 3): no sun, no bite, no conversion. It never despawns, is never counted
 * toward a cap, and keeps its bar and its place in {@link BossIndex} up to date. Server and client both load this class: nothing client-side may be named here.
 */
public abstract class BossNpc extends PathfinderMob {
    private static final int PERIOD = 10;

    protected BossNpc(EntityType<? extends BossNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public abstract BossId boss();

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % PERIOD == 0 && isAlive()) {
            BossBars.update(this);
            BossIndex.get(level().getServer()).seen(boss(), getUUID(), level().dimension().location().toString(), blockPosition());
            serverPeriod();
        }
    }

    /** Called every {@value #PERIOD} ticks on the server while alive. */
    protected void serverPeriod() {
    }

    /** The base value of an attribute times a config factor, set once when the director makes the boss. */
    public void scale(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double factor) {
        AttributeInstance a = getAttribute(attribute);
        if (a != null) {
            a.setBaseValue(a.getBaseValue() * factor);
        }
    }

    /** Health and damage factors of the config, applied to a new boss and then filled up. */
    public void applyFactors() {
        scale(Attributes.MAX_HEALTH, BossesConfig.healthFactor());
        scale(Attributes.ATTACK_DAMAGE, BossesConfig.damageFactor());
        setHealth(getMaxHealth());
    }
}
