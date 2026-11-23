package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.AimMode;
import net.ofatech.supernaturalbloodlines.core.wheel.HeldPowers;
import net.ofatech.supernaturalbloodlines.core.wheel.LastPower;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelRules;
import net.ofatech.supernaturalbloodlines.parts.hud.client.ClientLoadout;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudSend;
import net.ofatech.supernaturalbloodlines.parts.hud.net.SetLoadoutPayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.SetPowerModePayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.UsePowerPayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelViewPayload;

/**
 * Everything the wheel sends: a use (and the release of a held one, always, when the wheel closes, loses focus or the player dies), a mode change, a slot assignment, the
 * open and close notice, and the aim. Great Sage's powers are run on the client. The server re-checks everything it is sent. Client only.
 */
public final class WheelActions {
    private static final HeldPowers HELD = new HeldPowers();
    private static LastPower last;
    private static final AimMode AIM = new AimMode((ref, mode, target) -> sendUse(ref, mode, target, false));
    private static WheelEntry aimEntry;

    private WheelActions() {
    }

    public static AimMode aim() {
        return AIM;
    }

    static void devAimEntry(WheelEntry e) {
        aimEntry = e;
    }

    public static WheelEntry aimEntry() {
        return aimEntry;
    }

    public static LastPower last() {
        if (last == null) {
            Path file = FMLPaths.CONFIGDIR.get().resolve(SupernaturalBloodlines.MODID + "-wheel-last.json");
            last = new LastPower(file, System::currentTimeMillis, m -> SupernaturalBloodlines.LOGGER.warn(m), HudClientConfig::wheelRememberLast);
        }
        return last;
    }

    private static int crosshairTarget() {
        var e = Minecraft.getInstance().crosshairPickEntity;
        return e == null ? -1 : e.getId();
    }

    private static void sendUse(PowerRef ref, int mode, int target, boolean release) {
        HudSend.send(new UsePowerPayload(ref.key(), mode, target, release));
    }

    private static final net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState VIEW = new net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState();

    /** Tells the server whether a screen that lists the powers is open (it only sends the catalogue to a viewer): the wheel and the hub share one request. */
    public static void view(net.ofatech.supernaturalbloodlines.core.wheel.WheelViewState.Viewer viewer, boolean open) {
        VIEW.set(viewer, open).ifPresent(on -> HudSend.send(new WheelViewPayload(on)));
    }

    /**
     * Uses a power: an instant or toggle at once, a held one until {@link #release}, an aimed one by starting the aim (the caller closes the wheel). Returns false when it did
     * not start (nothing is sent for a power that is locked: the panel says why).
     */
    public static boolean use(WheelEntry e, int mode) {
        if (e.state().locked()) {
            return false;
        }
        last().set(e.ref(), mode);
        if (e.kind() == PowerKind.AIMED) {
            AIM.start(e.ref(), mode);
            aimEntry = e;
            return true;
        }
        if (e.ref().source() == PowerSource.GREAT_SAGE) {
            if (e.kind() == PowerKind.HELD) {
                pressHeldGreatSage(e);
                return heldScan;
            }
            return GreatSageProvider.run(e.ref().id(), false);
        }
        sendUse(e.ref(), mode, e.kind() == PowerKind.INSTANT ? crosshairTarget() : -1, false);
        if (e.kind() == PowerKind.HELD) {
            HELD.press(e.ref(), mode);
        }
        return true;
    }

    public static boolean holding() {
        return HELD.any() || heldScan;
    }

    private static boolean heldScan;

    /** Starts the held Great Sage scan (its release is {@link #release}). */
    public static void pressHeldGreatSage(WheelEntry e) {
        if (GreatSageProvider.run(e.ref().id(), false)) {
            heldScan = true;
        }
    }

    /** Ends everything held: the server's held powers and Great Sage's scan. Always sent, so nothing is left running. */
    public static void release() {
        for (HeldPowers.Held h : HELD.releaseAll()) {
            sendUse(h.ref(), h.mode(), -1, true);
        }
        if (heldScan) {
            heldScan = false;
            GreatSageProvider.run(GreatSageProvider.SCAN, true);
        }
    }

    public static void changeMode(WheelEntry e, int delta) {
        if (e.ref().source() == PowerSource.GREAT_SAGE || e.modeNames().size() < 2) {
            return;
        }
        HudSend.send(new SetPowerModePayload(e.ref().key(), delta < 0 ? -1 : 1));
    }

    /** What the six slots of the target hold, as entry ids (null for empty). */
    public static List<ResourceLocation> holds(WheelRules.SlotTarget target) {
        if (DevWheel.devHolds() != null) {
            return DevWheel.devHolds();
        }
        List<ResourceLocation> out = new ArrayList<>();
        LocalPlayer p = Minecraft.getInstance().player;
        if (target == WheelRules.SlotTarget.TENSURA_PRESET && p != null && ModList.get().isLoaded("tensura")) {
            out.addAll(TensuraWheelLink.holds(p));
        } else if (target == WheelRules.SlotTarget.LOADOUT) {
            for (var id : ClientLoadout.loadout().slots()) {
                out.add(id.orElse(null));
            }
        }
        while (out.size() < SlotPicker.SLOTS) {
            out.add(null);
        }
        return out;
    }

    public static int activePreset() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && ModList.get().isLoaded("tensura") ? TensuraWheelLink.activePreset(p) : 0;
    }

    public static int presetCount() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && ModList.get().isLoaded("tensura") ? TensuraWheelLink.presetCount(p) : 1;
    }

    public static WheelRules.SlotTarget target(WheelEntry e) {
        return WheelRules.slotTarget(e.ref().source(), e.slottable());
    }

    /** Puts the power in slot {@code index} of its target and switches the HUD to the mode that slot belongs to. */
    public static void assign(WheelEntry e, int index) {
        WheelRules.SlotTarget target = target(e);
        LocalPlayer p = Minecraft.getInstance().player;
        SlotPicker.Send send = SlotPicker.assign(target, index, e.ref().id().toString(), e.state().mode(), activePreset(), presetCount());
        if (send instanceof SlotPicker.SendLoadout l) {
            HudSend.send(new SetLoadoutPayload(l.slot(), l.actionId()));
            HudClientConfig.rememberMode(HudClientConfig.Mode.SUPERNATURAL);
        } else if (send instanceof SlotPicker.SendTensura t && p != null && ModList.get().isLoaded("tensura")) {
            TensuraWheelLink.assign(p, index, e.ref().id(), t.mode());
            HudClientConfig.rememberMode(HudClientConfig.Mode.TENSURA);
        }
    }

    /** The key of the slot a power is in now, for the panel's "Slot" row; empty when it is in none. */
    public static String slotOf(WheelEntry e) {
        WheelRules.SlotTarget target = target(e);
        if (target == WheelRules.SlotTarget.NONE) {
            return "";
        }
        List<ResourceLocation> holds = holds(target);
        for (int i = 0; i < holds.size(); i++) {
            if (e.ref().id().equals(holds.get(i))) {
                return net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys.SLOTS[i].getTranslatedKeyMessage().getString();
            }
        }
        return "";
    }

    /** A new world, a lost connection or the player's death: nothing held or aimed carries over. */
    public static void reset() {
        VIEW.clear();
        HELD.releaseAll();
        heldScan = false;
        AIM.cancel();
        aimEntry = null;
    }
}
