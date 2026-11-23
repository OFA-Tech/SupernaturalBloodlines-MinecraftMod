package net.ofatech.supernaturalbloodlines.parts.species.rite;


import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
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
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * Drops a bottle of Original's Blood, naming the Original on the circle. Enchanted's {@code entities} are consumed
 * ingredients, never a condition on a player, so "the target is an Original" is checked here; if not, nothing is made and Enchanted
 * gives the ingredients back. Someone else's blood needs their consent (they sneak on the centre), or the rite would tell the caster who is an
 * Original: see {@link RiteTargets#bloodAnswer}.
 */
public record OriginalBloodRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "original_blood");
    public static final String NOT_ORIGINAL = "supernaturalbloodlines.refusal.not_original";
    /** What the one on the centre is told when they agreed and are not an Original. */
    public static final String NO_ORIGINAL_BLOOD = "supernaturalbloodlines.refusal.no_original_blood_in_you";
    public static final MapCodec<OriginalBloodRiteFactory> CODEC = MapCodec.unit(OriginalBloodRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new OriginalBloodRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get()));
    }

    private static final class OriginalBloodRite extends Rite {
        OriginalBloodRite(BaseRiteParams base, RiteParams params) {
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
            if (!RiteTargets.askForBlood(caster, target, OriginalRules.isOriginal(BloodlineAccess.get(target).species()), NOT_ORIGINAL, NO_ORIGINAL_BLOOD)) {
                return cancel();
            }
            int bottles = (int) LadderQuery.value(target, "S-ORI-2", "bottles", 1);
            for (int i = 0; i < bottles; i++) {
                ItemStack blood = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
                BloodStamp.write(blood, new BloodSource(target.getUUID(), target.getGameProfile().getName(), false, BloodKind.of(BloodlineAccess.get(target).species())));
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, blood));
            }
            return false;
        }
    }
}
