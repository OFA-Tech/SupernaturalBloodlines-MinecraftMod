package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

/**
 * Finishing plan 11's turning with a bite of the HUD's Bite action (R20e). The species part installs it; without it nobody is in the
 * turning here, so the Bite never offers it.
 */
public final class TurningFeed {
    private static volatile Predicate<ServerPlayer> can = p -> false;
    private static volatile Consumer<ServerPlayer> feed = p -> {
    };

    private TurningFeed() {
    }

    public static void set(Predicate<ServerPlayer> canFeed, Consumer<ServerPlayer> feeder) {
        can = canFeed;
        feed = feeder;
    }

    /** In the turning, and human-like blood would complete it. */
    public static boolean canFeed(ServerPlayer p) {
        return can.test(p);
    }

    public static void feed(ServerPlayer p) {
        feed.accept(p);
    }
}
