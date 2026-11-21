package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.FinnRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import org.jetbrains.annotations.Nullable;

/**
 * Finn (R175, R176): stands in a ring of particles under the eclipse and channels a ritual that hurts nobody. Left alone he finishes it, the people near are
 * told, and he withdraws until a later eclipse. Hurt for more than a fifth of his health, or pulled away from the ring, he breaks it off and fights like
 * anyone else. Brought down he pays Esther's Grimoire.
 */
public final class FinnEntity extends NpcOriginalBoss {
    private boolean ritualing;
    private boolean fighting;
    private int ritualTicks;
    private int ritualTotal;
    private float damageTaken;
    @Nullable
    private BlockPos centre;

    public FinnEntity(EntityType<? extends FinnEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 140.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    @Override
    public BossId boss() {
        return BossId.FINN;
    }

    @Override
    protected ItemStack reward() {
        return new ItemStack(BossItems.ESTHER_GRIMOIRE.get());
    }

    /** Finn's Notes: the eclipse rites (the Broken Line, the Imprisoned Original). */
    @Override
    protected java.util.List<ItemStack> extraRewards() {
        return java.util.List.of(net.ofatech.supernaturalbloodlines.parts.species.unlock.RitePageItem.of(net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable.FINN));
    }

    public boolean ritualing() {
        return ritualing;
    }

    public boolean fighting() {
        return fighting;
    }

    public int ritualTicks() {
        return ritualTicks;
    }

    @Nullable
    public BlockPos centre() {
        return centre;
    }

    /** Starts the ritual at {@code ring}: it lasts {@code seconds}. */
    public void beginRitual(BlockPos ring, int seconds) {
        centre = ring;
        ritualTotal = FinnRules.totalTicks(seconds);
        ritualTicks = 0;
        damageTaken = 0F;
        ritualing = true;
        fighting = false;
    }

    @Override
    protected void serverPeriod() {
        super.serverPeriod();
        if (ritualing && !isDesiccated()) {
            drawRing();
            ritualTick(10);
        }
    }

    /** Lets {@code ticks} of the ritual pass and looks at where it stands. The tests call it with whole seconds at once. */
    public void ritualTick(int ticks) {
        if (!ritualing || !(level() instanceof ServerLevel level) || centre == null) {
            return;
        }
        ritualTicks += ticks;
        evaluate(level);
    }

    private void evaluate(ServerLevel level) {
        double away = Math.sqrt(distanceToSqr(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5));
        switch (FinnRules.step(ritualTicks, ritualTotal, damageTaken, getMaxHealth(), away, BossDirector.eclipseNow(level.getServer()))) {
            case DONE -> finish(level);
            case INTERRUPTED -> breakOff(level);
            case ENDED -> withdraw(false);
            case CHANNELING -> { }
        }
    }

    /** The ritual was left alone: nobody is hurt, the people near are told, and he goes until a later eclipse. */
    private void finish(ServerLevel level) {
        ritualing = false;
        tell(level, "supernaturalbloodlines.boss.finn.done");
        withdraw(true);
    }

    private void breakOff(ServerLevel level) {
        ritualing = false;
        fighting = true;
        tell(level, "supernaturalbloodlines.boss.finn.broken");
        if (getLastHurtByMob() != null) {
            setTarget(getLastHurtByMob());
        }
    }

    private void tell(ServerLevel level, String key) {
        double r = FinnRules.RANGE * FinnRules.RANGE;
        for (ServerPlayer p : BossDirector.playersIn(level)) {
            if (p.distanceToSqr(this) <= r) {
                p.sendSystemMessage(Component.translatable(key));
            }
        }
    }

    /** He goes: lost to the index, and after a finished ritual the next roll is a week away. */
    private void withdraw(boolean finished) {
        if (level() instanceof ServerLevel level) {
            BossIndex index = BossIndex.get(level.getServer());
            index.apply(boss(), BossRules.Event.LOST);
            if (finished) {
                index.of(boss()).lastRollDay = FinnRules.afterSuccess(DesiccationRules.dayOf(level.getServer().overworld().getDayTime()));
            }
            index.setDirty();
        }
        discard();
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float before = getHealth();
        super.actuallyHurt(source, amount);
        if (ritualing && level() instanceof ServerLevel level && centre != null) {
            damageTaken += Math.max(0F, before - getHealth());
            evaluate(level);
        }
    }

    private void drawRing() {
        if (!(level() instanceof ServerLevel level) || centre == null) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0 + ritualTicks * 0.02;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centre.getX() + 0.5 + Math.cos(angle) * 4.0, centre.getY() + 0.15, centre.getZ() + 0.5 + Math.sin(angle) * 4.0,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.ENCHANT, centre.getX() + 0.5, centre.getY() + 1.2, centre.getZ() + 0.5, 6, 0.4, 0.6, 0.4, 0.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Goal() {
            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                return ritualing;
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !ritualing && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> fighting && p instanceof Player pl && !pl.isCreative() && !pl.isSpectator()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ritualing", ritualing);
        tag.putBoolean("fighting", fighting);
        tag.putInt("ritual_ticks", ritualTicks);
        tag.putInt("ritual_total", ritualTotal);
        tag.putFloat("damage_taken", damageTaken);
        if (centre != null) {
            tag.putLong("centre", centre.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ritualing = tag.getBoolean("ritualing");
        fighting = tag.getBoolean("fighting");
        ritualTicks = tag.getInt("ritual_ticks");
        ritualTotal = tag.getInt("ritual_total");
        damageTaken = tag.getFloat("damage_taken");
        centre = tag.contains("centre") ? BlockPos.of(tag.getLong("centre")) : null;
    }

    public static FinnEntity create(Level level) {
        return BossRegistries.FINN.get().create(level);
    }
}
