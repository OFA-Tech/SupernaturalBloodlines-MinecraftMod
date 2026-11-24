package net.ofatech.supernaturalbloodlines.shared.supernatural;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What the artifacts part may ask about circles without importing the species part (it needs only Vampirism and Enchanted): the species part installs the
 * source. Default: no circle, no plan.
 */
public final class CircleQuery {
    public interface Source {
        /** The members of the circle open at that chalk (0 when none). */
        int size(ServerLevel level, BlockPos chalk);

        /** The members who pay for the rite that is starting, after the drops (0 when no plan is staged, that is a solo cast). */
        int staged();
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
}