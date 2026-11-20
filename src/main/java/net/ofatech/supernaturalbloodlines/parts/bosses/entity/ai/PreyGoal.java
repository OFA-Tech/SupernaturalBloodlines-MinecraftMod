package net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.jetbrains.annotations.Nullable;

/** Hunts one named living thing (a player or a villager the director picked) for as long as it is alive, in this level and attackable. */
public final class PreyGoal extends TargetGoal {
    private final Supplier<UUID> prey;
    @Nullable
    private LivingEntity target;

    public PreyGoal(Mob mob, Supplier<UUID> prey) {
        super(mob, false);
        this.prey = prey;
    }

    /** The prey, when it is here and alive; null otherwise. */
    @Nullable
    public static LivingEntity find(Mob mob, @Nullable UUID id) {
        if (id == null || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity e = level.getEntity(id);
        if (e == null && SireCall.player(level.getServer(), id) instanceof ServerPlayer p && p.level() == level) {
            e = p; // the same lookup the other hunters use for players
        }
        return e instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    @Override
    public boolean canUse() {
        LivingEntity p = find(mob, prey.get());
        if (p == null || !mob.canAttack(p)) {
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
