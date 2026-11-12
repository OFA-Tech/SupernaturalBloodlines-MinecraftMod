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
    void humanMovesToVampire() {
        assertEquals(Optional.of("tensura:vampire"), VampireMapping.raceOnJoin("tensura:human"));
    }

    @Test
    void elfKeepsRace() {
        assertTrue(VampireMapping.raceOnJoin("tensura:elf").isEmpty());
    }

    @Test
    void nullRaceKeepsRace() {
        assertTrue(VampireMapping.raceOnJoin(null).isEmpty());
    }

    @Test
    void vampireLineKeepsRace() {
        assertTrue(VampireMapping.raceOnJoin("tensura:ghoul").isEmpty());
        assertTrue(VampireMapping.raceOnJoin("tensura:vampire").isEmpty());
        assertTrue(VampireMapping.raceOnJoin("tensura:vampire_lord").isEmpty());
    }

    @Test
    void overMaxFloorIsClampedToMax() {
        VampireMapping p = VampireMapping.parse("tensura:human", List.of("x=99"), MAX).mapping();
        assertEquals(OptionalInt.of(MAX), p.levelForRace("x", 3));
    }

    @Test
    void currentAboveMaxIsNotLowered() {
        assertEquals(OptionalInt.of(20), m.levelForRace("tensura:vampire_lord", 20));
        assertEquals(OptionalInt.of(20), VampireMapping.defaults(14).levelForRace("tensura:vampire_lord", 20));
    }

    @Test
    void parseRejectsBadEntriesAndMapsMinusOneToMax() {
        VampireMapping.Parsed parsed = VampireMapping.parse(
                "my:cure", List.of("a:b=3", "a:c=-1", "junk", "a:d=x", "a:e=0", "=4", "a:f=-2"), MAX);
        VampireMapping p = parsed.mapping();
        assertEquals(List.of("junk", "a:d=x", "a:e=0", "=4", "a:f=-2"), parsed.rejected());
        assertEquals("my:cure", p.cureRace());
        assertEquals(Optional.of("my:cure"), p.raceOnCure("a:b"));
        assertEquals(OptionalInt.of(3), p.levelForRace("a:b", 0));
        assertEquals(OptionalInt.of(MAX), p.levelForRace("a:c", 0));
        assertTrue(p.levelForRace("junk", 0).isEmpty());
        assertTrue(p.levelForRace("", 0).isEmpty());
    }

    @Test
    void defaultsRejectNothing() {
        assertTrue(VampireMapping.parse(VampireMapping.DEFAULT_CURE_RACE, VampireMapping.DEFAULT_FLOORS, MAX)
                .rejected().isEmpty());
    }
}
