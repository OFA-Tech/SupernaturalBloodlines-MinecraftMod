package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.Nullable;

/**
 * One class for the seven apex races, configured by an {@link ApexSpec}. Placeholder numbers: the owner tunes them
 * ({@link ApexSpec} for the stats, the constants here for energy). An apex race is the end of the line: no next evolution.
 */
public class ApexRace extends TensuraRace {
    /** Placeholder: base max aura + magicule needed to evolve (a divine human needs 2,000,000). */
    static final double EP_REQUIREMENT = 1_000_000;
    private static final double MIN_BASE_ENERGY = 250_000;
    private static final double MAX_BASE_ENERGY = 350_000;
    private static final ResourceLocation STATS_ID = ResourceLocation.fromNamespaceAndPath(ApexSpec.RACE_NAMESPACE, "apex_race_stats");
    private static final Set<String> WARNED = new HashSet<>();

    private final ApexSpec spec;

    public ApexRace(ApexSpec spec) {
        super(ManasRace.Difficulty.HARD);
        this.spec = spec;
        addAttributeModifier(Attributes.MAX_HEALTH, STATS_ID, spec.health(), AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, STATS_ID, spec.attack(), AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, STATS_ID, spec.speed(), AttributeModifier.Operation.ADD_VALUE);
    }

    public ApexSpec spec() {
        return spec;
    }

    @Override
    public Pair<Double, Double> getBaseAuraRange() {
        return Pair.of(MIN_BASE_ENERGY, MAX_BASE_ENERGY);
    }

    @Override
    public Pair<Double, Double> getBaseMagiculeRange() {
        return Pair.of(MIN_BASE_ENERGY, MAX_BASE_ENERGY);
    }

    @Override
    public Map<EvolutionRequirement, Float> getEvolutionRequirements(ManasRaceInstance previous, LivingEntity entity) {
        return Map.of(new EvolutionRequirement.EPRequirement(EP_REQUIREMENT), 100.0F);
    }

    @Override
    public List<ManasRace> getNextEvolutions(ManasRaceInstance instance, LivingEntity entity) {
        return new ArrayList<>();
    }

    @Override
    @Nullable
    public ManasRace getDefaultEvolution(ManasRaceInstance instance, LivingEntity entity) {
        return null;
    }

    @Override
    public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance instance, LivingEntity entity) {
        List<ManasSkill> skills = new ArrayList<>();
        for (String id : spec.intrinsicSkills()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ManasSkill skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (skill != null) {
                skills.add(skill);
            } else if (WARNED.add(id)) {
                SupernaturalBloodlines.LOGGER.warn("species_tensura: apex race {} lists unknown skill '{}', skipped", spec.id(), id);
            }
        }
        return skills;
    }
}
