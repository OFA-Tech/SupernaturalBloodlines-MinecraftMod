package net.ofatech.supernaturalbloodlines.core.rite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class RaceLinksTest {
    private static final Path IDS = Path.of("docs/superpowers/research/tensura-addons/ids");

    /** The ids of a research list ({@code id | name} per line). */
    private static List<String> ids(String file) throws IOException {
        return Files.readAllLines(IDS.resolve(file)).stream().map(l -> l.split("\\|")[0].trim()).filter(s -> s.contains(":")).toList();
    }

    @Test
    void theFiveBloodfiendsAreVampires() {
        Set<String> expected = Set.of("ascension:fledgling_bloodfiend", "ascension:kindred_bloodfiend", "ascension:blood_noble", "ascension:elder_bloodfiend",
                "ascension:progenitor_bloodfiend");
        for (String id : expected) {
            assertTrue(RaceLinks.isVampire(id), id);
            assertEquals(Set.of(RiteKind.VAMPIRE), RaceLinks.kindsOfRace(id), id);
        }
        assertEquals(expected, RaceLinks.VAMPIRE_RACES);
    }

    @Test
    void direwolfAndEverySpiritWolfAreWerewolvesAndNothingElseOfMysticismIs() throws IOException {
        Set<String> mapped = new TreeSet<>();
        for (String id : ids("mysticism_races.txt")) {
            boolean expected = id.equals("mysticism:direwolf") || id.endsWith("_spirit_wolf");
            assertEquals(expected, RaceLinks.isWerewolf(id), id);
            if (expected) {
                mapped.add(id);
                assertEquals(Set.of(RiteKind.WEREWOLF), RaceLinks.kindsOfRace(id), id);
            }
        }
        assertEquals(13, mapped.size(), "the direwolf and twelve spirit wolves: " + mapped);
        for (String notLinked : List.of("mysticism:divine_wolf", "mysticism:frost_wolf", "mysticism:guitar_wolf", "mysticism:star_wolf", "mysticism:scorch_wolf",
                "mysticism:tempest_star_wolf", "mysticism:blazing_scorch_wolf", "mysticism:nivalis_frost_wolf", "mysticism:amped_guitar_wolf")) {
            assertFalse(RaceLinks.isWerewolf(notLinked), notLinked + " is not named by the decision");
        }
    }

    @Test
    void noIdOfAnyOtherAddonListIsMapped() throws IOException {
        for (String file : List.of("ascension_races.txt", "tensurahorizons_races.txt", "tensura_kumodesu_races.txt", "stextras_races.txt")) {
            for (String id : ids(file)) {
                boolean bloodfiend = RaceLinks.VAMPIRE_RACES.contains(id);
                assertEquals(bloodfiend, RaceLinks.isVampire(id), id);
                assertFalse(RaceLinks.isWerewolf(id), id + " is not a werewolf race");
                assertEquals(bloodfiend ? Set.of(RiteKind.VAMPIRE) : Set.of(), RaceLinks.kindsOfRace(id), id);
            }
        }
        for (String own : List.of("ascension:kitsune", "ascension:lich", "tensura:vampire_lord", "tensura:spirit_beast", "tensura:direwolf", "tensura:human")) {
            assertEquals(Set.of(), RaceLinks.kindsOfRace(own), own);
        }
    }

    @Test
    void aForeignNamespaceNeverMatchesTheSuffix() {
        assertFalse(RaceLinks.isWerewolf("tensura:magic_spirit_wolf"));
        assertFalse(RaceLinks.isWerewolf("other:magic_spirit_wolf"));
        assertFalse(RaceLinks.isWerewolf("mysticism:_spirit_wolf_x"));
        assertTrue(RaceLinks.isWerewolf("mysticism:magic_spirit_wolf"));
    }

    @Test
    void nothingAndNullAreNothing() {
        assertEquals(Set.of(), RaceLinks.kindsOfRace(""));
        assertEquals(Set.of(), RaceLinks.kindsOfRace(null));
        assertFalse(RaceLinks.isVampire(null));
        assertFalse(RaceLinks.isWerewolf(null));
    }
}
