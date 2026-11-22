package net.ofatech.supernaturalbloodlines.shared.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RectUnionTest {
    @Test
    void theUnionHoldsBothAndNothingMore() {
        Rect u = new Rect(10, 10, 20, 20).union(new Rect(25, 0, 30, 15));
        assertEquals(new Rect(10, 0, 45, 30), u);
        assertEquals(u, new Rect(25, 0, 30, 15).union(new Rect(10, 10, 20, 20)));
        assertEquals(new Rect(1, 2, 3, 4), new Rect(1, 2, 3, 4).union(new Rect(2, 3, 1, 1)));
    }
}