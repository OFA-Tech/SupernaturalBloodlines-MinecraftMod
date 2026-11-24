package net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite;

import com.mojang.serialization.MapCodec;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipeCodec;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;
import net.ofatech.supernaturalbloodlines.core.evolution.Sacrifice;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.RiteEp;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.EpPriced;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryApply;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryChoice;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;

/**
 * The Rite of Mastery (plan 19, spec 8.5, E16): masters the one skill the target has chosen, if it is of this rite file's tier. The rite file names the item
 * kinds; how many of each is the config's, taken in {@code onStart} from what Enchanted took and what lies loose on the circle. The EP is spent last, after the
 * mastery is confirmed, so a refusal or a veto costs nothing but the altar's power and Enchanted gives the ingredients back.
 */
public record MasteryRiteFactory(Tier tier) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "mastery");
    public static final String DONE = "supernaturalbloodlines.rite.mastery_done";
    public static final String DONE_CASTER = "supernaturalbloodlines.rite.mastery_done_caster";
    public static final String SHORT_OFFERING = "supernaturalbloodlines.rite.mastery_offering";
    /** How far from the centre a loose item still counts as thrown on the circle (the large circle is the widest). */
    public static final double OFFERING_RADIUS = 8;

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

    private static final class MasteryRite extends Rite implements EpPriced {
        private final Tier riteTier;

        @Override
        public double epCost() {
            return EvolutionRitesConfig.masteryEp(riteTier);
        }

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
            ServerPlayer caster = RiteTargets.performer(findEntity(params.caster), params.caster, target).orElse(null);
            if (caster == null) {
                return cancel(); // the performer left: nobody is asked and nobody is changed
            }

            // before anything about the target is read, so a short offering says nothing of them; a cancel gives back what was taken, the extras too
            if (!takeOffering(params)) {
                caster.displayClientMessage(Component.translatable(SHORT_OFFERING), true);
                return cancel();
            }

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
                    inst.map(i -> EvoSkills.retired(target.server, i.getSkill())).orElse(false), MasteryRules.enoughEp(EnergyHelper.getBaseMaxEP(target), cost),
                    MasteryChoice.onCooldown(target));
            Optional<MasteryRules.Reason> reason = MasteryRules.check(facts);
            if (reason.isPresent()) {
                return refuse(caster, target, reason.get());
            }

            MasteryApply.Result result = MasteryApply.Result.VETOED;
            try {
                result = MasteryApply.apply(target, inst.get());
            } catch (RuntimeException thrown) {
                // something threw after (or before) the mastery was confirmed: what the player now has decides, so a mastered skill is never left unpaid
                SupernaturalBloodlines.LOGGER.warn("The Rite of Mastery threw while mastering {}", choice.get(), thrown);
                if (MasteryApply.done(target, inst.get())) {
                    result = MasteryApply.Result.DONE;
                }
            }
            if (result != MasteryApply.Result.DONE) {
                RiteTargets.refuseAbout(caster, target, result == MasteryApply.Result.VETOED ? MasteryRules.VETOED : MasteryRules.KEY + "already");
                return cancel();
            }
            RiteEp.charge(target, cost);
            MasteryChoice.markRite(target);
            MasteryChoice.clear(target);
            target.displayClientMessage(Component.translatable(DONE), true);
            if (caster != target) {
                caster.displayClientMessage(Component.translatable(DONE_CASTER), true);
            }
            return false;
        }

        /**
         * Takes the config's counts of the kinds the rite file names: what Enchanted already took plus what lies loose on the circle. Short: nothing is taken (the
         * loose items stay, Enchanted gives its own back on cancel). More than needed was taken (Enchanted takes a whole stack): the surplus goes back to the circle.
         */
        private boolean takeOffering(RiteParams params) {
            Map<String, Integer> configured = EvolutionRitesConfig.masterySacrifice(riteTier);
            Map<String, Integer> need = new LinkedHashMap<>();
            for (ItemStack kind : type.getItems()) {
                String id = BuiltInRegistries.ITEM.getKey(kind.getItem()).toString();
                need.put(id, configured.getOrDefault(id, 1));
            }
            Map<String, Integer> taken = new LinkedHashMap<>();
            for (ItemStack s : params.consumedItems) {
                taken.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
            }
            List<ItemEntity> loose = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(OFFERING_RADIUS, 3, OFFERING_RADIUS),
                    e -> e.isAlive() && need.containsKey(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString()));
            Map<String, Integer> lying = new LinkedHashMap<>();
            for (ItemEntity e : loose) {
                lying.merge(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString(), e.getItem().getCount(), Integer::sum);
            }
            Sacrifice.Plan plan = Sacrifice.plan(need, taken, lying);
            if (!plan.enough()) {
                return false;
            }
            for (Map.Entry<String, Integer> e : plan.take().entrySet()) {
                int left = e.getValue();
                for (ItemEntity entity : loose) {
                    if (left <= 0) {
                        break;
                    }
                    if (!BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString().equals(e.getKey())) {
                        continue;
                    }
                    int n = Math.min(left, entity.getItem().getCount());
                    params.consumedItems.add(entity.getItem().copyWithCount(n));
                    entity.getItem().shrink(n);
                    if (entity.getItem().isEmpty()) {
                        entity.discard();
                    }
                    left -= n;
                }
            }
            for (Map.Entry<String, Integer> e : plan.giveBack().entrySet()) {
                int left = e.getValue();
                List<ItemStack> keep = new ArrayList<>();
                for (ItemStack s : params.consumedItems) {
                    if (left > 0 && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(e.getKey())) {
                        int n = Math.min(left, s.getCount());
                        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s.copyWithCount(n)));
                        s.shrink(n);
                        left -= n;
                    }
                    if (!s.isEmpty()) {
                        keep.add(s);
                    }
                }
                params.consumedItems.clear();
                params.consumedItems.addAll(keep);
            }
            return true;
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
