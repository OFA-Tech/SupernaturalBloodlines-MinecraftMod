package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LyingBodyTest {
    @Test
    void theBoxIsLowAndAsWideAsTheBodyIsLong() {
        assertTrue(LyingBody.HEIGHT < 0.8F, "the box is as tall as " + LyingBody.HEIGHT);
        assertEquals(1.8F, LyingBody.LENGTH, "the player model is 1.8 from head to heel");
        assertTrue(LyingBody.EYE_HEIGHT < LyingBody.HEIGHT);
    }
}
