package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DamageTallyTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void nobodyWhenNothingWasDealt() {
        DamageTally t = new DamageTally();
        assertEquals(Optional.empty(), t.top());
        t.add(A, 0F);
        t.add(A, -3F);
        assertTrue(t.isEmpty() && t.top().isEmpty(), "a blow that did no damage counts for nothing");
    }

    @Test
    void theMostDamageWinsWhateverTheOrder() {
        DamageTally t = new DamageTally();
        t.add(A, 10F);
        t.add(B, 6F);
        t.add(B, 3F);
        assertEquals(A, t.top().orElseThrow(), "10 beats 9");
        t.add(B, 2F);
        assertEquals(B, t.top().orElseThrow(), "11 beats 10");
    }

    @Test
    void aTieGoesToWhoeverHitLast() {
        DamageTally t = new DamageTally();
        t.add(A, 5F);
        t.add(B, 5F);
        assertEquals(B, t.top().orElseThrow(), "B hit last");
        t.add(A, 0.0001F);
        t.add(C, 4F);
        t.add(C, 1F);
        // A has 5.0001: A wins outright; control for the tie rule: B and C at 5, C last
        t = new DamageTally();
        t.add(B, 5F);
        t.add(C, 4F);
        t.add(C, 1F);
        assertEquals(C, t.top().orElseThrow(), "B and C are tied at 5 and C hit last");
    }

    @Test
    void aSavedTallyComesBackWithTheSameWinnerAndKeepsCounting() {
        DamageTally t = new DamageTally();
        t.add(A, 5F);
        t.add(B, 5F);
        DamageTally back = new DamageTally();
        t.entries().forEach((id, e) -> back.put(id, e.amount(), e.seq()));
        assertEquals(t.top(), back.top());
        back.add(A, 5F);
        assertEquals(A, back.top().orElseThrow(), "the order goes on after a load, so a new blow is after the old ones");
        DamageTally tie = new DamageTally();
        tie.put(A, 5F, 7);
        tie.put(B, 5F, 3);
        assertEquals(A, tie.top().orElseThrow(), "the higher order is the later hit");
        tie.add(B, 0.5F);
        assertEquals(B, tie.top().orElseThrow());
    }
}