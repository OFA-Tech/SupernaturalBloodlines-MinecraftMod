package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PainTest {
    @Test
    void hurtNeverTakesTheLastHeart() {
        assertEquals(8.0f, Pain.left(20.0f, 12.0f));
        assertEquals(1.0f, Pain.left(5.0f, 12.0f));
        assertEquals(1.0f, Pain.left(1.0f, 100.0f));
        assertEquals(20.0f, Pain.left(20.0f, -3.0f));
    }
}