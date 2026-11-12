package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Called from the Tensura sun mixins (mixin.tensura). Vampirism's own sun damage is not touched. */
public final class VampireSunRule {
    private VampireSunRule() {}

    public static boolean isFactionVampire(LivingEntity entity) {
        return entity instanceof Player p && VampirismAPI.factionPlayerHandler(p).isInFaction(VReference.VAMPIRE_FACTION);
    }
}
