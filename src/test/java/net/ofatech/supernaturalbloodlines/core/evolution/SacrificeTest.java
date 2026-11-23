package net.ofatech.supernaturalbloodlines.core.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SacrificeTest {
    private static Map<String, Integer> m(Object... kv) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return out;
    }

    @Test
    void parsesIdAndCount() {
        assertEquals(Map.entry("minecraft:diamond_block", 5), Sacrifice.parseLine("minecraft:diamond_block:5").orElseThrow());
        assertEquals(Map.entry("a:b", 1), Sacrifice.parseLine("  a:b:1 ").orElseThrow());
    }

    @Test
    void refusesWhatIsNotALine() {
        for (String bad : List.of("", "diamond:5", "minecraft:diamond_block", "minecraft:diamond_block:0", "minecraft:diamond_block:-2", "minecraft:diamond_block:five",
                "minecraft:diamond block:5", "minecraft:x:99999", ":x:5", "a::5")) {
            assertTrue(Sacrifice.parseLine(bad).isEmpty(), bad);
        }
    }

    @Test
    void aBadLineIsSkippedAndTheFirstOfAKindWins() {
        assertEquals(m("a:b", 3, "c:d", 2), Sacrifice.parse(List.of("a:b:3", "junk", "a:b:9", "c:d:2")));
    }

    @Test
    void enoughWhenWhatEnchantedTookPlusWhatLiesLooseCoversIt() {
        var plan = Sacrifice.plan(m("a:b", 5), m("a:b", 1), m("a:b", 4));
        assertTrue(plan.enough());
        assertEquals(m("a:b", 4), plan.take());
        assertTrue(plan.giveBack().isEmpty());
    }

    @Test
    void notEnoughTakesWhatThereIsAndSaysSo() {
        var plan = Sacrifice.plan(m("a:b", 5, "c:d", 1), m("a:b", 1, "c:d", 1), m("a:b", 3));
        assertFalse(plan.enough());
        assertEquals(m("a:b", 3), plan.take());
    }

    @Test
    void whatEnchantedTookBeyondTheSacrificeGoesBack() {
        var plan = Sacrifice.plan(m("a:b", 2), m("a:b", 64), m());
        assertTrue(plan.enough());
        assertEquals(m("a:b", 62), plan.giveBack());
        assertTrue(plan.take().isEmpty());
    }

    @Test
    void aKindEnchantedNeverSawIsTakenWhole() {
        var plan = Sacrifice.plan(m("a:b", 3), m(), m("a:b", 3));
        assertTrue(plan.enough());
        assertEquals(m("a:b", 3), plan.take());
    }

    @Test
    void anEmptySacrificeIsAlwaysEnough() {
        assertTrue(Sacrifice.plan(m(), m("x:y", 4), m()).enough());
    }
}
