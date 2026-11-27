package net.ofatech.supernaturalbloodlines.parts.artifacts.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.Codec;
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
import net.ofatech.supernaturalbloodlines.core.group.coven.RingRules;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory.Faction;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;
import net.ofatech.supernaturalbloodlines.shared.supernatural.CircleQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

/**
 * A circle of linked witches forges a ring, already bound to whoever stands on the circle's centre (else the performer) (R262, R263). It takes no ring: the rite
 * file's items are the materials, and the ring is dropped bound exactly as {@link BindItemRiteFactory} does. It reads the circle through
 * {@link CircleQuery} (the artifacts part does not know the species part); with the species part off the seam answers "no circle" and the rite refuses.
 * What is said of the one on the centre goes through {@link RiteTargets#refuseAbout}, so the performer learns nothing of them.
 */
public record CovenForgeRiteFactory(Item output, Faction faction, int minCircle, boolean needsPack, String ring) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "coven_forge");
    public static final String CIRCLE = "supernaturalbloodlines.rite.coven_forge.circle";
    public static final String OFF = "supernaturalbloodlines.rite.coven_forge.off";
    public static final String NOT_FOR_YOU = "supernaturalbloodlines.refusal.coven_forge";
    public static final MapCodec<CovenForgeRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("output").forGetter(CovenForgeRiteFactory::output),
            FactionRiteFactory.enumCodec(Faction.class).optionalFieldOf("faction", Faction.NONE).forGetter(CovenForgeRiteFactory::faction),
            Codec.INT.optionalFieldOf("min_circle", 3).forGetter(CovenForgeRiteFactory::minCircle),
            Codec.BOOL.optionalFieldOf("needs_pack", false).forGetter(CovenForgeRiteFactory::needsPack),
            Codec.STRING.fieldOf("ring").forGetter(CovenForgeRiteFactory::ring)
    ).apply(i, CovenForgeRiteFactory::new));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new CovenForgeRite(base, params, this);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(output));
    }

    private static final class CovenForgeRite extends Rite {
        private final CovenForgeRiteFactory factory;

        CovenForgeRite(BaseRiteParams base, RiteParams params, CovenForgeRiteFactory factory) {
            super(base, params);
            this.factory = factory;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel(); // silent: no player on the centre
            }
            ServerPlayer receiver = found.get();
            ServerPlayer caster = RiteTargets.performer(findEntity(params.caster), params.caster, receiver).orElse(null);
            if (caster == null) {
                return cancel(); // silent: the performer left
            }
            if (!CircleQuery.switchOn(factory.ring)) {
                RiteTell.refuse(caster, OFF);
                return cancel();
            }
            if (!RingRules.circleOk(CircleQuery.staged(), factory.minCircle)) {
                RiteTell.refuse(caster, CIRCLE);
                return cancel();
            }
            boolean factionOk = factory.faction.accepts(FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(receiver)));
            boolean receiverOk = factionOk && (!factory.needsPack || CircleQuery.inPack(level, receiver.getUUID()));
            if (receiverOk && factory.faction == Faction.VAMPIRE) {
                receiverOk = RingRules.sunReceiverOk(true, CircleQuery.dayWalker(level, receiver.getUUID()), CircleQuery.switchOn("sun_ring_day_walker_only"));
            }
            if (!receiverOk) {
                RiteTargets.refuseAbout(caster, receiver, NOT_FOR_YOU);
                return cancel();
            }
            ItemStack ring = new ItemStack(factory.output);
            ring.set(ArtifactItems.BOUND_TO.get(), receiver.getUUID());
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, ring));
            if (BuiltInRegistries.ITEM.getKey(factory.output).getPath().equals("sun_ring")) {
                TaskSignals.fire(receiver, "sun_ring_bound", 1);
            }
            return false;
        }
    }
}