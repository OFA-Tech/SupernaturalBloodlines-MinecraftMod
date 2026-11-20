package net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.ofatech.supernaturalbloodlines.core.boss.KolRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity;

/** Kol's one trick (R173): a leap at the target from a few blocks away, then a pause. */
public final class BossLeapGoal extends Goal {
    private final KolEntity kol;

    public BossLeapGoal(KolEntity kol) {
        this.kol = kol;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = kol.getTarget();
        return target != null && target.isAlive() && KolRules.leaps(kol.distanceToSqr(target), kol.onGround(), kol.leapPause());
    }

    @Override
    public void start() {
        kol.leapAt(kol.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
