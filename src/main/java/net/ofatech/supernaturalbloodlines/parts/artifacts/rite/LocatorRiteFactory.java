package net.ofatech.supernaturalbloodlines.parts.artifacts.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.items.component.EDataComponents;
import net.favouriteless.enchanted.common.items.component.EntityRefData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.shared.species.LocatorTargets;

/**
 * The Rites of the Locator (R315, R317, R318, R325): a witch-type caster attunes a Vampire Compass to the owner of a taglock, or to whoever a sealed bottle
 * of blood was taken from. The target must be online, but the rite never checks their species.
 * Their identity is sealed on the compass and they are told once that somebody is looking for them.
 */
public record LocatorRiteFactory(boolean blood) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "locator");
    public static final String WONT_ANSWER = "supernaturalbloodlines.locator.wont_answer";
    public static final String TOLD = "supernaturalbloodlines.locator.told";
    public static final String ENDED = "supernaturalbloodlines.locator.ended";
    public static final MapCodec<LocatorRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("source", "taglock").forGetter(f -> f.blood() ? "blood" : "taglock")
    ).apply(i, source -> new LocatorRiteFactory(source.toLowerCase(Locale.ROOT).equals("blood"))));

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new LocatorRite(base, params, blood);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of(new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get()));
    }

    /** Whom a taglock or a bottle of blood names, if it names somebody. */
    static Optional<UUID> targetOf(List<ItemStack> items, boolean blood) {
        for (ItemStack stack : items) {
            if (blood) {
                Optional<UUID> id = LocatorTargets.bloodOf(stack);
                if (id.isPresent()) {
                    return id;
                }
            } else {
                EntityRefData ref = stack.get(EDataComponents.ENTITY_REF.get());
                if (ref != null) {
                    return Optional.of(ref.uuid());
                }
            }
        }
        return Optional.empty();
    }

    private static final class LocatorRite extends Rite {
        private final boolean blood;

        LocatorRite(BaseRiteParams base, RiteParams params, boolean blood) {
            super(base, params);
            this.blood = blood;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel(); // silent: the performer left
            }
            Optional<LocatorTargets.Settings> settings = LocatorTargets.settings();
            Optional<UUID> target = targetOf(params.consumedItems, blood);
            Optional<ItemStack> compass = params.consumedItems.stream().filter(s -> s.is(ArtifactItems.VAMPIRE_COMPASS.get())).findFirst();
            Optional<String> seal = target.flatMap(LocatorTargets::seal);
            boolean works = settings.isPresent() && settings.get().enabled() && BloodlineAccess.get(caster).species().witchType()
                    && compass.isPresent() && seal.isPresent();
            if (!works) {
                RiteTell.refuse(caster, WONT_ANSWER);
                return false; // not cancelled: the rest of the ingredients and the power are spent, whatever the reason
            }
            UUID owner = target.orElseThrow();
            // spec 4.1: an offline target is refused, so nobody is tracked without the notice (a test's fake player is only in a level)
            if (level.getServer().getPlayerList().getPlayer(owner) == null
                    && java.util.stream.StreamSupport.stream(level.getServer().getAllLevels().spliterator(), false)
                            .noneMatch(world -> world.getPlayerByUUID(owner) instanceof ServerPlayer)) {
                RiteTell.refuse(caster, "supernaturalbloodlines.gene.refusal.owner_away");
                return false;
            }
            long until = level.getGameTime() + settings.get().minutes() * 1200L;
            ItemStack attuned = compass.get().copy();
            attuned.set(ArtifactItems.LOCATOR_SEAL.get(), seal.get());
            attuned.set(ArtifactItems.LOCATOR_UNTIL.get(), until);
            attuned.set(ArtifactItems.LOCATOR_NEAR.get(), -1);
            attuned.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, attuned));
            if (settings.get().tellTarget()) {
                LocatorNotices.tell(level.getServer(), target.get(), until);
            }
            return false;
        }
    }
}
