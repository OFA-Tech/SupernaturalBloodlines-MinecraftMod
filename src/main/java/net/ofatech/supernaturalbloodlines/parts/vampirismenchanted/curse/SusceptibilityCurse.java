package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.curse;

import net.favouriteless.enchanted.api.curses.Curse;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Curse of Susceptibility (R322 to R324): the cursed player takes more damage. It has nothing to do each tick: the extra damage is applied by
 * {@link PlagueCurses}' damage listener, which reads this curse's strength from the player's curses.
 */
public final class SusceptibilityCurse implements Curse {
    @Override
    public void tick(ServerPlayer target, int strength, long age) {
    }

    @Override
    public Type<?> type() {
        return PlagueCurses.SUSCEPTIBILITY;
    }
}
