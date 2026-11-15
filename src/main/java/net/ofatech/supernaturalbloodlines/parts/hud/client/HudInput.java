package net.ofatech.supernaturalbloodlines.parts.hud.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.hud.net.TriggerActionPayload;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.parts.hud.client.tensura.TensuraInput;
import org.lwjgl.glfw.GLFW;

/**
 * The six slot keys and the mode key. A slot key is watched by its physical state, not through {@code KeyMapping.isDown()}: NeoForge only fires a
 * mapping whose modifier matches the modifiers held, so a plain R mapping would be silent while Alt is held, and Alt + slot key is how Tensura changes a
 * skill's mode. Only while a world is open with no screen. Press and release go to the source of the current mode ({@link HudModeState}).
 */
public final class HudInput {
    private static final boolean[] DOWN = new boolean[Loadout.SLOTS];
    /** The mode each held key was pressed in, so its release goes to the same source even if the mode was toggled meanwhile. */
    private static final SlotMode[] PRESSED_IN = new SlotMode[Loadout.SLOTS];

    private HudInput() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean live = HudClientConfig.hudEnabled() && p != null && mc.screen == null && mc.getOverlay() == null;
        if (live) {
            while (HudKeys.HUD_MODE.consumeClick()) {
                HudModeState.toggle();
            }
        }
        for (int i = 0; i < DOWN.length; i++) {
            boolean now = live && physicallyDown(mc, HudKeys.SLOTS[i]);
            if (now && !DOWN[i]) {
                DOWN[i] = true;
                PRESSED_IN[i] = HudModeState.current().orElse(null);
                press(p, i, PRESSED_IN[i]);
            } else if (!now && DOWN[i]) {
                DOWN[i] = false;
                release(i, PRESSED_IN[i]);
                PRESSED_IN[i] = null;
            }
        }
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
                PacketDistributor.sendToServer(new TriggerActionPayload(slot, looked == null ? -1 : looked.getId(), false));
            }
        }
    }

    private static void release(int slot, SlotMode mode) {
        if (mode == SlotMode.TENSURA) {
            TensuraInput.release(slot);
        } else if (mode == SlotMode.SUPERNATURAL && ClientLoadout.loadout().get(slot).isPresent()) {
            PacketDistributor.sendToServer(new TriggerActionPayload(slot, -1, true));
        }
    }

    private static boolean physicallyDown(Minecraft mc, KeyMapping km) {
        InputConstants.Key key = km.getKey();
        if (key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        long window = mc.getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }
}
