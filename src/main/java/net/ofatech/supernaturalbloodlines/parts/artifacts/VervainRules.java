package net.ofatech.supernaturalbloodlines.parts.artifacts;

import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.ofatech.supernaturalbloodlines.core.Supernatural;

/**
 * Vervain. Whoever has the {@code vervain} effect (the tea) or vervain in the offhand is immune to compulsion and
 * burns the vampiric creatures that bite them or hit them barehanded; a vampiric player who eats vervain or the tea
 * is hurt too. Vampirism has no event for the bite itself, only {@link BloodDrinkEvent}, which is what we use.
 */
public final class VervainRules {
    static final float BURN_DAMAGE = 4f;
    static final int WEAKNESS_TICKS = 10 * 20;
    static final float EATING_DAMAGE = 6f;

    private VervainRules() {
    }

    /** Immune to compulsion (plan 8 asks this). */
    public static boolean protects(Player p) {
        return p.hasEffect(ArtifactItems.VERVAIN_EFFECT) || p.getOffhandItem().is(ArtifactItems.VERVAIN.get());
    }

    public static void register() {
        // the event is abstract: a listener goes on each of its subclasses (players, other creatures)
        NeoForge.EVENT_BUS.addListener((BloodDrinkEvent.PlayerDrinkBloodEvent e) -> onBlood(e));
        NeoForge.EVENT_BUS.addListener((BloodDrinkEvent.EntityDrinkBloodEvent e) -> onBlood(e));
        NeoForge.EVENT_BUS.addListener(VervainRules::onHit);
        NeoForge.EVENT_BUS.addListener(VervainRules::onEaten);
    }

    private static void onBlood(BloodDrinkEvent e) {
        if (e.getBloodSource().getEntity().orElse(null) instanceof Player victim && protects(victim)) {
            burn(e.getVampire().getRepresentingEntity());
            e.setAmount(0);
            e.setSaturationModifier(0f);
        }
    }

    /** After the damage landed, so a blocked or cancelled hit burns nobody. */
    private static void onHit(LivingDamageEvent.Post e) {
        DamageSource s = e.getSource();
        boolean melee = s.is(DamageTypes.PLAYER_ATTACK) || s.is(DamageTypes.MOB_ATTACK) || s.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        if (e.getEntity() instanceof Player victim && protects(victim) && melee
                && s.getEntity() instanceof LivingEntity attacker && s.getDirectEntity() == attacker
                && attacker.getMainHandItem().isEmpty() && Supernatural.isVampiric(attacker)) {
            burn(attacker);
        }
    }

    private static void onEaten(LivingEntityUseItemEvent.Finish e) {
        ItemStack eaten = e.getItem();
        if (e.getEntity() instanceof Player p && (eaten.is(ArtifactItems.VERVAIN.get()) || eaten.is(ArtifactItems.VERVAIN_TEA.get()))
                && Supernatural.isVampiric(p)) {
            p.hurt(p.damageSources().magic(), EATING_DAMAGE);
        }
    }

    private static void burn(LivingEntity vampire) {
        if (!vampire.level().isClientSide) {
            vampire.hurt(vampire.damageSources().magic(), BURN_DAMAGE);
            vampire.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
        }
    }
}
