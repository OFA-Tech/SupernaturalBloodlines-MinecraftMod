package net.ofatech.supernaturalbloodlines.core.hud;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What one HUD slot shows. Built by the sources (Tensura, Vampirism) once per client tick and only read by the renderer.
 *
 * @param icon            texture of the icon, null for an empty slot
 * @param keyLabel        the slot's key as the player sees it (cut to 3 characters by the renderer)
 * @param cooldown        0 to 1, how much of the cooldown is left
 * @param cooldownSeconds whole seconds left, 0 when ready
 * @param active          a toggled skill, or a lasting action that is running
 * @param greyedReason    a lang key explaining why the slot cannot be used now, null when it can
 * @param name            the ability's name, for the Alt tooltip
 * @param detail          a second tooltip line (the mode, or what Bite does for the form), null for none
 */
public record SlotView(@Nullable ResourceLocation icon, String keyLabel, float cooldown, int cooldownSeconds, boolean active,
                       @Nullable String greyedReason, String name, @Nullable String detail) {
    public static final SlotView EMPTY = new SlotView(null, "", 0f, 0, false, null, "", null);

    public boolean isEmpty() {
        return icon == null;
    }

    public boolean greyed() {
        return greyedReason != null;
    }

    public SlotView withKey(String key) {
        return new SlotView(icon, key, cooldown, cooldownSeconds, active, greyedReason, name, detail);
    }
}
