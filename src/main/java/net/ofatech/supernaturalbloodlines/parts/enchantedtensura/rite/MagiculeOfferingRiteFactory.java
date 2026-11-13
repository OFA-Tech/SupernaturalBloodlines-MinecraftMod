package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.rite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.List;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.PartItems;

/**
 * On start, takes {@code magicules} from the caster and drops {@code count} condensed magicule at the circle. If the
 * caster has too few, the rite fails: the caster is told, nothing is taken and Enchanted returns the items.
 */
public record MagiculeOfferingRiteFactory(double magicules, int count) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "magicule_offering");
    public static final String OFFERING_FAILED = "supernaturalbloodlines.enchanted_tensura.offering_failed";
    public static final MapCodec<MagiculeOfferingRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("magicules", 5000.0).forGetter(MagiculeOfferingRiteFactory::magicules),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(MagiculeOfferingRiteFactory::count)
    ).apply(i, MagiculeOfferingRiteFactory::new));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new MagiculeOfferingRite(base, params, magicules, count);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(PartItems.CONDENSED_MAGICULE.get(), count));
    }

    private static final class MagiculeOfferingRite extends Rite {
        private final double magicules;
        private final int count;

        MagiculeOfferingRite(BaseRiteParams base, RiteParams params, double magicules, int count) {
            super(base, params);
            this.magicules = magicules;
            this.count = count;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof LivingEntity caster)) {
                return cancel();
            }
            IExistence existence = TensuraStorages.getExistenceFrom(caster);
            if (existence.getMagicule() < magicules) {
                if (caster instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.translatable(OFFERING_FAILED));
                }
                return cancel();
            }
            existence.setMagicule(existence.getMagicule() - magicules);
            existence.markDirty();
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    new ItemStack(PartItems.CONDENSED_MAGICULE.get(), count)));
            return false;
        }
    }
}
