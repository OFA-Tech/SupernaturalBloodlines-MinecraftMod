package net.ofatech.supernaturalbloodlines.core.supernatural;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class SupernaturalRaceTest {
    private static final String VAMPIRE = "vampirism:vampire";
    private static final String WOLF = "werewolves:werewolf";
    private static final String HUNTER_FACTION = "vampirism:hunter";

    private static BloodlineState with(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    private static void row(BloodlineState s, String faction, SupernaturalRace creature, SupernaturalRace magic) {
        assertEquals(creature, SupernaturalRace.creature(s, faction), "creature of " + s.species() + " in " + faction);
        assertEquals(Optional.ofNullable(magic), SupernaturalRace.magic(s), "magic of " + s.species() + " in " + faction);
    }

    @Test
    void creatureTableRowByRow() {
        row(BloodlineState.NONE.toBuilder().ghoul(true).build(), VAMPIRE, GHOUL, null);
        row(with(Species.ORIGINAL_HYBRID), WOLF, ORIGINAL_HYBRID, null);
        row(with(Species.ORIGINAL), VAMPIRE, ORIGINAL, null);
        row(with(Species.TRIBRID), WOLF, TRIBRID, null);
        row(with(Species.HYBRID), WOLF, HYBRID, null);
        row(with(Species.HERETIC), VAMPIRE, HERETIC, null);
        row(with(Species.POTENTIAL), null, HUNTER, null);
        row(with(Species.HUNTER_OF_THE_FIVE), HUNTER_FACTION, HUNTER, null);
        row(BloodlineState.NONE, HUNTER_FACTION, HUNTER, null);
        row(BloodlineState.NONE, VAMPIRE, SupernaturalRace.VAMPIRE, null);
        row(BloodlineState.NONE, WOLF, WEREWOLF, null);
        row(BloodlineState.NONE, null, HUMAN, null);
        row(BloodlineState.NONE, "somemod:other", HUMAN, null);
    }

    @Test
    void witchWithNoFactionIsHumanAndWitch() {
        row(with(Species.WITCH), null, HUMAN, WITCH);
    }

    @Test
    void witchWerewolfClimbsBoth() {
        row(with(Species.WITCH), WOLF, WEREWOLF, WITCH);
    }

    @Test
    void siphonerWerewolfAndPlainSiphoner() {
        row(with(Species.SIPHONER), WOLF, WEREWOLF, SIPHONER);
        row(with(Species.SIPHONER), null, HUMAN, SIPHONER);
    }

    @Test
    void witchOrSiphonerVampireIsVampireWithMagic() {
        row(with(Species.WITCH), VAMPIRE, SupernaturalRace.VAMPIRE, WITCH);
        row(with(Species.SIPHONER), VAMPIRE, SupernaturalRace.VAMPIRE, SIPHONER);
    }

    @Test
    void tribridAndHereticAreSingleRaces() {
        row(with(Species.TRIBRID), WOLF, TRIBRID, null);
        row(with(Species.HERETIC), VAMPIRE, HERETIC, null);
        // even with the witch layer flag a tribrid has no magic race
        row(BloodlineState.NONE.toBuilder().species(Species.TRIBRID).witchLayer(true).build(), WOLF, TRIBRID, null);
    }

    @Test
    void ghoulWinsOverEverythingForTheCreatureRace() {
        BloodlineState ghoul = BloodlineState.NONE.toBuilder().ghoul(true).species(Species.ORIGINAL).build();
        assertEquals(GHOUL, SupernaturalRace.creature(ghoul, VAMPIRE));
        assertEquals(GHOUL, SupernaturalRace.creature(ghoul, "tensura:vampire_lord"));
    }

    @Test
    void aSpeciesBeatsTheFactionAndHunterBeatsVampire() {
        // a witch hunter is a hunter with a magic race
        row(with(Species.WITCH), HUNTER_FACTION, HUNTER, WITCH);
        // the Five are hunters even if the faction is missing
        row(with(Species.HUNTER_OF_THE_FIVE), null, HUNTER, null);
    }

    @Test
    void idsAreLowercaseNamesAndRoundTrip() {
        for (SupernaturalRace r : values()) {
            assertEquals(r.name().toLowerCase(java.util.Locale.ROOT), r.id());
            assertEquals(Optional.of(r), SupernaturalRace.byId(r.id()));
        }
        assertTrue(SupernaturalRace.byId("dragon").isEmpty());
        assertEquals(12, values().length);
    }
}
