package net.ofatech.supernaturalbloodlines.core.hud;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What a Tensura-mode slot key sends, as data, so the forwarding table is tested without the game. It does what Tensura's own
 * {@code TensuraKeybinds.onAbilityPressed} does, in the same order (read with javap):
 * <ul>
 * <li>a mode key (Next or Previous ability mode) is down: change the skill's mode ({@code changeModePacket(slot, presetId, alternative)}, with
 * {@code presetId} -1 for the active preset and the next preset's index for the right column; the server applies it to that preset);</li>
 * <li>otherwise activate: {@code skillActivationPacket(skillId, keyNumber, mode)};</li>
 * <li>release: {@code skillReleasePacket(skillId, keyNumber, mode)} (ManasCore counts the held ticks itself).</li>
 * </ul>
 * The key number is the slot's index inside its preset (0 to 2) for both columns: Tensura checks {@code isAbilityInActivePreset(keyNumber, skill, mode)},
 * and the HUD part's next-preset mixin makes that true for preset N+1. Slots 0-2 are the left column, 3-5 the right one.
 */
public final class InputPlan {
    public enum Kind { NONE, ACTIVATE, CHANGE_MODE, RELEASE }

    /**
     * @param keyNumber the slot index inside the preset (0 to 2)
     * @param preset    for CHANGE_MODE, the preset whose slot changes (-1 means the active one, as Tensura's own key sends); -1 otherwise
     */
    public record Step(Kind kind, @Nullable ResourceLocation skill, int keyNumber, int mode, int preset, boolean backwards) {
        public static final Step NONE = new Step(Kind.NONE, null, 0, 0, -1, false);
    }

    private static final int COLUMN = 3;

    private InputPlan() {
    }

    public static boolean rightColumn(int slot) {
        return slot >= COLUMN;
    }

    public static int row(int slot) {
        return slot % COLUMN;
    }

    public static Step forPress(int slot, @Nullable ResourceLocation skill, int mode, boolean nextModeDown, boolean previousModeDown, int activePreset, int presetCount) {
        if (slot < 0 || slot >= 2 * COLUMN || skill == null) {
            return Step.NONE;
        }
        int row = row(slot);
        if (nextModeDown || previousModeDown) {
            int preset = rightColumn(slot) ? PresetPair.right(activePreset, presetCount) : -1;
            return new Step(Kind.CHANGE_MODE, null, row, 0, preset, !nextModeDown);
        }
        return new Step(Kind.ACTIVATE, skill, row, mode, -1, false);
    }

    /** @param skill the skill that was pressed on this slot (not whatever is in the slot now), null when nothing was */
    public static Step forRelease(int slot, @Nullable ResourceLocation skill, int mode) {
        if (slot < 0 || slot >= 2 * COLUMN || skill == null) {
            return Step.NONE;
        }
        return new Step(Kind.RELEASE, skill, row(slot), mode, -1, false);
    }
}
