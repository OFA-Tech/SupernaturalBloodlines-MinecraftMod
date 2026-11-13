package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;

/**
 * Blood thirst of a hybrid type (R42): {@link BloodlineState#thirst()}, 0 to 20. Drinking a blood bottle raises it by
 * {@link #GAIN}, it falls by 1 every 1200 ticks, Weakness I at 0 and Strength I from 15. Only the thirst field
 * changes, so no species listener runs; the value is synced when it changes, about once a minute.
 * <p>
 * Deferred: a bite as a source of blood (Werewolves has no bite hook, see docs/parts/species.md).
 */
public final class BloodThirst {
    public static final int GAIN = 5;
    public static final int FALL_TICKS = 1200;
    public static final int STRONG_FROM = 15;

    private static final int EFFECT_STEP = 20;
    private static final int EFFECT_TICKS = 100;
    private static final int RENEW_BELOW = 40;

    private BloodThirst() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % EFFECT_STEP == 0) {
                refreshEffects(p);
                if (p.tickCount % FALL_TICKS == 0) {
                    decay(p);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(BloodThirst::onRightClick);
    }

    private static boolean applies(ServerPlayer p) {
        return HybridTraits.isHybrid(BloodlineAccess.get(p).species());
    }

    private static void setThirst(ServerPlayer p, int thirst) {
        BloodlineState s = BloodlineAccess.get(p);
        if (s.thirst() != thirst) {
            BloodlineAccess.set(p, s.toBuilder().thirst(thirst).build());
        }
    }

    /** One step of 1200 ticks. */
    public static void decay(ServerPlayer p) {
        if (applies(p)) {
            setThirst(p, BloodlineAccess.get(p).thirst() - 1);
        }
    }

    /** Short effects, renewed every second, so they fade by themselves when the species or the thirst changes. */
    public static void refreshEffects(ServerPlayer p) {
        if (!applies(p)) {
            return;
        }
        int thirst = BloodlineAccess.get(p).thirst();
        if (thirst == 0) {
            renew(p, MobEffects.WEAKNESS);
        } else if (thirst >= STRONG_FROM) {
            renew(p, MobEffects.DAMAGE_BOOST);
        }
    }

    /** Re-adding an effect sends a packet; only do it once the running one is about to end. */
    private static void renew(ServerPlayer p, Holder<MobEffect> effect) {
        MobEffectInstance running = p.getEffect(effect);
        if (running == null || running.getDuration() < RENEW_BELOW) {
            p.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, false, true));
        }
    }

    /**
     * A werewolf-faction player cannot start Vampirism's blood bottle ({@code use()} passes below vampire level 1),
     * so a thirsty hybrid's click on one is taken over, like during the transition.
     */
    static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        ItemStack stack = e.getItemStack();
        if (!(e.getEntity() instanceof ServerPlayer p) || !stack.is(TransitionHandler.BLOOD_DRINKS) || !applies(p)
                || BloodlineAccess.get(p).thirst() >= 20) {
            return;
        }
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        TransitionHandler.useUpBottle(p, stack);
        setThirst(p, BloodlineAccess.get(p).thirst() + GAIN);
    }
}
