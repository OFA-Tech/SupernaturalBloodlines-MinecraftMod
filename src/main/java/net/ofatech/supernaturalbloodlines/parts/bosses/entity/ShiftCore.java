package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.MorphRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;

/** The shift the two shifting bosses share (plan 17c and 17f): standing still while it runs, counting its ticks, the swap and the end, and the beast's bonuses. */
final class ShiftCore {
    private ShiftCore() {
    }

    /**
     * The wolf's clips by the name {@code KlausRules.animation} gives, built once per boss (a {@code RawAnimation} is immutable and has equals, so the controller
     * sees the same one every frame and allocates nothing): the death pose held, the howl clip (the model's {@code howl} or {@code roar}) once, the arrival
     * {@code shift_in} once, and the three loops.
     */
    static java.util.Map<String, software.bernie.geckolib.animation.RawAnimation> clips(String model, String howlClip) {
        String p = "animation." + model + ".";
        return java.util.Map.of(
                "death", software.bernie.geckolib.animation.RawAnimation.begin().thenPlayAndHold(p + "death"),
                "howl", software.bernie.geckolib.animation.RawAnimation.begin().thenPlay(p + howlClip),
                "shift_in", software.bernie.geckolib.animation.RawAnimation.begin().thenPlay(p + "shift_in"),
                "idle", software.bernie.geckolib.animation.RawAnimation.begin().thenLoop(p + "idle"),
                "walk", software.bernie.geckolib.animation.RawAnimation.begin().thenLoop(p + "walk"),
                "run", software.bernie.geckolib.animation.RawAnimation.begin().thenLoop(p + "run"));
    }

    /** Before the entity's own tick: he stands where he is while he shifts, so the tick cannot move him. */
    static void hold(Mob boss, ShiftBody body) {
        if (!boss.level().isClientSide && body.form() == BossForm.SHIFTING) {
            boss.getNavigation().stop();
            boss.setDeltaMovement(0.0, Math.min(0.0, boss.getDeltaMovement().y), 0.0);
        }
    }

    /** One more tick of the shift (server): the counter, the swap on its tick, and the end of the shift. */
    static void advance(ShiftBody body) {
        // a dying body is not in the middle of anything: no count, no swap, no cue (the death clip wins)
        if (body instanceof LivingEntity dying && dying.isDeadOrDying()) {
            return;
        }
        BossForm form = body.form();
        int before = body.shiftTicks();
        // the cues of the tick that is about to be counted (growl at 0 ... landing at 32); none for one who is dying: the death clip wins
        if (body instanceof LivingEntity living && body.level() instanceof ServerLevel level) {
            MorphCues.play(level, living, before, MorphRules.cues(form, before));
        }
        int counted = ShiftRules.nextTicks(form, before);
        if (counted != before) {
            body.setShiftTicks(counted);
            if (counted == ShiftRules.SWAP_TICK && form == BossForm.SHIFTING && body.level() instanceof ServerLevel server) {
                body.onSwap(server);
            }
        }
        BossForm after = ShiftRules.afterTick(form, body.shiftTicks());
        if (after != form) {
            body.setForm(after);
        }
    }

    /** The beast bites harder and runs faster: a transient modifier each, never saved. */
    static void beastBonuses(LivingEntity boss, ResourceLocation damageId, ResourceLocation speedId, boolean on) {
        bonus(boss, Attributes.ATTACK_DAMAGE, damageId, ShiftRules.BEAST_DAMAGE_BONUS, on);
        bonus(boss, Attributes.MOVEMENT_SPEED, speedId, ShiftRules.BEAST_SPEED_BONUS, on);
    }

    private static void bonus(LivingEntity boss, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean on) {
        AttributeInstance a = boss.getAttribute(attribute);
        if (a == null) {
            return;
        }
        a.removeModifier(id);
        if (on) {
            a.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
