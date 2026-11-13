package net.ofatech.supernaturalbloodlines.parts.species.rite;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * Rite of Ancestral Calling (R60): the player on the circle becomes a siphoner with the config chance
 * {@code siphoner_chance}, else a witch. The roll uses the level's random; the rules decide who may (no vampires, no
 * hunters, no second species), and a refusal is shown to the caster before anything changes.
 */
public record AncestralCallingRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ancestral_calling");
    public static final MapCodec<AncestralCallingRiteFactory> CODEC = MapCodec.unit(AncestralCallingRiteFactory::new);

    public static BloodlineEvent pick(RandomSource random, double siphonerChance) {
        return random.nextDouble() < siphonerChance ? BloodlineEvent.ANCESTRAL_CALLING_SIPHONER : BloodlineEvent.ANCESTRAL_CALLING_WITCH;
    }

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new AncestralCallingRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class AncestralCallingRite extends Rite {
        AncestralCallingRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            BloodlineEvent event = pick(level.random, SpeciesConfig.siphonerChance());
            BloodlineContext ctx = SpeciesContext.of(target, null);
            BloodlineState before = BloodlineAccess.get(target);
            Outcome preview = BloodlineRules.apply(before, event, ctx);
            if (preview.refused()) {
                ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;
                caster.displayClientMessage(Component.translatable(preview.refusalKey()), true);
                return cancel();
            }
            BloodlineAccess.applyEvent(target, event, ctx);
            return false;
        }
    }
}
