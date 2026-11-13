package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class PackAccessTest {
    private static final PackEvent[] LINEAGE_GIVING = {
            PackEvent.GENE_AWAKENED, PackEvent.RITE_OF_LINEAGE_AWAKEN, PackEvent.BITTEN_BY_ALPHA, PackEvent.BITTEN_BY_WILD};

    @Test
    void humansAndWerewolvesMayGainALineage() {
        for (PackEvent e : LINEAGE_GIVING) {
            for (Species s : new Species[]{Species.NONE, Species.WITCH, Species.SIPHONER, Species.HYBRID}) {
                assertNull(PackAccess.blocked(BaseFaction.NONE, s, e), e + " " + s);
            }
            assertNull(PackAccess.blocked(BaseFaction.WEREWOLF, Species.NONE, e), e.toString());
        }
    }

    @Test
    void vampiresAndHuntersNeverGainABittenOrBornLineage() {
        for (PackEvent e : LINEAGE_GIVING) {
            assertEquals("wolf_wrong_faction", PackAccess.blocked(BaseFaction.VAMPIRE, Species.NONE, e), e.toString());
            assertEquals("wolf_wrong_faction", PackAccess.blocked(BaseFaction.HUNTER, Species.NONE, e), e.toString());
        }
    }

    @Test
    void originalsHereticsAndTheFiveAreLeftToTheBloodlineRules() {
        for (PackEvent e : LINEAGE_GIVING) {
            for (Species s : new Species[]{Species.ORIGINAL, Species.HERETIC, Species.POTENTIAL, Species.HUNTER_OF_THE_FIVE}) {
                assertEquals("wolf_wrong_species", PackAccess.blocked(BaseFaction.NONE, s, e), e + " " + s);
            }
        }
    }

    @Test
    void otherPackEventsAreNotGated() {
        assertNull(PackAccess.blocked(BaseFaction.VAMPIRE, Species.NONE, PackEvent.LEFT_PACK));
        assertNull(PackAccess.blocked(BaseFaction.HUNTER, Species.HUNTER_OF_THE_FIVE, PackEvent.KILLED_INNOCENT));
    }
}
