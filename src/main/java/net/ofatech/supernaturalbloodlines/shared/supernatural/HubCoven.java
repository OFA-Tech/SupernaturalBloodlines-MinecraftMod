package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;

/** The coven view of a player, from whichever part keeps the covens (the species part installs it); with none installed nobody has one. */
public final class HubCoven {
    private static volatile Function<ServerPlayer, CovenView> source = p -> CovenView.NONE;

    private HubCoven() {
    }

    public static void set(Function<ServerPlayer, CovenView> function) {
        source = function == null ? p -> CovenView.NONE : function;
    }

    public static CovenView of(ServerPlayer p) {
        return source.apply(p);
    }
}