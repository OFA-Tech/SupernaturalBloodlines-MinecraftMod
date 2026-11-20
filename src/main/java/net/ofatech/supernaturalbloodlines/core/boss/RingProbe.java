package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Whether a worn Resurrection Ring would save this player from this death: the artifacts part installs the real answer, and without it the answer is no.
 * The Elixir asks, so the ring goes first whatever the order the two listeners were added in (R168).
 */
public final class RingProbe {
    private static volatile BiPredicate<ServerPlayer, DamageSource> probe = (p, s) -> false;
    private static volatile BiPredicate<ServerPlayer, DamageSource> pinned;

    private RingProbe() {
    }

    public static void install(BiPredicate<ServerPlayer, DamageSource> would) {
        probe = would;
    }

    /** Tests: pins the answer; null puts the installed one back. */
    public static void pin(BiPredicate<ServerPlayer, DamageSource> would) {
        pinned = would;
    }

    public static boolean wouldSave(ServerPlayer p, DamageSource source) {
        BiPredicate<ServerPlayer, DamageSource> fixed = pinned;
        return (fixed != null ? fixed : probe).test(p, source);
    }
}