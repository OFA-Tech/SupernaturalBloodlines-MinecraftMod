package net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;

/**
 * Brings one skill instance to mastered the verified way (research {@code mastery.md} 1.5, option A): through {@code addMasteryPoint}, so
 * {@code SKILL_MASTERY} and {@code onSkillMastered} fire (evolutions, the statistic and the advancement) and the storage is marked dirty. A
 * resistance has no mastery: the rite finishes its learning instead.
 */
public final class MasteryApply {
    public enum Result { DONE, VETOED, ALREADY }

    private MasteryApply() {
    }

    public static Result apply(ServerPlayer target, ManasSkillInstance inst) {
        if (EvoSkills.isResistance(inst.getSkill())) {
            if (inst.getMastery() >= 0) {
                return Result.ALREADY;
            }
            inst.setMastery(0);
            inst.onLearnSkill(target);
            inst.markDirty();
            return Result.DONE;
        }
        if (inst.isMastered(target)) {
            return Result.ALREADY;
        }
        double before = inst.getMastery();
        if (before < 0) {
            inst.setMastery(0);
        }
        inst.addMasteryPoint(target, inst.getMaxMastery() - inst.getMastery());
        if (!inst.isMastered(target)) {
            // a SKILL_MASTERY listener vetoed or scaled it (Ascension's sealed skills do): the fallback of the research would override a veto made on purpose
            inst.setMastery(before);
            SkillAPI.getSkillsFrom(target).markDirty();
            return Result.VETOED;
        }
        return Result.DONE;
    }

    /** Whether the instance has nothing left to master (or finish). */
    public static boolean done(ServerPlayer target, ManasSkillInstance inst) {
        return EvoSkills.isResistance(inst.getSkill()) ? inst.getMastery() >= 0 : inst.isMastered(target);
    }
}
