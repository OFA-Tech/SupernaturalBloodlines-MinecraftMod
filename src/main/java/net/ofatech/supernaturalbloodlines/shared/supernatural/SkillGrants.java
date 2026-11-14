package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants and takes back the Tensura skills of the TENSURA skills. The supernatural_tensura part installs the real granter; without it
 * (no Tensura, or that part off) nothing is granted and {@code granted} stays as it is.
 */
public final class SkillGrants {
    @FunctionalInterface
    public interface Granter {
        /**
         * @param wanted  Tensura skill ids the player's active skills want now
         * @param granted what this mod granted before (the only skills it may take back)
         * @return the new {@code granted}
         */
        Set<String> sync(ServerPlayer player, Set<String> wanted, Set<String> granted);
    }

    private static volatile Granter granter;

    private SkillGrants() {
    }

    public static void set(Granter g) {
        granter = g;
    }

    public static Set<String> sync(ServerPlayer player, Set<String> wanted, Set<String> granted) {
        Granter g = granter;
        return g == null ? granted : g.sync(player, wanted, granted);
    }
}
