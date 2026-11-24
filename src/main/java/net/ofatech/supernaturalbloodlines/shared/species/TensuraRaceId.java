package net.ofatech.supernaturalbloodlines.shared.species;

import java.util.function.Function;
import net.minecraft.world.entity.player.Player;

/**
 * The id of a player's Tensura race, for the parts that must stay free of Tensura (the species part's rite kinds, plan 19d). Installed by species_tensura;
 * without it every player has no race ({@code ""}).
 */
public final class TensuraRaceId {
    private static volatile Function<Player, String> source = p -> "";

    private TensuraRaceId() {
    }

    public static void install(Function<Player, String> reader) {
        source = reader == null ? p -> "" : reader;
    }

    /** A test seam: installs {@code reader} and returns the one it replaced. */
    public static Function<Player, String> swap(Function<Player, String> reader) {
        Function<Player, String> old = source;
        install(reader);
        return old;
    }

    public static String of(Player p) {
        String id = source.apply(p);
        return id == null ? "" : id;
    }
}
