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
 * @param mastery         how far a Tensura skill has got, null for a slot without mastery (Supernatural mode, an empty slot)
 * @param description     a short description of the skill for the Alt label, null for none
 */
public record SlotView(@Nullable ResourceLocation icon, String keyLabel, float cooldown, int cooldownSeconds, boolean active,
                       @Nullable String greyedReason, String name, @Nullable String detail, @Nullable Mastery mastery, @Nullable String description) {
    public static final SlotView EMPTY = new SlotView(null, "", 0f, 0, false, null, "", null);

    public SlotView(@Nullable ResourceLocation icon, String keyLabel, float cooldown, int cooldownSeconds, boolean active,
                    @Nullable String greyedReason, String name, @Nullable String detail) {
        this(icon, keyLabel, cooldown, cooldownSeconds, active, greyedReason, name, detail, null, null);
    }

    public SlotView(@Nullable ResourceLocation icon, String keyLabel, float cooldown, int cooldownSeconds, boolean active,
                    @Nullable String greyedReason, String name, @Nullable String detail, @Nullable Mastery mastery) {
        this(icon, keyLabel, cooldown, cooldownSeconds, active, greyedReason, name, detail, mastery, null);
    }

    public boolean isEmpty() {
        return icon == null;
    }

    public boolean greyed() {
        return greyedReason != null;
    }

    public SlotView withKey(String key) {
        return new SlotView(icon, key, cooldown, cooldownSeconds, active, greyedReason, name, detail, mastery, description);
    }

    public SlotView withMastery(@Nullable Mastery m) {
        return new SlotView(icon, keyLabel, cooldown, cooldownSeconds, active, greyedReason, name, detail, m, description);
    }

    public SlotView withDescription(@Nullable String d) {
        return new SlotView(icon, keyLabel, cooldown, cooldownSeconds, active, greyedReason, name, detail, mastery, d);
    }
}
