package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Set;
import org.junit.jupiter.api.Test;

class BossBarFormsTest {
    @Test
    void onlyTheFourBossesWithFormsHaveChipsAndTheOthersHaveNone() {
        assertEquals(Set.SHIFTER, BossBarForms.setOf(BossId.KLAUS));
        assertEquals(Set.SHIFTER, BossBarForms.setOf(BossId.DEUCALION));
        assertEquals(Set.SHIFTER, BossBarForms.setOf(BossId.PETER_HALE));
        assertEquals(Set.HOST, BossBarForms.setOf(BossId.BEAST_OF_GEVAUDAN));
        for (BossId one : new BossId[] {BossId.MIKAEL, BossId.ELIJAH, BossId.KOL, BossId.REBEKAH, BossId.FINN}) {
            assertEquals(Set.NONE, BossBarForms.setOf(one), one.toString());
        }
    }

    @Test
    void aShiftersFormIsTheChipThatIsLit() {
        assertEquals(0, BossBarForms.shifter(BossForm.HUMAN).active());
        assertEquals(1, BossBarForms.shifter(BossForm.SHIFTING).active());
        assertEquals(2, BossBarForms.shifter(BossForm.BEAST).active());
        assertTrue(BossBarForms.shifter(BossForm.SHIFTING).shifting());
        assertFalse(BossBarForms.shifter(BossForm.HUMAN).shifting());
        assertFalse(BossBarForms.shifter(BossForm.BEAST).shifting());
        assertFalse(BossBarForms.host(1).shifting(), "the Beast's middle chip is not a shift");
    }

    @Test
    void theChipsAreNamedInOrderAndTheBeastsChipIsTheSameWordInBothRows() {
        assertEquals("supernaturalbloodlines.boss.bar.form.human", Set.SHIFTER.chipKey(0));
        assertEquals("supernaturalbloodlines.boss.bar.form.shifting", Set.SHIFTER.chipKey(1));
        assertEquals("supernaturalbloodlines.boss.bar.form.beast", Set.SHIFTER.chipKey(2));
        assertEquals("supernaturalbloodlines.boss.bar.form.host", Set.HOST.chipKey(0));
        assertEquals("supernaturalbloodlines.boss.bar.form.beast", Set.HOST.chipKey(1));
        assertEquals("supernaturalbloodlines.boss.bar.form.spirit", Set.HOST.chipKey(2));
        assertEquals(0, Set.NONE.chips());
        assertEquals(3, Set.SHIFTER.chips());
    }

    @Test
    void anActiveChipOutsideTheRowIsClampedAndABossWithoutChipsLightsNone() {
        assertEquals(2, new Look(Set.SHIFTER, 9).active());
        assertEquals(0, new Look(Set.SHIFTER, -4).active());
        assertEquals(0, new Look(Set.NONE, 2).active());
    }

    @Test
    void aLookReadFromTheWireThatDoesNotBelongToTheBossIsNoChips() {
        assertEquals(new Look(Set.SHIFTER, 1), BossBarForms.checked(BossId.KLAUS, Set.SHIFTER.ordinal(), 1));
        assertEquals(Look.NONE, BossBarForms.checked(BossId.MIKAEL, Set.SHIFTER.ordinal(), 1), "Mikael does not shift");
        assertEquals(Look.NONE, BossBarForms.checked(BossId.KLAUS, Set.HOST.ordinal(), 1), "Klaus is not the Beast");
        assertEquals(Look.NONE, BossBarForms.checked(BossId.KLAUS, 77, 1));
        assertEquals(Look.NONE, BossBarForms.checked(BossId.KLAUS, -1, 1));
    }
}
