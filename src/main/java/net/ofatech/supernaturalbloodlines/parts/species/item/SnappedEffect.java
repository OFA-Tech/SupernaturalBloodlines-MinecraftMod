package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * A snapped neck (Neck Snap, R214) on a creature that is not a player (a player uses {@code Downed}): it lies as if dead for the duration. {@code Mob.setNoAi}
 * for as long as it lasts, restored when it ends (a creature that already had no AI keeps having none); nothing hurts it but what the Original rules let past
 * (white oak, a heart-rip, the void, /kill). It never lands on a player.
 */
public final class SnappedEffect extends MobEffect {
    public SnappedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A5A6E);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Added e) -> {
            if (isSnap(e.getEffectInstance()) && e.getEntity() instanceof Mob m && !m.level().isClientSide) {
                if (!(e.getOldEffectInstance() != null && isSnap(e.getOldEffectInstance()))) {
                    m.setData(SpeciesRegistries.SNAPPED_HAD_NO_AI, m.isNoAi());
                }
                m.setNoAi(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Expired e) -> {
            if (e.getEffectInstance() != null && isSnap(e.getEffectInstance())) {
                wake(e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Remove e) -> {
            if (e.getEffectInstance() != null && isSnap(e.getEffectInstance())) {
                wake(e.getEntity());
            }
        });
        // LOWEST, like the downed player: after everything that might add to the damage, before it lands
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingIncomingDamageEvent e) -> {
            if (e.getEntity() instanceof Mob m && isSnapped(m) && !OriginalRules.bypassesProtection(e.getSource())) {
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

    private static void wake(LivingEntity e) {
        if (e instanceof Mob m && !m.level().isClientSide) {
            m.setNoAi(m.hasData(SpeciesRegistries.SNAPPED_HAD_NO_AI) && m.getData(SpeciesRegistries.SNAPPED_HAD_NO_AI));
            m.removeData(SpeciesRegistries.SNAPPED_HAD_NO_AI);
        }
    }
}
