package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;

/**
 * The mode of a power that is in no Tensura slot (Tensura keeps the mode in the preset slot, not on the skill), and of every Infinity stone. A skill in a slot uses the
 * slot's own mode, so the wheel, the slots and Tensura's screens agree whenever the skill is slotted. Saved with the player and kept on death.
 */
public final class WheelModes {
    private WheelModes() {
    }

    public static int get(ServerPlayer p, ResourceLocation skill) {
        return Math.max(0, p.getData(ModAttachments.WHEEL_MODES).getOrDefault(skill, 0));
    }

    /** Stores {@code mode} clamped to {@code [0, modeCount)}. @return the mode stored */
    public static int set(ServerPlayer p, ResourceLocation skill, int mode, int modeCount) {
        int clamped = Math.max(0, Math.min(mode, Math.max(1, modeCount) - 1));
        Map<ResourceLocation, Integer> copy = new HashMap<>(p.getData(ModAttachments.WHEEL_MODES));
        copy.put(skill, clamped);
        p.setData(ModAttachments.WHEEL_MODES, Map.copyOf(copy));
        return clamped;
    }
}
