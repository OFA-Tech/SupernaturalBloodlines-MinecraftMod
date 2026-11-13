package net.ofatech.supernaturalbloodlines.parts.artifacts.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.AshDagger;

/**
 * Makes The Cure, but only with a desiccated Original in the circle (any player within {@link #RADIUS} blocks of its
 * centre). Otherwise the caster is told and Enchanted gives every ingredient back.
 */
public record RequiresDesiccatedRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "requires_desiccated");
    public static final String NEEDS_DESICCATED = "supernaturalbloodlines.rite.needs_desiccated";
    public static final MapCodec<RequiresDesiccatedRiteFactory> CODEC = MapCodec.unit(RequiresDesiccatedRiteFactory::new);
    /** The medium circle is about this wide. */
    public static final double RADIUS = 6.0;

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new CureRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(ArtifactItems.THE_CURE.get()));
    }

    private static final class CureRite extends Rite {
        CureRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            boolean found = !level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(RADIUS), CureRite::desiccatedOriginal).isEmpty();
            if (!found) {
                if (findEntity(params.caster) instanceof ServerPlayer caster) {
                    caster.displayClientMessage(Component.translatable(NEEDS_DESICCATED), true);
                }
                return cancel();
            }
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, new ItemStack(ArtifactItems.THE_CURE.get())));
            return false;
        }

        private static boolean desiccatedOriginal(ServerPlayer p) {
            Species s = BloodlineAccess.get(p).species();
            return p.isAlive() && AshDagger.isDesiccated(p) && (s == Species.ORIGINAL || s == Species.ORIGINAL_HYBRID);
        }
    }
}
