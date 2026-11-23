package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * A snapped neck (Neck Snap, R214) on a creature that is not a player (a player uses {@code Downed}): it lies as if dead for the duration. Nothing of this
 * is saved on the creature but the effect itself: its speed is held at 0 by the effect's own (transient) attribute modifiers, it loses and cannot take a
 * target, and nothing it strikes with counts. So there is no {@code NoAI} flag to be left behind when the world is rolled back or the effect is lost, and a
 * creature that already had no AI keeps exactly what it had. Nothing hurts it but what the Original rules let past (white oak, a heart-rip, the void, /kill).
 * When the effect ends (it runs out, or is removed) the creature cannot be snapped again for {@code neck_snap_grace_seconds}. It never lands on a player.
 */
public final class SnappedEffect extends MobEffect {
    public static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "effect.snapped");

    public SnappedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A5A6E);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.FLYING_SPEED, SPEED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Added e) -> {
            if (isSnap(e.getEffectInstance()) && e.getEntity() instanceof Mob m && !m.level().isClientSide) {
                m.setTarget(null);
                m.getNavigation().stop();
            }
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Expired e) -> {
            if (e.getEffectInstance() != null && isSnap(e.getEffectInstance())) {
                startGrace(e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Remove e) -> {
            if (e.getEffectInstance() != null && isSnap(e.getEffectInstance())) {
                startGrace(e.getEntity());
            }
        });
        // it takes no target while it lies there
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (LivingChangeTargetEvent e) -> {
            if (e.getEntity() instanceof Mob m && isSnapped(m) && e.getNewAboutToBeSetTarget() != null) {
                e.setCanceled(true);
            }
        });
        // LOWEST, like the downed player: nothing hurts it but what the Original rules let past; and nothing it strikes with lands
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingIncomingDamageEvent e) -> {
            if (e.getEntity() instanceof Mob m && isSnapped(m) && !OriginalRules.bypassesProtection(e.getSource())) {
                e.setCanceled(true);
            } else if (e.getSource().getEntity() instanceof Mob striker && isSnapped(striker)) {
                e.setCanceled(true);
            }
        });
    }

    private static boolean isSnap(net.minecraft.world.effect.MobEffectInstance i) {
        return i.is(SpeciesRegistries.SNAPPED);
    }

    public static boolean isSnapped(LivingEntity e) {
        return e.hasEffect(SpeciesRegistries.SNAPPED);
    }

    /** The snap has ended for this creature or player: it cannot be snapped again for the grace time. */
    public static void startGrace(LivingEntity e) {
        if (!e.level().isClientSide) {
            e.setData(SpeciesRegistries.SNAP_GRACE_UNTIL, e.level().getGameTime() + SpeciesConfig.neckSnapGraceSeconds() * 20L);
        }
    }

    /** Whether the grace after a snap is still running. */
    public static boolean inGrace(LivingEntity e) {
        return e.getData(SpeciesRegistries.SNAP_GRACE_UNTIL) > e.level().getGameTime();
    }
}
