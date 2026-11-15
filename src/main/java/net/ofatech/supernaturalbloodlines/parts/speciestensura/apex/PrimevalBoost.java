package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.tensura.PrimevalAlpha;

/**
 * L-WOL-6: a True Alpha (not dormant) on {@code alpha_primeval} has the apex stat bonuses x1.5. The race itself carries the x1, so the extra
 * half is one permanent modifier per attribute, kept in step by {@link net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer#sync}
 * (it runs on a rank change, a faction change, login and every 200 ticks, which is when a Tensura evolution is noticed).
 */
public final class PrimevalBoost {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "primeval_true_alpha");
    /** The True Alpha's multiple of the apex bonuses (owner, 2026-11-14). */
    public static final double TRUE_ALPHA_FACTOR = 1.5;

    private PrimevalBoost() {}

    /** Idempotent: the extra bonus is on exactly while {@link PrimevalAlpha#active}. */
    public static void sync(ServerPlayer p) {
        boolean on = PrimevalAlpha.active(p);
        ApexSpec spec = ApexSpec.ALPHA_PRIMEVAL;
        apply(p, Attributes.MAX_HEALTH, on ? spec.health() * (TRUE_ALPHA_FACTOR - 1) : 0);
        apply(p, Attributes.ATTACK_DAMAGE, on ? spec.attack() * (TRUE_ALPHA_FACTOR - 1) : 0);
        apply(p, Attributes.MOVEMENT_SPEED, on ? spec.speed() * (TRUE_ALPHA_FACTOR - 1) : 0);
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
            a.addOrReplacePermanentModifier(new AttributeModifier(ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
