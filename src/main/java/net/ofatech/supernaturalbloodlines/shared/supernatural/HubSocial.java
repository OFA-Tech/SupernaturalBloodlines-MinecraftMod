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
    /** @param rank {@code alpha}, {@code beta} or {@code emissary} */
    public record Member(String name, String rank, boolean online) {
    }

    public record Social(Optional<String> sire, List<String> sired, List<Member> pack, int fiveCount) {
        public static final Social NONE = new Social(Optional.empty(), List.of(), List.of(), 0);

        public Social {
            sired = List.copyOf(sired);
            pack = List.copyOf(pack);
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
