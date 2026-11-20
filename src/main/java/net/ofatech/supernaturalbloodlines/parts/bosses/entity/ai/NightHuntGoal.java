package net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/**
 * Hunts one named player (the one the director picked) for as long as they are alive, in this level and attackable, and only by night: by day he
 * lets go ({@link MikaelRules#mayChase}). He never goes after an Original or an Original hybrid unless {@code mikael_stakes_originals} is on.
 */
public final class NightHuntGoal extends TargetGoal {
    private final Supplier<UUID> prey;
    @Nullable
    private ServerPlayer target;

    public NightHuntGoal(Mob mob, Supplier<UUID> prey) {
        super(mob, false);
        this.prey = prey;
    }

    private boolean allowed(ServerPlayer p) {
        return MikaelRules.mayChase(MikaelRules.isNight(mob.level().getDayTime()), BossDirector.isOriginalPlayer(p), BossesConfig.mikaelStakesOriginals());
    }

    @Override
    public boolean canUse() {
        UUID id = prey.get();
        if (id == null || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerPlayer p = SireCall.player(level.getServer(), id);
        if (p == null || p.level() != mob.level() || !p.isAlive() || !mob.canAttack(p) || !allowed(p)) {
            return false;
        }
        target = p;
        return true;
    }

    @Override
    public void start() {
        mob.setTarget(target);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t == target && t.isAlive() && allowed(target) && super.canContinueToUse();
    }
}
