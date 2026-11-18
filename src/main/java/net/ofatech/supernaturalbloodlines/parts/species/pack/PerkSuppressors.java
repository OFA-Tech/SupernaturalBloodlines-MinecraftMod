package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one list of sources that take the Alpha perks away for a moment (the lunar eclipse): {@link RankEffects} (the rank's own bonuses) and
 * {@link PackBonus} (the Beta count) both ask it, so they cannot disagree about whether the perks are in force.
 */
public final class PerkSuppressors {
    private static final List<Predicate<ServerPlayer>> ALL = new CopyOnWriteArrayList<>();

    private PerkSuppressors() {}

    /** A source says "not now" for a player; it calls {@code RankEffects.sync} / the pack tick when that changes. */
    public static void add(Predicate<ServerPlayer> suppressed) {
        ALL.add(suppressed);
    }

    public static boolean any(ServerPlayer p) {
        return anyOf(ALL, p);
    }

    static boolean anyOf(List<Predicate<ServerPlayer>> sources, ServerPlayer p) {
        return sources.stream().anyMatch(s -> s.test(p));
    }
}

