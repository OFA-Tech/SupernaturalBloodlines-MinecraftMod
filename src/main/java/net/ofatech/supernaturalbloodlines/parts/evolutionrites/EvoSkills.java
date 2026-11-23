package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;

/** The few lookups of Tensura's and ManasCore's skills the evolution rites share. */
public final class EvoSkills {
    /** Rebalanced's tag of the skills it retired (every one of its modules ships the same file). */
    public static final ResourceLocation RETIRED_TAG = ResourceLocation.fromNamespaceAndPath("tensura_rebalanced", "retired");

    private EvoSkills() {
    }

    public static Optional<ManasSkill> skill(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : skill(rl);
    }

    public static Optional<ManasSkill> skill(ResourceLocation id) {
        return Optional.ofNullable(SkillAPI.getSkillRegistry().get(id));
    }

    public static Optional<ManasSkillInstance> held(ServerPlayer p, ResourceLocation id) {
        return SkillAPI.getSkillsFrom(p).getSkill(id);
    }

    public static Optional<ManasSkillInstance> held(ServerPlayer p, ManasSkill skill) {
        return SkillAPI.getSkillsFrom(p).getSkill(skill);
    }

    /** The tier of a skill by its Tensura type; a skill that is not a Tensura {@code Skill} counts as Extra. */
    public static Tier tierOf(ManasSkill skill) {
        if (skill instanceof Skill s) {
            return Tier.ofSkillType(s.getType().name(), true);
        }
        return Tier.ofSkillType("", false);
    }

    public static boolean isResistance(ManasSkill skill) {
        return skill instanceof Skill s && s.getType() == Skill.SkillType.RESISTANCE;
    }

    /** Whether Rebalanced (if it is installed) has retired this skill: a skill in its {@code retired} tag. Never throws. */
    public static boolean retired(MinecraftServer server, ManasSkill skill) {
        try {
            var key = SkillAPI.getSkillRegistryKey();
            var registry = server.registryAccess().registry(key);
            if (registry.isEmpty()) {
                return false;
            }
            ResourceLocation id = skill.getRegistryName();
            if (id == null) {
                return false;
            }
            Optional<Holder.Reference<ManasSkill>> holder = registry.get().getHolder(ResourceKey.create(key, id));
            return holder.isPresent() && holder.get().is(TagKey.create(key, RETIRED_TAG));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
