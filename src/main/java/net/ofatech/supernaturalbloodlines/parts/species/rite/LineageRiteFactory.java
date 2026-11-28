package net.ofatech.supernaturalbloodlines.parts.species.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Stub for Lineage Rite factory — not yet implemented; refuses with a clear message instead of silently cancelling. */
public record LineageRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "lineage");
    public static final MapCodec<LineageRiteFactory> CODEC = MapCodec.unit(LineageRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new Rite(base, params) {
            @Override
            protected boolean onStart(RiteParams params) {
                // TODO(plan-23): implement lineage rite logic; stub refuses until then.
                return cancel(); // silent: no player on the centre
            }
        };
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }
}