package net.ofatech.supernaturalbloodlines.parts.species.augustine;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.AugustineBloodRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/**
 * What being an Augustine means in play (R200, R203): a little stronger than a vampire (+10% attack, +2 health, both configurable) and a very different
 * eater: triple the blood from a vampire, a third from anyone else. Garlic and holy water are the skills' (see {@link AugustineGifts}).
 */
public final class AugustineRules {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "augustine");

    private AugustineRules() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
        // the modifiers are saved with the player; these only repair gaps (and follow a changed config)
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncIfPlayer(e.getEntity()));
        // LOWEST: after the bite's other rules (BloodEffects, the vervain refusal, the ash dagger's starving), so a bite something emptied stays 0
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AugustineRules::onDrinkBlood);
        AugustineGifts.register();
    }

    public static boolean isAugustine(net.minecraft.world.entity.player.Player p) {
        return BloodlineAccess.get(p).species() == Species.AUGUSTINE;
    }

    /** Idempotent: makes the player's attributes match their species and the config. */
    public static void sync(ServerPlayer p) {
        boolean on = isAugustine(p);
        apply(p, Attributes.MAX_HEALTH, SpeciesConfig.augustineExtraHealth(), AttributeModifier.Operation.ADD_VALUE, on);
        apply(p, Attributes.ATTACK_DAMAGE, SpeciesConfig.augustineAttackBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
    }

    private static void syncIfPlayer(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, boolean on) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        AttributeModifier now = a.getModifier(MODIFIER_ID);
        if (on && amount > 0) {
            if (now == null || now.amount() != amount) {
                a.removeModifier(MODIFIER_ID);
                a.addPermanentModifier(new AttributeModifier(MODIFIER_ID, amount, operation));
            }
        } else if (now != null) {
            a.removeModifier(MODIFIER_ID);
        }
    }

    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        if (!(e.getVampire().getRepresentingEntity() instanceof ServerPlayer p) || !isAugustine(p)) {
            return;
        }
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        boolean vampire = source != null && VampirismAPI.factionRegistry().getFaction(source) == VReference.VAMPIRE_FACTION; // a hybrid player counts (R2)
        e.setAmount(AugustineBloodRules.scale(e.getAmount(), vampire, SpeciesConfig.augustineBloodFromVampire(), SpeciesConfig.augustineBloodFromOther()));
    }
}
