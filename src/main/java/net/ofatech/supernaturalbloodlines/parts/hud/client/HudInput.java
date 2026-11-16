package net.ofatech.supernaturalbloodlines.parts.hud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.ofatech.supernaturalbloodlines.parts.hud.net.TriggerActionPayload;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.parts.hud.client.tensura.TensuraInput;
import org.lwjgl.glfw.GLFW;

/**
 * The six slot keys and the mode key. A slot key is not read through {@code KeyMapping.isDown()}: NeoForge only fires a mapping whose modifier matches
 * the modifiers held, so a plain R mapping would be silent while Alt is held, and Alt + slot key is how Tensura changes a skill's mode. Instead the raw
 * key events are noted (so a tap shorter than a tick is not lost) and acted on in the next client tick, in order. A mapping bound with its own modifier
 * still needs it held; NONE means any modifiers (so a slot key also fires with Ctrl or Shift held: nothing else in the pack needs those on the same key). Only while a world is open with no screen. A key that is held when a screen opens is released at once,
 * and one still held when the screen closes needs a fresh press; a press in the tick a screen closed is ignored too (a slot key that is also the inventory key,
 * or another screen's close key, must not fire as the screen goes). With no connection (the world is closing) nothing is sent and the held state is dropped. Press and release go to the source of the mode the key was pressed in ({@link HudModeState}).
 */
public final class HudInput {
    private static final boolean[] DOWN = new boolean[Loadout.SLOTS];
    private static final boolean[] PENDING_PRESS = new boolean[Loadout.SLOTS];
    private static final boolean[] PENDING_RELEASE = new boolean[Loadout.SLOTS];
    /** The mode each held key was pressed in, so its release goes to the same source even if the mode was toggled meanwhile. */
    private static final SlotMode[] PRESSED_IN = new SlotMode[Loadout.SLOTS];
    /** A screen was open at the end of the last tick. */
    private static boolean screenWasOpen;

    private HudInput() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> reset());
        NeoForge.EVENT_BUS.addListener((InputEvent.Key e) -> note(InputConstants.Type.KEYSYM, e.getKey(), e.getAction()));
        NeoForge.EVENT_BUS.addListener((InputEvent.MouseButton.Pre e) -> note(InputConstants.Type.MOUSE, e.getButton(), e.getAction()));
    }

    private static void note(InputConstants.Type type, int code, int action) {
        if (action == GLFW.GLFW_REPEAT) {
            return;
        }
        for (int i = 0; i < DOWN.length; i++) {
            InputConstants.Key key = HudKeys.SLOTS[i].getKey();
            if (key.getType() != type || key.getValue() != code) {
                continue;
            }
            if (action == GLFW.GLFW_PRESS) {
                // a key held over a screen needs a fresh press: events only come for presses, and a screen swallows its own
                PENDING_PRESS[i] = Minecraft.getInstance().screen == null && !screenWasOpen && modifierHeld(HudKeys.SLOTS[i].getKeyModifier())
                    && slotAllowed(Screen.hasControlDown(), Screen.hasShiftDown());
            } else {
                PENDING_RELEASE[i] = true;
            }
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (mc.getConnection() == null) {
            reset();
            return;
        }
        boolean live = HudClientConfig.hudEnabled() && p != null && mc.screen == null && mc.getOverlay() == null;
        // the mode key's physical state is followed every tick, screen or not, so closing a screen while it is held is not a press
        KeyMapping mode = HudKeys.HUD_MODE;
        boolean capsDown = mode.getKeyModifier() == KeyModifier.NONE && physicallyDown(mc, mode);
        if (live) {
            while (HudKeys.HUD_MODE.consumeClick()) {
                HudModeState.toggle();
            }
            // a plain mapping does not fire while Alt, Ctrl or Shift is held (NeoForge looks up the held modifier's mappings): a Caps press with one held still switches
            if (capsDown && !capsWasDown && (Screen.hasAltDown() || Screen.hasControlDown() || Screen.hasShiftDown())) {
                HudModeState.toggle();
            }
        }
        capsWasDown = capsDown;
        for (int i = 0; i < DOWN.length; i++) {
            boolean press = PENDING_PRESS[i];
            boolean release = PENDING_RELEASE[i];
            PENDING_PRESS[i] = false;
            PENDING_RELEASE[i] = false;
            if (press && live) {
                if (DOWN[i]) {
                    end(i); // a press with the previous one never released (the release event was lost): close it first
                }
                DOWN[i] = true;
                PRESSED_IN[i] = HudModeState.current().orElse(null);
                press(p, i, PRESSED_IN[i]);
            }
            // the release of a key we pressed: its event, or the key is up or the keys went dead (a screen opened, the HUD was switched off)
            if (DOWN[i] && (release || !live || !physicallyDown(mc, HudKeys.SLOTS[i]))) {
                end(i);
            }
        }
        screenWasOpen = mc.screen != null;
    }


    /** Drops everything held and pending without sending a thing (the connection is gone or going). */
    public static void reset() {
        for (int i = 0; i < DOWN.length; i++) {
            DOWN[i] = false;
            PENDING_PRESS[i] = false;
            PENDING_RELEASE[i] = false;
            PRESSED_IN[i] = null;
        }
        screenWasOpen = false;
        if (net.neoforged.fml.ModList.get().isLoaded("tensura")) {
            TensuraInput.reset();
        }
    }

    private static void end(int slot) {
        DOWN[slot] = false;
        release(slot, PRESSED_IN[slot]);
        PRESSED_IN[slot] = null;
    }
    private static void press(LocalPlayer p, int slot, SlotMode mode) {
        if (mode == SlotMode.TENSURA) {
            TensuraInput.press(p, slot);
        } else if (mode != null && mode.offersBite()) {
            var id = ClientLoadout.loadout().get(slot);
            if (id.isPresent()) {
                if (Loadout.BITE.equals(id.get())) {
                    BiteView.pressed();
                }
                Entity looked = Minecraft.getInstance().crosshairPickEntity;
                HudSend.send(new TriggerActionPayload(slot, looked == null ? -1 : looked.getId(), false));
            }
        }
    }

    private static void release(int slot, SlotMode mode) {
        if (mode == SlotMode.TENSURA) {
            TensuraInput.release(slot);
        } else if (mode == SlotMode.SUPERNATURAL && ClientLoadout.loadout().get(slot).isPresent()) {
            HudSend.send(new TriggerActionPayload(slot, -1, true));
        }
    }

    /** A mapping bound with a modifier needs it held; NONE means "any modifiers", because Alt + slot key is Tensura's mode change. */
    /** A slot key fires with no modifier or with Alt (Alt + a slot key is Tensura's mode change); Ctrl and Shift are other shortcuts' (Ctrl + Z), so the slot ignores them. */
    static boolean slotAllowed(boolean ctrl, boolean shift) {
        return !ctrl && !shift;
    }

    private static boolean modifierHeld(net.neoforged.neoforge.client.settings.KeyModifier m) {
        return switch (m) {
            case CONTROL -> Screen.hasControlDown();
            case SHIFT -> Screen.hasShiftDown();
            case ALT -> Screen.hasAltDown();
            default -> true;
        };
    }

    private static boolean capsWasDown;

    private static boolean physicallyDown(Minecraft mc, KeyMapping km) {
        InputConstants.Key key = km.getKey();
        if (key.equals(InputConstants.UNKNOWN) || !modifierHeld(km.getKeyModifier())) {
            return false;
        }
        long window = mc.getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }
}
