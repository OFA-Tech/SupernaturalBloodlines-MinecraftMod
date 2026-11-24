package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;

/** The group view of a player, from whichever part keeps the groups (the species part installs it); with none installed nobody has a group. */
public final class HubGroups {
    private static volatile Function<ServerPlayer, GroupView> source = p -> GroupView.NONE;

    private HubGroups() {
    }

    public static void set(Function<ServerPlayer, GroupView> function) {
        source = function == null ? p -> GroupView.NONE : function;
    }

    public static GroupView of(ServerPlayer p) {
        return source.apply(p);
    }
}
