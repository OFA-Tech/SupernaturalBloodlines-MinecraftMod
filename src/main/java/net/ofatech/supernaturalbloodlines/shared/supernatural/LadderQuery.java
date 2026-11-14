package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlice;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/**
 * What the other parts may ask the ladders. The supernatural part installs the answers; without it the layer is the whole layer
 * (today's behaviour) and every rung is 0.
 */
public final class LadderQuery {
    public interface Provider {
        Optional<LayerSlice> layerSlice(Player player);

        int rung(Player player, SupernaturalRace race);
    }

    private static final Provider NONE = new Provider() {
        @Override
        public Optional<LayerSlice> layerSlice(Player player) {
            return Optional.empty();
        }

        @Override
        public int rung(Player player, SupernaturalRace race) {
            return 0;
        }
    };

    private static volatile Provider provider = NONE;

    private LadderQuery() {
    }

    public static void set(Provider p) {
        provider = p;
    }

    /** The part of the species layer granted so far; empty means the whole layer. */
    public static Optional<LayerSlice> layerSlice(Player player) {
        return provider.layerSlice(player);
    }

    /** The highest rung reached on that race's ladder (0 for none). */
    public static int rung(Player player, SupernaturalRace race) {
        return provider.rung(player, race);
    }
}
