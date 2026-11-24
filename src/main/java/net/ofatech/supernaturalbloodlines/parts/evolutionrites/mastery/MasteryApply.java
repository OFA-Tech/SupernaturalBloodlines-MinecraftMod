package net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.RiteEp;

/**
 * Brings one skill instance to mastered the verified way (research {@code mastery.md} 1.5, option A): through {@code addMasteryPoint}, so
 * {@code SKILL_MASTERY} and {@code onSkillMastered} fire (evolutions, the statistic and the advancement) and the storage is marked dirty. A resistance has
 * no mastery: its learning is finished through Tensura's own {@code addLearnPoint}, which fires {@code SkillLearningEvent} and {@code UNLOCK_SKILL} (so a
 * veto applies) and, when it goes through, runs the skill's own {@code onLearnSkill} and the toggle. A veto at either puts the old mastery back.
 */
public final class MasteryApply {
    public enum Result { DONE, VETOED, ALREADY }

    /** A test seam: runs right after the mastery is confirmed, where a mod's own listener could still throw. */
    public static volatile Runnable afterConfirm = () -> { };

    private MasteryApply() {
    }

    public static Result apply(ServerPlayer target, ManasSkillInstance inst) {
        if (done(target, inst)) {
            return Result.ALREADY;
        }
        double before = inst.getMastery();
        if (EvoSkills.isResistance(inst.getSkill()) && inst.getSkill() instanceof TensuraSkill tensura) {
            // Tensura's own completion of a resistance's learning (mode 0): the new mastery is min(old + points, 0)
            // Tensura charges its own price inside its unlock listener, which may run before another listener's veto: a refused completion gives it back
            double ep = EnergyHelper.getBaseMaxEP(target);
            boolean went = tensura.addLearnPoint(inst, target, 0, MasteryRules.POINTS);
            if (!went || inst.getMastery() < 0) {
                RiteEp.restore(target, ep);
                inst.setMastery(before);
                SkillAPI.getSkillsFrom(target).markDirty();
                return Result.VETOED;
            }
            afterConfirm.run();
            return Result.DONE;
        }
        if (before < 0) {
            inst.setMastery(0);
        }
        inst.addMasteryPoint(target, MasteryRules.POINTS);
        if (!inst.isMastered(target)) {
            // a SKILL_MASTERY listener vetoed or scaled it (Ascension's sealed skills do): the fallback of the research would override a veto made on purpose
            inst.setMastery(before);
            SkillAPI.getSkillsFrom(target).markDirty();
            return Result.VETOED;
        }
        afterConfirm.run();
        return Result.DONE;
    }

    /** Whether the instance has nothing left to master (or finish). */
    public static boolean done(ServerPlayer target, ManasSkillInstance inst) {
        return EvoSkills.isResistance(inst.getSkill()) ? inst.getMastery() >= 0 : inst.isMastered(target);
    }
}
