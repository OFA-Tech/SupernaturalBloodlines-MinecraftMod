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
    private int lastBarKey = -1;

    protected BossNpc(EntityType<? extends BossNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public abstract BossId boss();

    /** The form indicator of this boss's bar: no chips unless the boss has forms. */
    public net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look barLook() {
        return net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look.NONE;
    }

    /** What the bar shows of this boss besides its health: when it changes the bar is updated at once and not at the next pass. */
    private int barKey() {
        net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look look = barLook();
        return look.set().ordinal() * 8 + look.active();
    }

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
        if (!level().isClientSide) {
            int key = barKey();
            boolean looked = key != lastBarKey;
            lastBarKey = key;
            if (looked && tickCount % PERIOD != 0 && isAlive() && hasBar()) {
                BossBars.update(this); // a shift begins or ends: the chip moves now, not up to half a second later
            }
        }
        if (!level().isClientSide && tickCount % PERIOD == 0 && isAlive()) {
            if (stale()) {
                discard(); // a body of a story that has moved on (the one the index called lost, found again): it is not the boss any more
                return;
            }
            if (hasBar()) {
                BossBars.update(this);
            } else {
                BossBars.remove(getUUID());
            }
            if (tracked() && !BossIndex.get(level().getServer()).seen(boss(), getUUID(), level().dimension().location().toString(), blockPosition(), isOpenNow())) {
                discard(); // the world has another one of this boss: this is the old one that came back
                return;
            }
            serverPeriod();
        }
    }

    /** The file name of the man's skin in {@code textures/entity/boss/}: the boss's id unless it says otherwise. */
    public String skin() {
        return boss().id();
    }

    /** Whether the hat layer of the man's skin is drawn (Deucalion's glasses go with the shift); the client reads it. */
    public boolean hatVisible() {
        return true;
    }

    /** Whether the story has no place for this body now, so it is discarded before the index is told it was seen. */
    protected boolean stale() {
        return false;
    }

    /** Whether this body shows a boss bar: a calm protected body and a falling one do not. */
    protected boolean hasBar() {
        return true;
    }

    /** Whether the index follows this body: false for a body that has already handed its place to another (the Beast that is falling). */
    protected boolean tracked() {
        return true;
    }

    /** Whether the boss is lying down open to white oak; false for a boss that has no such state. */
    protected boolean isOpenNow() {
        return false;
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
