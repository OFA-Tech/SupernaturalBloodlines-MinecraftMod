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

    private static final SlotView[] OTHER = new SlotView[Loadout.SLOTS];
    private static long otherAt = Long.MIN_VALUE;

    /** The slots {@code other} mode would show, for the panel that appears with Alt; built at most once a tick. */
    public static SlotView[] preview(SlotMode other) {
        LocalPlayer p = Minecraft.getInstance().player;
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        if (p == null) {
            Arrays.fill(OTHER, SlotView.EMPTY);
        } else if (now != otherAt) {
            otherAt = now;
            if (other == SlotMode.TENSURA) {
                TensuraSlots.refresh(p, OTHER);
            } else {
                ActionSlots.refresh(p, OTHER);
            }
        }
        return OTHER;
    }

    private static void tick() {
        LiveHud.tick(Minecraft.getInstance());
        LocalPlayer p = Minecraft.getInstance().player;
        HudStanding.tick(p);
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
