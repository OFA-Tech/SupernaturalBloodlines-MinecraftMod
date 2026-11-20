package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.ofatech.supernaturalbloodlines.core.boss.BossForm;

/** A boss with a man's body and a wolf's: what {@code ShiftRenderer} reads to pick the one to draw. */
public interface ShiftBody {
    BossForm form();

    int shiftTicks();
}
