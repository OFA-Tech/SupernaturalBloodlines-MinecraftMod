package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;
import org.junit.jupiter.api.Test;

/** Home's kind chip is {@code PlayerRaces.of}: the table of what it says for each bloodline and faction (plan 23 Task 4). */
class HomeChipTest {
    private static final String VAMPIRE = SupernaturalRace.VAMPIRE_FACTION;
    private static final String HUNTER = SupernaturalRace.HUNTER_FACTION;
    private static final String WEREWOLF = SupernaturalRace.WEREWOLF_FACTION;

    private static BloodlineState of(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    private static List<SupernaturalRace> chip(BloodlineState b, String faction) {
        return PlayerRaces.of(b, faction);
    }

    @Test
    void aLevelOneVampireIsAVampire() {
        assertEquals(List.of(SupernaturalRace.VAMPIRE), chip(BloodlineState.NONE, VAMPIRE), "the owner's case: /vampirism level vampirism:vampire 1");
    }

    @Test
    void theFactionNamesThePlainKinds() {
        assertEquals(List.of(SupernaturalRace.HUMAN), chip(BloodlineState.NONE, null));
        assertEquals(List.of(SupernaturalRace.HUNTER), chip(BloodlineState.NONE, HUNTER));
        assertEquals(List.of(SupernaturalRace.WEREWOLF), chip(BloodlineState.NONE, WEREWOLF));
    }

    @Test
    void aGhoulIsAGhoulWhateverItsFaction() {
        BloodlineState ghoul = BloodlineState.NONE.toBuilder().ghoul(true).build();
        for (String f : new String[] {null, VAMPIRE, HUNTER, WEREWOLF}) {
            assertEquals(List.of(SupernaturalRace.GHOUL), chip(ghoul, f), "faction " + f);
        }
    }

    @Test
    void theSpeciesNameTheirOwnKinds() {
        assertEquals(List.of(SupernaturalRace.HYBRID), chip(of(Species.HYBRID), WEREWOLF));
        assertEquals(List.of(SupernaturalRace.TRIBRID), chip(of(Species.TRIBRID), VAMPIRE));
        assertEquals(List.of(SupernaturalRace.ORIGINAL), chip(of(Species.ORIGINAL), VAMPIRE));
        assertEquals(List.of(SupernaturalRace.HERETIC), chip(of(Species.HERETIC), VAMPIRE));
        assertEquals(List.of(SupernaturalRace.AUGUSTINE), chip(of(Species.AUGUSTINE), VAMPIRE));
        assertEquals(List.of(SupernaturalRace.HUNTER), chip(of(Species.POTENTIAL), null));
    }

    @Test
    void aWitchKeepsHerCreatureRaceUnderIt() {
        assertEquals(List.of(SupernaturalRace.HUMAN, SupernaturalRace.WITCH), chip(of(Species.WITCH), null));
        assertEquals(List.of(SupernaturalRace.VAMPIRE, SupernaturalRace.WITCH), chip(of(Species.WITCH), VAMPIRE));
        assertEquals(List.of(SupernaturalRace.WEREWOLF, SupernaturalRace.SIPHONER), chip(of(Species.SIPHONER), WEREWOLF));
    }

    /** Every race shows for some bloodline and faction: none is unreachable from the chip. */
    @Test
    void everyRaceAppears() {
        Set<SupernaturalRace> seen = EnumSet.noneOf(SupernaturalRace.class);
        for (Species s : Species.values()) {
            for (boolean ghoul : new boolean[] {false, true}) {
                for (String f : new String[] {null, VAMPIRE, HUNTER, WEREWOLF}) {
                    seen.addAll(chip(BloodlineState.NONE.toBuilder().species(s).ghoul(ghoul).build(), f));
                }
            }
        }
        assertEquals(EnumSet.allOf(SupernaturalRace.class), seen);
    }
}
