package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer;
import org.junit.jupiter.api.Test;

class WheelHudViewTest {
    @Test
    void everyLayerIsHiddenWhileTheWheelIsOpenExceptTheWorldsOverlays() {
        for (String id : new String[] {"minecraft:hotbar", "minecraft:player_health", "minecraft:chat", "minecraft:crosshair", "minecraft:boss_overlay", "minecraft:effects",
                "supernaturalbloodlines:ability_hud", "tensura:status", "vampirism:blood", "somemod:added_later"}) {
            assertTrue(WheelHud.hides(true, id), id);
        }
        assertFalse(WheelHud.hides(true, "minecraft:camera_overlays"));
        assertFalse(WheelHud.hides(true, "minecraft:debug_overlay"));
    }

    @Test
    void nothingIsHiddenWhileTheWheelIsClosed() {
        for (String id : new String[] {"minecraft:hotbar", "supernaturalbloodlines:ability_hud", "tensura:status", "minecraft:camera_overlays"}) {
            assertFalse(WheelHud.hides(false, id), id);
        }
    }

    @Test
    void descriptionGetsWhatIsLeftAfterTheModeLineAndTheLockBox() {
        assertEquals(4, WheelPanelText.descriptionLines(50, 130, 0, false));
        assertEquals(4, WheelPanelText.descriptionLines(50, 130, 2, false));
        assertEquals(1, WheelPanelText.descriptionLines(50, 130, 2, true), "80 - 20 - 42 - 2 = 16: one line");
        assertEquals(0, WheelPanelText.descriptionLines(90, 130, 2, true));
        assertEquals(0, WheelPanelText.descriptionLines(130, 100, 0, false), "no room, no lines, never a negative");
    }

    @Test
    void theHubAndTheWheelShareOneRequestToTheServer() {
        WheelViewState s = new WheelViewState();
        assertEquals(Optional.of(true), s.set(Viewer.HUB, true), "the hub opens: ask for the catalogue");
        assertEquals(Optional.empty(), s.set(Viewer.HUB, true), "set up again (a resize): nothing sent twice");
        assertEquals(Optional.empty(), s.set(Viewer.WHEEL, true), "already watching");
        assertEquals(Optional.empty(), s.set(Viewer.HUB, false), "the wheel still needs it");
        assertTrue(s.any());
        assertEquals(Optional.of(false), s.set(Viewer.WHEEL, false), "the last one stops it");
        assertEquals(Optional.empty(), s.set(Viewer.WHEEL, false), "a second close sends nothing");
        s.set(Viewer.HUB, true);
        s.clear();
        assertFalse(s.any());
        assertEquals(Optional.of(true), s.set(Viewer.HUB, true), "after a new world the hub asks again");
    }

    @Test
    void anotherModsSwitchIsLoweredOnceAndGivenBackAsItWas() {
        LowerWhileOpen l = new LowerWhileOpen();
        assertEquals(Optional.of(false), l.open(true, false));
        assertEquals(Optional.empty(), l.open(false, false), "a second open writes nothing and keeps what was saved");
        assertEquals(Optional.of(true), l.close(false, false), "the value it had");
        assertEquals(Optional.empty(), l.close(true, false), "a second close writes nothing");
        assertFalse(l.isLowered());
    }

    @Test
    void aSwitchThatWasAlreadyOffStaysOff() {
        LowerWhileOpen l = new LowerWhileOpen();
        assertEquals(Optional.empty(), l.open(false, false));
        assertEquals(Optional.empty(), l.close(false, false));
    }

    @Test
    void aValueTheOtherModChangedMeanwhileIsNotOverwritten() {
        LowerWhileOpen l = new LowerWhileOpen();
        l.open(true, false);
        assertEquals(Optional.empty(), l.close(true, false), "it was turned back on by someone else: nothing to restore");
        assertEquals(Optional.empty(), new LowerWhileOpen().close(true, true), "a close without an open writes nothing, whatever it would lower to");
    }}
