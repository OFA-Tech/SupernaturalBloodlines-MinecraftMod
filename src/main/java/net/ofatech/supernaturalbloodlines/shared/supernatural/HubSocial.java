package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the hub shows about other players: names, ranks and online flags only, never their attachments. The species part (which owns the
 * sireline index, the pack index and the Five roster) installs it; without it the hub shows nobody.
 */
public final class HubSocial {
    /** @param rank {@code alpha}, {@code beta}, {@code emissary} and so on; an Alpha with stacked sparks carries the count after a colon ({@code alpha:3}) */
    public record Member(String name, String rank, boolean online) {
        /** The rank without the spark count: {@code alpha:3} is {@code alpha}. */
        public String base() {
            int i = rank.indexOf(':');
            return i < 0 ? rank : rank.substring(0, i);
        }

        /** How many Alpha sparks this member holds (1 when the rank does not say). */
        public int sparks() {
            int i = rank.indexOf(':');
            if (i < 0) {
                return 1;
            }
            try {
                return Math.max(1, Integer.parseInt(rank.substring(i + 1)));
            } catch (NumberFormatException bad) {
                return 1;
            }
        }
    }

    /** One of the family below the player: {@code depth} 1 is sired by them, 2 by those, and so on. */
    public record Kin(String name, int depth) {
    }

    /**
     * A rite of this mod the server has loaded (so its part and mods are on), as the hub lists it.
     *
     * @param items   "count id" per item
     * @param shapes  the circle shape ids
     * @param factory the rite's factory type id; {@code faction} is its {@code faction} setting, or "" when it has none
     */
    public record Rite(String id, int power, List<String> items, List<String> shapes, String factory, String faction) {
        public Rite {
            items = List.copyOf(items);
            shapes = List.copyOf(shapes);
        }
    }

    /**
     * @param ancestors the sire's sire and up, nearest first (the sire itself is {@code sire}); "" for a name the server does not know
     * @param line      everyone below the player, depth-first
     */
    public record Social(Optional<String> sire, List<String> sired, List<Member> pack, int fiveCount, List<String> ancestors, List<Kin> line,
                         List<Rite> rites) {
        public static final Social NONE = new Social(Optional.empty(), List.of(), List.of(), 0, List.of(), List.of(), List.of());

        public Social {
            sired = List.copyOf(sired);
            pack = List.copyOf(pack);
            ancestors = List.copyOf(ancestors);
            line = List.copyOf(line);
            rites = List.copyOf(rites);
        }
    }

    private static volatile Function<ServerPlayer, Social> source = p -> Social.NONE;

    private HubSocial() {
    }

    public static void set(Function<ServerPlayer, Social> function) {
        source = function;
    }

    public static Social of(ServerPlayer p) {
        return source.apply(p);
    }
}
