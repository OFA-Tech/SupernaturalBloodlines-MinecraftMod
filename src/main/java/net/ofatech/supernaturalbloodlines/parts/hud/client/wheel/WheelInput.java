package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Effect;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine.Input;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelPalette;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudSend;
import net.ofatech.supernaturalbloodlines.parts.hud.net.ClientCatalogue;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelViewPayload;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import org.lwjgl.glfw.GLFW;

/**
 * The wheel key outside the wheel: G down starts the gesture, a poll each tick opens the wheel once it has been held {@code wheel_tap_ms}, a tap repeats the last power. Also
 * the aim: after an aimed power is chosen the next click fires it at the crosshair, a right click, a tap of G or any screen cancels it. Only while a world is open with the
 * HUD on and a server that has the wheel's channel (without one the key does nothing and says so once). Client only.
 */
public final class WheelInput {
    private static GestureMachine machine = GestureMachine.IDLE;
    private static boolean toldNoChannel;

    private WheelInput() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((InputEvent.Key e) -> onKey(e.getKey(), e.getScanCode(), e.getAction()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (!(e.getNewScreen() instanceof WheelScreen)) {
                step(Input.SCREEN_OPENED, false);
                WheelActions.aim().cancel();
            }
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> onInteraction(e));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre e) -> {
            if (net.ofatech.supernaturalbloodlines.core.wheel.WheelHud.hides(Minecraft.getInstance().screen instanceof WheelScreen, e.getName().toString())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> reset());
    }

    public static void registerLayers(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.CROSSHAIR, AimLayer.ID, new AimLayer());
    }

    static void screenClosed(GestureMachine from) {
        machine = GestureMachine.IDLE;
    }

    public static void reset() {
        machine = GestureMachine.IDLE;
        toldNoChannel = false;
        WheelActions.reset();
        WheelList.reset();
        ClientCatalogue.clear();
    }

    private static boolean live() {
        Minecraft mc = Minecraft.getInstance();
        return HudClientConfig.hudEnabled() && mc.player != null && mc.getConnection() != null && mc.screen == null && mc.getOverlay() == null;
    }

    private static void onKey(int key, int scan, int action) {
        if (action == GLFW.GLFW_REPEAT || !HudKeys.WHEEL.matches(key, scan)) {
            return;
        }
        if (action == GLFW.GLFW_PRESS) {
            if (!live()) {
                return;
            }
            if (!HudSend.canSend(WheelViewPayload.TYPE)) {
                if (!toldNoChannel) {
                    toldNoChannel = true;
                    Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("supernaturalbloodlines.wheel.hint.no_channel"), false);
                }
                return;
            }
            step(Input.G_DOWN, false);
        } else if (Minecraft.getInstance().screen != null) {
            return; // the wheel screen reads its own key release
        } else if (machine.pending() || machine.open()) {
            if (WheelActions.aim().active()) {
                // a tap of G while aiming cancels the aim; the press that started it is over
                WheelActions.aim().cancel();
                machine = GestureMachine.IDLE;
                return;
            }
            step(Input.G_UP, false);
        } else if (action == GLFW.GLFW_RELEASE && WheelActions.aim().active()) {
            WheelActions.aim().cancel();
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            reset();
            return;
        }
        if (WheelActions.aim().active() && (mc.screen != null || mc.player == null || !mc.player.isAlive())) {
            WheelActions.aim().cancel();
        }
        if (WheelActions.holding() && mc.screen == null && (mc.player == null || !mc.player.isAlive() || !mc.isWindowActive())) {
            WheelActions.release();
        }
        if (machine.pending()) {
            if (!live() || !physicallyDown(mc)) {
                machine = GestureMachine.IDLE;
                return;
            }
            step(Input.TICK, false);
        }
    }

    private static boolean physicallyDown(Minecraft mc) {
        InputConstants.Key key = HudKeys.WHEEL.getKey();
        return !key.equals(InputConstants.UNKNOWN) && InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
    }

    private static void step(Input in, boolean overPower) {
        GestureMachine.Step s = machine.step(in, Util.getMillis(), HudClientConfig.wheelTapMs(), overPower);
        machine = s.machine();
        if (s.effect() == Effect.OPEN) {
            Minecraft.getInstance().setScreen(new WheelScreen(machine));
        } else if (s.effect() == Effect.REPEAT_LAST) {
            repeatLast();
        }
    }

    /** The tap: use the last power again, in the mode it had. Nothing when there is none or it is not there any more. */
    private static void repeatLast() {
        var last = WheelActions.last().get();
        if (last.isEmpty()) {
            return;
        }
        for (WheelEntry e : WheelList.current()) {
            if (e.ref().equals(last.get().ref())) {
                if (e.kind() == net.ofatech.supernaturalbloodlines.core.wheel.PowerKind.HELD) {
                    WheelActions.use(e, last.get().mode());
                    WheelActions.release();
                } else {
                    WheelActions.use(e, last.get().mode());
                }
                return;
            }
        }
    }

    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered e) {
        if (!WheelActions.aim().active()) {
            return;
        }
        if (e.isAttack()) {
            var hit = Minecraft.getInstance().crosshairPickEntity;
            WheelActions.aim().confirm(hit == null ? -1 : hit.getId());
        } else if (e.isUseItem()) {
            WheelActions.aim().cancel();
        } else {
            return;
        }
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    /** The aim hint under the crosshair while an aimed power waits for its click. */
    static final class AimLayer implements net.minecraft.client.gui.LayeredDraw.Layer {
        static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "wheel_aim");

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.DeltaTracker delta) {
            WheelEntry e = WheelActions.aimEntry();
            if (!WheelActions.aim().active() || e == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            HudKit kit = HudKit.of(HudStyle.current());
            WheelPalette pal = WheelPalette.of(switch (HudStyle.current()) {
                case GRIMOIRE -> net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look.GRIMOIRE;
                case BLOOD_MOON -> net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look.BLOOD_MOON;
                case TENSURA -> net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look.TENSURA;
            });
            WheelRenderer.aimHint(g, mc.font, kit, pal, e.icon(), g.guiWidth(), g.guiHeight());
        }
    }
}
