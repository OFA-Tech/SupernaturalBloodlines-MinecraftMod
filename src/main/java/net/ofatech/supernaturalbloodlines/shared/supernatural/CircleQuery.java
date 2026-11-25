package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What the artifacts part may ask about circles and groups without importing the species part (it needs only Vampirism and Enchanted): the species part
 * installs the source. Default: no circle, no plan, nobody in a pack, every switch off.
 */
public final class CircleQuery {
    public interface Source {
        /** The members of the circle open at that chalk (0 when none). */
        int size(ServerLevel level, BlockPos chalk);

        /** The members who pay for the rite that is starting, after the drops (0 when no plan is staged, that is a solo cast). */
        int staged();

        default boolean inPack(ServerLevel level, UUID player) {
            return false;
        }

        /** A Day Walker of a clan. */
        default boolean dayWalker(ServerLevel level, UUID player) {
            return false;
        }

        /** An Admin switch of the coven rings: {@code moon_ring}, {@code sun_ring} or {@code sun_ring_day_walker_only}. */
        default boolean switchOn(String name) {
            return false;
        }
    }

    private static volatile Source source = new Source() {
        @Override
        public int size(ServerLevel level, BlockPos chalk) {
            return 0;
        }

        @Override
        public int staged() {
            return 0;
        }
    };

    private CircleQuery() {
    }

    public static void set(Source s) {
        source = s;
    }

    public static int size(ServerLevel level, BlockPos chalk) {
        return source.size(level, chalk);
    }

    public static int staged() {
        return source.staged();
    }

    public static boolean inPack(ServerLevel level, UUID player) {
        return source.inPack(level, player);
    }

    public static boolean dayWalker(ServerLevel level, UUID player) {
        return source.dayWalker(level, player);
    }

    public static boolean switchOn(String name) {
        return source.switchOn(name);
    }
}