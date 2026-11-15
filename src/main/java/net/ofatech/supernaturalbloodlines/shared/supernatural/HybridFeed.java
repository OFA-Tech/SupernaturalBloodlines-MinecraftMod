package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Feeding a hybrid type's blood thirst by a bite (R110 row 3). The species part installs it; without it no one is a hybrid here and
 * feeding does nothing, so the HUD's Bite never offers it.
 */
public final class HybridFeed {
    public static final int MAX_THIRST = 20;

    private static volatile Predicate<Player> hybrid = p -> false;
    private static volatile Consumer<ServerPlayer> feed = p -> {
    };

    private HybridFeed() {
    }

    public static void set(Predicate<Player> isHybrid, Consumer<ServerPlayer> feeder) {
        hybrid = isHybrid;
        feed = feeder;
    }

    public static boolean isHybrid(Player p) {
        return hybrid.test(p);
    }

    public static void feed(ServerPlayer p) {
        feed.accept(p);
    }
}
