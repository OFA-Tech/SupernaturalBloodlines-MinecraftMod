package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;

/** How many players or mobs a player has sired. The species part (which owns the sireline index) installs it; without it nobody has sired anyone. */
public final class SireCount {
    private static volatile ToIntFunction<ServerPlayer> count = p -> 0;

    private SireCount() {
    }

    public static void set(ToIntFunction<ServerPlayer> function) {
        count = function;
    }

    public static int of(ServerPlayer p) {
        return count.applyAsInt(p);
    }
}
