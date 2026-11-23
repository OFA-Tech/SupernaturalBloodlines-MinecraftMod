package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RulesTest {
    // ActRulesTest
    @Test
    void everyCombinationMatchesTheOldInlineChecks() {
        for (int bits = 0; bits < 16; bits++) {
            boolean blocked = (bits & 1) != 0;
            boolean spectator = (bits & 2) != 0;
            boolean alive = (bits & 4) != 0;
            boolean sleeping = (bits & 8) != 0;
            assertEquals(blocked || spectator || !alive || sleeping, ActRules.cannotAct(blocked, spectator, alive, sleeping), "bits " + bits);
        }
    }

    @Test
    void theBlockedFlagAloneStopsAnAliveAwakePlayer() {
        assertTrue(ActRules.cannotAct(true, false, true, false));
        assertFalse(ActRules.cannotAct(false, false, true, false));
    }

    // WheelRulesTest
    @Test
    void stoneIsNeverSlottable() {
        assertEquals(WheelRules.SlotTarget.NONE, WheelRules.slotTarget(PowerSource.INFINITY, true));
        assertEquals(WheelRules.SlotTarget.NONE, WheelRules.slotTarget(PowerSource.GREAT_SAGE, true));
    }

    @Test
    void tensuraNotSlottableIsNone() {
        assertEquals(WheelRules.SlotTarget.NONE, WheelRules.slotTarget(PowerSource.TENSURA, false));
        assertEquals(WheelRules.SlotTarget.TENSURA_PRESET, WheelRules.slotTarget(PowerSource.TENSURA, true));
    }

    @Test
    void ourOwnAndTheFactionsGoToTheLoadout() {
        for (PowerSource s : new PowerSource[] {PowerSource.BLOODLINE, PowerSource.VAMPIRISM, PowerSource.WEREWOLVES}) {
            assertEquals(WheelRules.SlotTarget.LOADOUT, WheelRules.slotTarget(s, false), s.name());
        }
    }

    // SlotPlanTest
    @Test
    void leftColumnIsActivePreset() {
        assertEquals(new SlotPlan.TensuraSlot(3, 0), SlotPlan.tensura(0, 3, 9));
        assertEquals(new SlotPlan.TensuraSlot(3, 2), SlotPlan.tensura(2, 3, 9));
    }

    @Test
    void rightColumnIsNextPreset() {
        assertEquals(new SlotPlan.TensuraSlot(4, 0), SlotPlan.tensura(3, 3, 9));
        assertEquals(new SlotPlan.TensuraSlot(4, 2), SlotPlan.tensura(5, 3, 9));
    }

    @Test
    void lastPresetPairsWithFirst() {
        assertEquals(new SlotPlan.TensuraSlot(0, 2), SlotPlan.tensura(5, 8, 9));
    }

    @Test
    void outOfRangePresetWraps() {
        assertEquals(new SlotPlan.TensuraSlot(3, 0), SlotPlan.tensura(0, 12, 9));
        assertEquals(new SlotPlan.TensuraSlot(4, 0), SlotPlan.tensura(3, 12, 9));
        assertEquals(new SlotPlan.TensuraSlot(0, 1), SlotPlan.tensura(1, -9, 9));
    }

    @Test
    void aHudSlotOutsideTheSixIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> SlotPlan.tensura(6, 0, 9));
        assertThrows(IllegalArgumentException.class, () -> SlotPlan.tensura(-1, 0, 9));
    }

    // ReplacedPowersTest
    @Test
    void leapHiddenOnlyWhileSprintLeapIsOn() {
        PowerRef leap = new PowerRef(PowerSource.WEREWOLVES, ResourceLocation.parse("werewolves:leap"));
        assertTrue(ReplacedPowers.hidden(leap, true));
        assertFalse(ReplacedPowers.hidden(leap, false));
    }

    @Test
    void nothingElseIsHidden() {
        assertFalse(ReplacedPowers.hidden(new PowerRef(PowerSource.WEREWOLVES, ResourceLocation.parse("werewolves:rage")), true));
        assertFalse(ReplacedPowers.hidden(new PowerRef(PowerSource.VAMPIRISM, ResourceLocation.parse("werewolves:leap")), true));
        assertFalse(ReplacedPowers.hidden(new PowerRef(PowerSource.TENSURA, ResourceLocation.parse("tensura:coercion")), true));
    }
}
