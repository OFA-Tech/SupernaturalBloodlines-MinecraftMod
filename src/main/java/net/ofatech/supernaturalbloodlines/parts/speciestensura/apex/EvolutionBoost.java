package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.LayerSpec;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;

/**
 * R100 (plan 10): a species speeds up EP gain, and nothing else. Tensura scales every EP a kill gives by the player's
 * {@code AURA_GAIN} and {@code MAGICULE_GAIN} attributes ({@code DeathHandler.gainEPEntity}; its Gourmand skill uses them too),
 * so the bonus is one permanent modifier on both, kept in step by {@link SpeciesLayer#sync}. No event or mixin, and the
 * evolution progress and its requirements are Tensura's own.
 */
public final class EvolutionBoost {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ep_boost");

    private EvolutionBoost() {}

    /** The EP multiple: the configured one for a species with a layer, 1 for anyone else. */
    public static double factor(boolean hasLayer, double multiplier) {
        return hasLayer ? multiplier : 1.0;
    }

    /** What the player is credited for {@code gain} EP: x1.25 (the config) with a species, as is without. The only boost entry point. */
    public static double boostedEp(Player p, double gain) {
        boolean hasLayer = BloodlineAccess.get(p).species() != Species.NONE && !SpeciesLayer.spec(p).equals(LayerSpec.NONE);
        return gain * factor(hasLayer, SpeciesTensuraConfig.evolutionMultiplier());
    }

    /** Idempotent: makes the player's EP gain attributes carry the bonus {@link #boostedEp} promises. */
    public static void sync(ServerPlayer p) {
        double bonus = boostedEp(p, 1.0) - 1.0;
        apply(p, TensuraAttributes.AURA_GAIN, bonus);
        apply(p, TensuraAttributes.MAGICULE_GAIN, bonus);
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attribute, double bonus) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        if (bonus <= 0) {
            a.removeModifier(ID);
            return;
        }
        AttributeModifier current = a.getModifier(ID);
        if (current == null || Math.abs(current.amount() - bonus) > 1e-12) {
            a.addOrReplacePermanentModifier(new AttributeModifier(ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
