package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;

/** The seam through which the hub reads the viewer's own clan: the species part installs the source. With none installed the view is empty. */
public final class HubClan {
    private static volatile Function<ServerPlayer, ClanView> source = p -> ClanView.NONE;

    private HubClan() {
    }

    public static void set(Function<ServerPlayer, ClanView> function) {
        source = function == null ? p -> ClanView.NONE : function;
    }

    public static ClanView of(ServerPlayer p) {
        return source.apply(p);
    }
}
