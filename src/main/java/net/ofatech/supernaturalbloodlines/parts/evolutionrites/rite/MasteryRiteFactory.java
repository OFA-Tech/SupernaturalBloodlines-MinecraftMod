package net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite;

import com.mojang.serialization.MapCodec;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipeCodec;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryApply;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryChoice;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * The Rite of Mastery (plan 19, spec 8.5, E16): masters the one skill the target has chosen, if it is of this rite file's tier. The EP is spent last,
 * after the mastery is confirmed, so a refusal or a veto costs nothing but the altar's power and Enchanted gives the ingredients back.
 */
public record MasteryRiteFactory(Tier tier) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "mastery");
    public static final String DONE = "supernaturalbloodlines.rite.mastery_done";
    public static final String DONE_CASTER = "supernaturalbloodlines.rite.mastery_done_caster";

    public static final MapCodec<MasteryRiteFactory> CODEC = EvolutionRecipeCodec.MASTERY_TIER.xmap(MasteryRiteFactory::new, MasteryRiteFactory::tier);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new MasteryRite(base, params, tier);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class MasteryRite extends Rite {
        private final Tier riteTier;

        MasteryRite(BaseRiteParams base, RiteParams params, Tier riteTier) {
            super(base, params);
            this.riteTier = riteTier;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;

            Optional<ResourceLocation> choice = MasteryChoice.get(target);
            Optional<ManasSkillInstance> inst = choice.flatMap(id -> EvoSkills.held(target, id));
            double cost = EvolutionRitesConfig.masteryEp(riteTier);
            Tier skillTier = inst.map(i -> EvoSkills.tierOf(i.getSkill())).orElse(riteTier);
            boolean fullMoon = MoonRule.isFullMoonNight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime());
            MasteryRules.Facts facts = new MasteryRules.Facts(EvolutionRitesConfig.masteryEnabled() && EvolutionRitesConfig.ritesEnabled()
                    && (riteTier != Tier.ULTIMATE || EvolutionRitesConfig.ultimateRitesEnabled()),
                    SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species()), fullMoon, LunarEclipse.active(level.getServer()), caster == target,
                    target.isShiftKeyDown(), choice.isPresent(), inst.isPresent(), inst.map(ManasSkillInstance::isTemporarySkill).orElse(false),
                    inst.map(i -> MasteryApply.done(target, i)).orElse(false), skillTier, riteTier,
                    choice.map(id -> EvolutionRitesConfig.masteryRefusedSkills().contains(id.toString())).orElse(false),
                    inst.map(i -> EvoSkills.retired(target.server, i.getSkill())).orElse(false), EnergyHelper.getBaseMaxEP(target) > cost,
                    MasteryChoice.onCooldown(target));
            Optional<MasteryRules.Reason> reason = MasteryRules.check(facts);
            if (reason.isPresent()) {
                return refuse(caster, target, reason.get());
            }

            MasteryApply.Result result = MasteryApply.apply(target, inst.get());
            if (result != MasteryApply.Result.DONE) {
                RiteTargets.refuseAbout(caster, target, result == MasteryApply.Result.VETOED ? MasteryRules.VETOED : MasteryRules.KEY + "already");
                return cancel();
            }
            EnergyHelper.setBaseMaxEP(target, EnergyHelper.getBaseMaxEP(target) - cost);
            MasteryChoice.markRite(target);
            MasteryChoice.clear(target);
            target.displayClientMessage(Component.translatable(DONE), true);
            if (caster != target) {
                caster.displayClientMessage(Component.translatable(DONE_CASTER), true);
            }
            return false;
        }

        private boolean refuse(ServerPlayer caster, ServerPlayer target, MasteryRules.Reason reason) {
            String key = MasteryRules.keyOf(reason, riteTier);
            if (reason == MasteryRules.Reason.COOLDOWN) {
                int days = MasteryRules.daysLeft(MasteryChoice.lastRite(target), MasteryChoice.now(target), EvolutionRitesConfig.masteryCooldownDays());
                sayAbout(caster, target, Component.translatable(key, days));
            } else if (MasteryRules.aboutTarget(reason)) {
                sayAbout(caster, target, Component.translatable(key));
            } else if (reason == MasteryRules.Reason.NO_CONSENT) {
                RiteTargets.Told told = RiteTargets.consentAnswer(false, false, key);
                caster.displayClientMessage(Component.translatable(told.caster()), true);
                target.displayClientMessage(Component.translatable(told.target()), true);
            } else {
                caster.displayClientMessage(Component.translatable(key), true);
            }
            return cancel();
        }

        /** The same split as {@link RiteTargets#refuseAbout}, for a message with an argument. */
        private static void sayAbout(ServerPlayer caster, ServerPlayer target, Component targetLine) {
            if (caster == target) {
                target.displayClientMessage(targetLine, true);
            } else {
                caster.displayClientMessage(Component.translatable(net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory.NO_EFFECT), true);
                target.displayClientMessage(targetLine, true);
            }
        }
    }
}
