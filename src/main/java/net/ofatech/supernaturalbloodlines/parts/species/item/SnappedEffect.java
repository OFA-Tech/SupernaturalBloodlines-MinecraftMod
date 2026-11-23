package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * A snapped neck (Neck Snap, R214) on a creature that is not a player (a player uses {@code Downed}): it lies as if dead for the duration. The hold is a
 * <b>transient</b> speed modifier (-100%, never written to the creature's save: a permanent one, which is what an effect's own attribute templates become,
 * would outlive a rollback or a lost effect), added when the effect is, put back when the creature joins a level again (a reload) and every tick the effect
 * runs, and taken away when it expires or is removed. Each tick it also zeroes the creature's horizontal and upward movement and stops its navigation, it
 * loses and cannot take a target, it cannot teleport (an Enderman), and nothing it strikes with lands. No {@code NoAI} flag is written either, and a creature
 * that already had no AI keeps exactly what it had. Nothing hurts it but what the Original rules let past (white oak, a heart-rip, the void, /kill). When
 * the effect ends the creature cannot be snapped again for {@code neck_snap_grace_seconds}. It never lands on a player, and no cure (milk) ends it.
 */
public final class SnappedEffect extends MobEffect {
    public static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "effect.snapped");
    private static final List<Holder<Attribute>> HELD = List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED);

    public SnappedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A5A6E);
    }

    @Override
    public void fillEffectCures(java.util.Set<EffectCure> cures, net.minecraft.world.effect.MobEffectInstance instance) {
        // milk and the like do not end a snapped neck
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            hold(entity);
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0, Math.min(v.y, 0.0), 0.0);
            if (entity instanceof Mob m) {
                m.getNavigation().stop();
            }
        }
        return true;
    }

    /** Puts the transient hold on (idempotent): speed and flying speed to nothing. */
    public static void hold(LivingEntity e) {
        for (Holder<Attribute> attribute : HELD) {
            AttributeInstance instance = e.getAttributes().getInstance(attribute);
            if (instance != null && instance.getModifier(SPEED_ID) == null) {
                instance.addTransientModifier(new AttributeModifier(SPEED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    private static void release(LivingEntity e) {
        for (Holder<Attribute> attribute : HELD) {
            AttributeInstance instance = e.getAttributes().getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(SPEED_ID);
            }
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Added e) -> {
            if (isSnap(e.getEffectInstance()) && !e.getEntity().level().isClientSide) {
                hold(e.getEntity());
                if (e.getEntity() instanceof Mob m) {
                    m.setTarget(null);
                    m.getNavigation().stop();
                }
            }
        });
        // a creature that is loaded again keeps the effect (saved) but not the transient hold
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getEntity() instanceof LivingEntity living && !e.getLevel().isClientSide && isSnapped(living)) {
                hold(living);
            }
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Expired e) -> {
            if (e.getEffectInstance() != null && isSnap(e.getEffectInstance())) {
                release(e.getEntity());
                startGrace(e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Remove e) -> {
            if (e.getEffectInstance() != null && isSnap(e.getEffectInstance())) {
                release(e.getEntity());
                startGrace(e.getEntity());
            }
        });
        // it takes no target while it lies there, and does not blink away
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (LivingChangeTargetEvent e) -> {
            if (e.getEntity() instanceof Mob m && isSnapped(m) && e.getNewAboutToBeSetTarget() != null) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (EntityTeleportEvent.EnderEntity e) -> {
            if (isSnapped(e.getEntityLiving())) {
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
