package net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
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
import net.ofatech.supernaturalbloodlines.core.evolution.GrantForm;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoGrant;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.Recipes;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.RiteEp;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.EpPriced;
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

    /** The skill the rite grants: the probe's override when it names one (the merged Raphael), else the recipe's own. An override that does not exist is no grant, never a fallback. */
    public static Optional<ManasSkill> resolveGrant(EvolutionRecipe recipe, Optional<AddonProbe> probe) {
        return EvoSkills.skill(probe.flatMap(AddonProbe::grantOverride).orElse(recipe.grants()));
    }

    /** Whether the target holds the grant, or any skill the probe says counts as holding it (either Raphael). */
    public static boolean holdsGrant(ServerPlayer target, Optional<AddonProbe> probe, Optional<ManasSkill> grant) {
        if (grant.isPresent() && EvoSkills.held(target, grant.get()).isPresent()) {
            return true;
        }
        return probe.map(p -> p.alsoHolds().stream().map(ResourceLocation::tryParse).anyMatch(id -> id != null && EvoSkills.held(target, id).isPresent())).orElse(false);
    }

    /** What the learn takes from base Max EP: Tensura's price for the default form, nothing for a free grant (it carries {@code NoMagiculeCost}). */
    public static double priceOf(EvolutionRecipe recipe, ServerPlayer target, ManasSkill skill) {
        return recipe.grantForm() == GrantForm.FREE ? 0 : EvoSkills.price(target, skill);
    }

    /**
     * The one place a skill changes hands (E6, E8, E15): learn it, and only when that is confirmed forget what the recipe consumes and spend its EP. A target
     * who holds the skill by now (a third mod gave it) is {@code ALREADY}; a learn that a listener vetoed, or that leaves too little EP, is {@code VETOED} and
     * costs nothing. Tensura's own price for the skill is charged by the learn itself and is not the rite's EP cost.
     */
    public static Grant grant(ServerPlayer target, EvolutionRecipe recipe, ManasSkill skill) {
        return grant(target, recipe, skill, Optional.empty());
    }

    /** As {@link #grant(ServerPlayer, EvolutionRecipe, ManasSkill)}; a confirmed grant then lets the addon's probe write what its own road writes ({@link AddonProbe#onGranted}). */
    public static Grant grant(ServerPlayer target, EvolutionRecipe recipe, ManasSkill skill, Optional<AddonProbe> probe) {
        beforeGrant.run();
        if (EvoSkills.held(target, skill).isPresent()) {
            return Grant.ALREADY;
        }
        // Tensura charges the skill's own price (from base Max EP) inside its unlock listener, which may run before another listener's veto: a refused learn gives it back
        double epBefore = EnergyHelper.getBaseMaxEP(target);
        var skills = SkillAPI.getSkillsFrom(target);
        var removeTime = recipe.grantForm().removeTime();
        boolean learned = recipe.grantForm() == GrantForm.FREE ? EvoGrant.learnFree(target, skill)
                : removeTime.isPresent() ? SkillHelper.learnSkill(target, skill, removeTime.getAsInt()) : SkillHelper.learnSkill(target, skill);
        if (!learned) {
            RiteEp.restore(target, epBefore);
            return Grant.VETOED;
        }
        double owed = RiteEp.owedBy(target, recipe.epCost());
        if (owed > 0 && EnergyHelper.getBaseMaxEP(target) <= owed) {
            // the learn took its price and left too little to pay the rite's own: undo it all
            skills.forgetSkill(skill);
            RiteEp.restore(target, epBefore);
            return Grant.VETOED;
        }
        for (String id : recipe.consumes()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                skills.forgetSkill(rl);
            }
        }
        RiteEp.charge(target, recipe.epCost());
        probe.ifPresent(p -> p.onGranted(target, recipe));
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

    private static final class EvolutionRite extends Rite implements EpPriced {
        private final EvolutionRecipe recipe;

        EvolutionRite(BaseRiteParams base, RiteParams params, EvolutionRecipe recipe) {
            super(base, params);
            this.recipe = recipe;
        }

        @Override
        public double epCost() {
            return recipe.epCost();
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

            Optional<AddonProbe> probe = Recipes.usableProbe(recipe, Recipes::modLoaded);
            Optional<ManasSkill> grant = resolveGrant(recipe, probe);
            Optional<EvolutionRules.Reason> reason = EvolutionRules.check(recipe, facts(caster, target, probe, grant));
            if (reason.isPresent()) {
                return refuse(caster, target, reason.get());
            }

            switch (grant(target, recipe, grant.get(), probe)) {
                case ALREADY -> {
                    return refuse(caster, target, EvolutionRules.Reason.ALREADY);
                }
                case VETOED -> {
                    return refuse(caster, target, EvolutionRules.Reason.NOT_READY);
                }
                case DONE -> { }
            }
            RiteTell.say(target, DONE);
            if (caster != target) {
                RiteTell.say(caster, DONE_CASTER);
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
            boolean holds = holdsGrant(target, probe, grant);
            boolean kindOk = recipe.subjectKinds().isEmpty() || RiteUnlocks.kindsOf(target).stream().anyMatch(recipe.subjectKinds()::contains);
            boolean needs = recipe.needs().stream().allMatch(n -> EvoSkills.held(target, ResourceLocation.parse(n.skill()))
                    .filter(inst -> !n.mastered() || inst.isMastered(target)).isPresent());
            boolean satisfied = probe.map(p -> p.satisfied(target, recipe)).orElse(false);
            boolean owned = probe.map(p -> p.ownedByOther(target, recipe)).orElse(false);
            // Tensura charges its own price for the skill from the same base Max EP inside the learn: both must fit, checked before anything is learned
            boolean enoughEp = grant.isEmpty() || MasteryRules.enoughEp(EnergyHelper.getBaseMaxEP(target) - priceOf(recipe, target, grant.get()), RiteEp.owedBy(target, recipe.epCost()));
            boolean retired = grant.isPresent() && EvoSkills.retired(level.getServer(), grant.get());
            boolean enabled = EvolutionRitesConfig.recipeOpen(recipe.id(), recipe.tier()) && probe.map(p -> p.enabledIn(level)).orElse(true);
            return new EvolutionRules.Facts(enabled, Recipes.modLoaded(recipe.requiresMod()), hooks, roadOpen,
                    SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species()), fullMoon, MoonRule.eclipseNight(level.dimensionType().hasFixedTime(), level.getDayTime(), net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed.active(level.getServer())), caster == target,
                    target.isShiftKeyDown(), holds, kindOk, needs, satisfied, owned, enoughEp, retired);
        }

        private boolean refuse(ServerPlayer caster, ServerPlayer target, EvolutionRules.Reason reason) {
            String key = EvolutionRules.keyOf(reason, recipe.tier());
            if (EvolutionRules.aboutTarget(reason)) {
                RiteTargets.refuseAbout(caster, target, key);
            } else if (reason == EvolutionRules.Reason.NO_CONSENT) {
                RiteTargets.Told told = RiteTargets.consentAnswer(false, false, key);
                RiteTargets.tell(caster, target, told);
            } else {
                RiteTell.refuse(caster, key);
            }
            return cancel();
        }
    }
}
