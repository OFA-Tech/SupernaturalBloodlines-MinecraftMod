package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record PackContext(@Nullable UUID otherAlpha, int werewolfLevel, long gameTime, int maxLevel, long betaDaysTicks,
                          int defenderKillsNeeded, boolean otherAlphaHasPack) {

    /**
     * 30 in-game days as a Beta, 300 defender kills (R91). {@code maxLevel} is 0 until the caller supplies the
     * real cap, and a 0 cap refuses TRUE_ALPHA_EARNED.
     */
    public static PackContext defaults() {
        return new PackContext(null, 0, 0, 0, 30L * 24000, 300, false);
    }
}

