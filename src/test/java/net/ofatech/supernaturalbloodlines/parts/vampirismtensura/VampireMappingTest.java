package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class VampireMappingTest {
    private static final int MAX = 14;
    private final VampireMapping m = VampireMapping.defaults(MAX);

    @Test
    void vampireRaceGivesLevelOne() {
        assertEquals(OptionalInt.of(1), m.levelForRace("tensura:vampire", 0));
    }

    @Test
    void floorNeverLowersLevel() {
        assertEquals(OptionalInt.of(12), m.levelForRace("tensura:vampire_lord", 12));
    }

    @Test
    void lordFloorSeven() {
        assertEquals(OptionalInt.of(7), m.levelForRace("tensura:vampire_lord", 3));
        assertEquals(OptionalInt.of(7), m.levelForRace("tensura:vampire_overcomer", 3));
    }

    @Test
    void divineIsMax() {
        assertEquals(OptionalInt.of(MAX), m.levelForRace("tensura:divine_vampire", 2));
    }

    @Test
    void ghoulAndHumanGiveEmpty() {
        assertTrue(m.levelForRace("tensura:ghoul", 5).isEmpty());
        assertTrue(m.levelForRace("tensura:human", 5).isEmpty());
    }

    @Test
    void cureOnlyResetsVampireLine() {
        assertEquals(Optional.of("tensura:human"), m.raceOnCure("tensura:vampire_lord"));
        assertEquals(Optional.of("tensura:human"), m.raceOnCure("tensura:ghoul"));
        assertTrue(m.raceOnCure("supernaturalbloodlines:hybrid").isEmpty());
        assertTrue(m.raceOnCure(null).isEmpty());
    }

    @Test
    void parseSkipsBadEntriesAndMapsMinusOneToMax() {
        VampireMapping p = VampireMapping.parse(
                "tensura:human", List.of("a:b=3", "a:c=-1", "junk", "a:d=x", "a:e=0", "=4"), MAX);
        assertEquals(OptionalInt.of(3), p.levelForRace("a:b", 0));
        assertEquals(OptionalInt.of(MAX), p.levelForRace("a:c", 0));
        assertTrue(p.levelForRace("a:d", 0).isEmpty());
        assertTrue(p.levelForRace("a:e", 0).isEmpty());
    }
}
