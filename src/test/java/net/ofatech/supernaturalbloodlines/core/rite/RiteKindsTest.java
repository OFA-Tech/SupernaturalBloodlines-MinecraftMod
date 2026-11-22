package net.ofatech.supernaturalbloodlines.core.rite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class RiteKindsTest {
    @Test
    void aHumanWithNothingIsNoKind() {
        assertEquals(Set.of(), RiteKinds.of(Species.NONE, false, false, false));
    }

    @Test
    void theFactionsAreKinds() {
        assertEquals(Set.of(RiteKind.VAMPIRE), RiteKinds.of(Species.NONE, true, false, false));
        assertEquals(Set.of(RiteKind.WEREWOLF), RiteKinds.of(Species.NONE, false, true, false));
        assertEquals(Set.of(RiteKind.HUNTER), RiteKinds.of(Species.NONE, false, false, true));
    }

    @Test
    void anOriginalIsAVampireAndAnOriginal() {
        assertEquals(Set.of(RiteKind.ORIGINAL, RiteKind.VAMPIRE), RiteKinds.of(Species.ORIGINAL, false, false, false));
    }

    @Test
    void anOriginalHybridIsEverythingButAWitch() {
        assertEquals(Set.of(RiteKind.ORIGINAL, RiteKind.HYBRID, RiteKind.VAMPIRE, RiteKind.WEREWOLF), RiteKinds.of(Species.ORIGINAL_HYBRID, false, false, false));
    }

    @Test
    void hybridsAndTribridsRunWithTheWolf() {
        assertEquals(Set.of(RiteKind.HYBRID, RiteKind.WEREWOLF), RiteKinds.of(Species.HYBRID, false, false, false));
        assertEquals(Set.of(RiteKind.TRIBRID, RiteKind.HYBRID, RiteKind.WEREWOLF), RiteKinds.of(Species.TRIBRID, false, false, false));
    }

    @Test
    void witchesSiphonersAndHunters() {
        assertEquals(Set.of(RiteKind.WITCH), RiteKinds.of(Species.WITCH, false, false, false));
        assertEquals(Set.of(RiteKind.SIPHONER), RiteKinds.of(Species.SIPHONER, false, false, false));
        assertEquals(Set.of(RiteKind.HUNTER), RiteKinds.of(Species.HUNTER_OF_THE_FIVE, false, false, false));
        assertEquals(Set.of(RiteKind.VAMPIRE), RiteKinds.of(Species.HERETIC, false, false, false));
        assertEquals(Set.of(RiteKind.VAMPIRE), RiteKinds.of(Species.AUGUSTINE, false, false, false), "an Augustine is a vampire for the rites");
    }

    @Test
    void aFactionAddsToTheSpecies() {
        Set<RiteKind> kinds = RiteKinds.of(Species.WITCH, true, true, false);
        assertTrue(kinds.containsAll(Set.of(RiteKind.WITCH, RiteKind.VAMPIRE, RiteKind.WEREWOLF)));
        assertFalse(kinds.contains(RiteKind.HUNTER));
    }
}
