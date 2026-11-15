package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.parts.hud.client.tensura.TensuraSlots;
import net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.ActionSlots;

/**
 * The six {@link SlotView}s of the current mode, rebuilt once per client tick (what the renderer of 13b Task 4 reads each frame). Each source is reached
 * through its own adapter class, only when its mod is installed and its mode is the current one.
 */
public final class HudSlots {
    private static final SlotView[] VIEWS = new SlotView[Loadout.SLOTS];

    static {
        Arrays.fill(VIEWS, SlotView.EMPTY);
    }

    private HudSlots() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    /** The views of the last tick; the array is reused, so do not keep it. */
    public static SlotView[] views() {
        return VIEWS;
    }

    private static void tick() {
        LocalPlayer p = Minecraft.getInstance().player;
        SlotMode mode = HudModeState.current().orElse(null);
        if (p == null || mode == null) {
            Arrays.fill(VIEWS, SlotView.EMPTY);
            return;
        }
        if (mode == SlotMode.TENSURA) {
            TensuraSlots.refresh(p, VIEWS);
        } else {
            ActionSlots.refresh(p, VIEWS);
        }
    }
}
