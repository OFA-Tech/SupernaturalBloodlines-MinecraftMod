package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.Supernatural;

/**
 * Stuns every supernatural (vampire, werewolf, species, mob or player) within {@link #RANGE} blocks for
 * {@link #STUN_TICKS}: Slowness V and Nausea. The user and everything not supernatural are left alone.
 */
public class HearingDeviceItem extends Item {
    public static final double RANGE = 5.0;
    public static final int STUN_TICKS = 100;
    public static final int COOLDOWN_TICKS = 1200;

    public HearingDeviceItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        // vanilla already refuses a use during the cooldown; the check is for callers that skip it
        if (user.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(RANGE),
                    t -> t != user && t.distanceToSqr(user) <= RANGE * RANGE && Supernatural.is(t) && mayStun(user, t))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 4), user);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, STUN_TICKS), user);
            }
            level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
        user.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** A player target is stunned only if {@code user} could hit them (PvP on) and they are not creative or spectating. */
    private static boolean mayStun(Player user, LivingEntity target) {
        return !(target instanceof Player tp) || user.canHarmPlayer(tp) && !tp.isCreative() && !tp.isSpectator();
    }
}
