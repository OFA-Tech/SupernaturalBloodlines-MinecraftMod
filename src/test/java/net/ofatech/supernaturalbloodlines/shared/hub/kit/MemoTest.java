package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MemoTest {
    @Test
    void aSecondFrameWithTheSameInputsReusesTheFirstAnswer() {
        Memo<String, Integer> memo = new Memo<>(8);
        AtomicInteger runs = new AtomicInteger();
        for (int frame = 0; frame < 5; frame++) {
            assertEquals(7, memo.get("same inputs", () -> {
                runs.incrementAndGet();
                return 7;
            }));
        }
        assertEquals(1, runs.get());
        assertEquals(1, memo.misses());
    }

    @Test
    void anotherInputIsAnotherAnswerSoAChangedLanguageOrSizeIsNeverStale() {
        Memo<String, String> memo = new Memo<>(8);
        assertEquals("Learn", memo.get("en", () -> "Learn"));
        assertEquals("Aprender", memo.get("pt", () -> "Aprender"));
        assertEquals("Learn", memo.get("en", () -> "WRONG"));
        assertEquals(2, memo.misses());
    }

    @Test
    void itCannotGrowForeverAndClearForgetsEverything() {
        Memo<Integer, Integer> memo = new Memo<>(4);
        for (int i = 0; i < 100; i++) {
            memo.get(i, () -> 1);
        }
        assertTrue(memo.size() <= 4, "size " + memo.size());
        memo.clear();
        assertEquals(0, memo.size());
    }
}