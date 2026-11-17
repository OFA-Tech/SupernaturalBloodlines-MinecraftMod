package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

class RingFillTest {
    private static BiPredicate<Integer, Integer> square(int r) {
        return (x, z) -> Math.max(Math.abs(x), Math.abs(z)) == r;
    }

    @Test
    void squareRingEncloses() {
        assertTrue(RingFill.enclosed(square(3), 0, 0, 8));
    }

    @Test
    void gapOpensIt() {
        BiPredicate<Integer, Integer> gap = (x, z) -> square(3).test(x, z) && !(x == 3 && z == 0);
        assertFalse(RingFill.enclosed(gap, 0, 0, 8));
    }

    @Test
    void diamondOfDiagonalBlocksEncloses() {
        assertTrue(RingFill.enclosed((x, z) -> Math.abs(x) + Math.abs(z) == 3, 0, 0, 8));
    }

    @Test
    void ringLargerThanTheLimitIsOpen() {
        assertFalse(RingFill.enclosed(square(5), 0, 0, 4));
    }

    @Test
    void centreOnAWallIsNotEnclosed() {
        assertFalse(RingFill.enclosed((x, z) -> x == 0 && z == 0, 0, 0, 8));
    }

    @Test
    void noWallsIsOpen() {
        assertFalse(RingFill.enclosed((x, z) -> false, 0, 0, 8));
    }
}
