package net.ofatech.supernaturalbloodlines.parts.species.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * Drops a bottle of Original's Blood, naming the Original on the circle. Enchanted's {@code entities} are consumed
 * ingredients, never a condition on a player, so "the target is an Original" is checked here; if not, the caster is
 * told, nothing is made and Enchanted gives the ingredients back.
 */
public record OriginalBloodRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "original_blood");
    public static final String NOT_ORIGINAL = "supernaturalbloodlines.refusal.not_original";
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
            if (!OriginalRules.isOriginal(BloodlineAccess.get(target).species())) {
                ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;
                caster.displayClientMessage(Component.translatable(NOT_ORIGINAL), true);
                return cancel();
            }
            ItemStack blood = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            blood.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), target.getUUID());
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, blood));
            return false;
        }
    }
}
