package net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;

/**
 * The skill a player has chosen for the Rite of Mastery, and when they last had one (the cooldown). Kept in the part of the player's data that survives
 * death. The choice is validated on the server when it is made and again when the rite runs: the skill may have been lost or mastered meanwhile.
 */
public final class MasteryChoice {
    private static final String CHOICE = "supernaturalbloodlines_mastery_choice";
    private static final String LAST = "supernaturalbloodlines_mastery_last";

    /** A test seam for the game time the cooldown is measured in. */
    public static volatile LongSupplier clock = null;

    /** One held skill, as the picker lists it: the reason it cannot be chosen is empty when it can. */
    public record Option(ResourceLocation skill, Tier tier, Optional<MasteryRules.Reason> refusal) {
    }

    private MasteryChoice() {
    }

    public static long now(ServerPlayer p) {
        LongSupplier c = clock;
        return c != null ? c.getAsLong() : p.server.overworld().getGameTime();
    }

    public static Optional<ResourceLocation> get(ServerPlayer p) {
        CompoundTag flags = RiteUnlocks.flags(p);
        return flags.contains(CHOICE) ? Optional.ofNullable(ResourceLocation.tryParse(flags.getString(CHOICE))) : Optional.empty();
    }

    public static void clear(ServerPlayer p) {
        RiteUnlocks.flags(p).remove(CHOICE);
    }

    public static long lastRite(ServerPlayer p) {
        CompoundTag flags = RiteUnlocks.flags(p);
        return flags.contains(LAST) ? flags.getLong(LAST) : -1;
    }

    public static void markRite(ServerPlayer p) {
        RiteUnlocks.flags(p).putLong(LAST, now(p));
    }

    public static void setLast(ServerPlayer p, long time) {
        RiteUnlocks.flags(p).putLong(LAST, time);
    }

    public static boolean onCooldown(ServerPlayer p) {
        return MasteryRules.onCooldown(lastRite(p), now(p), EvolutionRitesConfig.masteryCooldownDays());
    }

    /** The reason a held skill can never be chosen, whichever tier of the rite is worked, or empty. */
    public static Optional<MasteryRules.Reason> unchoosable(ServerPlayer p, ManasSkillInstance inst) {
        return MasteryRules.unchoosable(inst.isTemporarySkill(), MasteryApply.done(p, inst),
                EvolutionRitesConfig.masteryRefusedSkills().contains(String.valueOf(inst.getSkillId())), EvoSkills.retired(p.server, inst.getSkill()));
    }

    /** Every skill the player holds, with its tier and, when it cannot be chosen, why. Sorted by tier then id so the list is stable. */
    public static List<Option> options(ServerPlayer p) {
        List<Option> out = new ArrayList<>();
        for (ManasSkillInstance inst : SkillAPI.getSkillsFrom(p).getLearnedSkills()) {
            ManasSkill skill = inst.getSkill();
            out.add(new Option(inst.getSkillId(), EvoSkills.tierOf(skill), unchoosable(p, inst)));
        }
        out.sort(Comparator.comparing((Option o) -> o.tier()).thenComparing(o -> o.skill().toString()));
        return out;
    }

    /** Validates and stores a choice. Returns the reason it was refused, or empty when it is stored. */
    public static Optional<MasteryRules.Reason> choose(ServerPlayer p, ResourceLocation skill) {
        Optional<ManasSkillInstance> held = EvoSkills.held(p, skill);
        if (held.isEmpty()) {
            return Optional.of(MasteryRules.Reason.NOT_HELD);
        }
        Optional<MasteryRules.Reason> why = unchoosable(p, held.get());
        if (why.isEmpty()) {
            RiteUnlocks.flags(p).putString(CHOICE, skill.toString());
        }
        return why;
    }
}
