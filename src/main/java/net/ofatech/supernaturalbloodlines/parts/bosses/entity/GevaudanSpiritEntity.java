package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;

/**
 * The Spirit of Gevaudan (R185): what leaves the Beast when it collapses. Flying smoke with no model: it runs from every player for
 * {@code beast_spirit_seconds}, and <b>only the Valet Pike hurts it</b> ({@code BeastEvents}); a blow of the pike ends the Beast for good. When the time is up it
 * escapes ({@link BeastDirector#escape}). It is not a boss of the index: the Host holds that place. This class names no client class.
 */
public final class GevaudanSpiritEntity extends PathfinderMob {
    private boolean pikeHit;

    public GevaudanSpiritEntity(EntityType<? extends GevaudanSpiritEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 24.0F, 1.2, 1.6));
        goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
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
    public boolean isPushable() {
        return false;
    }

    /** Set by the event that lets the pike through; a death without it is an escape, not a kill. */
    public void markPikeHit() {
        pikeHit = true;
    }

    public boolean pikeHit() {
        return pikeHit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (tickCount % 2 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 6, 0.35, 0.5, 0.35, 0.01);
                server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 1, 0.3, 0.4, 0.3, 0.01);
            }
            if (tickCount % 20 == 0 && isAlive() && BeastDirector.spiritTimeIsUp(server, this)) {
                BeastDirector.escape(server, this);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("pike_hit", pikeHit);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pikeHit = tag.getBoolean("pike_hit");
    }

    public static GevaudanSpiritEntity create(Level level) {
        return BossRegistries.GEVAUDAN_SPIRIT.get().create(level);
    }
}
