package net.ofatech.supernaturalbloodlines.core.rite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RiteUnlockTableTest {
    @Test
    void theNumbersOfThePlan() {
        assertEquals(8, RiteUnlockTable.OPEN.size());
        assertEquals(23, RiteUnlockTable.GATED.size());
        assertEquals(13, RiteUnlockTable.GATED.values().stream().filter(g -> g.lore().isEmpty()).count(), "kind only");
        assertEquals(7, RiteUnlockTable.GATED.values().stream().filter(g -> g.kinds().isEmpty()).count(), "lore only");
        assertEquals(3, RiteUnlockTable.GATED.values().stream().filter(g -> !g.kinds().isEmpty() && !g.lore().isEmpty()).count(), "kind or lore");
    }

    @Test
    void aRiteIsOpenOrGatedNeverBoth() {
        for (String open : RiteUnlockTable.OPEN) {
            assertFalse(RiteUnlockTable.isGated(open), open);
        }
        assertEquals(RiteUnlockTable.OPEN.size(), Set.copyOf(RiteUnlockTable.OPEN).size(), "an open rite twice");
    }

    @Test
    void everyGatedRiteHasASource() {
        RiteUnlockTable.GATED.forEach((rite, gate) -> assertTrue(!gate.kinds().isEmpty() || !gate.lore().isEmpty(), rite + " can never be learned"));
    }

    @Test
    void anOpenRiteIsKnownToAnyone() {
        for (String open : RiteUnlockTable.OPEN) {
            assertTrue(RiteUnlockTable.knows(open, Set.of(), Set.of()), open);
        }
    }

    @Test
    void aKindKnowsItsRitesAndNoOthers() {
        assertTrue(RiteUnlockTable.knows("alpha_trial", Set.of(RiteKind.WEREWOLF), Set.of()));
        assertFalse(RiteUnlockTable.knows("alpha_trial", Set.of(RiteKind.VAMPIRE, RiteKind.WITCH), Set.of()));
        assertFalse(RiteUnlockTable.knows("alpha_trial", Set.of(), Set.of()));
        assertFalse(RiteUnlockTable.knows("no_such_rite", Set.of(RiteKind.values()), Set.of("kol", "esther", "finn", "elijah", "old_rites", "curses")));
    }

    @Test
    void loreAloneTeachesALoreRite() {
        assertFalse(RiteUnlockTable.knows("rite_of_cloaking", Set.of(RiteKind.values()), Set.of()), "no kind knows Cloaking");
        assertTrue(RiteUnlockTable.knows("rite_of_cloaking", Set.of(), Set.of(RiteUnlockTable.KOL)));
        assertFalse(RiteUnlockTable.knows("rite_of_cloaking", Set.of(), Set.of(RiteUnlockTable.ESTHER)));
    }

    @Test
    void kindOrLoreIsEither() {
        assertTrue(RiteUnlockTable.knows("rite_of_the_cure", Set.of(RiteKind.WITCH), Set.of()));
        assertTrue(RiteUnlockTable.knows("rite_of_the_cure", Set.of(), Set.of(RiteUnlockTable.ELIJAH)));
        assertFalse(RiteUnlockTable.knows("rite_of_the_cure", Set.of(RiteKind.SIPHONER), Set.of(RiteUnlockTable.FINN)));
    }

    @Test
    void whatEachLoreSourceTeaches() {
        assertEquals(List.of("rite_of_cloaking"), RiteUnlockTable.taughtBy(RiteUnlockTable.KOL));
        assertEquals(List.of("rite_of_linking", "rite_of_severance"), RiteUnlockTable.taughtBy(RiteUnlockTable.ESTHER));
        assertEquals(List.of("rite_of_the_broken_line", "rite_of_the_imprisoned_original"), RiteUnlockTable.taughtBy(RiteUnlockTable.FINN));
        assertEquals(List.of("rite_of_the_cure"), RiteUnlockTable.taughtBy(RiteUnlockTable.ELIJAH));
        assertEquals(List.of("immortality", "ward_of_return"), RiteUnlockTable.taughtBy(RiteUnlockTable.OLD_RITES));
        assertEquals(List.of("curse_magicule_drain", "curse_skill_seal"), RiteUnlockTable.taughtBy(RiteUnlockTable.CURSES));
        assertEquals(List.of(), RiteUnlockTable.taughtBy("nothing"));
    }

    @Test
    void everyLoreIdTeachesSomething() {
        for (String lore : RiteUnlockTable.loreIds()) {
            assertFalse(RiteUnlockTable.taughtBy(lore).isEmpty(), lore);
        }
        assertEquals(Set.of("kol", "esther", "finn", "elijah", "old_rites", "curses"), RiteUnlockTable.loreIds());
    }

    @Test
    void thePagesAreTheLoreThatIsNotAGrimoire() {
        assertFalse(RiteUnlockTable.isPage(RiteUnlockTable.KOL));
        assertFalse(RiteUnlockTable.isPage(RiteUnlockTable.ESTHER));
        for (String page : List.of("finn", "elijah", "old_rites", "curses")) {
            assertTrue(RiteUnlockTable.isPage(page), page);
        }
    }

    @Test
    void knownByKindsIsTheKindsRites() {
        assertEquals(Set.of(), RiteUnlockTable.knownByKinds(Set.of()));
        assertTrue(RiteUnlockTable.knownByKinds(Set.of(RiteKind.WITCH)).containsAll(Set.of("rite_of_severance", "ward_of_return", "rite_of_the_cure")));
        assertFalse(RiteUnlockTable.knownByKinds(Set.of(RiteKind.WITCH)).contains("rite_of_cloaking"));
        assertNotNull(RiteUnlockTable.GATED.get("silver_moon"));
        assertNull(RiteUnlockTable.GATED.get("purification"));
    }

    @Test
    void theChoicesAreAllEachGatedRiteAndEachLore() {
        List<String> choices = RiteUnlockTable.choices();
        assertEquals("all", choices.get(0));
        assertTrue(choices.containsAll(RiteUnlockTable.GATED.keySet()), "a gated rite is not offered");
        assertTrue(choices.containsAll(RiteUnlockTable.loreIds()), "a lore id is not offered");
        assertEquals(1 + RiteUnlockTable.GATED.size() + RiteUnlockTable.loreIds().size(), choices.size());
        assertFalse(choices.contains("purification"), "an open rite is not a choice: it cannot be learned or forgotten");
    }
}
