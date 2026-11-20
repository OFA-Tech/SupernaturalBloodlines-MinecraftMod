package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;

/**
 * A boss with a man's body and a wolf's (Klaus, Deucalion): what {@code ShiftRenderer} reads to pick the one to draw, and what {@link ShiftCore} drives. The
 * shared part of the shift lives there; the subclasses keep what differs (the trigger, the hitbox, the swap's effects and the reward).
 */
public interface ShiftBody {
    BossForm form();

    int shiftTicks();

    void setShiftTicks(int ticks);

    void setForm(BossForm form);

    /** The body is swapped (tick {@link net.ofatech.supernaturalbloodlines.core.boss.ShiftRules#SWAP_TICK}): the hitbox, smoke, howl and the like. */
    void onSwap(ServerLevel level);

    Level level();
}
