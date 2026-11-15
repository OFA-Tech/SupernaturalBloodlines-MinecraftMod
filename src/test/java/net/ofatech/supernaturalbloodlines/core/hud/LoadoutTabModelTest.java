package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.LoadoutTabModel.Assignment;
import org.junit.jupiter.api.Test;

class LoadoutTabModelTest {
    private static final ResourceLocation JUMP = ResourceLocation.parse("vampirism:jump_boost");
    private static final ResourceLocation TELEPORT = ResourceLocation.parse("vampirism:teleport");

    @Test
    void anActionThenASlotAssigns() {
        LoadoutTabModel m = new LoadoutTabModel();
        assertTrue(m.pickAction(JUMP).isEmpty());
        assertEquals(JUMP, m.pickedAction());
        assertEquals(Optional.of(new Assignment(2, JUMP)), m.pickSlot(2));
        assertNull(m.pickedAction(), "the selection is spent");
    }

    @Test
    void aSlotThenAnActionAssigns() {
        LoadoutTabModel m = new LoadoutTabModel();
        assertTrue(m.pickSlot(4).isEmpty());
        assertEquals(4, m.pickedSlot());
        assertEquals(Optional.of(new Assignment(4, TELEPORT)), m.pickAction(TELEPORT));
        assertNull(m.pickedSlot());
    }

    @Test
    void pickingTheSameThingTwiceUnpicksIt() {
        LoadoutTabModel m = new LoadoutTabModel();
        m.pickAction(JUMP);
        m.pickAction(JUMP);
        assertNull(m.pickedAction());
        m.pickSlot(1);
        m.pickSlot(1);
        assertNull(m.pickedSlot());
    }

    @Test
    void pickingAnotherActionReplacesThePick() {
        LoadoutTabModel m = new LoadoutTabModel();
        m.pickAction(JUMP);
        m.pickAction(TELEPORT);
        assertEquals(TELEPORT, m.pickedAction());
        assertEquals(Optional.of(new Assignment(0, TELEPORT)), m.pickSlot(0));
    }

    @Test
    void clearingASlotSendsAnEmptyIdAndDropsTheSelection() {
        LoadoutTabModel m = new LoadoutTabModel();
        m.pickAction(JUMP);
        Optional<Assignment> cleared = m.clearSlot(3);
        assertEquals(Optional.of(new Assignment(3, null)), cleared);
        assertNull(m.pickedAction());
    }

    @Test
    void aSlotThatDoesNotExistIsIgnored() {
        LoadoutTabModel m = new LoadoutTabModel();
        assertTrue(m.pickSlot(6).isEmpty());
        assertTrue(m.clearSlot(-1).isEmpty());
        assertNull(m.pickedSlot());
    }
}
