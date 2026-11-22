package net.ofatech.supernaturalbloodlines.parts.species.augustine;

import de.teamlapen.vampirism.api.EnumStrength;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * The two skills only an Augustine has (R203): Garlic Ward (S-AUG-1) and Blessed Skin (S-AUG-2). Both are rules of the skill, so a plain vampire who somehow
 * owns neither takes full damage, and an Augustine who has not bought one does not have it.
 */
public final class AugustineGifts {
    public static final String GARLIC_WARD = "S-AUG-1";
    public static final String BLESSED_SKIN = "S-AUG-2";
    private static final ResourceLocation GARLIC = ResourceLocation.fromNamespaceAndPath("vampirism", "garlic");
    private static final ResourceLocation HOLY_WATER = ResourceLocation.fromNamespaceAndPath("vampirism", "holy_water");

    private AugustineGifts() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, MobEffectEvent.Applicable.class, AugustineGifts::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingIncomingDamageEvent.class, AugustineGifts::onIncomingDamage);
    }

    /** Garlic's own effect is never applied, and neither is the blindness of standing in strong garlic. */
    private static void onEffectApplicable(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !AugustineRules.isAugustine(p) || !LadderQuery.owns(p, GARLIC_WARD)) {
            return;
        }
        var effect = e.getEffectInstance().getEffect();
        boolean garlic = effect.unwrapKey().map(k -> k.location().equals(GARLIC)).orElse(false);
        boolean blindness = effect.is(MobEffects.BLINDNESS);
        if (net.ofatech.supernaturalbloodlines.core.bloodline.AugustineBloodRules.wardDenies(garlic, blindness,
                blindness && VampirismAPI.vampirePlayer(p).isGettingGarlicDamage(p.level(), false) == EnumStrength.STRONG)) {
            e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Holy water hurts less (halved by default: the skill's {@code holy_reduction}). */
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && AugustineRules.isAugustine(p) && e.getSource().typeHolder().unwrapKey()
                .map(k -> k.location().equals(HOLY_WATER)).orElse(false) && LadderQuery.owns(p, BLESSED_SKIN)) {
            double reduction = Math.max(0.0, Math.min(1.0, LadderQuery.value(p, BLESSED_SKIN, "holy_reduction", 0.5)));
            e.setAmount((float) (e.getAmount() * (1.0 - reduction)));
        }
    }
}
