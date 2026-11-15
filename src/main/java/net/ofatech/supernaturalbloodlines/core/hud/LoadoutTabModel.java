package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The selection logic of the hub's Loadout tab, with no screen in it: pick an action and then a slot, or a slot and then an action; the second pick
 * assigns. A slot picked twice, or an action picked twice, is unpicked. Clearing a slot is its own gesture.
 */
public final class LoadoutTabModel {
    /** @param id null clears the slot */
    public record Assignment(int slot, @Nullable ResourceLocation id) {
    }

    @Nullable
    private ResourceLocation action;
    @Nullable
    private Integer slot;

    @Nullable
    public ResourceLocation pickedAction() {
        return action;
    }

    @Nullable
    public Integer pickedSlot() {
        return slot;
    }

    public Optional<Assignment> pickAction(ResourceLocation id) {
        if (slot != null) {
            return assign(slot, id);
        }
        action = id.equals(action) ? null : id;
        return Optional.empty();
    }

    public Optional<Assignment> pickSlot(int index) {
        if (!Loadout.validSlot(index)) {
            return Optional.empty();
        }
        if (action != null) {
            return assign(index, action);
        }
        slot = slot != null && slot == index ? null : Integer.valueOf(index);
        return Optional.empty();
    }

    public Optional<Assignment> clearSlot(int index) {
        if (!Loadout.validSlot(index)) {
            return Optional.empty();
        }
        reset();
        return Optional.of(new Assignment(index, null));
    }

    public void reset() {
        action = null;
        slot = null;
    }

    private Optional<Assignment> assign(int index, ResourceLocation id) {
        reset();
        return Optional.of(new Assignment(index, id));
    }
}
