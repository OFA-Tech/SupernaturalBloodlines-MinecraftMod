package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

/** Whether a ring of wall cells closes around a centre cell: a 4-neighbour flood fill that must stay within {@code maxRadius}. */
public final class RingFill {
    private RingFill() {
    }

    public static boolean enclosed(BiPredicate<Integer, Integer> wall, int cx, int cz, int maxRadius) {
        if (wall.test(cx, cz)) {
            return false;
        }
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{cx, cz});
        seen.add(key(cx, cz));
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : steps) {
                int x = c[0] + d[0];
                int z = c[1] + d[1];
                if (Math.abs(x - cx) > maxRadius || Math.abs(z - cz) > maxRadius) {
                    return false;
                }
                if (!wall.test(x, z) && seen.add(key(x, z))) {
                    queue.add(new int[]{x, z});
                }
            }
        }
        return true;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
