package net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite;

import com.mojang.serialization.MapCodec;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
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
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipeCodec;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRules;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.Recipes;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.AddonProbe;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * An evolution rite (plan 19): a witch-type performer opens a road, for the consenting one on the circle (or herself), to a skill that already exists.
 * The rite file's {@code factory} block is the recipe. The order of checks is {@link EvolutionRules#check}; everything about the target goes through
 * {@link RiteTargets#refuseAbout}, so the performer learns nothing of a stranger on the circle (E5).
 */
public record EvolutionRiteFactory(EvolutionRecipe recipe) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "evolution");
    public static final String DONE = "supernaturalbloodlines.rite.evolution_done";
    public static final String DONE_CASTER = "supernaturalbloodlines.rite.evolution_done_caster";
    public static final String ANNOUNCE = "supernaturalbloodlines.rite.evolution_announce";

    /** A test seam: runs between the last check and the grant, where another mod could have given the target the skill. */
    public static volatile Runnable beforeGrant = () -> { };

    public static final MapCodec<EvolutionRiteFactory> CODEC = EvolutionRecipeCodec.RECIPE.xmap(EvolutionRiteFactory::new, EvolutionRiteFactory::recipe);

    public enum Grant { DONE, ALREADY, VETOED }

    /**
     * The one place a skill changes hands (E6, E8, E15): learn it, and only when that is confirmed forget what the recipe consumes and spend its EP. A target
     * who holds the skill by now (a third mod gave it) is {@code ALREADY}; a learn that a listener vetoed, or that leaves too little EP, is {@code VETOED} and
     * costs nothing. Tensura's own price for the skill is charged by the learn itself and is not the rite's EP cost.
     */
    public static Grant grant(ServerPlayer target, EvolutionRecipe recipe, ManasSkill skill) {
        beforeGrant.run();
        if (EvoSkills.held(target, skill).isPresent()) {
            return Grant.ALREADY;
        }
        // Tensura charges the skill's own price (from base Max EP) inside its unlock listener, which may run before another listener's veto: a refused learn gives it back
        double epBefore = EnergyHelper.getBaseMaxEP(target);
        var skills = SkillAPI.getSkillsFrom(target);
        if (!SkillHelper.learnSkill(target, skill)) {
            EnergyHelper.setBaseMaxEP(target, epBefore);
            return Grant.VETOED;
        }
        if (recipe.epCost() > 0 && EnergyHelper.getBaseMaxEP(target) <= recipe.epCost()) {
            // the learn took its price and left too little to pay the rite's own: undo it all
            skills.forgetSkill(skill);
            EnergyHelper.setBaseMaxEP(target, epBefore);
            return Grant.VETOED;
        }
        for (String id : recipe.consumes()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                skills.forgetSkill(rl);
            }
        }
        if (recipe.epCost() > 0) {
            EnergyHelper.setBaseMaxEP(target, EnergyHelper.getBaseMaxEP(target) - recipe.epCost());
        }
        return Grant.DONE;
    }

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new EvolutionRite(base, params, recipe);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class EvolutionRite extends Rite {
        private final EvolutionRecipe recipe;

        EvolutionRite(BaseRiteParams base, RiteParams params, EvolutionRecipe recipe) {
            super(base, params);
            this.recipe = recipe;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;

            Optional<AddonProbe> probe = Recipes.usableProbe(recipe, Recipes::modLoaded);
            Optional<ManasSkill> grant = EvoSkills.skill(recipe.grants());
            Optional<EvolutionRules.Reason> reason = EvolutionRules.check(recipe, facts(caster, target, probe, grant));
            if (reason.isPresent()) {
                return refuse(caster, target, reason.get());
            }

            switch (grant(target, recipe, grant.get())) {
                case ALREADY -> {
                    return refuse(caster, target, EvolutionRules.Reason.ALREADY);
                }
                case VETOED -> {
                    return refuse(caster, target, EvolutionRules.Reason.NOT_READY);
                }
                case DONE -> { }
            }
            target.displayClientMessage(Component.translatable(DONE), true);
            if (caster != target) {
                caster.displayClientMessage(Component.translatable(DONE_CASTER), true);
            }
            if (recipe.tier() == Tier.ULTIMATE && EvolutionRitesConfig.announceUltimates()) {
                target.server.getPlayerList().broadcastSystemMessage(Component.translatable(ANNOUNCE, target.getDisplayName()), false);
            }
            return false;
        }

        private EvolutionRules.Facts facts(ServerPlayer caster, ServerPlayer target, Optional<AddonProbe> probe, Optional<ManasSkill> grant) {
            boolean hooks = probe.isPresent() && grant.isPresent();
            boolean roadOpen = probe.map(AddonProbe::roadOpen).orElse(false);
            boolean fullMoon = MoonRule.isFullMoonNight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime());
            boolean holds = grant.isPresent() && EvoSkills.held(target, grant.get()).isPresent();
            boolean kindOk = recipe.subjectKinds().isEmpty() || RiteUnlocks.kindsOf(target).stream().anyMatch(recipe.subjectKinds()::contains);
            boolean needs = recipe.needs().stream().allMatch(n -> EvoSkills.held(target, ResourceLocation.parse(n.skill()))
                    .filter(inst -> !n.mastered() || inst.isMastered(target)).isPresent());
            boolean satisfied = probe.map(p -> p.satisfied(target, recipe)).orElse(false);
            boolean owned = probe.map(p -> p.ownedByOther(target, recipe)).orElse(false);
            boolean enoughEp = recipe.epCost() <= 0 || EnergyHelper.getBaseMaxEP(target) > recipe.epCost();
            return new EvolutionRules.Facts(EvolutionRitesConfig.recipeOpen(recipe.id(), recipe.tier()), Recipes.modLoaded(recipe.requiresMod()), hooks, roadOpen,
                    SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species()), fullMoon, LunarEclipse.active(level.getServer()), caster == target,
                    target.isShiftKeyDown(), holds, kindOk, needs, satisfied, owned, enoughEp);
        }

        private boolean refuse(ServerPlayer caster, ServerPlayer target, EvolutionRules.Reason reason) {
            String key = EvolutionRules.keyOf(reason, recipe.tier());
            if (EvolutionRules.aboutTarget(reason)) {
                RiteTargets.refuseAbout(caster, target, key);
            } else if (reason == EvolutionRules.Reason.NO_CONSENT) {
                RiteTargets.Told told = RiteTargets.consentAnswer(false, false, key);
                caster.displayClientMessage(Component.translatable(told.caster()), true);
                target.displayClientMessage(Component.translatable(told.target()), true);
            } else {
                caster.displayClientMessage(Component.translatable(key), true);
            }
            return cancel();
        }
    }
}
