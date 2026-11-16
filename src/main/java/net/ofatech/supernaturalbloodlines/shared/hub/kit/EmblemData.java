package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;

/**
 * What a theme draws its emblem from: the ladder's progress (0 to 1), its rungs in order (the Blood Moon's gems, the Tensura hexes), the
 * entity to show live, and the drag the screen owns, so a drag on the emblem turns whichever model any theme draws.
 */
public record EmblemData(float progress, List<Rung> rungs, LivingEntity entity, ModelDrag drag) {
    public record Rung(String name, RungStatus status) {
    }

    public EmblemData {
        progress = Math.max(0f, Math.min(1f, progress));
        rungs = List.copyOf(rungs);
    }
}
