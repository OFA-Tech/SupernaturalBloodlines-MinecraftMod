package net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.jetbrains.annotations.Nullable;

/** Hunts one named player (the one the director picked) for as long as they are alive, in this level and attackable. */
public final class NightHuntGoal extends TargetGoal {
    private final Supplier<UUID> prey;
    @Nullable
    private ServerPlayer target;

    public NightHuntGoal(Mob mob, Supplier<UUID> prey) {
        super(mob, false);
        this.prey = prey;
    }

    @Override
    public boolean canUse() {
        UUID id = prey.get();
        if (id == null || !(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return false;
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        if (p == null || p.level() != mob.level() || !p.isAlive() || !mob.canAttack(p)) {
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
        return t != null && t == target && t.isAlive() && super.canContinueToUse();
    }
}
