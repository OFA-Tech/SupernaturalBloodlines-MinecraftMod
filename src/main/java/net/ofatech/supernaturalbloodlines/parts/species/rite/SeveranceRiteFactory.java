package net.ofatech.supernaturalbloodlines.parts.species.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.witch.Siphoning;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * Rite of Severance (R101b): a witch-type performer (witch, siphoner, heretic or tribrid) breaks the whole sire bond of the sired player
 * on the circle. Full moon night only (the phase-0 day, inside Werewolves' night window; not at noon). The ingredients (the JSON) are Sire's Blood and a rare reagent; the blood has to be the blood of
 * the target's own sire, which the pure rules check ({@link BloodlineEvent#RITE_OF_SEVERANCE}). When anything is wrong the caster is told
 * and Enchanted gives the ingredients back.
 */
public record SeveranceRiteFactory(boolean requiresFullMoon) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "severance");
    public static final String NOT_WITCH = "supernaturalbloodlines.refusal.severance_not_witch";
    public static final String NO_BLOOD = "supernaturalbloodlines.refusal.severance_no_blood";
    public static final String DONE = "supernaturalbloodlines.rite.severance_done";
    public static final MapCodec<SeveranceRiteFactory> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            FactionRiteFactory.fullMoonByDefault().forGetter(SeveranceRiteFactory::requiresFullMoon)
    ).apply(i, SeveranceRiteFactory::new));

    /** WITCH, SIPHONER, HERETIC and TRIBRID: the species that work magic (witch rites) or take it (siphoning). */
    public static boolean isWitchType(Species s) {
        return WitchAltarBonus.hasBonus(s) || Siphoning.canSiphon(s);
    }

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new SeveranceRite(base, params, requiresFullMoon);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class SeveranceRite extends Rite {
        private final boolean requiresFullMoon;

        SeveranceRite(BaseRiteParams base, RiteParams params, boolean requiresFullMoon) {
            super(base, params);
            this.requiresFullMoon = requiresFullMoon;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel(); // silent: no player on the centre
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = RiteTargets.performer(findEntity(params.caster), params.caster, target).orElse(null);
            if (caster == null) {
                return cancel(); // silent: the performer left: only the performer may stand for themselves, so nobody is asked and nobody is changed
            }

            if (requiresFullMoon && !MoonRule.nightFor(true, level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime(),
                    net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed.active(level.getServer()))) {
                return refuse(caster, FactionRiteFactory.NEEDS_FULL_MOON);
            }
            if (!isWitchType(BloodlineAccess.get(caster).species())) {
                return refuse(caster, NOT_WITCH);
            }
            UUID bloodOf = params.consumedItems.stream().filter(s -> s.is(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.SIRES_BLOOD.get()))
                    .map(SiresBloodItem::sourceOf).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            if (bloodOf == null) {
                return refuse(caster, NO_BLOOD);
            }

            var ctx = SpeciesContext.of(target, bloodOf);
            Outcome preview = BloodlineRules.apply(BloodlineAccess.get(target), BloodlineEvent.RITE_OF_SEVERANCE, ctx);
            if (preview.refused()) {
                RiteTargets.refuseAbout(caster, target, preview.refusalKey()); // a bond, a sire: the target's own, not the caster's to learn
                return cancel();
            }
            BloodlineAccess.applyEvent(target, BloodlineEvent.RITE_OF_SEVERANCE, ctx);
            RiteTell.say(target, DONE);
            return false;
        }

        private boolean refuse(ServerPlayer caster, String key) {
            RiteTell.refuse(caster, key);
            return cancel();
        }
    }
}
