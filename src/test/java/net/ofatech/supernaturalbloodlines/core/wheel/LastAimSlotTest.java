package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker.Send;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker.SendLoadout;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker.SendNothing;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker.SendTensura;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Task 4 (last power, aim mode) and task 5 (the slot picker's plan) of W4. */
class LastAimSlotTest {
    private static final PowerRef COERCION = new PowerRef(PowerSource.TENSURA, ResourceLocation.parse("tensura:coercion"));

    private static LastPower last(Path dir, boolean remember) {
        return new LastPower(dir.resolve("wheel-last.json"), () -> 1000L, m -> { }, () -> remember);
    }

    @Test
    void lastPowerSurvivesAReload(@TempDir Path dir) {
        last(dir, true).set(COERCION, 2);
        Optional<LastPower.Last> again = last(dir, true).get();
        assertEquals(Optional.of(new LastPower.Last(COERCION, 2)), again);
    }

    @Test
    void corruptLastPowerFileMeansNone(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("wheel-last.json");
        Files.writeString(f, "{ not json");
        List<String> warned = new ArrayList<>();
        LastPower l = new LastPower(f, () -> 5L, warned::add, () -> true);
        assertTrue(l.get().isEmpty());
        assertEquals(1, warned.size());
        assertFalse(Files.exists(f), "the bad file is set aside, not overwritten");
        // a file with a name that is no power, and one with a bad mode
        Files.writeString(f, "{\"power\":\"nonsense\",\"mode\":\"1\"}");
        assertTrue(new LastPower(f, () -> 5L, m -> { }, () -> true).get().isEmpty());
        Files.writeString(f, "{\"power\":\"" + COERCION.key() + "\",\"mode\":\"x\"}");
        assertEquals(0, new LastPower(f, () -> 5L, m -> { }, () -> true).get().orElseThrow().mode());
    }

    @Test
    void theSamePowerAgainWritesNoFile(@TempDir Path dir) throws IOException {
        LastPower l = last(dir, true);
        l.set(COERCION, 1);
        Path f = dir.resolve("wheel-last.json");
        assertTrue(Files.exists(f));
        Files.delete(f);
        l.set(COERCION, 1);
        assertFalse(Files.exists(f), "nothing changed: no write");
        l.set(COERCION, 2);
        assertTrue(Files.exists(f), "a new mode is written");
        Files.delete(f);
        l.set(new PowerRef(PowerSource.TENSURA, ResourceLocation.parse("tensura:other")), 2);
        assertTrue(Files.exists(f), "a new power is written");
    }

    @Test
    void lastPowerOffSettingNeverWrites(@TempDir Path dir) throws IOException {
        LastPower off = last(dir, false);
        off.set(COERCION, 1);
        assertEquals(COERCION, off.get().orElseThrow().ref(), "kept in memory for the session");
        try (var files = Files.list(dir)) {
            assertEquals(0, files.count(), "nothing on disk");
        }
        Files.writeString(dir.resolve("wheel-last.json"), "{\"power\":\"" + COERCION.key() + "\",\"mode\":\"0\"}");
        assertTrue(last(dir, false).get().isEmpty(), "and nothing read");
    }

    @Test
    void aimConfirmSendsOnePayloadAndEnds() {
        List<String> sent = new ArrayList<>();
        AimMode aim = new AimMode((ref, mode, target) -> sent.add(ref.key() + "@" + mode + ">" + target));
        aim.start(COERCION, 1);
        assertTrue(aim.active());
        assertTrue(aim.confirm(42));
        assertFalse(aim.confirm(42), "a second click sends nothing");
        assertFalse(aim.active());
        assertEquals(List.of(COERCION.key() + "@1>42"), sent);
    }

    @Test
    void anAimEndsWhenTheHudIsHiddenOrItWaitedTooLong() {
        AimMode aim = new AimMode((ref, mode, target) -> { });
        assertEquals(AimMode.Cancel.NONE, aim.check(true, 5), "nothing is aimed: nothing to cancel");
        aim.start(COERCION, 0, 100);
        assertEquals(AimMode.Cancel.NONE, aim.check(false, 100 + AimMode.TIMEOUT_TICKS - 1));
        assertEquals(AimMode.Cancel.TIMED_OUT, aim.check(false, 100 + AimMode.TIMEOUT_TICKS));
        assertEquals(AimMode.Cancel.GUI_HIDDEN, aim.check(true, 101));
        assertEquals(AimMode.Cancel.GUI_HIDDEN, AimMode.cancelReason(true, 0, 1));
    }

    @Test
    void aimCancelSendsNothing() {
        List<String> sent = new ArrayList<>();
        AimMode aim = new AimMode((ref, mode, target) -> sent.add("x"));
        aim.start(COERCION, 0);
        assertTrue(aim.cancel());
        assertFalse(aim.cancel());
        assertFalse(aim.confirm(-1));
        assertTrue(sent.isEmpty());
        assertTrue(aim.aim().isEmpty());
    }

    // ---- the slot picker ----

    @Test
    void slotKeyOverAnEntryAssignsThatSlot() {
        Send s = SlotPicker.assign(WheelRules.SlotTarget.LOADOUT, 2, "supernaturalbloodlines:bite", 0, 0, 1);
        assertEquals(new SendLoadout(2, "supernaturalbloodlines:bite"), s);
        assertEquals(new SendNothing(), SlotPicker.assign(WheelRules.SlotTarget.LOADOUT, 6, "x:y", 0, 0, 1));
        assertEquals(new SendNothing(), SlotPicker.assign(WheelRules.SlotTarget.LOADOUT, -1, "x:y", 0, 0, 1));
    }

    @Test
    void tensuraAssignUsesTheNextPresetForTheRightColumn() {
        // preset 1 of 3 is active: slots 0-2 are preset 1 rows 0-2, slots 3-5 are preset 2 rows 0-2
        assertEquals(new SendTensura(1, 0, "tensura:coercion", 2), SlotPicker.assign(WheelRules.SlotTarget.TENSURA_PRESET, 0, "tensura:coercion", 2, 1, 3));
        assertEquals(new SendTensura(2, 1, "tensura:coercion", 2), SlotPicker.assign(WheelRules.SlotTarget.TENSURA_PRESET, 4, "tensura:coercion", 2, 1, 3));
        // the next preset wraps
        assertEquals(new SendTensura(0, 2, "tensura:coercion", 0), SlotPicker.assign(WheelRules.SlotTarget.TENSURA_PRESET, 5, "tensura:coercion", 0, 2, 3));
    }

    @Test
    void slotsForShowsWhatEachHoldsAndWhereItWouldGo() {
        List<SlotPicker.Slot> loadout = SlotPicker.slotsFor(WheelRules.SlotTarget.LOADOUT, List.of("a:b", "", "c:d"), 0, 1);
        assertEquals(6, loadout.size());
        assertEquals("a:b", loadout.get(0).holds());
        assertEquals("", loadout.get(1).holds());
        assertEquals("", loadout.get(5).holds(), "a list shorter than six leaves the rest empty");
        List<SlotPicker.Slot> tensura = SlotPicker.slotsFor(WheelRules.SlotTarget.TENSURA_PRESET, List.of(), 0, 2);
        assertEquals(0, tensura.get(0).preset());
        assertEquals(1, tensura.get(3).preset());
        assertEquals(2, tensura.get(5).row());
    }

    @Test
    void stoneAndGreatSageHaveNoSlotPicker() {
        assertTrue(SlotPicker.slotsFor(WheelRules.SlotTarget.NONE, List.of("a"), 0, 1).isEmpty());
        assertEquals(new SendNothing(), SlotPicker.assign(WheelRules.SlotTarget.NONE, 0, "infinity:soul", 0, 0, 1));
        assertEquals(WheelRules.SlotTarget.NONE, WheelRules.slotTarget(PowerSource.INFINITY, true));
        assertEquals(WheelRules.SlotTarget.NONE, WheelRules.slotTarget(PowerSource.GREAT_SAGE, true));
    }
}
