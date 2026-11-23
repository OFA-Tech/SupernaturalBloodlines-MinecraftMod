package net.ofatech.supernaturalbloodlines.parts.species.rite;


import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.HybridTraits;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * Drops a bottle of Hybrid's Blood from the hybrid (HYBRID, ORIGINAL_HYBRID or TRIBRID) on the circle. Like
 * {@link OriginalBloodRiteFactory}, the species is checked here because Enchanted's JSON cannot; if it is wrong nothing is
 * made and Enchanted gives the ingredients back. Someone else's blood needs their consent (they sneak on the centre): see
 * {@link RiteTargets#bloodAnswer}.
 */
public record HybridBloodRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hybrid_blood");
    public static final String NOT_HYBRID = "supernaturalbloodlines.refusal.not_hybrid";
    /** What the one on the centre is told when they agreed and are not a hybrid. */
    public static final String NO_HYBRID_BLOOD = "supernaturalbloodlines.refusal.no_hybrid_blood_in_you";
    public static final MapCodec<HybridBloodRiteFactory> CODEC = MapCodec.unit(HybridBloodRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new HybridBloodRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(SpeciesRegistries.HYBRID_BLOOD.get()));
    }

    private static final class HybridBloodRite extends Rite {
        HybridBloodRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = RiteTargets.performer(findEntity(params.caster), params.caster, target).orElse(null);
            if (caster == null) {
                return cancel(); // the performer left: only the performer may stand for themselves, so nobody is asked and nobody is changed
            }
            if (!RiteTargets.askForBlood(caster, target, HybridTraits.isHybrid(BloodlineAccess.get(target).species()), NOT_HYBRID, NO_HYBRID_BLOOD)) {
                return cancel();
            }
            ItemStack blood = new ItemStack(SpeciesRegistries.HYBRID_BLOOD.get());
            BloodStamp.write(blood, new BloodSource(target.getUUID(), target.getGameProfile().getName(), false, BloodKind.of(BloodlineAccess.get(target).species())));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, blood));
            return false;
        }
    }
}
