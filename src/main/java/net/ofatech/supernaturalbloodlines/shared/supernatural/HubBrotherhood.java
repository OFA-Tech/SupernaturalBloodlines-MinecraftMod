package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;

/** The seam through which the hub reads the viewer's own brotherhood: the species part installs the source. With none installed the view is empty. */
public final class HubBrotherhood {
    private static volatile Function<ServerPlayer, BrotherhoodView> source = p -> BrotherhoodView.NONE;

    private HubBrotherhood() {
    }

    public static void set(Function<ServerPlayer, BrotherhoodView> function) {
        source = function == null ? p -> BrotherhoodView.NONE : function;
    }

    public static BrotherhoodView of(ServerPlayer p) {
        return source.apply(p);
    }
}