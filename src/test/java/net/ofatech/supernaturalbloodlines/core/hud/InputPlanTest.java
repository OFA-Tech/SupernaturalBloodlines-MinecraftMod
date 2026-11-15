package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.InputPlan.Kind;
import net.ofatech.supernaturalbloodlines.core.hud.InputPlan.Step;
import org.junit.jupiter.api.Test;

/** What a slot key sends to Tensura, as a table. Slots are numbered 0 to 5: 0-2 the left column, 3-5 the right one (preset N+1). */
class InputPlanTest {
    private static final ResourceLocation SKILL = ResourceLocation.parse("tensura:aura_slash");
    private static final int ACTIVE = 4;
    private static final int PRESETS = 9;

    private static Step press(int slot, ResourceLocation skill, int mode, boolean nextDown, boolean prevDown) {
        return InputPlan.forPress(slot, skill, mode, nextDown, prevDown, ACTIVE, PRESETS);
    }

    @Test
    void aLeftPressActivatesWithTheSlotIndexAsKeyNumber() {
        Step s = press(1, SKILL, 2, false, false);
        assertEquals(new Step(Kind.ACTIVATE, SKILL, 1, 2, -1, false), s);
    }

    @Test
    void aRightPressActivatesTheNextPresetsSlotWithTheSameKeyNumber() {
        // slot 3 of the HUD is slot 0 of preset N+1: the key number Tensura checks is the index inside the preset
        assertEquals(new Step(Kind.ACTIVATE, SKILL, 0, 1, -1, false), press(3, SKILL, 1, false, false));
        assertEquals(new Step(Kind.ACTIVATE, SKILL, 2, 0, -1, false), press(5, SKILL, 0, false, false));
    }

    @Test
    void altPlusALeftKeyChangesTheModeOfTheActivePreset() {
        assertEquals(new Step(Kind.CHANGE_MODE, null, 2, 0, -1, false), press(2, SKILL, 0, true, false));
        assertEquals(new Step(Kind.CHANGE_MODE, null, 2, 0, -1, true), press(2, SKILL, 0, false, true));
    }

    @Test
    void altPlusARightKeyChangesTheModeOfTheNextPresetWithItsPresetId() {
        assertEquals(new Step(Kind.CHANGE_MODE, null, 1, 0, ACTIVE + 1, false), press(4, SKILL, 0, true, false));
        assertEquals(new Step(Kind.CHANGE_MODE, null, 1, 0, ACTIVE + 1, true), press(4, SKILL, 0, false, true));
    }

    @Test
    void theRightColumnOfThelastPresetChangesPresetOne() {
        Step s = InputPlan.forPress(3, SKILL, 0, true, false, PRESETS - 1, PRESETS);
        assertEquals(0, s.preset(), "preset 9 pairs with preset 1");
    }

    @Test
    void nextWinsWhenBothModeKeysAreDown() {
        assertEquals(false, press(0, SKILL, 0, true, true).backwards());
    }

    @Test
    void anEmptySlotSendsNothing() {
        assertEquals(Kind.NONE, press(0, null, 0, false, false).kind());
        assertEquals(Kind.NONE, press(4, null, 0, true, false).kind(), "not even a mode change");
    }

    @Test
    void aSlotOutOfRangeSendsNothing() {
        assertEquals(Kind.NONE, press(6, SKILL, 0, false, false).kind());
        assertEquals(Kind.NONE, press(-1, SKILL, 0, false, false).kind());
    }

    @Test
    void releaseCarriesTheKeyNumberAndTheModeTheSkillWasPressedWith() {
        // Tensura sends skillReleasePacket(id, keyNumber, mode); the held ticks are counted by ManasCore on the server
        assertEquals(new Step(Kind.RELEASE, SKILL, 2, 3, -1, false), InputPlan.forRelease(2, SKILL, 3));
        assertEquals(new Step(Kind.RELEASE, SKILL, 1, 0, -1, false), InputPlan.forRelease(4, SKILL, 0), "slot 5 is index 1 of the next preset");
        assertEquals(Kind.NONE, InputPlan.forRelease(4, null, 0).kind(), "nothing was pressed");
    }

    @Test
    void columnsAndRows() {
        assertEquals(0, InputPlan.row(0));
        assertEquals(2, InputPlan.row(2));
        assertEquals(0, InputPlan.row(3));
        assertEquals(2, InputPlan.row(5));
        assertEquals(false, InputPlan.rightColumn(2));
        assertEquals(true, InputPlan.rightColumn(3));
    }
}
