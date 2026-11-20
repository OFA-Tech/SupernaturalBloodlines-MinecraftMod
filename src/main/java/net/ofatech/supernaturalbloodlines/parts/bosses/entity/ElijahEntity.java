package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.ElijahRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;

/**
 * Elijah (R167, R170): calm. He fights only the one who hit him, until an oath is broken: then he is hostile for good ({@link BossIndex.Entry#hostile}).
 * Right-click offers the deal ({@link Deals}). A person in a suit: no weapon, no sun, no bite.
 */
public final class ElijahEntity extends NpcOriginalBoss {
    public ElijahEntity(EntityType<? extends ElijahEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    public BossId boss() {
        return BossId.ELIJAH;
    }

    @Override
    protected ItemStack reward() {
        return new ItemStack(BossItems.THE_ELIXIR.get());
    }

    /** Whether an oath was broken: he fights every player. */
    public boolean hostileForGood() {
        return level() instanceof ServerLevel level && BossIndex.get(level.getServer()).of(BossId.ELIJAH).hostile;
    }

    private long listenUntil;

    /** He stands still and looks at whoever he made the offer to, for up to {@code ticks}, unless he has something to fight. */
    public void listenFor(int ticks) {
        listenUntil = level().getGameTime() + ticks;
    }

    public void stopListening() {
        listenUntil = 0;
    }

    public boolean listening() {
        return getTarget() == null && level().getGameTime() < listenUntil;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.Goal() {
            {
                setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                return listening();
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::wouldAttack));
    }

    /** Whether he picks this one as a target on his own: only a player who can be hurt, and only after an oath was broken (R170). */
    public boolean wouldAttack(net.minecraft.world.entity.LivingEntity target) {
        return target instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && ElijahRules.fights(hostileForGood(), false);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || isDesiccated()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer p) {
            Deals.talk(this, p);
        }
        return InteractionResult.CONSUME;
    }

    public static ElijahEntity create(Level level) {
        return BossRegistries.ELIJAH.get().create(level);
    }
}