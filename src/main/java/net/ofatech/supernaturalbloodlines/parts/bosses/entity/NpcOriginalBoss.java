package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;

/**
 * One of the six Originals who are not players. It carries {@code ORIGINAL_ROOT}, so the line rules, the Imprisoned Original rite and Revelation
 * treat it like a player Original. Any defeat except white oak on an open one only desiccates it (it returns after {@code original_return_days});
 * a white oak death is for good and kills the line ({@code NpcOriginalEvents}).
 */
public abstract class NpcOriginalBoss extends BossNpc {
    private static final EntityDataAccessor<Boolean> DESICCATED = SynchedEntityData.defineId(NpcOriginalBoss.class, EntityDataSerializers.BOOLEAN);

    protected NpcOriginalBoss(EntityType<? extends NpcOriginalBoss> type, Level level) {
        super(type, level);
    }

    public NpcOriginal original() {
        return boss().npcOriginal().orElseThrow();
    }

    /** What this boss pays the first time it is brought down (or killed, by {@code boss_reward_on}). */
    protected abstract ItemStack reward();

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DESICCATED, false);
    }

    private boolean pendingDesiccation;

    /** The blow that is landing now leaves 1 health: bring him down after it, so the damage cannot finish him. */
    public void desiccateAfterHit() {
        pendingDesiccation = true;
    }

    public boolean takePendingDesiccation() {
        boolean was = pendingDesiccation;
        pendingDesiccation = false;
        return was;
    }

    public boolean isDesiccated() {
        return entityData.get(DESICCATED);
    }

    @Override
    protected boolean isOpenNow() {
        return isDesiccated();
    }

    /** Only how it looks (the flag the renderer reads), for the dev screenshots: nothing else of the desiccation happens. */
    public void lookDesiccated(boolean on) {
        entityData.set(DESICCATED, on);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("desiccated", isDesiccated());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DESICCATED, tag.getBoolean("desiccated"));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            setData(SpeciesRegistries.ORIGINAL_ROOT, original().id());
            setData(SpeciesRegistries.NPC_OPEN, isDesiccated());
            setNoAi(isDesiccated());
            if (isDesiccated() && level() instanceof ServerLevel level) {
                // after a restart the warning latch is empty: the line hears again that their Original lies open
                LineWarning.opened(level.getServer(), original().uuid(), true);
            }
        }
    }

    @Override
    protected void serverPeriod() {
        checkReturn();
    }

    /**
     * Gets up when the return day has come (days of game time, {@link BossClock}). The entity does it itself (every half second), so one that was
     * asleep in an unloaded chunk returns when it loads.
     */
    public void checkReturn() {
        if (isDesiccated() && level() instanceof ServerLevel level) {
            var entry = BossIndex.get(level.getServer()).of(boss());
            if (DesiccationRules.due(BossClock.today(level), entry.returnDay)) {
                recover();
            }
        }
    }

    /** Brought down: health 1, no AI, open to white oak, the line warned, the blood dropped (at most once per window), the reward paid (once). */
    public void desiccate() {
        if (isDesiccated() || !(level() instanceof ServerLevel level)) {
            return;
        }
        BossIndex index = BossIndex.get(level.getServer());
        long today = BossClock.today(level);
        entityData.set(DESICCATED, true);
        setData(SpeciesRegistries.NPC_OPEN, true);
        setTarget(null);
        setNoAi(true);
        setHealth(1.0F);
        var entry = index.of(boss());
        entry.returnDay = DesiccationRules.returnDay(today, BossesConfig.originalReturnDays());
        index.apply(boss(), BossRules.Event.DESICCATED);
        if (today >= entry.bloodDay) {
            entry.bloodDay = entry.returnDay;
            spawnAtLocation(SiresBloodItem.of(original().uuid(), capitalised(original().id())));
        }
        boolean paid = pay(RewardRules.Defeat.DESICCATED);
        index.setDirty();
        LineWarning.opened(level.getServer(), original().uuid(), true);
        if (paid) {
            BossIndex.flush(level.getServer());
        }
    }

    /** Gets up again: full health, the AI back, closed to white oak. */
    public void recover() {
        if (!isDesiccated() || !(level() instanceof ServerLevel level)) {
            return;
        }
        entityData.set(DESICCATED, false);
        setData(SpeciesRegistries.NPC_OPEN, false);
        setNoAi(false);
        setHealth(getMaxHealth());
        BossIndex.get(level.getServer()).apply(boss(), BossRules.Event.RETURNED);
        LineWarning.risen(level.getServer(), original().uuid());
    }

    /** Pays the reward if the rules say so, marks it paid and writes the index to disk at once (a crash must not pay it twice). Whether it paid. */
    public boolean pay(RewardRules.Defeat defeat) {
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        BossIndex index = BossIndex.get(level.getServer());
        var entry = index.of(boss());
        if (!RewardRules.pays(BossesConfig.rewardOn(), defeat, entry.rewarded)) {
            return false;
        }
        entry.rewarded = true;
        index.setDirty();
        level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.5, getZ(), reward()));
        BossIndex.flush(level.getServer());
        return true;
    }

    public BossState stateNow() {
        return level().getServer() == null ? BossState.UNSEEN : BossIndex.get(level().getServer()).state(boss());
    }

    private static String capitalised(String id) {
        return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }
}
