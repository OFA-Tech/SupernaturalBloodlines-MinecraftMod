package net.ofatech.supernaturalbloodlines.parts.artifacts.rite;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory.Faction;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * Takes the {@code input} item among the rite's consumed items and drops {@code output} bound to the player on the
 * circle (else the caster). {@code faction} is the faction that player has to be in (default: any). When the rite
 * cannot work the caster is told and Enchanted gives every ingredient back. An input that is already bound is refused,
 * so a stolen ring cannot be turned into one's own.
 */
public record BindItemRiteFactory(Item input, Item output, Faction faction) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bind_item");
    public static final String NO_ITEM = "supernaturalbloodlines.rite.bind_no_item";
    public static final String ALREADY_BOUND = "supernaturalbloodlines.rite.bind_already_bound";
    public static final MapCodec<BindItemRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input").forGetter(BindItemRiteFactory::input),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("output").forGetter(BindItemRiteFactory::output),
            FactionRiteFactory.enumCodec(Faction.class).optionalFieldOf("faction", Faction.NONE).forGetter(BindItemRiteFactory::faction)
    ).apply(i, BindItemRiteFactory::new));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new BindItemRite(base, params, this);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(output));
    }

    private static final class BindItemRite extends Rite {
        private final BindItemRiteFactory factory;

        BindItemRite(BaseRiteParams base, RiteParams params, BindItemRiteFactory factory) {
            super(base, params);
            this.factory = factory;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;

            Optional<ItemStack> source = params.consumedItems.stream().filter(s -> s.is(factory.input)).findFirst();
            if (source.isEmpty()) {
                caster.displayClientMessage(Component.translatable(NO_ITEM), true);
                return cancel();
            }
            if (source.get().get(ArtifactItems.BOUND_TO.get()) != null) {
                caster.displayClientMessage(Component.translatable(ALREADY_BOUND), true);
                return cancel();
            }
            if (!factory.faction.accepts(FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(target)))) {
                caster.displayClientMessage(Component.translatable(FactionRiteFactory.WRONG_FACTION), true);
                return cancel();
            }
            ItemStack bound = new ItemStack(factory.output);
            bound.set(ArtifactItems.BOUND_TO.get(), target.getUUID());
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, bound));
            return false;
        }
    }
}
